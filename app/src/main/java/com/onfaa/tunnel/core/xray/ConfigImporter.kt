package com.onfaa.tunnel.core.xray

import com.onfaa.tunnel.domain.model.TunnelProfile
import com.onfaa.tunnel.domain.model.TunnelProtocol
import java.net.URLDecoder
import java.security.MessageDigest
import kotlinx.coroutines.CancellationException

class ConfigException(message: String) : Exception(message)

/**
 * Real VLESS/VMess import: libXray converts the share link with
 * convertShareLinksToXrayJson, then the resulting outbound is embedded in the
 * full TUN configuration and validated by the real Xray engine (testXray,
 * which constructs without starting the TUN).
 */
object ConfigImporter {

    suspend fun importLink(rawLink: String): Result<TunnelProfile> = try {
        Result.success(importOrThrow(rawLink))
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (failure: Throwable) {
        Result.failure(failure)
    }

    private suspend fun importOrThrow(rawLink: String): TunnelProfile {
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
        if (!XrayBridge.isAvailable()) {
            throw ConfigException("Moteur libXray indisponible dans cette compilation")
        }

        val response = XrayBridge.convertShareLink(link)
        if (!response.success) {
            throw ConfigException(conversionError(response.error))
        }
        val data = response.data ?: throw ConfigException("Réponse de conversion vide")
        val outbounds = data.optJSONArray("outbounds") ?: throw ConfigException("Aucune sortie Xray produite")
        if (outbounds.length() != 1) {
            throw ConfigException("Le lien doit décrire exactement une configuration (${outbounds.length()} trouvées)")
        }

        val outbound = outbounds.getJSONObject(0)
        val protocolName = outbound.optString("protocol").lowercase()
        val protocol = when (protocolName) {
            "vless" -> TunnelProtocol.VLESS
            "vmess" -> TunnelProtocol.VMESS
            else -> throw ConfigException("Protocole converti non pris en charge : $protocolName")
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
        val profile = TunnelProfile(
            id = sha256(outboundJson).take(16),
            displayName = displayName(link, protocol, address),
            protocol = protocol,
            endpoint = "$address:$port",
            security = security,
            outboundJson = outboundJson
        )

        val validation = XrayBridge.testXray(XrayConfigBuilder.build(profile, tunFd = 0))
        if (!validation.success) {
            throw ConfigException(
                "Configuration refusée par le moteur Xray : ${XrayErrorSanitizer.sanitize(validation.error)}"
            )
        }
        return profile
    }

    private fun conversionError(error: String): String {
        val sanitized = XrayErrorSanitizer.sanitize(error)
        return when {
            sanitized.contains("no valid outbound", ignoreCase = true) ->
                "Lien VLESS/VMess invalide : aucun serveur exploitable"
            sanitized.contains("unsupported share format", ignoreCase = true) ->
                "Format de lien non reconnu"
            sanitized.contains("isolated process", ignoreCase = true) ->
                "Déconnectez le tunnel avant d'importer une configuration"
            else -> "Conversion impossible : $sanitized"
        }
    }

    private fun displayName(link: String, protocol: TunnelProtocol, address: String): String {
        val fragment = link.substringAfter('#', "")
        if (fragment.isNotBlank()) {
            val decoded = runCatching { URLDecoder.decode(fragment, "UTF-8") }.getOrDefault(fragment)
            if (decoded.isNotBlank()) return decoded.trim().take(64)
        }
        return "${protocol.name} $address"
    }

    private fun sha256(value: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
}
