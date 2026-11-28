package app.lineagifier.statusbar

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.hardware.camera2.CameraManager
import android.net.TrafficStats
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ServiceCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import app.lineagifier.shell.Root
import app.lineagifier.undo.UndoLog
import kotlinx.coroutines.*
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** compose needs a lifecycle + saved state owner to live in a window that isn't an activity */
private class OverlayOwner : LifecycleOwner, SavedStateRegistryOwner, ViewModelStoreOwner {
    private val reg = LifecycleRegistry(this)
    private val ssrc = SavedStateRegistryController.create(this)
    override val lifecycle: Lifecycle get() = reg
    override val savedStateRegistry: SavedStateRegistry get() = ssrc.savedStateRegistry
    override val viewModelStore = ViewModelStore()
    fun start() { ssrc.performRestore(null); reg.currentState = Lifecycle.State.RESUMED }
    fun stop() { reg.currentState = Lifecycle.State.DESTROYED; viewModelStore.clear() }
    fun attach(v: View) { v.setViewTreeLifecycleOwner(this); v.setViewTreeSavedStateRegistryOwner(this); v.setViewTreeViewModelStoreOwner(this) }
}

/**
 * draws lineagifier's own lineage-style status bar items (clock, battery, network traffic) in an overlay under the
 * real, transparent status bar, plus the control center handle and panel. the stock clock/battery are hidden with
 * icon_blacklist and the stock icons are padded aside (fabricated overlays), so nothing overlaps.
 */
class OverlayService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var wm: WindowManager
    private val owner = OverlayOwner()
    private var bar: ComposeView? = null
    private var handle: ComposeView? = null
    private var panel: ComposeView? = null

    private var config by mutableStateOf(BarConfig())
    private var statusVisible by mutableStateOf(true)
    private var barHeightPx by mutableIntStateOf(0)
    private var basePadStart = 0
    private var basePadEnd = 0
    private var lastPads = -1 to -1
    private var lastW = 0 to 0
    private var padJob: Job? = null

    private var battery by mutableIntStateOf(0)
    private var charging by mutableStateOf(false)
    private var torchOn by mutableStateOf(false)
    /** what systemui itself does: dark icons when the app under the bar is light */
    private var darkIcons by mutableStateOf<Boolean?>(null)
    private var appearanceJob: Job? = null

    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> reload() }
    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, i: Intent) {
            val l = i.getIntExtra(BatteryManager.EXTRA_LEVEL, 0); val s = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100)
            battery = if (s > 0) l * 100 / s else l
            charging = i.getIntExtra(BatteryManager.EXTRA_STATUS, 0).let { it == BatteryManager.BATTERY_STATUS_CHARGING || it == BatteryManager.BATTERY_STATUS_FULL }
        }
    }
    private val torchCb = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) { torchOn = enabled }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WindowManager::class.java)
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel("overlay", "status bar & control center", NotificationManager.IMPORTANCE_MIN))
        val n = Notification.Builder(this, "overlay").setSmallIcon(android.R.drawable.stat_notify_more)
            .setContentTitle("lineagifier status bar is running").setOngoing(true).build()
        ServiceCompat.startForeground(this, 1, n, if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0)
        owner.start()
        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        try { getSystemService(CameraManager::class.java).registerTorchCallback(torchCb, null) } catch (_: Throwable) {}
        BarConfig.prefs(this).registerOnSharedPreferenceChangeListener(prefsListener)
        reload()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        reload()
        return START_STICKY
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // an overlay change (sizing) or rotation changes the bar height
        measureBar(); bar?.let { wm.updateViewLayout(it, barParams()) }
    }

    private fun reload() {
        config = BarConfig.load(this)
        if (!config.needsService) { stopSelf(); return }
        measureBar()
        if (config.barEnabled) {
            showBar()
            watchAppearance()
            // base padding may have changed (sizing on/off): re-pad with the last measured widths
            lastPads = -1 to -1
            if (lastW != 0 to 0) requestPads(lastW.first, lastW.second)
        } else { removeView(bar); bar = null }
        if (config.ccEnabled) showHandle() else { removeView(handle); handle = null; closePanel() }
    }

    @SuppressLint("DiscouragedApi", "InternalInsetResource")
    private fun measureBar() {
        // aosp 13: max(cutout safe inset, status_bar_height_default), same as SystemBarUtils
        val id = resources.getIdentifier("status_bar_height_default", "dimen", "android")
        val def = if (id != 0) resources.getDimensionPixelSize(id) else (24 * resources.displayMetrics.density).toInt()
        val cut = if (Build.VERSION.SDK_INT >= 30) wm.currentWindowMetrics.windowInsets.displayCutout?.safeInsetTop ?: 0 else 0
        barHeightPx = maxOf(def, cut)
        val m = resources.displayMetrics
        if (config.sizingEnabled) {
            val (w, h) = Sizing.realScreen(this)
            val s = Sizing.iphone13(w, h, config.safeTopPt, config.padPt)
            basePadStart = s.padStartPx; basePadEnd = s.padEndPx
        } else {
            basePadStart = (8 * m.density).toInt(); basePadEnd = (8 * m.density).toInt()   // aosp 13 systemui defaults
        }
    }

    private fun watchAppearance() {
        appearanceJob?.cancel()
        if (config.textColor != "auto") return
        appearanceJob = scope.launch {
            while (isActive) {
                val r = withContext(Dispatchers.IO) { Root.run("dumpsys window displays | grep -A1 mLastStatusBarAppearanceRegions") }
                darkIcons = BarLogic.lightStatusBar(r.out)
                delay(2000)
            }
        }
    }

    private fun baseFlags() = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS

    private fun params(w: Int, h: Int, flags: Int, gravity: Int, y: Int = 0) = WindowManager.LayoutParams(
        w, h, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, flags, PixelFormat.TRANSLUCENT,
    ).apply {
        this.gravity = gravity; this.y = y
        if (Build.VERSION.SDK_INT >= 28) layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        if (Build.VERSION.SDK_INT >= 30) setFitInsetsTypes(0)
        title = "lineagifier"
    }

    private fun barParams() = params(WindowManager.LayoutParams.MATCH_PARENT, barHeightPx,
        baseFlags() or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE, Gravity.TOP or Gravity.START)

    private fun compose(content: @Composable () -> Unit) = ComposeView(this).also { owner.attach(it); it.setContent(content) }

    private fun showBar() {
        if (bar != null) return
        bar = compose { Bar() }.also { v ->
            // hide our bar whenever the real status bar is hidden (fullscreen video, games)
            if (Build.VERSION.SDK_INT >= 30) v.setOnApplyWindowInsetsListener { _, insets ->
                statusVisible = insets.isVisible(WindowInsets.Type.statusBars()); insets
            }
            wm.addView(v, barParams())
        }
    }

    private fun showHandle() {
        if (handle != null) return
        val d = resources.displayMetrics.density
        handle = compose { Handle() }.also {
            wm.addView(it, params((22 * d).toInt(), (config.ccHandleHeightDp * d).toInt(), baseFlags(), Gravity.TOP or Gravity.END, barHeightPx))
        }
    }

    private fun openPanel() {
        if (panel != null) return
        panel = compose { Panel() }.also {
            wm.addView(it, params(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT, baseFlags(), Gravity.TOP))
        }
    }

    private fun closePanel() { removeView(panel); panel = null }

    private fun removeView(v: View?) { if (v != null) try { wm.removeView(v) } catch (_: Throwable) {} }

    /** pads the stock icons aside by exactly the space our blocks take, so they never overlap */
    private fun requestPads(leftW: Int, rightW: Int) {
        lastW = leftW to rightW
        val gap = (6 * resources.displayMetrics.density).toInt()
        val start = basePadStart + if (leftW > 0) leftW + gap else 0
        val end = basePadEnd + if (rightW > 0) rightW + gap else 0
        if (start to end == lastPads) return
        lastPads = start to end
        padJob?.cancel()
        padJob = scope.launch {
            delay(400)  // text widths settle over a few frames; don't thrash systemui
            withContext(Dispatchers.IO) { Sizing.setPaddings(Root, UndoLog(File(filesDir, "undo-log.json")), start, end) }
        }
    }

    // ---------- the bar ----------

    @Composable
    private fun Bar() {
        val c = config
        val night = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        // auto: follow systemui's own icon color; until the first read, fall back to dark mode
        val color = when (c.textColor) { "white" -> Color.White; "black" -> Color.Black; else -> if (darkIcons ?: !night) Color.Black else Color.White }
        val now by produceState(Date()) { while (true) { value = Date(); delay(if (c.clockSeconds) 1000 else 5000) } }
        val traffic by produceState(0L to 0L, c.traffic) {
            var rx = TrafficStats.getTotalRxBytes(); var tx = TrafficStats.getTotalTxBytes()
            while (c.traffic != "off") {
                delay(1000)
                val r2 = TrafficStats.getTotalRxBytes(); val t2 = TrafficStats.getTotalTxBytes()
                value = (r2 - rx).coerceAtLeast(0) to (t2 - tx).coerceAtLeast(0); rx = r2; tx = t2
            }
        }
        val d = LocalDensity.current
        val h = with(d) { barHeightPx.toDp() }
        if (!statusVisible) return
        val clock: @Composable () -> Unit = { ClockText(now, c, color) }
        val trafficV: @Composable () -> Unit = { if (c.traffic != "off") TrafficText(traffic, c.traffic == "both", color) }
        val batteryV: @Composable () -> Unit = { BatteryView(battery, charging, c.batteryStyle, color) }
        var lw by remember { mutableIntStateOf(0) }
        var rw by remember { mutableIntStateOf(0) }
        LaunchedEffect(lw, rw) { requestPads(lw, rw) }
        Box(Modifier.fillMaxWidth().height(h)) {
            Row(Modifier.align(Alignment.CenterStart).padding(start = with(d) { basePadStart.toDp() }).onSizeChanged { lw = it.width },
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (c.clockPos == "left") clock()
                if (c.trafficSide == "left") trafficV()
            }
            Row(Modifier.align(Alignment.CenterEnd).padding(end = with(d) { basePadEnd.toDp() }).onSizeChanged { rw = it.width },
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (c.trafficSide == "right") trafficV()
                if (c.clockPos == "right") clock()
                batteryV()
            }
        }
    }

    @Composable
    private fun ClockText(now: Date, c: BarConfig, color: Color) {
        val pattern = buildString {
            if (c.showDate) append("EEE d  ")
            append(if (c.clock24h) "HH:mm" else "h:mm")
            if (c.clockSeconds) append(":ss")
            if (!c.clock24h) append(" a")
        }
        Text(SimpleDateFormat(pattern, Locale.getDefault()).format(now), color = color, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }

    @Composable
    private fun TrafficText(t: Pair<Long, Long>, both: Boolean, color: Color) {
        fun fmt(b: Long) = when { b >= 1_000_000 -> "%.1f MB/s".format(b / 1_000_000f); b >= 1000 -> "${b / 1000} KB/s"; else -> "$b B/s" }
        Column(horizontalAlignment = Alignment.End) {
            if (both) Text("↑ ${fmt(t.second)}", color = color, fontSize = 8.sp, lineHeight = 9.sp)
            Text("↓ ${fmt(t.first)}", color = color, fontSize = 8.sp, lineHeight = 9.sp)
        }
    }

    @Composable
    private fun BatteryView(level: Int, charging: Boolean, style: String, color: Color) {
        if (style == "hidden") return
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            if (style == "percent" || style == "both") Text("$level%", color = color, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            if (style == "icon" || style == "both") Canvas(Modifier.size(width = 24.dp, height = 12.dp)) {
                val nub = 2.dp.toPx(); val sw = 1.2.dp.toPx(); val r = CornerRadius(3.dp.toPx())
                drawRoundRect(color.copy(alpha = 0.45f), Offset.Zero, Size(size.width - nub, size.height), r, style = Stroke(sw))
                val fill = (size.width - nub - 4 * sw) * (level / 100f)
                drawRoundRect(if (charging) Color(0xFF4CD964) else if (level <= 15) Color(0xFFFF3B30) else color,
                    Offset(2 * sw, 2 * sw), Size(fill, size.height - 4 * sw), CornerRadius(1.5.dp.toPx()))
                drawRoundRect(color.copy(alpha = 0.45f), Offset(size.width - nub, size.height * 0.33f), Size(nub, size.height * 0.34f), CornerRadius(1f))
            }
        }
    }

    // ---------- control center ----------

    @Composable
    private fun Handle() {
        Box(Modifier.fillMaxSize().pointerInput(Unit) {
            var dy = 0f
            detectVerticalDragGestures(onDragStart = { dy = 0f }, onDragEnd = { if (dy > 30.dp.toPx()) openPanel() }) { _, d -> dy += d }
        }.clickable(remember { MutableInteractionSource() }, null) { openPanel() }) {
            // a faint pill so you can find it, like ios' grabber
            Box(Modifier.align(Alignment.CenterEnd).padding(end = 3.dp).width(4.dp).height(44.dp).clip(RoundedCornerShape(2.dp)).background(Color.White.copy(alpha = 0.35f)))
        }
    }

    @Composable
    private fun Panel() {
        var shown by remember { mutableStateOf(false) }
        var available by remember { mutableStateOf(emptySet<String>()) }
        var states by remember { mutableStateOf(emptyMap<String, Boolean>()) }
        var bright by remember { mutableFloatStateOf(0.5f) }
        var vol by remember { mutableFloatStateOf(ControlCenter.volume(this@OverlayService)) }
        val ids = ControlCenter.TILES.map { it.id }.filter { it in config.ccTiles && it in available }
        LaunchedEffect(Unit) {
            shown = true
            available = withContext(Dispatchers.IO) { ControlCenter.available(this@OverlayService, Root) }
            bright = withContext(Dispatchers.IO) { ControlCenter.brightness(Root) } / 255f
            while (true) {
                states = withContext(Dispatchers.IO) { ControlCenter.states(Root, available, torchOn) }
                delay(1500)
            }
        }
        fun close() { scope.launch { shown = false; delay(200); closePanel() } }
        fun toggle(id: String) = scope.launch {
            val on = states[id] == true
            when (id) {
                "torch" -> ControlCenter.torchCamera(this@OverlayService)?.let { getSystemService(CameraManager::class.java).setTorchMode(it, !on) }
                "screenshot" -> { close(); delay(600); withContext(Dispatchers.IO) { Root.run(ControlCenter.toggleCommand(id, on)!!) }; return@launch }
                else -> ControlCenter.toggleCommand(id, on)?.let { cmd -> withContext(Dispatchers.IO) { Root.run(cmd) } }
            }
            states = states + (id to !on)
        }
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = if (shown) 0.55f else 0f))
            .clickable(remember { MutableInteractionSource() }, null) { close() }) {
            AnimatedVisibility(shown, enter = slideInVertically { -it / 3 } + fadeIn(), exit = slideOutVertically { -it / 3 } + fadeOut()) {
                Column(Modifier.statusBarsPadding().padding(16.dp).clickable(remember { MutableInteractionSource() }, null) {},
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ids.chunked(4).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            row.forEach { id -> TileView(id, ControlCenter.TILES.first { it.id == id }.label, states[id] == true, id == "screenshot") { toggle(id) } }
                        }
                    }
                    SliderCard("brightness", bright) { bright = it; scope.launch(Dispatchers.IO) { ControlCenter.setBrightness(Root, (it * 255).toInt()) } }
                    SliderCard("volume", vol) { vol = it; ControlCenter.setVolume(this@OverlayService, it) }
                }
            }
        }
    }

    @Composable
    private fun TileView(id: String, label: String, on: Boolean, action: Boolean, onClick: () -> Unit) {
        val size: Dp = 72.dp
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(size)) {
            Box(Modifier.size(size).clip(RoundedCornerShape(22.dp))
                .background(if (on && !action) Color(0xFF167C80) else Color(0xCC2C2C2E)).clickable { onClick() },
                contentAlignment = Alignment.Center) {
                TileIcon(id)
            }
            Text(label, color = Color.White, fontSize = 11.sp, maxLines = 1, modifier = Modifier.padding(top = 4.dp))
        }
    }

    @Composable
    private fun SliderCard(label: String, value: Float, onChange: (Float) -> Unit) {
        Column(Modifier.widthIn(max = 324.dp).fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Color(0xCC2C2C2E)).padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(label, color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
            Slider(value, onChange, colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White, inactiveTrackColor = Color.White.copy(alpha = 0.25f)))
        }
    }

    override fun onDestroy() {
        BarConfig.prefs(this).unregisterOnSharedPreferenceChangeListener(prefsListener)
        try { unregisterReceiver(batteryReceiver) } catch (_: Throwable) {}
        try { getSystemService(CameraManager::class.java).unregisterTorchCallback(torchCb) } catch (_: Throwable) {}
        removeView(bar); removeView(handle); removeView(panel)
        owner.stop()
        scope.cancel()
        super.onDestroy()
    }
}

/** starts the overlay service again after a reboot if the bar or control center is on */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED && BarConfig.load(ctx).needsService)
            ctx.startForegroundService(Intent(ctx, OverlayService::class.java))
    }
}
