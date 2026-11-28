package app.lineagifier.statusbar

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.ServiceInfo
import android.content.res.Configuration
import android.graphics.PixelFormat
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
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
import app.lineagifier.qs.Backend
import app.lineagifier.qs.Backends
import app.lineagifier.qs.QsActions
import app.lineagifier.qs.QsPanel
import app.lineagifier.qs.QsUi
import app.lineagifier.qs.SysUi
import app.lineagifier.qs.TileDef
import app.lineagifier.qs.TileState
import app.lineagifier.qs.Tiles
import app.lineagifier.shell.Root
import app.lineagifier.ui.MainActivity
import kotlinx.coroutines.*

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
 * lineage-style quick settings in an overlay: a small handle on the right edge under the status bar opens the
 * panel (android's own pull-down belongs to systemui). also keeps the screen on for the caffeine tile.
 */
class OverlayService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private lateinit var wm: WindowManager
    private val owner = OverlayOwner()
    private var handle: ComposeView? = null
    private var panel: ComposeView? = null
    private var caffeineView: View? = null

    private var config by mutableStateOf(BarConfig())
    private var torchOn by mutableStateOf(false)
    private var caffeineLeft by mutableStateOf<Int?>(null)   // seconds, -1 = until turned off, null = off
    private var caffeineJob: Job? = null
    private lateinit var backends: Backends

    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ -> reload() }
    private val torchCb = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) { torchOn = enabled }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        wm = getSystemService(WindowManager::class.java)
        backends = Backends(this, Root)
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(NotificationChannel("overlay", "quick settings", NotificationManager.IMPORTANCE_MIN))
        val n = Notification.Builder(this, "overlay").setSmallIcon(android.R.drawable.stat_notify_more)
            .setContentTitle("lineagifier quick settings is running").setOngoing(true).build()
        ServiceCompat.startForeground(this, 1, n, if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0)
        owner.start()
        try { getSystemService(CameraManager::class.java).registerTorchCallback(torchCb, null) } catch (_: Throwable) {}
        BarConfig.prefs(this).registerOnSharedPreferenceChangeListener(prefsListener)
        scope.launch(Dispatchers.IO) { backends.probe() }
        reload()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int { reload(); return START_STICKY }

    private fun reload() {
        config = BarConfig.load(this)
        if (!config.needsService) { stopSelf(); return }
        removeView(handle); handle = null
        showHandle()
    }

    private fun statusBarHeight(): Int {
        val id = resources.getIdentifier("status_bar_height_default", "dimen", "android")
        val def = if (id != 0) resources.getDimensionPixelSize(id) else (24 * resources.displayMetrics.density).toInt()
        val cut = if (Build.VERSION.SDK_INT >= 30) wm.currentWindowMetrics.windowInsets.displayCutout?.safeInsetTop ?: 0 else 0
        return maxOf(def, cut)
    }

    private fun params(w: Int, h: Int, flags: Int, gravity: Int, y: Int = 0) = WindowManager.LayoutParams(
        w, h, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        flags or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        PixelFormat.TRANSLUCENT,
    ).apply {
        this.gravity = gravity; this.y = y
        if (Build.VERSION.SDK_INT >= 28) layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        if (Build.VERSION.SDK_INT >= 30) setFitInsetsTypes(0)
        title = "lineagifier"
    }

    private fun compose(content: @Composable () -> Unit) = ComposeView(this).also { owner.attach(it); it.setContent(content) }

    private fun showHandle() {
        val d = resources.displayMetrics.density
        handle = compose { Handle() }.also {
            wm.addView(it, params((22 * d).toInt(), (config.ccHandleHeightDp * d).toInt(), WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                Gravity.TOP or Gravity.END, statusBarHeight()))
        }
    }

    private fun removeView(v: View?) { if (v != null) try { wm.removeView(v) } catch (_: Throwable) {} }

    @Composable
    private fun Handle() {
        Box(Modifier.fillMaxSize().pointerInput(Unit) {
            var dy = 0f
            detectVerticalDragGestures(onDragStart = { dy = 0f }, onDragEnd = { if (dy > 30.dp.toPx()) openPanel() }) { _, d -> dy += d }
        }.clickable(remember { MutableInteractionSource() }, null) { openPanel() }) {
            Box(Modifier.align(Alignment.CenterEnd).padding(end = 3.dp).width(4.dp).height(44.dp).clip(RoundedCornerShape(2.dp)).background(Color.White.copy(alpha = 0.35f)))
        }
    }

    // ---------- the panel ----------

    private fun openPanel() {
        if (panel != null) return
        // focusable (no FLAG_NOT_FOCUSABLE) so the back key closes it, like the real shade
        panel = compose { Panel() }.also {
            it.isFocusableInTouchMode = true
            it.setOnKeyListener { _, code, ev ->
                if (code == android.view.KeyEvent.KEYCODE_BACK && ev.action == android.view.KeyEvent.ACTION_UP) { closePanel(); true } else false
            }
            wm.addView(it, params(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT, 0, Gravity.TOP))
        }
    }

    private fun closePanel() { removeView(panel); panel = null }

    @Composable
    private fun Panel() {
        val night = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val sysui = remember { SysUi(this) }
        val colors = remember(night) { sysui.colors(night) }
        val tiles = config.ccTiles.mapNotNull { Tiles.bySpec(it) }
        var shown by remember { mutableStateOf(false) }
        var states by remember { mutableStateOf(emptyMap<String, TileState>()) }
        var bright by remember { mutableFloatStateOf(0.5f) }
        var auto by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            shown = true
            val (b, a) = withContext(Dispatchers.IO) { backends.brightness() }
            bright = Tiles.linearToGamma(b, 1, 255) / Tiles.GAMMA_SPACE_MAX.toFloat(); auto = a
            while (isActive) {
                states = withContext(Dispatchers.IO) { tiles.associate { it.spec to backends.read(it.backend, torchOn, caffeineLeft) } }
                delay(1200)
            }
        }
        val actions = object : QsActions {
            override fun close() { scope.launch { shown = false; delay(220); closePanel() } }
            override fun tap(t: TileDef) {
                val st = states[t.spec] ?: return
                if (st.state == TileState.UNAVAILABLE) return
                val on = st.state == TileState.ON
                when (t.backend) {
                    Backend.TORCH -> backends.setTorch(!on)
                    Backend.SYNC -> backends.setSync(!on)
                    Backend.CAFFEINE -> cycleCaffeine()
                    Backend.ALARM, Backend.VPN -> { longPress(t); return }
                    else -> backends.toggle(t.backend, on)?.let { cmd -> scope.launch(Dispatchers.IO) { Root.run(cmd) } }
                }
                // optimistic, like systemui; the next poll corrects it if the command didn't take
                if (t.backend != Backend.CAFFEINE) states = states + (t.spec to st.copy(state = if (on) TileState.OFF else TileState.ON, secondary = null))
            }
            override fun longPress(t: TileDef) {
                if (t.backend == Backend.CAFFEINE) { caffeineForever(); return }
                t.settings?.let { open(Intent(it)) }
            }
            override fun brightness(gamma: Float) {
                bright = gamma; auto = false
                val lin = Tiles.gammaToLinear((gamma * Tiles.GAMMA_SPACE_MAX).toInt(), 1, 255)
                scope.launch(Dispatchers.IO) { backends.setBrightness(lin) }
            }
            override fun toggleAuto() { auto = !auto; val a = auto; scope.launch(Dispatchers.IO) { backends.setAutoBrightness(a) } }
            override fun edit() { open(Intent(this@OverlayService, MainActivity::class.java).putExtra("screen", "CONTROL")) }
            override fun settings() { open(Intent(android.provider.Settings.ACTION_SETTINGS)) }
            override fun power() { close(); scope.launch(Dispatchers.IO) { Root.run("input keyevent --longpress 26") } }
        }
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = if (shown) 0.35f else 0f))) {
            QsPanel(QsUi(tiles, states, bright, auto, colors, sysui), actions, shown)
        }
    }

    private fun open(i: Intent) {
        try { startActivity(i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (_: Throwable) {}
        closePanel()
    }

    // ---------- caffeine (lineage CaffeineTile: 5, 10, 30 minutes, until turned off, off) ----------

    private var caffeineIndex = -1
    private var caffeineLastClick = -1L

    private fun cycleCaffeine() {
        val now = android.os.SystemClock.elapsedRealtime()
        caffeineIndex = Tiles.caffeineClick(caffeineLeft != null, caffeineIndex, caffeineLastClick, now)
        caffeineLastClick = now
        startCaffeine()
    }

    /** CaffeineTile.handleLongClick: straight to infinity */
    private fun caffeineForever() { caffeineIndex = Tiles.CAFFEINE_DURATIONS.size - 1; startCaffeine() }

    private fun startCaffeine() {
        caffeineJob?.cancel()
        if (caffeineIndex < 0) { caffeineLeft = null; removeView(caffeineView); caffeineView = null; return }
        val secs = Tiles.CAFFEINE_DURATIONS[caffeineIndex]
        caffeineLeft = secs
        if (caffeineView == null) {
            // a 1px window that only exists to hold FLAG_KEEP_SCREEN_ON (lineage holds a screen-bright wakelock)
            caffeineView = View(this).also {
                wm.addView(it, params(1, 1, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, Gravity.TOP or Gravity.START))
            }
        }
        if (secs > 0) caffeineJob = scope.launch {
            while (isActive && (caffeineLeft ?: 0) > 0) { delay(1000); caffeineLeft = (caffeineLeft ?: 1) - 1 }
            caffeineIndex = -1; caffeineLeft = null; removeView(caffeineView); caffeineView = null
        }
    }

    override fun onDestroy() {
        BarConfig.prefs(this).unregisterOnSharedPreferenceChangeListener(prefsListener)
        try { getSystemService(CameraManager::class.java).unregisterTorchCallback(torchCb) } catch (_: Throwable) {}
        removeView(handle); removeView(panel); removeView(caffeineView)
        owner.stop()
        scope.cancel()
        super.onDestroy()
    }
}

/** starts the overlay service again after a reboot if quick settings is on */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED && BarConfig.load(ctx).needsService)
            ctx.startForegroundService(Intent(ctx, OverlayService::class.java))
    }
}
