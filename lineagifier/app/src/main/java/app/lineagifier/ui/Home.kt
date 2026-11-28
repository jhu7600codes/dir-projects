package app.lineagifier.ui

import android.os.Build
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.lineagifier.microg.Microg
import app.lineagifier.shell.RootStatus
import app.lineagifier.statusbar.BarConfig
import app.lineagifier.qs.Tiles
import app.lineagifier.statusbar.Sizing
import kotlin.math.roundToInt

@Composable
private fun FeatureRow(title: String, sub: String, checked: Boolean? = null, enabled: Boolean = true, onCheck: (Boolean) -> Unit = {}, onOpen: (() -> Unit)? = null) {
    Card(Modifier.clip(RoundedCornerShape(16.dp)).clickable(enabled = enabled && onOpen != null) { onOpen?.invoke() }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Medium, color = if (enabled) Color.Unspecified else Dim)
                Text(sub, color = Dim, fontSize = 13.sp)
            }
            if (checked != null) Switch(checked, onCheck, enabled = enabled)
            else if (onOpen != null) Text("›", fontSize = 24.sp, color = Dim)
        }
    }
}

@Composable
fun HomeScreen(s: UiState, vm: AppViewModel) {
    val ready = s.root == RootStatus.READY
    Page("lineagifier", "your twoyi, a little more lineage") {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Card {
                    Text("root", fontWeight = FontWeight.Medium)
                    val (msg, color) = when (s.root) {
                        null -> "checking…" to Dim
                        RootStatus.NO_SU -> "no su found. lineagifier needs root (kernelsu on twoyi)." to Bad
                        RootStatus.DENIED -> "su said no. allow lineagifier in kernelsu's superuser tab, then check again." to Color(0xFFFFB74D)
                        RootStatus.READY -> "ready, running as root" to Good
                    }
                    Text(msg, color = color, fontSize = 14.sp)
                    Text("android ${Build.VERSION.RELEASE} (sdk ${Build.VERSION.SDK_INT}) · ${Build.MODEL}", color = Dim, fontSize = 12.sp)
                    s.pkgs?.let { Text("${it.systemAll.size} system packages, ${it.installed.size} installed", color = Dim, fontSize = 12.sp) }
                    if (!ready) OutlinedButton(onClick = { vm.refreshRoot() }, modifier = Modifier.padding(top = 6.dp)) { Text("check again") }
                }
            }
            item { Section("apps") }
            item { FeatureRow("lineage apps", "install etar + closest prebuilts, set defaults", enabled = ready && s.catalog != null, onOpen = { vm.go(Screen.PICK) }) }
            item { FeatureRow("stock apps", "disable or remove stock apps once they're replaced", enabled = ready, onOpen = { vm.go(Screen.MANAGE) }) }
            item { FeatureRow("microG", microgLine(s.microg), enabled = ready, onOpen = { vm.go(Screen.MICROG) }) }
            item { Section("look") }
            item { FeatureRow("tweaks", "dark mode, animations, navigation, accent, icon shape", enabled = ready, onOpen = { vm.go(Screen.TWEAKS) }) }
            item { FeatureRow("iphone 13 status bar size", "bar height + padding fitted around the notch", checked = s.bar.sizingEnabled, enabled = ready && !s.barBusy,
                onCheck = { on -> vm.updateBar { it.copy(sizingEnabled = on) } }, onOpen = { vm.go(Screen.STATUSBAR) }) }
            item { FeatureRow("quick settings", "lineage 20's quick settings, from a handle on the right edge", checked = s.bar.ccEnabled, enabled = ready && !s.barBusy,
                onCheck = { on -> vm.updateBar { it.copy(ccEnabled = on) } }, onOpen = { vm.go(Screen.CONTROL) }) }
            s.barMessage?.let { m -> item { Text(m, color = if ("fail" in m || "couldn't" in m) Bad else Good, fontSize = 13.sp) } }
            item { Section("undo") }
            item { FeatureRow("restore everything", "${s.pendingUndo} change(s) logged", enabled = ready, onOpen = { vm.go(Screen.MANAGE) }) }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun Section(t: String) = Text(t, color = TealLight, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp, start = 4.dp))

private fun microgLine(m: Microg.State?): String = when {
    m == null -> "gmscore, signature spoofing via vector + fakegapps"
    m.gmsInstalled && m.spoofing == true -> "installed, signature spoofing works"
    m.gmsInstalled -> "installed, spoofing not active yet"
    m.gmsOurs -> "module installed, reboot to activate"
    else -> "not installed"
}

@Composable
fun MicrogScreen(s: UiState, vm: AppViewModel) {
    var confirm by remember { mutableStateOf(false) }
    val m = s.microg
    Page("microG", "google services replacement, the lineageos for microg way", bottom = {
        OutlinedButton(onClick = { vm.go(Screen.HOME) }, enabled = !s.microgBusy) { Text("back") }
        Button(onClick = { confirm = true }, enabled = !s.microgBusy && m != null && !(m.gmsInstalled && !m.gmsOurs)) { Text("install") }
    }) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Card {
                    Text("on this image", fontWeight = FontWeight.Medium)
                    if (m == null) Text("checking…", color = Dim) else {
                        Check("kernelsu (ksud)", m.ksud)
                        Check("microg module", m.gmsOurs)
                        Check("gmscore installed", m.gmsInstalled)
                        Check("zygisk: ${m.zygisk ?: "none"}", m.zygisk != null)
                        Check("xposed: ${m.xposed ?: "none"}", m.xposed != null)
                        Check("fakegapps", m.fakegapps)
                        Check("signature spoofing works", m.spoofing == true)
                        if (m.gmsInstalled && !m.gmsOurs) Text("another google services package is installed, lineagifier won't touch it", color = Bad, fontSize = 13.sp)
                    }
                }
            }
            item {
                Card {
                    Text("what install does", fontWeight = FontWeight.Medium)
                    Text("1. downloads gmscore 0.3.18 + companion from microg.org (sha256 checked against microg's signed index) and puts them in /system/priv-app with a kernelsu module\n" +
                        "2. neozygisk + vector (maintained lsposed) from github, checked against github's sha256, unless you already have them\n" +
                        "3. fakegapps, the xposed module that adds signature spoofing\n\n" +
                        "then: reboot, open vector from its notification, enable fakegapps for \"system framework\", reboot again.",
                        color = Dim, fontSize = 13.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(s.withGsf, { vm.setGsf(it) }); Text("also gsfproxy (only for very old apps)", fontSize = 13.sp)
                    }
                }
            }
            if (s.microgBusy) item {
                Card {
                    Text(s.microgProgress ?: "", fontSize = 13.sp)
                    s.microgFraction?.let { LinearProgressIndicator(progress = { it }, Modifier.fillMaxWidth().padding(top = 6.dp)) } ?: LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 6.dp))
                }
            }
            if (s.microgSteps.isNotEmpty()) item {
                Card {
                    s.microgSteps.forEach { st ->
                        Text((if (st.ok) "ok  " else "failed  ") + st.label, color = if (st.ok) Good else Bad, fontSize = 13.sp)
                        Text(st.detail, color = Dim, fontSize = 12.sp)
                    }
                    if (s.microgSteps.all { it.ok }) Button(onClick = { vm.reboot() }, modifier = Modifier.padding(top = 8.dp)) { Text("reboot now") }
                }
            }
        }
    }
    if (confirm) AlertDialog(
        onDismissRequest = { confirm = false },
        title = { Text("install microG?") },
        text = { Text("this adds kernelsu modules that hook the android framework. if twoyi doesn't boot afterwards, the modules can be removed from kernelsu or with lineagifier's restore, but if it won't boot at all you may need to reset the container. it downloads about 120 mb.") },
        confirmButton = { TextButton(onClick = { confirm = false; vm.installMicrog() }) { Text("install") } },
        dismissButton = { TextButton(onClick = { confirm = false }) { Text("cancel") } },
    )
}

@Composable
private fun Check(label: String, ok: Boolean) =
    Text((if (ok) "✓  " else "·  ") + label, color = if (ok) Good else Dim, fontSize = 14.sp)

@Composable
private fun <T> Choice(title: String, options: List<Pair<String, T>>, value: T, enabled: Boolean, onPick: (T) -> Unit) {
    Text(title, color = Dim, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEach { (label, v) ->
            FilterChip(selected = v == value, onClick = { onPick(v) }, enabled = enabled, label = { Text(label, fontSize = 12.sp) })
        }
    }
}

@Composable
private fun Toggle(title: String, value: Boolean, enabled: Boolean, onChange: (Boolean) -> Unit) =
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(title, fontSize = 14.sp, modifier = Modifier.weight(1f)); Switch(value, onChange, enabled = enabled)
    }

@Composable
fun StatusBarScreen(s: UiState, vm: AppViewModel) {
    val c = s.bar
    val en = !s.barBusy
    fun up(f: (BarConfig) -> BarConfig) = vm.updateBar(f)
    Page("status bar size", "fits android's own status bar to the iphone 13 screen", bottom = { OutlinedButton(onClick = { vm.go(Screen.HOME) }) { Text("back") } }) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Card {
                    Toggle("iphone 13 size", c.sizingEnabled, en) { on -> up { it.copy(sizingEnabled = on) } }
                    val (w, h) = s.screenPx
                    val spec = Sizing.iphone13(w, h, c.safeTopPt, c.padPt)
                    Text("screen ${w}x$h px → bar ${spec.heightPx}px, side padding ${spec.padStartPx}px", color = Dim, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    Text("these are ios' published iphone 13 numbers scaled to your screen. nudge them if it looks off.", color = Dim, fontSize = 12.sp)
                    var top by remember(c.safeTopPt) { mutableFloatStateOf(c.safeTopPt) }
                    Text("bar height: ${top.roundToInt()} pt", fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
                    Slider(top, { top = it }, valueRange = 20f..60f, enabled = en && c.sizingEnabled, onValueChangeFinished = { up { it.copy(safeTopPt = top) } })
                    var pad by remember(c.padPt) { mutableFloatStateOf(c.padPt) }
                    Text("side padding: ${pad.roundToInt()} pt", fontSize = 13.sp)
                    Slider(pad, { pad = it }, valueRange = 4f..60f, enabled = en && c.sizingEnabled, onValueChangeFinished = { up { it.copy(padPt = pad) } })
                }
            }
            s.barMessage?.let { m -> item { Text(m, color = if ("fail" in m) Bad else Good, fontSize = 13.sp) } }
        }
    }
}

@Composable
fun ControlScreen(s: UiState, vm: AppViewModel) {
    val c = s.bar
    val en = !s.barBusy
    Page("quick settings", "lineage 20's quick settings panel", bottom = { OutlinedButton(onClick = { vm.go(Screen.HOME) }) { Text("back") } }) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            item {
                Card {
                    Toggle("quick settings", c.ccEnabled, en) { on -> vm.updateBar { it.copy(ccEnabled = on) } }
                    Text("android's own pull-down belongs to systemui and can't be swapped without framework changes, so this panel opens from a small handle on the right edge, just under the status bar: swipe down on it or tap it. swipe down again in the panel to expand it, like lineage.", color = Dim, fontSize = 12.sp)
                    var hh by remember(c.ccHandleHeightDp) { mutableFloatStateOf(c.ccHandleHeightDp.toFloat()) }
                    Text("handle length: ${hh.roundToInt()} dp", fontSize = 13.sp, modifier = Modifier.padding(top = 6.dp))
                    Slider(hh, { hh = it }, valueRange = 60f..300f, enabled = en && c.ccEnabled, onValueChangeFinished = { vm.updateBar { it.copy(ccHandleHeightDp = hh.roundToInt()) } })
                }
            }
            item {
                Card {
                    Text("tiles", fontWeight = FontWeight.Medium)
                    Text("in panel order. the first 4 are the quick row. tiles your image can't do show up dimmed as unavailable, like on lineage.", color = Dim, fontSize = 12.sp)
                    c.ccTiles.forEachIndexed { i, spec ->
                        val t = Tiles.bySpec(spec) ?: return@forEachIndexed
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Text(t.fallbackLabel + if (t.lineage) "  ·  lineage" else "", fontSize = 14.sp, modifier = Modifier.weight(1f))
                            TextButton(onClick = { vm.updateBar { b -> b.copy(ccTiles = b.ccTiles.toMutableList().also { l -> if (i > 0) { l.removeAt(i); l.add(i - 1, spec) } }) } }, enabled = en && i > 0) { Text("↑") }
                            TextButton(onClick = { vm.updateBar { b -> b.copy(ccTiles = b.ccTiles - spec) } }, enabled = en) { Text("remove") }
                        }
                    }
                }
            }
            val missing = Tiles.ALL.filter { it.spec !in c.ccTiles }
            if (missing.isNotEmpty()) item {
                Card {
                    Text("add tiles", fontWeight = FontWeight.Medium)
                    missing.forEach { t ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Text(t.fallbackLabel + if (t.lineage) "  ·  lineage" else "", fontSize = 14.sp, modifier = Modifier.weight(1f))
                            TextButton(onClick = { vm.updateBar { b -> b.copy(ccTiles = b.ccTiles + t.spec) } }, enabled = en) { Text("add") }
                        }
                    }
                }
            }
            item {
                Text("not included: reading mode, livedisplay, profiles and powershare need lineage's hardware/sdk services; controls, wallet, cast, screen record, hotspot and usb tethering don't apply to a twoyi container.", color = Dim, fontSize = 12.sp)
            }
        }
    }
}
