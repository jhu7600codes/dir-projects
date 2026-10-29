package app.venderian.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

typealias Stroke = List<List<Float>>

@Serializable
data class Letter(
    val id: String,
    val roman: String = id,
    val ipa: String = "",
    val name: String = id,
    val cp: String = "",
    val hint: String = "",
    val strokes: List<Stroke>,
    val headline: Boolean = false,
) {
    val glyph: Char get() = cp.toInt(16).toChar()

    /** an english-ish respelling for text to speech: "pah", "ee", "uh" */
    val say: String
        get() = VOWEL_SAY[id] ?: when (id) {
            "kh" -> "kha"
            "y" -> "yah"
            else -> "${id}ah"
        }

    /** the example word in the hint, "spin" from "p as in 'spin'" */
    val example: String? get() = Regex("'([^']+)'").find(hint)?.groupValues?.get(1)

    companion object {
        private val VOWEL_SAY = mapOf("a" to "ah", "e" to "eh", "i" to "ee", "o" to "oh", "u" to "oo", "ë" to "uh")
    }
}

@Serializable
data class Mark(val about: String = "", val strokes: List<Stroke>)

@Serializable
data class Alphabet(
    val strokeWidth: Float,
    val consonants: List<Letter>,
    val vowels: List<Letter>,
    val digits: List<Letter>,
    val punctuation: List<Letter> = emptyList(),
    val marks: Map<String, Mark> = emptyMap(),
) {
    val letters: List<Letter> get() = consonants + vowels
    private val byId by lazy { letters.associateBy { it.id } }

    fun letter(id: String) = byId.getValue(id)

    /** the alphabet lessons, grouped the way docs/venderian.md explains the script */
    val lessons: List<Lesson> by lazy {
        listOf(
            Lesson(0, "vowels", "they float under the headline", listOf("a", "e", "i", "o", "u", "ë")),
            Lesson(1, "lips", "the long \\ leg", listOf("p", "b", "f", "v", "m", "w")),
            Lesson(2, "ridge", "the long / leg", listOf("t", "d", "th", "s", "z", "n")),
            Lesson(3, "back & palate", "bent legs", listOf("k", "g", "kh", "ng", "sh", "ch")),
            Lesson(4, "the rest", "flags and breath", listOf("r", "l", "y", "h")),
        ).map { it.copy(letters = it.ids.map(::letter)) }
    }

    companion object {
        private val json = Json { ignoreUnknownKeys = true }
        fun parse(text: String) = json.decodeFromString<Alphabet>(text)
    }
}

data class Lesson(
    val index: Int,
    val title: String,
    val subtitle: String,
    val ids: List<String>,
    val letters: List<Letter> = emptyList(),
)
