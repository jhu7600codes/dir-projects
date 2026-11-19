package com.patternbox.app

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.mutableStateListOf
import java.net.Inet4Address
import java.net.NetworkInterface

/** wifi addresses of this phone, so friends can type one in if finding the game doesn't work */
fun localIps(): List<String> = runCatching {
    NetworkInterface.getNetworkInterfaces().toList()
        .filter { it.isUp && !it.isLoopback }
        .flatMap { it.inetAddresses.toList() }
        .filterIsInstance<Inet4Address>()
        .map { it.hostAddress ?: "" }
        .filter { it.isNotEmpty() }
}.getOrDefault(emptyList())

data class FoundGame(val name: String, val host: String, val port: Int)

private const val SERVICE = "_patternbox._tcp."

/** tells phones on the same wifi that a game is open */
class Advertiser(context: Context, private val name: String, private val port: Int) {
    private val nsd = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private var listener: NsdManager.RegistrationListener? = null

    fun start() {
        val info = NsdServiceInfo().apply {
            serviceName = "patternbox $name".take(60)
            serviceType = SERVICE
            setPort(port)
        }
        val l = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(info: NsdServiceInfo) {}
            override fun onRegistrationFailed(info: NsdServiceInfo, code: Int) {}
            override fun onServiceUnregistered(info: NsdServiceInfo) {}
            override fun onUnregistrationFailed(info: NsdServiceInfo, code: Int) {}
        }
        listener = l
        runCatching { nsd.registerService(info, NsdManager.PROTOCOL_DNS_SD, l) }
    }

    fun stop() {
        listener?.let { runCatching { nsd.unregisterService(it) } }
        listener = null
    }
}

/** finds open games on the wifi */
class Discovery(context: Context) {
    private val nsd = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val main = Handler(Looper.getMainLooper())
    val games = mutableStateListOf<FoundGame>()
    private var listener: NsdManager.DiscoveryListener? = null

    fun start() {
        val l = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(type: String) {}
            override fun onDiscoveryStopped(type: String) {}
            override fun onStartDiscoveryFailed(type: String, code: Int) {}
            override fun onStopDiscoveryFailed(type: String, code: Int) {}
            override fun onServiceFound(info: NsdServiceInfo) {
                @Suppress("DEPRECATION")
                runCatching {
                    nsd.resolveService(info, object : NsdManager.ResolveListener {
                        override fun onResolveFailed(info: NsdServiceInfo, code: Int) {}
                        override fun onServiceResolved(info: NsdServiceInfo) {
                            val host = info.host?.hostAddress ?: return
                            val g = FoundGame(info.serviceName.removePrefix("patternbox "), host, info.port)
                            main.post { if (games.none { it.host == g.host && it.port == g.port }) games += g }
                        }
                    })
                }
            }
            override fun onServiceLost(info: NsdServiceInfo) {
                main.post { games.removeAll { it.name == info.serviceName.removePrefix("patternbox ") } }
            }
        }
        listener = l
        runCatching { nsd.discoverServices(SERVICE, NsdManager.PROTOCOL_DNS_SD, l) }
    }

    fun stop() {
        listener?.let { runCatching { nsd.stopServiceDiscovery(it) } }
        listener = null
    }
}

