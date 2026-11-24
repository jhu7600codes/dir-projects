package com.patternbox.app

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.staticCompositionLocalOf
import com.patternbox.core.Event
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.net.Inet4Address
import java.net.NetworkInterface

/*
 * everything in ui/ is shared by the android app and the linux app.
 * the few things that differ per platform come in through this file.
 */

/** key-value save storage. same shape as android's SharedPreferences so that one just wraps */
interface Store {
    fun getString(key: String, def: String?): String?
    fun getBoolean(key: String, def: Boolean): Boolean
    fun getInt(key: String, def: Int): Int
    fun getLong(key: String, def: Long): Long
    fun getFloat(key: String, def: Float): Float
    fun getStringSet(key: String, def: Set<String>?): Set<String>?
    fun edit(): Editor

    interface Editor {
        fun putString(key: String, v: String?): Editor
        fun putBoolean(key: String, v: Boolean): Editor
        fun putInt(key: String, v: Int): Editor
        fun putLong(key: String, v: Long): Editor
        fun putFloat(key: String, v: Float): Editor
        fun putStringSet(key: String, v: Set<String>?): Editor
        fun apply()
    }
}

/** sounds and vibration */
interface Feedback {
    fun play(e: Event) {}
    fun buzz(big: Boolean) {}
}

data class FoundGame(val name: String, val host: String, val port: Int)

/** finding wifi games automatically. the linux app doesn't, you type the ip there */
interface LanFinder {
    val games: SnapshotStateList<FoundGame>
    fun startDiscovery() {}
    fun stopDiscovery() {}
    fun advertise(name: String, port: Int) {}
    fun stopAdvertising() {}
}

class NoLanFinder : LanFinder {
    override val games = mutableStateListOf<FoundGame>()
}

class Platform(
    val name: String,
    /** on screen fight buttons (phone) vs keyboard hints (pc) */
    val touchControls: Boolean,
    val hasDynamicColor: Boolean,
    val dynamicScheme: @Composable (dark: Boolean) -> ColorScheme?,
    val feedback: Feedback,
    val lan: LanFinder,
    val backHandler: @Composable (enabled: Boolean, onBack: () -> Unit) -> Unit,
    /** pc only: pick a text file and return what's in it */
    val openTextFile: (() -> String?)? = null,
    /** pc only: save text to a file the user picks, true if saved */
    val saveTextFile: ((suggestedName: String, text: String) -> Boolean)? = null,
)

val LocalPlatform = staticCompositionLocalOf<Platform> { error("no platform set") }

/** run on the ui thread (network callbacks come in on their own threads) */
@OptIn(kotlinx.coroutines.DelicateCoroutinesApi::class)
fun onMain(block: () -> Unit) {
    GlobalScope.launch(Dispatchers.Main) { block() }
}

/** wifi addresses of this machine, so friends can type one in if finding the game doesn't work */
fun localIps(): List<String> = runCatching {
    NetworkInterface.getNetworkInterfaces().toList()
        .filter { it.isUp && !it.isLoopback && !it.isVirtual }
        .flatMap { it.inetAddresses.toList() }
        .filterIsInstance<Inet4Address>()
        .map { it.hostAddress ?: "" }
        .filter { it.isNotEmpty() && !it.startsWith("172.17.") }
}.getOrDefault(emptyList())

