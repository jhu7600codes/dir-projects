package app.krala.core

/**
 * Turns a reply into something a text to speech voice can perform: tone tags ([excited],
 * [whisper], ...) change pitch and speed until the next tag, [laughs] becomes an actual laugh,
 * "..." and [pause] become beats of silence, stretched words (sooo, нууу, hmmm) are held, and
 * ALL CAPS words get a hype bump. Pure logic, unit tested.
 */
object VoiceScript {
    enum class Tone(val pitch: Float, val rate: Float, val volume: Float) {
        NEUTRAL(1.0f, 1.0f, 1.0f),
        EXCITED(1.16f, 1.12f, 1.0f),
        HYPE(1.22f, 1.05f, 1.0f),
        SOFT(0.96f, 0.9f, 0.85f),
        WHISPER(0.92f, 0.85f, 0.45f),
        SAD(0.86f, 0.84f, 0.9f),
        TEASING(1.1f, 0.96f, 1.0f),
        CALM(0.95f, 0.92f, 0.95f),
        HOLD(1.0f, 0.5f, 1.0f),
    }

    sealed interface Segment {
        data class Speak(val text: String, val tone: Tone) : Segment
        data class Pause(val ms: Long) : Segment
        data class Laugh(val kind: LaughKind) : Segment
    }

    enum class LaughKind { LAUGH, GIGGLE, CHUCKLE }

    data class Result(val segments: List<Segment>, val endTone: Tone)

    private val toneTags = mapOf(
        "excited" to Tone.EXCITED, "hype" to Tone.EXCITED, "happy" to Tone.EXCITED, "cheerful" to Tone.EXCITED,
        "soft" to Tone.SOFT, "gentle" to Tone.SOFT, "warm" to Tone.SOFT,
        "whisper" to Tone.WHISPER, "whispers" to Tone.WHISPER, "whispering" to Tone.WHISPER,
        "sad" to Tone.SAD, "sadly" to Tone.SAD,
        "teasing" to Tone.TEASING, "playful" to Tone.TEASING, "smirks" to Tone.TEASING,
        "calm" to Tone.CALM, "normal" to Tone.NEUTRAL, "neutral" to Tone.NEUTRAL,
    )
    private val laughTags = mapOf(
        "laughs" to LaughKind.LAUGH, "laugh" to LaughKind.LAUGH, "laughing" to LaughKind.LAUGH,
        "giggles" to LaughKind.GIGGLE, "giggle" to LaughKind.GIGGLE,
        "chuckles" to LaughKind.CHUCKLE, "chuckle" to LaughKind.CHUCKLE, "snickers" to LaughKind.CHUCKLE,
        "смеётся" to LaughKind.LAUGH, "смеется" to LaughKind.LAUGH, "хихикает" to LaughKind.GIGGLE,
    )
    private val pauseTags = mapOf("pause" to 450L, "beat" to 350L, "sighs" to 600L, "sigh" to 600L, "breath" to 300L, "пауза" to 450L, "вздыхает" to 600L)

    private val token = Regex("""\[([\p{L} ]{2,20})]|\*([\p{L} ]{2,20})\*|\.{3,}|…|\s—\s|\s–\s""")
    private val held = Regex("""(\p{L})\1{2,}""")
    private val emoji = Regex("""[\x{1F300}-\x{1FAFF}\x{2600}-\x{27BF}\x{FE0F}\x{200D}]""")

    fun parse(raw: String, startTone: Tone = Tone.NEUTRAL): Result {
        val text = clean(raw)
        val out = mutableListOf<Segment>()
        var tone = startTone
        var pos = 0
        fun addText(s: String) {
            if (s.isBlank()) return
            speakRun(s, tone, out)
        }
        for (m in token.findAll(text)) {
            addText(text.substring(pos, m.range.first))
            pos = m.range.last + 1
            val tag = (m.groupValues[1].ifEmpty { m.groupValues[2] }).trim().lowercase()
            when {
                tag.isEmpty() -> out += Segment.Pause(if (m.value.contains('…') || m.value.startsWith(".")) 380L else 200L)
                tag in laughTags -> out += Segment.Laugh(laughTags.getValue(tag))
                tag in pauseTags -> out += Segment.Pause(pauseTags.getValue(tag))
                tag in toneTags -> tone = toneTags.getValue(tag)
                else -> Unit // unknown stage directions are dropped, never read aloud
            }
        }
        addText(text.substring(pos))
        return Result(merge(out), tone)
    }

    /** splits a plain run into held words, hype words and normal speech */
    private fun speakRun(s: String, tone: Tone, out: MutableList<Segment>) {
        val words = s.split(Regex("""(?<=\s)|(?=\s)"""))
        val buf = StringBuilder()
        fun flush() {
            if (buf.isNotBlank()) out += Segment.Speak(buf.toString().trim(), tone)
            buf.clear()
        }
        for (w in words) {
            val core = w.trim()
            val letters = core.filter { it.isLetter() }
            when {
                core.isNotEmpty() && held.containsMatchIn(core) -> {
                    flush()
                    out += Segment.Speak(held.replace(core) { it.groupValues[1] }, Tone.HOLD)
                }
                letters.length >= 2 && letters.all { it.isUpperCase() } && tone != Tone.WHISPER -> {
                    flush()
                    out += Segment.Speak(core.lowercase(), Tone.HYPE)
                }
                else -> buf.append(w)
            }
        }
        flush()
    }

    private fun merge(list: List<Segment>): List<Segment> {
        val out = mutableListOf<Segment>()
        for (s in list) {
            val last = out.lastOrNull()
            if (s is Segment.Speak && last is Segment.Speak && last.tone == s.tone) {
                out[out.lastIndex] = Segment.Speak(last.text + " " + s.text, s.tone)
            } else if (s is Segment.Pause && last is Segment.Pause) {
                out[out.lastIndex] = Segment.Pause(maxOf(last.ms, s.ms))
            } else out += s
        }
        return out
    }

    /** markdown, emojis, links and code are not for ears */
    fun clean(raw: String): String {
        var t = KralaTags.strip(raw)
        t = t.replace(Regex("""```[\s\S]*?(```|$)"""), " ")
        t = t.replace(Regex("""`([^`]*)`"""), "$1")
        t = t.replace(Regex("""\[([^\]]+)]\((https?://[^)]+)\)"""), "$1")
        t = t.replace(Regex("""https?://\S+"""), "")
        t = t.replace(Regex("""(?m)^\s{0,3}(#{1,6}|>|[-*+]|\d+[.)])\s+"""), "")
        t = t.replace("**", "").replace("__", "").replace("~~", "")
        t = emoji.replace(t, "")
        return t.replace(Regex("""[ \t]+"""), " ").trim()
    }

    /** the end of the last complete sentence in [s] at or after [from], or -1 */
    fun sentenceEnd(s: String, from: Int): Int {
        var end = -1
        var i = from
        while (i < s.length) {
            val c = s[i]
            if (c == '\n' || ((c == '.' || c == '!' || c == '?' || c == '…') && (i + 1 >= s.length || s[i + 1].isWhitespace()))) {
                if (i + 1 < s.length) end = i + 1
            }
            i++
        }
        return end
    }

    fun isCyrillic(s: String): Boolean {
        val letters = s.filter { it.isLetter() }
        if (letters.isEmpty()) return false
        return letters.count { it in 'Ѐ'..'ӿ' } * 3 >= letters.length
    }

    fun laughText(kind: LaughKind, russian: Boolean): String = when (kind) {
        LaughKind.LAUGH -> if (russian) "ха-ха-ха" else "ha ha ha"
        LaughKind.GIGGLE -> if (russian) "хи-хи" else "hee hee"
        LaughKind.CHUCKLE -> if (russian) "хе-хе" else "heh heh"
    }
}
