package com.patternbox.app

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.mutableStateListOf

private const val SERVICE = "_patternbox._tcp."

/** android finds games on the wifi with nsd (mdns) */
class NsdLanFinder(context: Context) : LanFinder {
    private val nsd = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private var listener: NsdManager.RegistrationListener? = null

    override fun advertise(name: String, port: Int) {
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

    override fun stopAdvertising() {
        listener?.let { runCatching { nsd.unregisterService(it) } }
        listener = null
    }

    private val main = Handler(Looper.getMainLooper())
    override val games = mutableStateListOf<FoundGame>()
    private var discovery: NsdManager.DiscoveryListener? = null

    override fun startDiscovery() {
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
        discovery = l
        runCatching { nsd.discoverServices(SERVICE, NsdManager.PROTOCOL_DNS_SD, l) }
    }

    override fun stopDiscovery() {
        discovery?.let { runCatching { nsd.stopServiceDiscovery(it) } }
        discovery = null
        games.clear()
    }
}

