package com.example.core

import java.net.Inet4Address
import java.net.NetworkInterface
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object NetworkUtils {
    const val DEFAULT_SERVER_PORT = 8080

    fun getLocalWifiIpAddress(): String {
        return try {
            val interfaces = NetworkInterface.getNetworkInterfaces()?.toList().orEmpty()
            val candidates = interfaces
                .filter { it.isUp && !it.isLoopback }
                .flatMap { iface ->
                    iface.inetAddresses.toList()
                        .filterIsInstance<Inet4Address>()
                        .filter { !it.isLoopbackAddress && it.isSiteLocalAddress }
                        .map { iface.name to (it.hostAddress ?: "") }
                }
            val wlan = candidates.firstOrNull { (name, _) ->
                name.startsWith("wlan", ignoreCase = true) || name.startsWith("ap", ignoreCase = true)
            }
            wlan?.second?.takeIf { it.isNotBlank() }
                ?: candidates.firstOrNull()?.second?.takeIf { it.isNotBlank() }
                ?: "127.0.0.1"
        } catch (_: Exception) {
            "127.0.0.1"
        }
    }

    fun formatDateTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.US)
        return sdf.format(Date(timestamp))
    }

    fun formatDateOnly(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        return sdf.format(Date(timestamp))
    }

    fun formatBytes(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        val kb = bytes / 1024.0
        if (kb < 1024) return String.format(Locale.US, "%.1f KB", kb)
        val mb = kb / 1024.0
        return String.format(Locale.US, "%.2f MB", mb)
    }
}
