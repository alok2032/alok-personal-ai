package com.ayra.ai.app.vpn

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class WireGuardConfig(
    val serverName: String = "Disabled",
    val endpoint: String = "",
    val clientAddress: String = "",
    val clientPrivateKey: String = "",
    val peerPublicKey: String = "",
    val dnsServers: String = "",
    val allowedIps: String = "",
    val mtu: Int = 1420
) {
    val isConfigured: Boolean get() = false
}

data class VpnShieldStatus(
    val isConnected: Boolean,
    val isConfigured: Boolean,
    val config: WireGuardConfig,
    val isKillSwitchActive: Boolean,
    val isDnsLeakProtected: Boolean,
    val bytesEncrypted: Long,
    val pingMs: Int
)

/**
 * Controller following Remediation Finding F03 (Option B):
 * Fake packet-dropping VPN has been completely decommissioned and removed.
 * Accurately reports disconnected status and disabled state.
 */
object AyraVpnPrivacyController {

    private val _vpnStatus = MutableStateFlow(
        VpnShieldStatus(
            isConnected = false,
            isConfigured = false,
            config = WireGuardConfig(),
            isKillSwitchActive = false,
            isDnsLeakProtected = false,
            bytesEncrypted = 0L,
            pingMs = 0
        )
    )
    val vpnStatus: StateFlow<VpnShieldStatus> = _vpnStatus.asStateFlow()

    fun initialize(context: Context) {
        // No-op. VPN service is disabled.
    }

    fun toggleVpn(context: Context): Boolean {
        // Honestly refuse to toggle fake VPN
        android.util.Log.i("VpnController", "VPN service is disabled per audit F03.")
        return false
    }

    fun clearConfig(context: Context) {
        // No-op
    }

    fun toggleKillSwitch() {
        // No-op
    }

    fun toggleDnsProtection() {
        // No-op
    }

    fun parseWireGuardConfig(conf: String, profileName: String = "Default"): WireGuardConfig {
        return WireGuardConfig(serverName = profileName)
    }

    fun saveConfig(context: Context, config: WireGuardConfig) {
        // No-op
    }

    fun setVpnConnected(connected: Boolean) {
        // Invariant: never claim connected
        _vpnStatus.value = _vpnStatus.value.copy(isConnected = false)
    }

    fun addEncryptedBytes(bytes: Long) {
        // No-op
    }

    fun formatStatusSummary(isBengali: Boolean): String {
        return if (isBengali) {
            "🔒 **প্রাইভেসি শিল্ড স্ট্যাটাস:**\n• অবস্থা: নিষ্ক্রিয় (VPN পরিষেবা অপসারিত)"
        } else {
            "🔒 **Privacy Shield Status:**\n• Status: Disconnected (VPN service decommissioned)"
        }
    }
}
