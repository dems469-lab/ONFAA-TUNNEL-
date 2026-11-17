package com.onfaa.tunnel

import android.app.Application
import com.onfaa.tunnel.data.config.TunnelConfigRepository
import com.onfaa.tunnel.vpn.LogSeverity
import com.onfaa.tunnel.vpn.TunnelStatusRepository

class OnfaaTunnelApp : Application() {
    override fun onCreate() {
        super.onCreate()
        TunnelStatusRepository.attachLogFile(this)
        TunnelConfigRepository.initialize(this)
        TunnelStatusRepository.log("Application démarrée", LogSeverity.INFO)
    }
}
