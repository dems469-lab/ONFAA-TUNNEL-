package com.onfaa.tunnel.feature.home

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.onfaa.tunnel.data.config.TunnelConfigRepository
import com.onfaa.tunnel.domain.model.TunnelProfile
import com.onfaa.tunnel.vpn.OnfaaTunnelVpnService
import com.onfaa.tunnel.vpn.TunnelState
import com.onfaa.tunnel.vpn.TunnelStatusRepository

@Composable
fun HomeScreen() {
    val context = LocalContext.current
    val state by TunnelStatusRepository.state.collectAsStateWithLifecycle()
    val profiles by TunnelConfigRepository.profiles.collectAsStateWithLifecycle()
    val selectedId by TunnelConfigRepository.selectedId.collectAsStateWithLifecycle()
    val selected = profiles.firstOrNull { it.id == selectedId }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            startTunnel(context)
        } else {
            TunnelStatusRepository.error("Permission VPN refusée par l'utilisateur")
        }
    }

    fun requestConnect() {
        val profile = selected
        if (profile == null) {
            TunnelStatusRepository.error("Aucune configuration sélectionnée")
            return
        }
        if (profile.incomplete) {
            TunnelStatusRepository.error(
                "Configuration incomplète : réimportez le lien de « ${profile.displayName} »"
            )
            return
        }
        val consent = VpnService.prepare(context)
        if (consent != null) {
            TunnelStatusRepository.set(TunnelState.AwaitingPermission)
            TunnelStatusRepository.log("Autorisation VPN demandée à Android")
            permissionLauncher.launch(consent)
        } else {
            startTunnel(context)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "ONFAA TUNNEL",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(16.dp))
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = statusTitle(state),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = statusDetail(state, selected),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        when (state) {
            is TunnelState.Connected, is TunnelState.Reconnecting, is TunnelState.Disconnecting -> {
                OutlinedButton(
                    onClick = { disconnectTunnel(context) },
                    enabled = state !is TunnelState.Disconnecting,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Déconnexion")
                }
            }

            else -> {
                Button(
                    onClick = { requestConnect() },
                    enabled = selected != null &&
                        !selected.incomplete &&
                        state !is TunnelState.AwaitingPermission &&
                        state !is TunnelState.Preparing &&
                        state !is TunnelState.Connecting &&
                        state !is TunnelState.Verifying,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Connexion")
                }
            }
        }
        if (selected == null) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Importez une configuration (VLESS, VMess, Trojan, Shadowsocks, SOCKS5) dans l'onglet Configurations.",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center
            )
        } else if (selected.incomplete) {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Ce profil a été migré sans identifiant : réimportez son lien dans Configurations.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun startTunnel(context: Context) {
    val intent = Intent(context, OnfaaTunnelVpnService::class.java)
        .setAction(OnfaaTunnelVpnService.ACTION_CONNECT)
    ContextCompat.startForegroundService(context, intent)
}

private fun disconnectTunnel(context: Context) {
    context.startService(
        Intent(context, OnfaaTunnelVpnService::class.java)
            .setAction(OnfaaTunnelVpnService.ACTION_DISCONNECT)
    )
}

private fun statusTitle(state: TunnelState): String = when (state) {
    TunnelState.Idle -> "Déconnecté"
    TunnelState.AwaitingPermission -> "Autorisation demandée…"
    TunnelState.Preparing -> "Préparation…"
    TunnelState.Connecting -> "Connexion en cours…"
    TunnelState.Verifying -> "Vérification de la connectivité…"
    is TunnelState.Connected -> "Connecté"
    is TunnelState.Reconnecting -> "Reconnexion…"
    TunnelState.Disconnecting -> "Déconnexion…"
    is TunnelState.Error -> "Erreur"
}

private fun statusDetail(state: TunnelState, selected: TunnelProfile?): String = when (state) {
    TunnelState.Idle -> selected?.let { "${it.protocol.label} — ${it.endpoint}" }
        ?: "Aucune connexion active."
    TunnelState.AwaitingPermission -> "Android demande l'autorisation d'établir le VPN."
    TunnelState.Preparing -> "Validation du profil et préparation de l'interface TUN."
    TunnelState.Connecting -> "Démarrage du moteur Xray et du TUN Android."
    TunnelState.Verifying -> "Le moteur est actif ; vérification réelle du trafic."
    is TunnelState.Connected -> "${state.profileName} — ${state.protocol.label}"
    is TunnelState.Reconnecting ->
        "Réseau perdu (tentative ${state.attempt}) : le tunnel sera revalidé dès le retour du réseau."
    TunnelState.Disconnecting -> "Arrêt du moteur et fermeture du TUN."
    is TunnelState.Error -> state.message
}
