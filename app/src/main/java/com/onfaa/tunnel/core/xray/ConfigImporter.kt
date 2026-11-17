package com.onfaa.tunnel.core.xray

import com.onfaa.tunnel.domain.model.InjectMode
import com.onfaa.tunnel.domain.model.TunnelProfile
import com.onfaa.tunnel.domain.model.TunnelProtocol
import java.net.URLDecoder
import java.security.MessageDigest
import java.util.Base64
import kotlinx.coroutines.CancellationException
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class ConfigException(message: String) : Exception(message)

/**
 * Real Xray outbound import (VLESS, VMess, Trojan, Shadowsocks, SOCKS5).
 *
 * Accepted inputs:
 *  - share link vless:// / vmess:// / trojan:// / ss:// / socks:// (converted
 *    by libXray convertShareLinksToXrayJson, then validated by the real engine),
 *  - Xray JSON client configuration (or a single supported outbound object).
 *
 * Every accepted profile is embedded in the full TUN configuration and checked
 * with testXray (constructs handlers, never starts the TUN) before it is
 * persisted. Nothing is accepted silently: unknown protocols, several
 * candidate outbounds, missing credentials and engine refusals all produce a
 * readable error. Raw inputs are never logged.
 */
object ConfigImporter {

    private val SUPPORTED_LINK_SCHEMES = setOf("vless", "vmess", "trojan", "ss", "socks")
    private val SUPPORTED_OUTBOUND_PROTOCOLS =
        setOf("vless", "vmess", "trojan", "shadowsocks", "socks")

    suspend fun importLink(rawLink: String, name: String? = null): Result<TunnelProfile> =
        runImport { importLinkOrThrow(rawLink, name) }

    suspend fun importJson(rawJson: String, name: String? = null): Result<TunnelProfile> =
        runImport { importJsonOrThrow(rawJson, name) }

    /**
     * Auto-detects the input kind (JSON object/array vs share link) so the
     * clipboard or a picked file can be imported without asking the user.
     */
    suspend fun importText(raw: String, name: String? = null): Result<TunnelProfile> =
        runImport {
            val text = raw.trim().removePrefix("\uFEFF").trim()
            if (text.isEmpty()) throw ConfigException("Contenu vide")
            if (text.startsWith("{") || text.startsWith("[")) {
                importJsonOrThrow(text, name)
            } else {
                importLinkOrThrow(text, name)
            }
        }

    private suspend fun runImport(block: suspend () -> TunnelProfile): Result<TunnelProfile> = try {
        Result.success(block())
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (failure: Throwable) {
        Result.failure(failure)
    }

    // ---------------------------------------------------------------- links

    private suspend fun importLinkOrThrow(rawLink: String, name: String?): TunnelProfile {
        val link = rawLink.trim()
        if (link.isEmpty()) throw ConfigException("Lien vide")
        val scheme = link.substringBefore("://", "").lowercase()
        if (scheme !in SUPPORTED_LINK_SCHEMES) {
            throw ConfigException(
                "Protocole non pris en charge : utilisez un lien vless://, vmess://, " +
                    "trojan://, ss:// ou socks://"
            )
        }
        if (scheme == "vmess" && !link.substringAfter("://").contains('@')) {
            // Historic v2rayN share format: vmess://base64(JSON). libXray only
            // converts the URI form, so decode and rebuild the outbound here,
            // then reuse the exact same JSON import path (structure checks +
            // real engine validation) as a pasted configuration.
            val (legacyName, json) = decodeLegacyVmessJson(link)
            return importJsonOrThrow(json, name ?: legacyName)
        }
        requireEngine()

        val response = XrayBridge.convertShareLink(link)
        if (!response.success) throw ConfigException(conversionError(response.error))
        val data = response.data ?: throw ConfigException("Réponse de conversion vide")
        val outbounds = data.optJSONArray("outbounds")
            ?: throw ConfigException("Aucune sortie Xray produite")
        if (outbounds.length() != 1) {
            throw ConfigException(
                "Le lien doit décrire exactement une configuration (${outbounds.length()} trouvées)"
            )
        }

        val profile = profileFromOutbound(
            outbound = outbounds.getJSONObject(0),
            preferredName = name,
            fallbackName = displayName(link, name)
        )
        validateWithEngine(profile)
        return profile
    }

    // ----------------------------------------------------------------- json

    private suspend fun importJsonOrThrow(rawJson: String, name: String?): TunnelProfile {
        val text = rawJson.trim().removePrefix("\uFEFF").trim()
        if (text.isEmpty()) throw ConfigException("JSON vide")

        val root = try {
            JSONObject(text)
        } catch (failure: JSONException) {
            throw ConfigException("JSON invalide : ${failure.message ?: "syntaxe inconnue"}")
        }

        val outbound = extractSingleOutbound(root)
            ?: throw ConfigException(
                "Aucun serveur compatible dans ce JSON : ajoutez une sortie " +
                    "\"protocol\" vless, vmess, trojan, shadowsocks ou socks " +
                    "avec son adresse, son port et ses identifiants"
            )
        val normalized = normalizeVnext(outbound)

        // Validate structure before touching the native engine so invalid input
        // gets a precise message even when libXray is unavailable.
        val preview = profileFromOutbound(normalized, preferredName = name, fallbackName = null)
        requireEngine()
        validateWithEngine(preview)
        return preview
    }

    private fun extractSingleOutbound(root: JSONObject): JSONObject? {
        val outbounds: JSONArray? = root.optJSONArray("outbounds")
        if (outbounds != null) {
            val supported = ArrayList<JSONObject>()
            for (index in 0 until outbounds.length()) {
                val candidate = outbounds.optJSONObject(index) ?: continue
                if (candidate.optString("protocol").lowercase() in SUPPORTED_OUTBOUND_PROTOCOLS) {
                    supported.add(candidate)
                }
            }
            return when (supported.size) {
                0 -> null
                1 -> supported[0]
                else -> throw ConfigException(
                    "Le JSON contient ${supported.size} sorties compatibles ; " +
                        "importez une configuration avec une seule sortie"
                )
            }
        }
        if (root.optString("protocol").lowercase() in SUPPORTED_OUTBOUND_PROTOCOLS) return root
        return null
    }

    /**
     * v2ray-style configurations wrap the server in
     * settings.vnext[0] with a users array. Xray 26 also accepts the flat
     * ServerObject form, so a single vnext entry is flattened here.
     */
    private fun normalizeVnext(outbound: JSONObject): JSONObject {
        val settings = outbound.optJSONObject("settings") ?: return outbound
        val vnext = settings.optJSONArray("vnext") ?: return outbound
        if (vnext.length() != 1) return outbound
        val server = vnext.optJSONObject(0) ?: return outbound
        val users = server.optJSONArray("users")
        if (users == null || users.length() != 1) return outbound
        val user = users.optJSONObject(0) ?: return outbound

        val flat = JSONObject()
        flat.put("address", server.optString("address"))
        flat.put("port", server.optInt("port"))
        val keys = user.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            flat.put(key, user.opt(key))
        }
        outbound.put("settings", flat)
        return outbound
    }

    // ----------------------------------------------------------------- json

    /**
     * Historic v2rayN share format: vmess://base64(JSON) whose fields are
     * add, port, id, aid, scy, net, type, host, path, tls, sni, alpn, fp and
     * ps. The payload is decoded, rebuilt as a single vmess outbound object
     * and returned with the optional [ps] display name; the standard JSON
     * import path then performs structure checks and real engine validation.
     */
    private fun decodeLegacyVmessJson(rawLink: String): Pair<String?, String> {
        val payload = rawLink.substringAfter("://", "").trim().ifEmpty {
            throw ConfigException("Lien VMess vide")
        }

        // Share links in the wild mix standard and URL-safe alphabets, drop
        // the padding, URL-encode the padding or append a fragment label.
        val base64 = payload.substringBefore('#')
            .replace("%3D", "=", ignoreCase = true)
            .replace('-', '+')
            .replace('_', '/')
            .replace(" ", "")
        val padded = base64 + "=".repeat((4 - base64.length % 4) % 4)
        val bytes = try {
            Base64.getMimeDecoder().decode(padded)
        } catch (failure: IllegalArgumentException) {
            throw ConfigException(
                "Lien VMess illisible : le bloc base64 du format historique est corrompu"
            )
        }
        val config = try {
            JSONObject(String(bytes, Charsets.UTF_8))
        } catch (failure: JSONException) {
            throw ConfigException(
                "Le décodage VMess base64 n'a pas produit de JSON exploitable"
            )
        }

        fun field(vararg keys: String): String {
            for (key in keys) {
                val value = config.optString(key, "").trim()
                if (value.isNotEmpty()) return value
            }
            return ""
        }

        val address = field("add", "address")
        if (address.isEmpty()) throw ConfigException("Adresse du serveur manquante dans le lien VMess")
        val uuid = field("id", "uuid")
        if (uuid.isEmpty()) throw ConfigException("UUID/identifiant manquant dans le lien VMess")
        val port = field("port", "p").toIntOrNull()
        if (port == null || port !in 1..65535) {
            throw ConfigException("Port du serveur invalide dans le lien VMess")
        }

        val network = field("net", "network").lowercase().ifBlank { "tcp" }
        val headerType = field("type").lowercase()
        val host = field("host")
        val path = field("path")
        val sni = field("sni", "peer")
        val alpn = field("alpn")
        val fingerprint = field("fp", "fingerprint")
        val tlsEnabled = field("tls").equals("tls", ignoreCase = true) ||
            field("security").equals("tls", ignoreCase = true)

        val user = JSONObject()
            .put("id", uuid)
            .put("alterId", field("aid", "alterId").toIntOrNull() ?: 0)
            // scy is the cipher; a top-level "security" field means the TLS
            // layer in some clients and must never become the cipher.
            .put("security", field("scy").ifBlank { "auto" })

        val stream = JSONObject()
            .put("network", network)
            .put("security", if (tlsEnabled) "tls" else "none")
        if (tlsEnabled) {
            val tlsSettings = JSONObject()
                .put("serverName", if (sni.isNotEmpty()) sni else if (host.isNotEmpty()) host else address)
            if (alpn.isNotEmpty()) {
                tlsSettings.put("alpn", JSONArray(alpn.split(',').map { it.trim() }.filter { it.isNotEmpty() }))
            }
            if (fingerprint.isNotEmpty()) tlsSettings.put("fingerprint", fingerprint)
            stream.put("tlsSettings", tlsSettings)
        }
        when (network) {
            "ws" -> {
                val wsSettings = JSONObject().put("path", path.ifBlank { "/" })
                if (host.isNotEmpty()) {
                    wsSettings.put("headers", JSONObject().put("Host", host))
                }
                stream.put("wsSettings", wsSettings)
            }
            "http", "h2" -> {
                val httpSettings = JSONObject().put("path", path.ifBlank { "/" })
                if (host.isNotEmpty()) httpSettings.put("host", JSONArray(listOf(host)))
                stream.put("httpSettings", httpSettings)
            }
            "grpc" -> stream.put("grpcSettings", JSONObject().put("serviceName", path))
            "kcp" -> stream.put(
                "kcpSettings",
                JSONObject().put("mtu", 1350).put(
                    "headerType",
                    headerType.ifBlank { "none" }
                )
            )
            "tcp" -> if (headerType == "http") {
                stream.put(
                    "tcpSettings",
                    JSONObject().put(
                        "header",
                        JSONObject()
                            .put("type", "http")
                            .put(
                                "request",
                                JSONObject()
                                    .put("version", "1.1")
                                    .put("method", "GET")
                                    .put("path", JSONArray(listOf(path.ifBlank { "/" })))
                                    .put(
                                        "headers",
                                        JSONObject()
                                            .put(
                                                "Host",
                                                JSONArray(listOf(host.ifBlank { address }))
                                            )
                                            .put("User-Agent", JSONArray(listOf("golang")))
                                            .put(
                                                "Accept-Encoding",
                                                JSONArray(listOf("gzip, deflate"))
                                            )
                                    )
                            )
                    )
                )
            }
        }

        val outbound = JSONObject()
            .put("protocol", "vmess")
            .put("settings", JSONObject().put("vnext", JSONArray().put(JSONObject()
                .put("address", address)
                .put("port", port)
                .put("users", JSONArray().put(user))
            )))
            .put("streamSettings", stream)

        return field("ps", "remark", "name").ifBlank { null } to outbound.toString()
    }

    // -------------------------------------------------------------- profile

    private fun profileFromOutbound(
        outbound: JSONObject,
        preferredName: String?,
        fallbackName: String?
    ): TunnelProfile {
        val protocolName = outbound.optString("protocol").lowercase()
        val protocol = when (protocolName) {
            "vless" -> TunnelProtocol.VLESS
            "vmess" -> TunnelProtocol.VMESS
            "trojan" -> TunnelProtocol.TROJAN
            "shadowsocks" -> TunnelProtocol.SHADOWSOCKS
            "socks" -> TunnelProtocol.SOCKS
            "" -> throw ConfigException("Protocole absent du serveur")
            else -> throw ConfigException("Protocole non pris en charge : $protocolName")
        }

        val settings = outbound.optJSONObject("settings")
            ?: throw ConfigException("Paramètres du serveur absents")
        val server = singleServer(settings)
        requireCredentials(protocol, server)

        val address = server.optString("address").trim()
        if (address.isEmpty()) throw ConfigException("Adresse du serveur manquante")
        val port = server.optInt("port", 0)
        if (port !in 1..65535) throw ConfigException("Port du serveur invalide")

        val security = outbound.optJSONObject("streamSettings")
            ?.optString("security")
            .orEmpty()
            .ifBlank { "none" }

        val outboundJson = outbound.toString()
        val explicitName = preferredName?.trim()?.takeIf { it.isNotEmpty() }
        val tag = outbound.optString("tag").trim().takeIf {
            it.isNotEmpty() && !it.equals("proxy", ignoreCase = true)
        }

        return TunnelProfile(
            id = sha256(outboundJson).take(16),
            displayName = explicitName ?: tag ?: fallbackName ?: "${protocol.label} $address",
            protocol = protocol,
            endpoint = "$address:$port",
            security = security,
            outboundJson = outboundJson,
            injectMode = when (security.lowercase()) {
                "tls", "reality" -> InjectMode.PROXY_SNI
                else -> InjectMode.PROXY
            },
            sni = outbound.optJSONObject("streamSettings")?.optString("sni")?.takeIf { it.isNotBlank() }
        )
    }

    /**
     * v2ray-style Shadowsocks/SOCKS JSON wraps the endpoint in
     * settings.servers[0]. Only a single server can be persisted per profile.
     */
    private fun singleServer(settings: JSONObject): JSONObject {
        val servers = settings.optJSONArray("servers") ?: return settings
        if (servers.length() != 1) {
            throw ConfigException(
                "Une seule entrée « servers » est acceptée (${servers.length()} trouvées)"
            )
        }
        return servers.optJSONObject(0)
            ?: throw ConfigException("Entrée « servers » invalide")
    }

    private fun requireCredentials(protocol: TunnelProtocol, server: JSONObject) {
        when (protocol) {
            TunnelProtocol.VLESS, TunnelProtocol.VMESS ->
                if (server.optString("id").isBlank()) {
                    throw ConfigException("UUID/identifiant du serveur manquant")
                }
            TunnelProtocol.TROJAN ->
                if (server.optString("password").isBlank()) {
                    throw ConfigException("Mot de passe Trojan manquant")
                }
            TunnelProtocol.SHADOWSOCKS -> {
                if (server.optString("method").isBlank()) {
                    throw ConfigException("Méthode de chiffrement Shadowsocks manquante")
                }
                if (server.optString("password").isBlank()) {
                    throw ConfigException("Mot de passe Shadowsocks manquant")
                }
            }
            TunnelProtocol.SOCKS -> Unit
        }
    }

    private suspend fun validateWithEngine(profile: TunnelProfile) {
        val validation = XrayBridge.testXray(XrayConfigBuilder.build(profile, tunFd = 0))
        if (!validation.success) {
            throw ConfigException(
                "Configuration refusée par le moteur Xray : " +
                    XrayErrorSanitizer.sanitize(validation.error)
            )
        }
    }

    private fun requireEngine() {
        if (!XrayBridge.isAvailable()) {
            throw ConfigException("Moteur libXray indisponible dans cette compilation")
        }
    }

    private fun conversionError(error: String): String {
        val sanitized = XrayErrorSanitizer.sanitize(error)
        return when {
            sanitized.contains("no valid outbound", ignoreCase = true) ->
                "Lien invalide : aucun serveur exploitable " +
                    "(Xray refuse notamment un VLESS public sans TLS ni chiffrement)"
            sanitized.contains("unsupported share format", ignoreCase = true) ->
                "Format de lien non reconnu"
            sanitized.contains("isolated process", ignoreCase = true) ->
                "Déconnectez le tunnel avant d'importer une configuration"
            else -> "Conversion impossible : $sanitized"
        }
    }

    private fun displayName(link: String, fallback: String?): String? {
        val fragment = link.substringAfter('#', "")
        if (fragment.isNotBlank()) {
            val decoded = runCatching { URLDecoder.decode(fragment, "UTF-8") }.getOrDefault(fragment)
            if (decoded.isNotBlank()) return decoded.trim().take(64)
        }
        return fallback
    }

    private fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
}
