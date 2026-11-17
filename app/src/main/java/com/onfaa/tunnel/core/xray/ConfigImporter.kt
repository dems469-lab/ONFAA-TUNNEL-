package com.onfaa.tunnel.core.xray

import com.onfaa.tunnel.domain.model.TunnelProfile
import com.onfaa.tunnel.domain.model.TunnelProtocol
import java.net.URLDecoder
import java.security.MessageDigest
import kotlinx.coroutines.CancellationException
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class ConfigException(message: String) : Exception(message)

/**
 * Real VLESS/VMess import.
 *
 * Accepted inputs:
 *  - share link vless:// / vmess:// (converted by libXray
 *    convertShareLinksToXrayJson, then validated by the real engine),
 *  - Xray JSON client configuration (or a single VLESS/VMess outbound object).
 *
 * Every accepted profile is embedded in the full TUN configuration and checked
 * with testXray (constructs handlers, never starts the TUN) before it is
 * persisted. Nothing is accepted silently: unknown protocols, several
 * candidate outbounds, missing credentials and engine refusals all produce a
 * readable error. Raw inputs are never logged.
 */
object ConfigImporter {

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
        if (scheme != "vless" && scheme != "vmess") {
            throw ConfigException("Protocole non pris en charge : utilisez un lien vless:// ou vmess://")
        }
        if (scheme == "vmess" && !link.substringAfter("://").contains('@')) {
            throw ConfigException(
                "Format VMess base64 historique non pris en charge par libXray ; " +
                    "utilisez un lien vmess://UUID@hôte:port ou une configuration Xray JSON"
            )
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
                "Aucun serveur VLESS/VMess dans ce JSON : ajoutez une sortie " +
                    "\"protocol\": \"vless\" ou \"vmess\" avec son adresse, son port et son identifiant"
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
                val protocol = candidate.optString("protocol").lowercase()
                if (protocol == "vless" || protocol == "vmess") supported.add(candidate)
            }
            return when (supported.size) {
                0 -> null
                1 -> supported[0]
                else -> throw ConfigException(
                    "Le JSON contient ${supported.size} sorties VLESS/VMess ; " +
                        "importez une configuration avec une seule sortie"
                )
            }
        }
        val protocol = root.optString("protocol").lowercase()
        if (protocol == "vless" || protocol == "vmess") return root
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

    // ------------------------------------------------------------- profile

    private fun profileFromOutbound(
        outbound: JSONObject,
        preferredName: String?,
        fallbackName: String?
    ): TunnelProfile {
        val protocolName = outbound.optString("protocol").lowercase()
        val protocol = when (protocolName) {
            "vless" -> TunnelProtocol.VLESS
            "vmess" -> TunnelProtocol.VMESS
            "" -> throw ConfigException("Protocole absent du serveur")
            else -> throw ConfigException("Protocole non pris en charge : $protocolName")
        }

        val settings = outbound.optJSONObject("settings")
            ?: throw ConfigException("Paramètres du serveur absents")
        if (settings.optString("id").isBlank()) {
            throw ConfigException("UUID/identifiant du serveur manquant")
        }
        val address = settings.optString("address").trim()
        if (address.isEmpty()) throw ConfigException("Adresse du serveur manquante")
        val port = settings.optInt("port", 0)
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
            displayName = explicitName ?: tag ?: fallbackName ?: "${protocol.name} $address",
            protocol = protocol,
            endpoint = "$address:$port",
            security = security,
            outboundJson = outboundJson
        )
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
                "Lien VLESS/VMess invalide : aucun serveur exploitable " +
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
