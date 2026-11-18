package com.onfaa.tunnel

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import com.onfaa.tunnel.data.config.TunnelConfigRepository
import com.onfaa.tunnel.data.preferences.UserPreferencesRepository
import com.onfaa.tunnel.data.subscription.SubscriptionStore
import com.onfaa.tunnel.vpn.LogSeverity
import com.onfaa.tunnel.vpn.TunnelStatusRepository

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
        TunnelConfigRepository.initialize(this)
        TunnelStatusRepository.log("Application démarrée", LogSeverity.INFO)
    }
}

val Context.onfaaAppContainer: AppContainer
    get() = (applicationContext as OnfaaTunnelApp).container
