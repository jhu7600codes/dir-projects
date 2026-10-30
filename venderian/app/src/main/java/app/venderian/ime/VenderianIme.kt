package app.venderian.ime

import android.content.Context
import android.inputmethodservice.InputMethodService
import android.text.InputType
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import app.venderian.VenderianApp
import app.venderian.convert.Converter
import app.venderian.convert.Sounds
import app.venderian.ui.theme.VenderianTheme
import kotlinx.coroutines.launch

/**
 * the venderian system keyboard. compose needs a lifecycle, so the service provides one
 * itself (an ime has no activity).
 */
class VenderianIme : InputMethodService(), LifecycleOwner, ViewModelStoreOwner, SavedStateRegistryOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedState = SavedStateRegistryController.create(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry
    override val viewModelStore = ViewModelStore()
    override val savedStateRegistry: SavedStateRegistry get() = savedState.savedStateRegistry

    private val prefs by lazy { getSharedPreferences("keyboard", Context.MODE_PRIVATE) }

    /** everything the keyboard ui shows */
    var state by mutableStateOf(ImeState())
        @androidx.annotation.VisibleForTesting internal set
    var converter by mutableStateOf<Converter?>(null)
        private set

    /** the text the translate button replaced, so it can be undone */
    private var undo: Pair<String, String>? = null
    private var cursor = 0
    /** where the last translation ended: text before it is already venderian */
    private var translatedUpTo = -1

    override fun onCreate() {
        super.onCreate()
        savedState.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
        state = ImeState(
            layout = Layout.entries.getOrElse(prefs.getInt("layout", 0)) { Layout.Qwerty },
            translate = prefs.getBoolean("translate", false),
            outputScript = prefs.getBoolean("outputScript", true),
        )
        lifecycleScope.launch { converter = (application as VenderianApp).converter.await() }
    }

    override fun onCreateInputView(): View {
        window.window?.decorView?.let {
            it.setViewTreeLifecycleOwner(this)
            it.setViewTreeViewModelStoreOwner(this)
            it.setViewTreeSavedStateRegistryOwner(this)
        }
        return ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@VenderianIme)
            setViewTreeViewModelStoreOwner(this@VenderianIme)
            setViewTreeSavedStateRegistryOwner(this@VenderianIme)
            setContent { VenderianTheme { KeyboardUi(this@VenderianIme) } }
        }
    }

    override fun onStartInputView(info: EditorInfo, restarting: Boolean) {
        super.onStartInputView(info, restarting)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
        val cls = info.inputType and InputType.TYPE_MASK_CLASS
        undo = null
        cursor = info.initialSelStart.coerceAtLeast(0)
        translatedUpTo = -1
        state = state.copy(
            numbersOnly = cls == InputType.TYPE_CLASS_NUMBER || cls == InputType.TYPE_CLASS_PHONE,
            enterLabel = enterLabel(info),
            shift = if (capsAtStart(info)) Shift.Once else Shift.Off,
            canUndo = false,
        )
        refresh()
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    }

    override fun onDestroy() {
        super.onDestroy()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        viewModelStore.clear()
    }

    override fun onUpdateSelection(oldSelStart: Int, oldSelEnd: Int, newSelStart: Int, newSelEnd: Int, candStart: Int, candEnd: Int) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candStart, candEnd)
        cursor = newSelStart
        if (newSelStart < translatedUpTo) translatedUpTo = -1
        refresh()
    }

    /** re-reads the text before the cursor for the preview strip */
    private fun refresh() {
        val before = currentInputConnection?.getTextBeforeCursor(MAX_CONTEXT, 0)?.toString().orEmpty()
        state = state.copy(before = before)
    }

    // ---- typing ----

    fun type(text: String) {
        val ic = currentInputConnection ?: return
        val out = if (state.shift != Shift.Off && state.layout == Layout.Qwerty) text.uppercase() else text
        ic.commitText(out, 1)
        if (state.shift == Shift.Once) state = state.copy(shift = Shift.Off)
        dropUndo()
        refresh()
    }

    /** a venderian letter key: the glyph, or its romanization when the output is roman */
    fun typeSound(sound: String) = type(if (state.outputScript) Sounds.glyph(sound).toString() else sound)

    fun backspace() {
        val ic = currentInputConnection ?: return
        val selected = ic.getSelectedText(0)
        if (!selected.isNullOrEmpty()) ic.commitText("", 1)
        else {
            // don't split a surrogate pair (emoji)
            val last = ic.getTextBeforeCursor(2, 0)
            val n = if (last != null && last.length == 2 && Character.isSurrogatePair(last[0], last[1])) 2 else 1
            ic.deleteSurroundingText(n, 0)
        }
        dropUndo()
        refresh()
    }

    fun enter() {
        val ic = currentInputConnection ?: return
        val info = currentInputEditorInfo
        val action = info.imeOptions and EditorInfo.IME_MASK_ACTION
        val noEnterAction = info.imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION != 0
        if (!noEnterAction && action != EditorInfo.IME_ACTION_NONE && action != EditorInfo.IME_ACTION_UNSPECIFIED) {
            ic.performEditorAction(action)
        } else {
            ic.commitText("\n", 1)
        }
        dropUndo()
    }

    fun cycleShift() {
        state = state.copy(shift = when (state.shift) {
            Shift.Off -> Shift.Once
            Shift.Once -> Shift.Locked
            Shift.Locked -> Shift.Off
        })
    }

    fun setLayout(layout: Layout) {
        state = state.copy(layout = layout)
        if (layout != Layout.Symbols) prefs.edit().putInt("layout", layout.ordinal).apply()
    }

    fun toggleTranslate() {
        state = state.copy(translate = !state.translate)
        prefs.edit().putBoolean("translate", state.translate).apply()
    }

    fun toggleOutput() {
        state = state.copy(outputScript = !state.outputScript)
        prefs.edit().putBoolean("outputScript", state.outputScript).apply()
    }

    // ---- the translate button ----

    /** the english at the end of the text before the cursor (stops at venderian already written) */
    fun englishTail(before: String = state.before): String {
        val done = if (translatedUpTo >= 0) (before.length - (cursor - translatedUpTo)).coerceIn(-1, before.length) else -1
        val lastScript = maxOf(before.indexOfLast { Sounds.isLetter(it) }, done - 1)
        val tail = before.substring(lastScript + 1)
        // keep the separating space if there was venderian in front
        return if (lastScript >= 0) tail.trimStart() else tail
    }

    fun convert(english: String): String {
        val c = converter ?: return ""
        val r = c.convert(english, if (state.translate) Converter.Mode.TRANSLATE else Converter.Mode.SOUND)
        return if (state.outputScript) r.script else r.roman
    }

    /** swaps the english before the cursor for venderian */
    fun translateNow() {
        val ic = currentInputConnection ?: return
        val english = englishTail().trimEnd()
        if (english.isBlank() || converter == null) return
        val trailing = englishTail().length - english.length
        val result = convert(english)
        ic.beginBatchEdit()
        ic.deleteSurroundingText(english.length + trailing, 0)
        ic.commitText(result + " ".repeat(trailing), 1)
        ic.endBatchEdit()
        undo = english to result
        translatedUpTo = cursor - trailing - english.length + result.length
        state = state.copy(canUndo = true)
        refresh()
    }

    fun undoTranslate() {
        val ic = currentInputConnection ?: return
        val (english, result) = undo ?: return
        val before = ic.getTextBeforeCursor(result.length + 8, 0)?.toString().orEmpty().trimEnd()
        if (before.endsWith(result)) {
            val trailing = (ic.getTextBeforeCursor(result.length + 8, 0)?.length ?: 0) - before.length
            ic.beginBatchEdit()
            ic.deleteSurroundingText(result.length + trailing, 0)
            ic.commitText(english + " ".repeat(trailing.coerceAtLeast(0)), 1)
            ic.endBatchEdit()
            translatedUpTo = -1
        }
        dropUndo()
        refresh()
    }

    private fun dropUndo() {
        if (undo != null) {
            undo = null
            state = state.copy(canUndo = false)
        }
    }

    fun nextKeyboard() {
        if (!switchToNextInputMethod(false)) {
            (getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager).showInputMethodPicker()
        }
    }

    private fun capsAtStart(info: EditorInfo): Boolean =
        info.inputType and InputType.TYPE_TEXT_FLAG_CAP_SENTENCES != 0 &&
            currentInputConnection?.getCursorCapsMode(info.inputType) != 0

    private fun enterLabel(info: EditorInfo): EnterKey {
        if (info.imeOptions and EditorInfo.IME_FLAG_NO_ENTER_ACTION != 0) return EnterKey.Newline
        return when (info.imeOptions and EditorInfo.IME_MASK_ACTION) {
            EditorInfo.IME_ACTION_SEARCH -> EnterKey.Search
            EditorInfo.IME_ACTION_SEND -> EnterKey.Send
            EditorInfo.IME_ACTION_GO -> EnterKey.Go
            EditorInfo.IME_ACTION_NEXT -> EnterKey.Next
            EditorInfo.IME_ACTION_DONE -> EnterKey.Done
            else -> EnterKey.Newline
        }
    }

    companion object {
        private const val MAX_CONTEXT = 2000
    }
}

enum class Layout { Qwerty, Venderian, Symbols }
enum class Shift { Off, Once, Locked }
enum class EnterKey { Newline, Search, Send, Go, Next, Done }

data class ImeState(
    val layout: Layout = Layout.Qwerty,
    val shift: Shift = Shift.Off,
    /** translate words with the dictionary (true) or spell everything by sound */
    val translate: Boolean = false,
    /** write venderian script (true) or its romanization */
    val outputScript: Boolean = true,
    val before: String = "",
    val canUndo: Boolean = false,
    val numbersOnly: Boolean = false,
    val enterLabel: EnterKey = EnterKey.Newline,
)
