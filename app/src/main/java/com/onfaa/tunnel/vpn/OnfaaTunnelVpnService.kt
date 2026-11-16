package com.onfaa.tunnel.vpn

import android.content.Intent
import android.net.VpnService

/**
 * Phase 1 preparation only.
 *
 * This service is registered for future Android VpnService integration.
 * It does NOT establish a VPN tunnel, does NOT start Xray/libXray, and does NOT
 * produce any connection or traffic statistics.
 *
 * Real tunnel orchestration with upstream XTLS/Xray-core via libXray will be
 * implemented in later phases after official API verification.
 */
class OnfaaTunnelVpnService : VpnService() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // No-op: real VPN startup will be implemented in Phase 3.
        stopSelf()
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        // No tunnel to tear down in Phase 1.
        super.onDestroy()
    }
}
