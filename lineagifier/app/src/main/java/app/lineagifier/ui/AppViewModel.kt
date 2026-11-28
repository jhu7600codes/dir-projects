package app.lineagifier.ui

import android.app.Application
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.lineagifier.catalog.Catalog
import app.lineagifier.catalog.Entry
import app.lineagifier.catalog.Installable
import app.lineagifier.install.Actions
import app.lineagifier.install.Decision
import app.lineagifier.install.Safety
import app.lineagifier.shell.Root
import app.lineagifier.shell.RootStatus
import app.lineagifier.system.PackageState
import app.lineagifier.system.Pm
import app.lineagifier.tweaks.Tweak
import app.lineagifier.tweaks.Tweaks
import app.lineagifier.undo.Restore
import app.lineagifier.undo.UndoLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.content.Intent
import app.lineagifier.microg.Microg
import app.lineagifier.statusbar.BarConfig
import app.lineagifier.statusbar.OverlayService
import app.lineagifier.statusbar.Sizing
import app.lineagifier.undo.Kind
import java.io.File

enum class Screen { HOME, PICK, TWEAKS, APPLY, MANAGE, MICROG, STATUSBAR, CONTROL }

enum class StepState { WAITING, RUNNING, OK, FAILED, SKIPPED }

data class Step(val title: String, val state: StepState = StepState.WAITING, val detail: String = "", val progress: Float? = null)

data class StockRow(val entry: Entry, val stock: String, val decision: Decision)

data class UiState(
    val screen: Screen = Screen.HOME,
    val root: RootStatus? = null,
    val catalog: Catalog? = null,
    val catalogError: String? = null,
    val pkgs: PackageState? = null,
    val pkgError: String? = null,
    /** entry id -> package id of the chosen installable */
    val picks: Map<String, String> = emptyMap(),
    val tweaks: List<Tweak> = emptyList(),
    val tweaksLoading: Boolean = false,
    /** tweak id -> chosen value */
    val tweakPicks: Map<String, String> = emptyMap(),
    val steps: List<Step> = emptyList(),
    val running: Boolean = false,
    val stockRows: List<StockRow> = emptyList(),
    val manageBusy: Boolean = false,
    val manageMessage: String? = null,
    val pendingUndo: Int = 0,
    val bar: BarConfig = BarConfig(),
    val barBusy: Boolean = false,
    val barMessage: String? = null,
    val screenPx: Pair<Int, Int> = 0 to 0,
    val microg: Microg.State? = null,
    val microgBusy: Boolean = false,
    val microgProgress: String? = null,
    val microgFraction: Float? = null,
    val microgSteps: List<Microg.Step> = emptyList(),
    val withGsf: Boolean = false,
)

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val _s = MutableStateFlow(UiState())
    val state = _s.asStateFlow()

    private val log = UndoLog(File(app.filesDir, "undo-log.json"))
    private val actions = Actions(app, Root, log)
    private val tweaks = Tweaks(Root, log, Build.VERSION.SDK_INT)

    init {
        try {
            val text = app.assets.open("catalog.json").bufferedReader().use { it.readText() }
            _s.update { it.copy(catalog = Catalog.parse(text)) }
        } catch (t: Throwable) {
            _s.update { it.copy(catalogError = "catalog.json didn't load: ${t.message}") }
        }
        _s.update { it.copy(bar = BarConfig.load(app), screenPx = Sizing.realScreen(app)) }
        refreshRoot()
    }

    fun refreshRoot() = viewModelScope.launch {
        val st = withContext(Dispatchers.IO) { Root.check() }
        _s.update { it.copy(root = st, pendingUndo = log.pending().size) }
        if (st == RootStatus.READY) {
            refreshPackages()
            // bring the overlay back after an app update or a force stop
            if (_s.value.bar.needsService) app.startForegroundService(Intent(app, OverlayService::class.java))
        }
    }

    fun go(screen: Screen) {
        _s.update { it.copy(screen = screen) }
        when (screen) {
            Screen.PICK -> refreshPackages()
            Screen.TWEAKS -> if (_s.value.tweaks.isEmpty()) loadTweaks()
            Screen.MANAGE -> refreshManage()
            Screen.MICROG -> refreshMicrog()
            else -> {}
        }
    }

    fun refreshPackages() = viewModelScope.launch {
        val r = Pm.read(Root)
        _s.update { it.copy(pkgs = r.getOrNull(), pkgError = r.exceptionOrNull()?.message) }
    }

    fun pick(entryId: String, pkg: String?) = _s.update {
        it.copy(picks = if (pkg == null) it.picks - entryId else it.picks + (entryId to pkg))
    }

    fun pickTweak(id: String, value: String?) = _s.update {
        it.copy(tweakPicks = if (value == null) it.tweakPicks - id else it.tweakPicks + (id to value))
    }

    private fun loadTweaks() = viewModelScope.launch {
        _s.update { it.copy(tweaksLoading = true) }
        val t = tweaks.probe()
        _s.update { it.copy(tweaks = t, tweaksLoading = false) }
    }

    private fun chosenInstallables(): List<Pair<Entry, Installable>> {
        val cat = _s.value.catalog ?: return emptyList()
        return cat.entries.mapNotNull { e ->
            val p = _s.value.picks[e.id] ?: return@mapNotNull null
            e.installables().firstOrNull { it.packageId == p }?.let { e to it }
        }
    }

    private fun setStep(i: Int, f: (Step) -> Step) = _s.update { s ->
        s.copy(steps = s.steps.mapIndexed { idx, st -> if (idx == i) f(st) else st })
    }

    fun apply() {
        if (_s.value.running) return
        val apps = chosenInstallables()
        val tw = _s.value.tweakPicks.toList()
        val steps = buildList {
            apps.forEach { (e, it) ->
                add(Step("install ${it.name}"))
                if (e.role != null) add(Step("make ${it.name} the default"))
            }
            tw.forEach { (id, v) ->
                val t = _s.value.tweaks.firstOrNull { it.id == id }
                add(Step("${t?.title ?: id}: ${t?.options?.firstOrNull { it.value == v }?.label ?: v}"))
            }
        }
        _s.update { it.copy(steps = steps, running = true, screen = Screen.APPLY) }

        viewModelScope.launch {
            var i = 0
            for ((e, item) in apps) {
                val installIdx = i++
                setStep(installIdx) { it.copy(state = StepState.RUNNING) }
                val r = actions.install(item) { what, p -> setStep(installIdx) { it.copy(detail = what, progress = p) } }
                setStep(installIdx) { it.copy(state = if (r.isSuccess) StepState.OK else StepState.FAILED, detail = r.fold({ it }, { it.message ?: "$it" }), progress = null) }
                if (e.role != null) {
                    val roleIdx = i++
                    if (r.isFailure) { setStep(roleIdx) { it.copy(state = StepState.SKIPPED, detail = "install failed") }; continue }
                    setStep(roleIdx) { it.copy(state = StepState.RUNNING) }
                    val d = actions.setDefault(e, item.packageId)
                    setStep(roleIdx) { it.copy(state = if (d.isSuccess) StepState.OK else StepState.FAILED, detail = d.fold({ it }, { it.message ?: "$it" })) }
                }
            }
            for ((id, v) in tw) {
                val idx = i++
                setStep(idx) { it.copy(state = StepState.RUNNING) }
                val r = tweaks.apply(id, v)
                setStep(idx) { it.copy(state = if (r.isSuccess) StepState.OK else StepState.FAILED, detail = r.fold({ it }, { it.message ?: "$it" })) }
            }
            _s.update { it.copy(running = false, pendingUndo = log.pending().size) }
            refreshPackages()
        }
    }

    fun refreshManage() = viewModelScope.launch {
        val cat = _s.value.catalog ?: return@launch
        val pkgs = Pm.read(Root).getOrElse { e -> _s.update { it.copy(manageMessage = e.message) }; return@launch }
        val defaults = actions.defaults(cat.entries.mapNotNull { it.role })
        // only stock apps that really exist on this image get a row
        val rows = cat.entries.flatMap { e -> e.replaces.filter { pkgs.isOnImage(it) }.map { StockRow(e, it, Safety.canRemove(it, e, pkgs, defaults)) } }
        _s.update { it.copy(pkgs = pkgs, stockRows = rows, pendingUndo = log.pending().size) }
    }

    fun removeStock(row: StockRow, how: Actions.Removal) = viewModelScope.launch {
        _s.update { it.copy(manageBusy = true, manageMessage = null) }
        val r = actions.removeStock(row.entry, row.stock, how)
        _s.update { it.copy(manageBusy = false, manageMessage = r.fold({ it }, { "refused: ${it.message}" })) }
        refreshManage()
    }

    fun setDefault(row: StockRow) = viewModelScope.launch {
        val repl = Safety.installedReplacement(row.entry, _s.value.pkgs ?: return@launch) ?: return@launch
        _s.update { it.copy(manageBusy = true, manageMessage = null) }
        val r = actions.setDefault(row.entry, repl)
        _s.update { it.copy(manageBusy = false, manageMessage = r.fold({ it }, { "failed: ${it.message}" })) }
        refreshManage()
    }

    fun restoreEverything() = viewModelScope.launch {
        _s.update { it.copy(manageBusy = true, manageMessage = null) }
        val lines = Restore.everything(log, Root)
        val bad = lines.filter { !it.ok }
        val msg = if (lines.isEmpty()) "nothing to restore"
        else if (bad.isEmpty()) "restored ${lines.size} change(s)"
        else "restored ${lines.size - bad.size}/${lines.size}, failed:\n" + bad.joinToString("\n") { "${it.entry.kind} ${it.entry.target}: ${it.message}" }
        _s.update { it.copy(manageBusy = false, manageMessage = msg, tweaks = emptyList()) }
        refreshManage()
    }

    // ---------- status bar, sizing, control center ----------

    private val app get() = getApplication<Application>()

    private fun barMsg(m: String?) = _s.update { it.copy(barMessage = m) }

    /** saves the new config, then makes the system match it */
    fun updateBar(f: (BarConfig) -> BarConfig) = viewModelScope.launch {
        val old = _s.value.bar
        val new = f(old)
        _s.update { it.copy(bar = new, barBusy = true, barMessage = null) }
        val msgs = mutableListOf<String>()
        withContext(Dispatchers.IO) {
            if (new.needsService && !old.needsService) grantOverlay()?.let { msgs += it }
            if (new.sizingEnabled != old.sizingEnabled || (new.sizingEnabled && (new.safeTopPt != old.safeTopPt || new.padPt != old.padPt))) {
                val r = if (new.sizingEnabled) {
                    val (w, h) = _s.value.screenPx
                    Sizing.apply(Root, log, Sizing.iphone13(w, h, new.safeTopPt, new.padPt))
                } else Sizing.disable(Root)
                msgs += r.fold({ it }, { "sizing failed: ${it.message}" })
            }
        }
        BarConfig.save(app, new)
        if (new.needsService) app.startForegroundService(Intent(app, OverlayService::class.java))
        _s.update { it.copy(barBusy = false, barMessage = msgs.joinToString("\n").ifBlank { null }, pendingUndo = log.pending().size) }
    }

    /** overlay windows + the service notification, granted with root so nothing has to be clicked */
    private suspend fun grantOverlay(): String? {
        val pkg = app.packageName
        val before = Root.run("appops get $pkg SYSTEM_ALERT_WINDOW").out.substringAfter(": ").substringBefore(";").trim().ifEmpty { "default" }
        if (before != "allow") {
            log.add(Kind.appop, "$pkg/SYSTEM_ALERT_WINDOW", previous = if (before in setOf("allow", "ignore", "deny", "default", "foreground")) before else "default")
            Root.run("appops set $pkg SYSTEM_ALERT_WINDOW allow")
        }
        Root.run("pm grant $pkg android.permission.POST_NOTIFICATIONS")
        return if (android.provider.Settings.canDrawOverlays(app)) null else "couldn't get the overlay permission"
    }

    // ---------- microg ----------

    fun refreshMicrog() = viewModelScope.launch {
        val st = withContext(Dispatchers.IO) { Microg.state(app, Root) }
        _s.update { it.copy(microg = st) }
    }

    fun setGsf(v: Boolean) = _s.update { it.copy(withGsf = v) }

    fun installMicrog() = viewModelScope.launch {
        if (_s.value.microgBusy) return@launch
        _s.update { it.copy(microgBusy = true, microgSteps = emptyList(), microgProgress = "checking") }
        val steps = withContext(Dispatchers.IO) {
            Microg.install(app, Root, log, _s.value.withGsf) { what, f -> _s.update { it.copy(microgProgress = what, microgFraction = f) } }
        }
        _s.update { it.copy(microgBusy = false, microgSteps = steps, microgProgress = null, microgFraction = null, pendingUndo = log.pending().size) }
        refreshMicrog()
    }

    fun reboot() = viewModelScope.launch(Dispatchers.IO) { Root.run("svc power reboot || reboot") }
}
