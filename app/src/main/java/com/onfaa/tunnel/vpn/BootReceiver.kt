package com.onfaa.tunnel.vpn

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.util.Log
import com.onfaa.tunnel.data.config.TunnelConfigRepository
import com.onfaa.tunnel.onfaaAppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Real device-boot auto-connect.
 *
 * On BOOT_COMPLETED the persisted [UserPreferences.autoConnect] preference is
 * honored only when every precondition holds: a profile is selected, the
 * profile is complete, and the VPN user consent was already granted (VpnService.prepare
 * returns null). No dialog is ever shown from the background, so the receiver
 * cannot trigger the platform consent prompt: if consent is missing the
 * connection is skipped and the reason is logged. The intent carries no extra,
 * so the service resolves the currently selected configuration exactly like a
 * manual tap.
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val appContext = context.applicationContext
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val prefs = appContext.onfaaAppContainer.userPreferencesRepository.preferences.first()
                if (!prefs.autoConnect) {
                    Log.i(TAG, "Boot auto-connect skipped: preference disabled")
                    return@launch
                }
                val selectedId = TunnelConfigRepository.selectedId.first()
                if (selectedId.isNullOrBlank()) {
                    TunnelStatusRepository.log(
                        "Auto-connexion au démarrage ignorée : aucune configuration sélectionnée"
                    )
                    return@launch
                }
                val currentState = TunnelStatusRepository.state.first()
                if (currentState !is TunnelState.Idle && currentState !is TunnelState.Error) {
                    Log.i(TAG, "Boot auto-connect skipped: tunnel already ${currentState::class.simpleName}")
                    return@launch
                }
                if (VpnService.prepare(appContext) != null) {
                    TunnelStatusRepository.log(
                        "Auto-connexion au démarrage ignorée : permission VPN non accordée"
                    )
                    return@launch
                }
                TunnelStatusRepository.log("Auto-connexion au démarrage du téléphone")
                val service = Intent(appContext, OnfaaTunnelVpnService::class.java)
                    .setAction(OnfaaTunnelVpnService.ACTION_CONNECT)
                appContext.startForegroundService(service)
            } catch (failure: Throwable) {
                Log.w(TAG, "Boot auto-connect failed: ${failure.javaClass.simpleName}")
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        const val TAG = "BootReceiver"
    }
}
