package app.lineagifier.ui

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import app.lineagifier.catalog.Entry
import app.lineagifier.catalog.Status
import app.lineagifier.install.Actions

@Composable
fun Page(title: String, subtitle: String? = null, bottom: @Composable RowScope.() -> Unit = {}, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxSize().systemBarsPadding().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(20.dp))
        Text(title, fontSize = 28.sp, fontWeight = FontWeight.Light, color = MaterialTheme.colorScheme.onBackground)
        if (subtitle != null) Text(subtitle, color = Dim, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
        Spacer(Modifier.height(16.dp))
        Column(Modifier.weight(1f)) { content() }
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End), content = bottom)
    }
}

@Composable
fun Card(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) =
    Column(modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Surface1).padding(16.dp), content = content)

@Composable
fun Chip(text: String, color: Color) =
    Text(text, fontSize = 11.sp, color = color, modifier = Modifier.clip(RoundedCornerShape(50)).background(color.copy(alpha = 0.15f)).padding(horizontal = 8.dp, vertical = 2.dp))

@Composable
fun PickScreen(s: UiState, vm: AppViewModel) {
    val cat = s.catalog ?: return
    Page("lineage apps", "lineage builds from apkmirror, verified prebuilts from f-droid", bottom = {
        OutlinedButton(onClick = { vm.go(Screen.HOME) }) { Text("back") }
        Button(onClick = { vm.go(Screen.TWEAKS) }) { Text("next: tweaks") }
    }) {
        if (s.apkBusy) LinearProgressIndicator(Modifier.fillMaxWidth().padding(bottom = 8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(cat.entries, key = { it.id }) { e -> EntryCard(e, s, vm) }
        }
    }
    s.apkMessage?.let { m ->
        AlertDialog(onDismissRequest = { vm.dismissApk() }, title = { Text(if (m.startsWith("refused") || m.startsWith("failed")) "didn't install" else "done") },
            text = { Text(m) }, confirmButton = {
                if ("reboot" in m) TextButton(onClick = { vm.dismissApk(); vm.reboot() }) { Text("reboot now") }
                TextButton(onClick = { vm.dismissApk() }) { Text("ok") }
            })
    }
    s.pendingSwap?.let { p ->
        AlertDialog(onDismissRequest = { vm.dismissApk() }, title = { Text("replace the stock ${p.entry.category}?") },
            text = {
                Text(buildString {
                    appendLine("${p.apk.packageId} ${p.apk.versionName} (min sdk ${p.apk.minSdk})")
                    appendLine("signed by sha256 ${p.apk.certSha256.firstOrNull()?.take(16)}… compare it with what apkmirror shows.")
                    appendLine()
                    appendLine(p.plan.reason + ".")
                    if (p.backupLauncher != null) appendLine("${p.backupLauncher} becomes home first, so you keep a launcher if trebuchet doesn't start.")
                    appendLine("the stock app's data is kept. restore removes the module and brings the stock app back after a reboot.")
                })
            },
            confirmButton = { TextButton(onClick = { vm.confirmSwap() }) { Text("replace") } },
            dismissButton = { TextButton(onClick = { vm.dismissApk() }) { Text("cancel") } })
    }
}

@Composable
private fun EntryCard(e: Entry, s: UiState, vm: AppViewModel) {
    Card {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(e.name, fontWeight = FontWeight.Medium)
                Text(e.category, color = Dim, fontSize = 12.sp)
            }
            if (e.apkmirror != null && e.status != Status.prebuilt) Chip("apkmirror", Good) else when (e.status) {
                Status.prebuilt -> Chip("prebuilt", Good)
                Status.build_from_source -> Chip("build from source", Color(0xFFFFB74D))
                Status.same_as_stock -> Chip("same as stock", TealLight)
                Status.unsupported -> Chip("not possible", Bad)
                Status.system_only -> Chip("root: system app", Color(0xFFFFB74D))
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(e.notes, color = Dim, fontSize = 13.sp)
        Text(e.source, color = TealLight, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
        val pkgs = s.pkgs
        if (pkgs != null && e.replaces.isNotEmpty()) {
            Spacer(Modifier.height(4.dp))
            e.replaces.forEach { p ->
                val st = when { !pkgs.isOnImage(p) -> "not on this image"; !pkgs.isEnabled(p) -> "disabled/removed"; else -> "on this image" }
                Text("stock: $p ($st)", color = Dim, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
            }
        }
        if (e.apkmirror != null) LineageApkRow(e, s, vm)
        val opts = e.installables()
        if (opts.isNotEmpty()) {
            Spacer(Modifier.height(6.dp))
            val chosen = s.picks[e.id]
            opts.forEach { o ->
                val installed = pkgs?.isInstalled(o.packageId) == true
                val tooNew = Build.VERSION.SDK_INT < o.download.minSdk
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                        .clickable(enabled = !tooNew) { vm.pick(e.id, if (chosen == o.packageId) null else o.packageId) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = chosen == o.packageId, enabled = !tooNew, onCheckedChange = { vm.pick(e.id, if (it) o.packageId else null) })
                    Column(Modifier.weight(1f)) {
                        Text(o.name + if (o.isLineage) "" else " (not lineage, closest prebuilt)", fontSize = 14.sp)
                        Text("${o.packageId} ${o.download.versionName} · ${o.download.size / 1_000_000} mb" +
                            (if (installed) " · installed" else "") + (if (tooNew) " · needs sdk ${o.download.minSdk}" else ""),
                            color = Dim, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun LineageApkRow(e: Entry, s: UiState, vm: AppViewModel) {
    val uri = androidx.compose.ui.platform.LocalUriHandler.current
    val pick = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenDocument()) { u ->
        if (u != null) vm.pickedApk(e, u)
    }
    Spacer(Modifier.height(6.dp))
    Text("the real lineage build is on apkmirror (pulled from lineage roms). download the android 13 / lineage 20 release in the browser, then pick the file here.",
        color = Dim, fontSize = 12.sp)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
        OutlinedButton(onClick = { uri.openUri(e.apkmirror!!) }) { Text("apkmirror", fontSize = 13.sp) }
        Button(onClick = { pick.launch(arrayOf("application/vnd.android.package-archive", "application/octet-stream", "*/*")) }, enabled = !s.apkBusy) {
            Text("install downloaded apk", fontSize = 13.sp)
        }
    }
}

@Composable
fun TweaksScreen(s: UiState, vm: AppViewModel) {
    Page("tweaks", "each command was tested on this image before it's offered", bottom = {
        OutlinedButton(onClick = { vm.go(if (s.picks.isEmpty()) Screen.HOME else Screen.PICK) }) { Text("back") }
        Button(onClick = { vm.apply() }, enabled = !s.tweaksLoading && (s.picks.isNotEmpty() || s.tweakPicks.isNotEmpty())) {
            Text("apply ${s.picks.size} app(s), ${s.tweakPicks.size} tweak(s)")
        }
    }) {
        if (s.tweaksLoading) { LinearProgressIndicator(Modifier.fillMaxWidth()); return@Page }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(s.tweaks, key = { it.id }) { t ->
                Card {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(t.title, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                        if (!t.available) Chip("unavailable", Bad) else t.current?.let { cur -> Chip("now: " + (t.options.firstOrNull { it.value == cur }?.label ?: if (cur == "null") "default" else cur.take(24)), Dim) }
                    }
                    Text(t.reason, color = Dim, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    if (t.available) {
                        Spacer(Modifier.height(4.dp))
                        val chosen = s.tweakPicks[t.id]
                        t.options.forEach { o ->
                            Row(Modifier.fillMaxWidth().clickable { vm.pickTweak(t.id, if (chosen == o.value) null else o.value) }, verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = chosen == o.value, onClick = { vm.pickTweak(t.id, if (chosen == o.value) null else o.value) })
                                Text(o.label, fontSize = 14.sp)
                            }
                        }
                        Text("tap again to leave it unchanged", color = Dim, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ApplyScreen(s: UiState, vm: AppViewModel) {
    val failed = s.steps.count { it.state == StepState.FAILED }
    Page(if (s.running) "applying" else "results",
        if (s.running) "don't close the app" else if (failed == 0) "all done" else "$failed step(s) failed, see below",
        bottom = {
            OutlinedButton(onClick = { vm.go(Screen.HOME) }, enabled = !s.running) { Text("home") }
            Button(onClick = { vm.go(Screen.MANAGE) }, enabled = !s.running) { Text("next: remove stock apps") }
        }) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(s.steps) { st ->
                Card {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(st.title, modifier = Modifier.weight(1f), fontSize = 14.sp)
                        when (st.state) {
                            StepState.WAITING -> Chip("waiting", Dim)
                            StepState.RUNNING -> Chip("running", TealLight)
                            StepState.OK -> Chip("ok", Good)
                            StepState.FAILED -> Chip("failed", Bad)
                            StepState.SKIPPED -> Chip("skipped", Dim)
                        }
                    }
                    st.progress?.let { if (st.state == StepState.RUNNING) LinearProgressIndicator(progress = { it }, modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) }
                    if (st.detail.isNotEmpty()) Text(st.detail, color = if (st.state == StepState.FAILED) Bad else Dim, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun ManageScreen(s: UiState, vm: AppViewModel) {
    var confirmRestore by remember { mutableStateOf(false) }
    Page("stock apps", "a stock app can only go once its replacement is installed and default", bottom = {
        OutlinedButton(onClick = { vm.go(Screen.HOME) }, enabled = !s.manageBusy) { Text("home") }
        Button(onClick = { confirmRestore = true }, enabled = !s.manageBusy && s.pendingUndo > 0,
            colors = ButtonDefaults.buttonColors(containerColor = Teal, contentColor = Color.White)) { Text("restore everything (${s.pendingUndo})") }
    }) {
        if (s.manageBusy) LinearProgressIndicator(Modifier.fillMaxWidth())
        s.manageMessage?.let { Text(it, color = if (it.startsWith("refused") || it.startsWith("failed")) Bad else Good, fontSize = 13.sp, modifier = Modifier.padding(bottom = 8.dp)) }
        if (s.stockRows.isEmpty() && !s.manageBusy) Text("none of the catalog's stock apps were found on this image", color = Dim)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(s.stockRows, key = { it.stock }) { row ->
                val enabled = s.pkgs?.isEnabled(row.stock) == true
                Card {
                    Text(row.stock, fontFamily = FontFamily.Monospace, fontSize = 14.sp)
                    Text("${row.entry.category} · ${if (enabled) "enabled" else "disabled/removed"}", color = Dim, fontSize = 12.sp)
                    Text(row.decision.reason, color = if (row.decision.allowed) Good else Dim, fontSize = 13.sp)
                    Spacer(Modifier.height(6.dp))
                    if (!row.decision.allowed && row.decision.replacement != null && row.entry.role != null)
                        Button(onClick = { vm.setDefault(row) }, enabled = !s.manageBusy, modifier = Modifier.padding(bottom = 6.dp)) { Text("make ${row.decision.replacement} default") }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { vm.removeStock(row, Actions.Removal.DISABLE) }, enabled = row.decision.allowed && !s.manageBusy) { Text("disable") }
                        OutlinedButton(onClick = { vm.removeStock(row, Actions.Removal.UNINSTALL) }, enabled = row.decision.allowed && !s.manageBusy) { Text("remove") }
                    }
                }
            }
        }
    }
    if (confirmRestore) AlertDialog(
        onDismissRequest = { confirmRestore = false },
        title = { Text("restore everything?") },
        text = { Text("re-enables and reinstalls every stock app lineagifier touched and puts back old settings, overlays and defaults. apps you installed stay installed.") },
        confirmButton = { TextButton(onClick = { confirmRestore = false; vm.restoreEverything() }) { Text("restore") } },
        dismissButton = { TextButton(onClick = { confirmRestore = false }) { Text("cancel") } },
    )
}
