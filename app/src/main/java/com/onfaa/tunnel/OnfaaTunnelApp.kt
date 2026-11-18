package com.onfaa.tunnel

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import com.onfaa.tunnel.core.xray.XrayBridge
import com.onfaa.tunnel.data.config.TunnelConfigRepository
import com.onfaa.tunnel.data.preferences.UserPreferencesRepository
import com.onfaa.tunnel.data.subscription.SubscriptionStore
import com.onfaa.tunnel.vpn.LogSeverity
import com.onfaa.tunnel.vpn.TunnelStatusRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

private val Context.userPreferencesDataStore by preferencesDataStore(
    name = "onfaa_user_preferences"
)

/** Small manual container; the project deliberately avoids a DI framework. */
class AppContainer(context: Context) {
    val userPreferencesRepository = UserPreferencesRepository(context.userPreferencesDataStore)

    val subscriptionStore = SubscriptionStore(context.userPreferencesDataStore)
}

class OnfaaTunnelApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        TunnelStatusRepository.attachLogFile(this)
        // Read the persisted tail before the new session writes anything.
        reportInterruptedSession()
        TunnelConfigRepository.initialize(this)
        TunnelStatusRepository.log("Application démarrée", LogSeverity.INFO)
    }

    /**
     * A previous run can end in the middle of the connection sequence (process
     * killed, engine crash, device reboot): the persisted log then ends on a
     * non-terminal step and the user only sees an idle screen with no reason.
     * When the engine is really not running, that interruption is written to
     * the log so the next export explains itself. No state is invented: this
     * only reports what really happened.
     */
    private fun reportInterruptedSession() {
        if (!XrayBridge.isAvailable()) return
        val last = TunnelStatusRepository.events.value.lastOrNull()?.message ?: return
        val interruptedMidConnection = (
            last.startsWith("Moteur Xray démarré") ||
                last.startsWith("Moteur prêt") ||
                last.startsWith("Vérification de la connectivité démarrée") ||
                last.startsWith("Attente du réseau VPN") ||
                last.startsWith("Réseau VPN actif") ||
                (last.startsWith("Vérification ") && !last.startsWith("Vérification périodique")) ||
                last.startsWith("TUN établi")
            )
        if (!interruptedMidConnection) return
        CoroutineScope(Dispatchers.Default).launch {
            val engineStillRunning = runCatching { XrayBridge.getXrayState() }.getOrDefault(false)
            if (!engineStillRunning) {
                TunnelStatusRepository.log(
                    "La session précédente s'est interrompue pendant la connexion " +
                        "(moteur arrêté) — relancez la connexion",
                    LogSeverity.WARNING
                )
            }
        }
    }
}

val Context.onfaaAppContainer: AppContainer
    get() = (applicationContext as OnfaaTunnelApp).container
