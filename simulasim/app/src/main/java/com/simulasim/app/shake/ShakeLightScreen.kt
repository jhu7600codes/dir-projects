package com.simulasim.app.shake

import android.app.Activity
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.WindowManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simulasim.app.ui.Accent
import com.simulasim.app.ui.Chip
import com.simulasim.app.ui.Green
import com.simulasim.app.ui.Red
import com.simulasim.app.ui.RowGap
import com.simulasim.app.ui.SimCard
import com.simulasim.app.ui.StatBar
import com.simulasim.app.ui.Surface2
import com.simulasim.app.ui.TextDim
import com.simulasim.app.ui.TextMain
import com.simulasim.app.ui.TopBar
import com.simulasim.app.ui.Vspace
import com.simulasim.app.ui.Yellow
import kotlinx.coroutines.delay
import kotlin.math.sqrt

private class Torch(ctx: Context) {
    private val cm = ctx.getSystemService(CameraManager::class.java)
    val id: String? = try {
        cm?.cameraIdList?.firstOrNull { cm.getCameraCharacteristics(it).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true }
    } catch (e: Exception) {
        null
    }
    val available get() = id != null
    private var last: Boolean? = null

    fun set(on: Boolean) {
        if (last == on) return
        last = on
        val i = id ?: return
        try {
            cm?.setTorchMode(i, on)
        } catch (_: Exception) {
            // camera busy (another app has it) — nothing useful to do
        }
    }
}

private enum class Mode(val label: String) { STEADY("steady"), STROBE("strobe"), SOS("sos") }

// morse SOS: ... --- ... in units of ~200ms (on, off) pairs
private val SOS = listOf(1, 1, 1, 1, 1, 3, 3, 1, 3, 1, 3, 3, 1, 1, 1, 1, 1, 7)

@Composable
fun ShakeLightScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val torch = remember { Torch(ctx) }
    val vibrator = remember { ctx.getSystemService(Vibrator::class.java) }

    var on by remember { mutableStateOf(false) }
    var sensitivity by remember { mutableFloatStateOf(0.5f) }
    var useScreen by remember { mutableStateOf(!torch.available) }
    var mode by remember { mutableStateOf(Mode.STEADY) }
    var strobeHz by remember { mutableFloatStateOf(6f) }
    var blink by remember { mutableStateOf(true) }
    var gForce by remember { mutableFloatStateOf(1f) }
    var peak by remember { mutableFloatStateOf(1f) }
    var toggles by remember { mutableIntStateOf(0) }
    var hasSensor by remember { mutableStateOf(true) }

    val threshold = 2.8f - sensitivity * 1.5f // 1.3g (twitchy) .. 2.8g (properly shake it)
    val currentThreshold by rememberUpdatedState(threshold)

    fun buzz(ms: Long) {
        try {
            vibrator?.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE))
        } catch (_: Exception) {}
    }

    DisposableEffect(Unit) {
        val sm = ctx.getSystemService(SensorManager::class.java)
        val accel = sm?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        hasSensor = accel != null
        var firstPeak = 0L
        var lastPeak = 0L
        val listener = object : SensorEventListener {
            override fun onSensorChanged(e: SensorEvent) {
                val (x, y, z) = Triple(e.values[0], e.values[1], e.values[2])
                val g = sqrt(x * x + y * y + z * z) / SensorManager.GRAVITY_EARTH
                gForce = g
                if (g > peak) peak = g
                if (g < currentThreshold) return
                val now = SystemClock.elapsedRealtime()
                if (now - lastPeak < 180) return // same jerk, still ringing
                lastPeak = now
                // two hard jerks within ~0.8s = toggle. one alone does nothing,
                // so it doesn't flick on every time you drop the phone in your pocket
                if (firstPeak != 0L && now - firstPeak < 800) {
                    on = !on
                    toggles++
                    firstPeak = 0L
                    buzz(if (on) 60 else 30)
                } else {
                    firstPeak = now
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        if (accel != null) sm.registerListener(listener, accel, SensorManager.SENSOR_DELAY_GAME)
        onDispose {
            sm?.unregisterListener(listener)
            torch.set(false)
        }
    }

    // drive the blink pattern for strobe/sos
    LaunchedEffect(on, mode, strobeHz) {
        blink = true
        if (!on) return@LaunchedEffect
        when (mode) {
            Mode.STEADY -> {}
            Mode.STROBE -> while (true) {
                blink = !blink
                delay((500f / strobeHz).toLong())
            }
            Mode.SOS -> while (true) {
                for (i in SOS.indices) {
                    blink = i % 2 == 0
                    delay(SOS[i] * 200L)
                }
            }
        }
    }

    val lightOn = on && blink

    LaunchedEffect(lightOn, useScreen) {
        torch.set(lightOn && !useScreen)
    }

    // full brightness for screen-light mode, back to normal otherwise
    val activity = ctx as? Activity
    DisposableEffect(on, useScreen) {
        val w = activity?.window
        if (w != null) {
            val lp = w.attributes
            lp.screenBrightness = if (on && useScreen) 1f else WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
            w.attributes = lp
            if (on) w.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            w?.let {
                val lp = it.attributes
                lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                it.attributes = lp
                it.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            TopBar("shakelight sim", onBack)
            Column(Modifier.padding(horizontal = 16.dp)) {
                Lamp(lightOn) { on = !on; toggles++; buzz(30) }
                Vspace(6)
                Text(
                    if (on) "ON — shake shake to turn off" else "shake the phone twice (chop chop) to turn on",
                    color = if (on) Yellow else TextDim,
                    fontSize = 14.sp,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
                Text("or tap the bulb", color = TextDim.copy(alpha = 0.6f), fontSize = 12.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
                Vspace(16)

                SimCard(Modifier.fillMaxWidth()) {
                    Text("g-force meter", color = TextMain, fontWeight = FontWeight.Bold)
                    Vspace(8)
                    StatBar("now", gForce / 4f, if (gForce >= threshold) Red else Green, valueText = "%.2fg".format(gForce))
                    Vspace(6)
                    StatBar("trigger at", threshold / 4f, Yellow, valueText = "%.2fg".format(threshold))
                    Vspace(6)
                    Text("peak: %.2fg · toggles: %d".format(peak, toggles), color = TextDim, fontSize = 12.sp)
                    if (!hasSensor) Text("no accelerometer found. tap mode only", color = Red, fontSize = 12.sp)
                }
                Vspace(10)

                SimCard(Modifier.fillMaxWidth()) {
                    Text("sensitivity", color = TextMain, fontWeight = FontWeight.Bold)
                    Slider(
                        value = sensitivity, onValueChange = { sensitivity = it },
                        colors = SliderDefaults.colors(thumbColor = Accent, activeTrackColor = Accent, inactiveTrackColor = Surface2),
                    )
                    Row {
                        Text("lazy", color = TextDim, fontSize = 11.sp, modifier = Modifier.weight(1f))
                        Text("twitchy", color = TextDim, fontSize = 11.sp)
                    }
                    Vspace(12)
                    Text("mode", color = TextMain, fontWeight = FontWeight.Bold)
                    Vspace(6)
                    Row(horizontalArrangement = RowGap) {
                        Mode.entries.forEach { m -> Chip(m.label, mode == m) { mode = m } }
                    }
                    if (mode == Mode.STROBE) {
                        Vspace(8)
                        Text("strobe speed: %.0f hz".format(strobeHz), color = TextDim, fontSize = 12.sp)
                        Slider(
                            value = strobeHz, onValueChange = { strobeHz = it }, valueRange = 1f..12f,
                            colors = SliderDefaults.colors(thumbColor = Yellow, activeTrackColor = Yellow, inactiveTrackColor = Surface2),
                        )
                    }
                    Vspace(12)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("use screen instead of flash", color = TextMain, fontWeight = FontWeight.Bold)
                            Text(
                                if (torch.available) "white screen at max brightness" else "no flash found on this phone, screen only",
                                color = TextDim, fontSize = 12.sp,
                            )
                        }
                        Switch(
                            checked = useScreen,
                            onCheckedChange = { if (torch.available) useScreen = it },
                            colors = SwitchDefaults.colors(checkedTrackColor = Accent),
                        )
                    }
                }
                Vspace(24)
            }
        }

        if (lightOn && useScreen) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.White)
                    .clickable { on = false },
                contentAlignment = Alignment.BottomCenter,
            ) {
                Text("tap or shake to turn off", color = Color.LightGray, modifier = Modifier.padding(32.dp))
            }
        } else if (on && useScreen) {
            // strobe/sos "off" frames in screen mode: black, so the blink actually reads
            Box(Modifier.fillMaxSize().background(Color.Black).clickable { on = false })
        }
    }
}

@Composable
private fun Lamp(lit: Boolean, onTap: () -> Unit) {
    val glow by animateFloatAsState(if (lit) 1f else 0f, label = "glow")
    Box(
        Modifier
            .fillMaxWidth()
            .height(240.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val c = Offset(size.width / 2, size.height / 2)
            if (glow > 0f) {
                drawCircle(
                    Brush.radialGradient(
                        listOf(Yellow.copy(alpha = 0.55f * glow), Yellow.copy(alpha = 0.12f * glow), Color.Transparent),
                        center = c, radius = size.minDimension * 0.75f,
                    ),
                    radius = size.minDimension * 0.75f, center = c,
                )
            }
        }
        Box(
            Modifier
                .size(130.dp)
                .background(
                    Brush.radialGradient(
                        if (lit) listOf(Color.White, Yellow) else listOf(Surface2, Color(0xFF15161C))
                    ),
                    shape = androidx.compose.foundation.shape.CircleShape,
                )
                .clickable(onClick = onTap),
            contentAlignment = Alignment.Center,
        ) {
            Text(if (lit) "ON" else "OFF", color = if (lit) Color(0xFF5A4500) else TextDim, fontWeight = FontWeight.Black, fontSize = 26.sp)
        }
    }
}
