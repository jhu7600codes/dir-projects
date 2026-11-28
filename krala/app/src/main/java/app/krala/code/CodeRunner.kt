package app.krala.code

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.JavascriptInterface
import android.webkit.WebSettings
import android.webkit.WebView
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

data class RunResult(val ok: Boolean, val output: String, val error: String?, val ms: Long)

/**
 * Runs code from replies on the phone, in a hidden webview with no access to the app or its
 * files: javascript directly, python through pyodide (downloaded from jsdelivr on first use,
 * then cached by the webview). A runaway script is killed by throwing the webview away.
 */
@Singleton
class CodeRunner @Inject constructor(@ApplicationContext private val context: Context) {
    private var web: WebView? = null
    private val ids = AtomicInteger()
    private val waiting = ConcurrentHashMap<Int, CompletableDeferred<String>>()
    private var ready = CompletableDeferred<Unit>()

    fun language(lang: String): String? = when (lang.lowercase().trim()) {
        "js", "javascript", "node", "mjs" -> "js"
        "py", "python", "python3" -> "python"
        else -> null
    }

    suspend fun run(lang: String, code: String): RunResult {
        val kind = language(lang) ?: return RunResult(false, "", "can't run $lang", 0)
        val start = System.currentTimeMillis()
        val id = ids.incrementAndGet()
        val result = CompletableDeferred<String>()
        waiting[id] = result
        withContext(Dispatchers.Main) {
            val w = ensure()
            ready.await()
            val fn = if (kind == "js") "runJs" else "runPy"
            w.evaluateJavascript("$fn($id, ${JsonPrimitive(code)})", null)
        }
        val limit = if (kind == "python") 180_000L else 20_000L
        val raw = withTimeoutOrNull(limit) { result.await() }
        waiting.remove(id)
        val ms = System.currentTimeMillis() - start
        if (raw == null) {
            withContext(Dispatchers.Main) { reset() }
            return RunResult(false, "", "stopped after ${limit / 1000}s (endless loop or a very slow download)", ms)
        }
        val o = runCatching { Json.parseToJsonElement(raw) as JsonObject }.getOrNull()
            ?: return RunResult(false, raw, null, ms)
        fun str(k: String) = (o[k] as? JsonPrimitive)?.contentOrNull
        return RunResult(
            ok = (o["ok"] as? JsonPrimitive)?.booleanOrNull == true,
            output = str("out").orEmpty().take(MAX_OUT),
            error = str("error"),
            ms = ms,
        )
    }

    @SuppressLint("SetJavaScriptEnabled", "JavascriptInterface")
    private fun ensure(): WebView {
        web?.let { return it }
        ready = CompletableDeferred()
        val w = WebView(context)
        w.settings.apply {
            javaScriptEnabled = true
            allowFileAccess = false
            allowContentAccess = false
            domStorageEnabled = false
            cacheMode = WebSettings.LOAD_DEFAULT
        }
        w.addJavascriptInterface(object {
            @JavascriptInterface
            fun done(id: Int, json: String) {
                waiting[id]?.complete(json)
            }

            @JavascriptInterface
            fun loaded() {
                ready.complete(Unit)
            }
        }, "KralaBridge")
        w.loadDataWithBaseURL("https://run.krala.local/", HARNESS, "text/html", "utf-8", null)
        web = w
        return w
    }

    private fun reset() {
        web?.destroy()
        web = null
        waiting.values.forEach { it.complete("""{"ok":false,"out":"","error":"stopped"}""") }
        waiting.clear()
    }

    companion object {
        const val MAX_OUT = 8_000

        private val HARNESS = """
<!doctype html><html><head><meta charset="utf-8"></head><body><script>
function fmt(v){ if (typeof v === 'string') return v; try { return JSON.stringify(v, null, 2) ?? String(v) } catch(e) { return String(v) } }
function send(id, o){ KralaBridge.done(id, JSON.stringify(o)) }
async function runJs(id, code){
  const out = []; const log = (...a) => out.push(a.map(fmt).join(' '));
  const c = { log, info: log, warn: log, error: log, debug: log, table: log };
  try {
    const fn = new Function('console', 'return (async () => {\n' + code + '\n})()');
    const r = await fn(c);
    if (r !== undefined) out.push('→ ' + fmt(r));
    send(id, { ok: true, out: out.join('\n') });
  } catch (e) { send(id, { ok: false, out: out.join('\n'), error: String(e && e.stack || e) }) }
}
let py = null;
function loadScript(src){ return new Promise((res, rej) => { const s = document.createElement('script'); s.src = src; s.onload = res; s.onerror = () => rej(new Error('could not download python (pyodide), are you online?')); document.head.appendChild(s) }) }
async function getPy(){
  if (!py) { await loadScript('https://cdn.jsdelivr.net/pyodide/v0.26.4/full/pyodide.js'); py = await loadPyodide() }
  return py
}
async function runPy(id, code){
  const out = [];
  try {
    const p = await getPy();
    p.setStdout({ batched: s => out.push(s) }); p.setStderr({ batched: s => out.push(s) });
    await p.loadPackagesFromImports(code);
    const r = await p.runPythonAsync(code);
    if (r !== undefined && r !== null) out.push('→ ' + String(r));
    send(id, { ok: true, out: out.join('\n') });
  } catch (e) { send(id, { ok: false, out: out.join('\n'), error: String(e && e.message || e) }) }
}
KralaBridge.loaded();
</script></body></html>
""".trimIndent()
    }
}
