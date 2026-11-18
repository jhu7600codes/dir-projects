package com.googol.calc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlin.random.Random

private val Bg = Color(0xFF0B0B12)
private val Panel = Color(0xFF15151F)
private val Key = Color(0xFF1E1E2B)
private val KeyOp = Color(0xFF2A2340)
private val Purple = Color(0xFFB794FF)
private val Pink = Color(0xFFFF7AB6)
private val Dim = Color(0xFF8A8AA0)

/** One of the "please calculate this" buttons. */
class Monster(
    val key: String,
    val expr: String,
    val steps: List<String>,
    val verdicts: List<String>,
)

val monsters = listOf(
    Monster(
        "TREE(3)", "TREE(3)",
        listOf("planting tree #1", "planting tree #2", "asking kruskal nicely", "checking homeomorphic embeddings",
            "tree #9 looks suspicious", "ran out of forest", "importing more forest"),
        listOf("no. the observable universe can't even hold the number of digits of the number of digits of this.",
            "tree(1) = 1. tree(2) = 3. tree(3) = no.",
            "i got to tree #47 and the trees started looking at me.",
            "friedman says it's finite. i say it's not my problem."),
    ),
    Monster(
        "SSCG(3)", "SSCG(3)",
        listOf("drawing subcubic graphs", "counting vertices", "SCG(13) waved at me", "graph minors are fighting",
            "consulting robertson-seymour", "graph is no longer subcubic, it's sub-everything"),
        listOf("no. this makes tree(3) look like a rounding error. and tree(3) makes everything look like a rounding error.",
            "sscg(3) is bigger than tree(tree(...tree(3)...)) and my battery is at 34%.",
            "i drew one graph and the phone got warm. no."),
    ),
    Monster(
        "SCG(13)", "SCG(13)",
        listOf("loading 13", "13 is fine", "13 is not fine", "13 has become sentient"),
        listOf("no. why 13. who chose 13. why would you do this.",
            "scg(13) > sscg(3) and i'm a calculator app made by a 15 year old. no."),
    ),
    Monster(
        "G64", "Graham's number",
        listOf("3↑3 = 27 ok easy", "3↑↑3 = 7625597484987 still fine", "3↑↑↑3 ...", "building a power tower 7.6 trillion 3s tall",
            "g1 done (lie)", "g2", "losing count of arrows", "arrows have arrows now"),
        listOf("no. it ends in ...2464195387. that's all you're getting.",
            "if you memorized every digit your head would collapse into a black hole. i'm protecting you. no.",
            "graham's number is tiny compared to tree(3). still no."),
    ),
    Monster(
        "Rayo(10¹⁰⁰)", "Rayo(googol)",
        listOf("learning first-order set theory", "writing a googol symbols", "symbol #1 of 10¹⁰⁰", "philosophy degree pending",
            "the axiom of choice chose violence"),
        listOf("no. this number was literally invented by beating another guy in a 'name a bigger number' duel. i refuse to participate.",
            "rayo's number is not computable. i am a computer. you see the problem.",
            "agustín rayo won a duel with this at mit. i lost a duel with this on a pixel."),
    ),
    Monster(
        "BB(6)", "Σ(6)",
        listOf("enumerating 6-state turing machines", "machine #1 halted", "machine #2 halted", "machine #3 is still going",
            "machine #3 is still going", "machine #3 is STILL going", "has machine #3 halted?? (undecidable)"),
        listOf("no. bb(5) took humanity 40 years. bb(6) is > 2↑↑↑5. i'm not doing that on a phone.",
            "i would tell you but first i need to solve the halting problem. brb. (no)",
            "bb(745) is independent of zfc. bb(6) isn't, but it is independent of my patience."),
    ),
    Monster(
        "Loader's", "D⁵(99)",
        listOf("compiling loader.c", "running the calculus of constructions", "D(99)", "D(D(99))", "D(D(D(99)))",
            "stack overflow (the real one, not the website)"),
        listOf("no. it's a C program shorter than this sentence and it would outlive the heat death of the universe.",
            "loader's number is computable. just not by me. or by anything. ever."),
    ),
    Monster(
        "f_ε₀(10)", "f_ε₀(10)",
        listOf("diagonalizing", "ω", "ω^ω", "ω^ω^ω", "ω^ω^ω^ω (blink twice if you need help)", "reached ε₀, crying"),
        listOf("no. peano arithmetic can't even prove this stops growing. and peano is smarter than me.",
            "fast-growing hierarchy more like fast-growing headache."),
    ),
    Monster(
        "∞ + 1", "∞ + 1",
        listOf("taking ∞", "adding 1", "checking if it got bigger"),
        listOf("no. it's still ∞. ∞ doesn't care. be more like ∞.",
            "ω + 1 is bigger than ω though. but 1 + ω is just ω. ordinals are weird. no."),
    ),
    Monster(
        "googolplex!", "googolplex!",
        listOf("1 × 2", "× 3", "× 4", "× 5", "this might take a bit", "× 6"),
        listOf("no. the factorial alone has more digits than there are planck volumes in... ok you know the drill.",
            "googolplex is honestly small for this app. and still no."),
    ),
    Monster(
        "Ω", "absolute infinity",
        listOf("summoning cantor", "cantor is upset", "proper class detected", "this isn't even a set"),
        listOf("no. you can't calculate god. cantor tried. it went badly.",
            "Ω is not a number, it's a lifestyle. no."),
    ),
)

val divZero = Monster(
    "÷0", "÷ 0",
    listOf("dividing", "dividing harder", "the zero is fighting back", "consulting riemann sphere",
        "consulting the teacher", "summoning a black hole (just a small one)"),
    listOf("no. you don't divide by zero. zero divides you.",
        "the riemann sphere says ∞. the real numbers say undefined. i say no.",
        "nice try. siri already fell for this one. i won't.",
        "you have 0 cookies and 0 friends. how many cookies does each friend get? no. that's the answer. no."),
)

sealed class Phase {
    data object Idle : Phase()
    data class Crunching(val m: Monster, val title: String) : Phase()
    data class Verdict(val m: Monster, val title: String, val text: String, val digits: Int, val pct: Float) : Phase()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(primary = Purple, background = Bg, surface = Bg)) {
                Surface(Modifier.fillMaxSize(), color = Bg) { Calc() }
            }
        }
    }
}

// ---- tiny regular calculator so the normal keys aren't decoration ----

private fun evaluate(expr: String): Double? {
    val nums = mutableListOf<Double>()
    val ops = mutableListOf<Char>()
    var cur = ""
    for (c in expr) {
        if (c in "+−×÷") {
            if (cur.isEmpty()) return null
            nums += cur.toDoubleOrNull() ?: return null; ops += c; cur = ""
        } else cur += c
    }
    nums += cur.toDoubleOrNull() ?: return null
    // × and ÷ first
    var i = 0
    while (i < ops.size) {
        if (ops[i] == '×' || ops[i] == '÷') {
            val r = if (ops[i] == '×') nums[i] * nums[i + 1] else nums[i] / nums[i + 1]
            nums[i] = r; nums.removeAt(i + 1); ops.removeAt(i)
        } else i++
    }
    var res = nums[0]
    for (j in ops.indices) res = if (ops[j] == '+') res + nums[j + 1] else res - nums[j + 1]
    return res
}

private fun fmt(d: Double): String =
    if (d == Math.floor(d) && kotlin.math.abs(d) < 1e15) d.toLong().toString()
    else "%.10g".format(d).trimEnd('0').trimEnd('.')

@Composable
fun Calc() {
    var expr by remember { mutableStateOf("") }
    var phase by remember { mutableStateOf<Phase>(Phase.Idle) }
    var bigCount by remember { mutableIntStateOf(0) }

    fun crunch(m: Monster, title: String) { phase = Phase.Crunching(m, title) }

    fun equals() {
        if (expr.isEmpty()) return
        if (Regex("÷0+(\\.0*)?(?![.0-9])").containsMatchIn(expr)) {
            crunch(divZero, expr); return
        }
        val r = evaluate(expr) ?: return
        if (r.isInfinite() || r.isNaN()) {
            crunch(Monster("∞", "overflow", listOf("this is getting big", "this is getting smaller than tree(3)"),
                listOf("no. you overflowed a double. cute. that's not even 10³⁰⁹. come back with tree(3).")), expr)
            return
        }
        expr = fmt(r)
    }

    Column(
        Modifier.fillMaxSize().systemBarsPadding().padding(12.dp),
    ) {
        Text("googologist's dream calculator", color = Dim, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
        Spacer(Modifier.height(8.dp))

        // display / crunch panel
        Box(
            Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(20.dp)).background(Panel).padding(16.dp),
        ) {
            AnimatedContent(phase, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "phase") { p ->
                when (p) {
                    Phase.Idle -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomEnd) {
                        Text(
                            expr.ifEmpty { "0" }, color = Color.White, fontSize = if (expr.length > 12) 30.sp else 48.sp,
                            fontFamily = FontFamily.Monospace, textAlign = TextAlign.End, maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    is Phase.Crunching -> Crunching(p) { v -> bigCount++; phase = v }
                    is Phase.Verdict -> VerdictView(p) { phase = Phase.Idle }
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        Text(
            if (bigCount == 0) "numbers refused: 0" else "numbers refused: $bigCount",
            color = Dim, fontSize = 11.sp, fontFamily = FontFamily.Monospace,
        )
        Spacer(Modifier.height(6.dp))

        val busy = phase is Phase.Crunching
        // the big boys
        val grid = monsters + divZero
        grid.chunked(4).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { m ->
                    KeyBtn(m.key, Modifier.weight(1f).height(44.dp), KeyOp, Pink, 13.sp, enabled = !busy) {
                        val title = if (m === divZero) "${expr.ifEmpty { "1" }} ÷ 0" else m.expr
                        crunch(m, title)
                    }
                }
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
            Spacer(Modifier.height(6.dp))
        }
        Spacer(Modifier.height(4.dp))

        // boring normal keypad
        val keys = listOf(
            listOf("AC", "⌫", "%", "÷"),
            listOf("7", "8", "9", "×"),
            listOf("4", "5", "6", "−"),
            listOf("1", "2", "3", "+"),
            listOf("0", ".", "=", ""),
        )
        keys.forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEach { k ->
                    if (k.isEmpty()) { Spacer(Modifier.weight(1f)); return@forEach }
                    val op = k in listOf("÷", "×", "−", "+", "=")
                    val w = if (k == "=") 2f else 1f
                    KeyBtn(
                        k, Modifier.weight(w).height(56.dp),
                        if (k == "=") Purple else if (op) KeyOp else Key,
                        if (k == "=") Bg else if (op) Purple else Color.White, 22.sp, enabled = !busy,
                    ) {
                        if (phase is Phase.Verdict) phase = Phase.Idle
                        when (k) {
                            "AC" -> expr = ""
                            "⌫" -> expr = expr.dropLast(1)
                            "=" -> equals()
                            "%" -> evaluate(expr)?.let { expr = fmt(it / 100) }
                            in listOf("÷", "×", "−", "+") -> {
                                if (expr.isNotEmpty() && expr.last() in "+−×÷") expr = expr.dropLast(1)
                                if (expr.isNotEmpty()) expr += k
                            }
                            else -> if (expr.length < 40) expr += k
                        }
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
        }
    }
}

@Composable
fun KeyBtn(
    label: String, modifier: Modifier, bg: Color, fg: Color, size: androidx.compose.ui.unit.TextUnit,
    enabled: Boolean = true, onClick: () -> Unit,
) {
    Box(
        modifier.clip(RoundedCornerShape(14.dp)).background(if (enabled) bg else bg.copy(alpha = 0.4f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = fg, fontSize = size, fontWeight = FontWeight.Medium, maxLines = 1)
    }
}

@Composable
fun Crunching(p: Phase.Crunching, done: (Phase.Verdict) -> Unit) {
    var step by remember { mutableStateOf(p.m.steps.first()) }
    var progress by remember { mutableFloatStateOf(0f) }
    var digits by remember { mutableStateOf("") }

    LaunchedEffect(p) {
        val total = Random.nextLong(3500, 6000)
        var t = 0L
        while (t < total) {
            delay(80); t += 80
            val x = t / 1000f
            // zooms to like 3% then crawls forever. very realistic
            progress = (0.035f * (1 - 1 / (1 + x * 2))).coerceAtMost(0.0349f)
            if (t % 640 == 0L) step = p.m.steps[((t / 640) % p.m.steps.size).toInt()]
            if (digits.length < 600) digits += Random.nextInt(10)
        }
        done(
            Phase.Verdict(
                p.m, p.title, p.m.verdicts.random(), digits.length,
                progress * 100 * Random.nextFloat() / 1e6f,
            ),
        )
    }

    Column(Modifier.fillMaxSize()) {
        Text(p.title, color = Pink, fontSize = 26.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text("calculating… $step", color = Dim, fontSize = 13.sp, fontFamily = FontFamily.Monospace)
        Spacer(Modifier.height(10.dp))
        LinearProgressIndicator(
            progress = { progress }, Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
            color = Purple, trackColor = Key,
        )
        Spacer(Modifier.height(4.dp))
        Text("%.6f%%".format(progress * 100), color = Dim, fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        Spacer(Modifier.height(10.dp))
        Text(
            digits, color = Color.White.copy(alpha = 0.55f), fontSize = 12.sp, fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(1f), overflow = TextOverflow.Clip,
        )
    }
}

@Composable
fun VerdictView(v: Phase.Verdict, dismiss: () -> Unit) {
    Column(
        Modifier.fillMaxSize().clickable(onClick = dismiss),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(v.title, color = Dim, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
        Text("= no.", color = Pink, fontSize = 56.sp, fontWeight = FontWeight.Black, fontFamily = FontFamily.Monospace)
        Spacer(Modifier.height(12.dp))
        Text(v.text, color = Color.White, fontSize = 17.sp, lineHeight = 23.sp)
        Spacer(Modifier.height(16.dp))
        Text(
            "digits computed: ${v.digits}\ndigits remaining: yes\nprogress: ${"%.2e".format(v.pct)}%",
            color = Dim, fontSize = 11.sp, fontFamily = FontFamily.Monospace,
        )
        Spacer(Modifier.height(8.dp))
        Text("tap to try again (it won't help)", color = Purple.copy(alpha = 0.7f), fontSize = 11.sp)
    }
}
