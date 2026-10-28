package dev.jhu.crossi.generator

/**
 * The dumb letter riddles: the answer is one letter spelled how it sounds (AIGH for A),
 * the clue points at that letter in some stupid way. Template clues are built here and
 * every one is checked to actually be true before it's used.
 */
class TrickSet(
    val alphabet: Alphabet,
    private val spellings: Map<Char, String>,
    private val handClues: Map<Char, List<String>>,
    private val templates: List<(Char, Rng) -> String?>,
) {
    private val letterOf: Map<String, Char> = spellings.entries.associate { (letter, spelling) -> spelling to letter }

    val answers: Collection<String> get() = spellings.values

    fun isTrick(answer: String): Boolean = answer in letterOf

    fun letterFor(answer: String): Char? = letterOf[answer]

    /** A trick clue for [answer], or null if it's not a trick answer. */
    fun clueFor(answer: String, rng: Rng): String? {
        val letter = letterOf[answer] ?: return null
        val options = ArrayList<String>()
        handClues[letter]?.let(options::addAll)
        for (template in templates) template(letter, rng)?.let(options::add)
        return if (options.isEmpty()) null else rng.pick(options)
    }

    fun allCluesFor(answer: String, rng: Rng): List<String> {
        val letter = letterOf[answer] ?: return emptyList()
        return handClues[letter].orEmpty() + templates.mapNotNull { it(letter, rng) }
    }

    companion object {
        val EN: TrickSet by lazy { english() }
        val RU: TrickSet by lazy { russian() }

        fun forAlphabet(alphabet: Alphabet): TrickSet = if (alphabet === Alphabet.RU) RU else EN

        private fun english(): TrickSet {
            val spellings = mapOf(
                'A' to "AIGH", 'B' to "BEE", 'C' to "SEE", 'D' to "DEE", 'E' to "EEE", 'F' to "EFF",
                'G' to "GEE", 'H' to "AITCH", 'I' to "EYE", 'J' to "JAY", 'K' to "KAY", 'L' to "ELL",
                'M' to "EMM", 'N' to "ENN", 'O' to "OWE", 'Q' to "CUE", 'R' to "ARR", 'S' to "ESS",
                'T' to "TEE", 'U' to "YOU", 'V' to "VEE", 'W' to "DOUBLEYOU", 'X' to "ECKS",
                'Y' to "WHY", 'Z' to "ZED",
            )
            val hand = mapOf(
                'A' to listOf("the grade everyone wants, spelled out", "first in line, alphabetically speaking"),
                'B' to listOf("the letter that buzzes", "plan ___, but just the letter"),
                'C' to listOf("the letter you can also do with your eyes", "the letter that sounds like the ocean"),
                'E' to listOf("the most used letter in english, spelled out very dramatically"),
                'F' to listOf("the grade nobody wants", "the letter you press to pay respects"),
                'G' to listOf("the letter that sounds like 'wow, really?'"),
                'I' to listOf("the letter you see with", "the letter that's all about me"),
                'J' to listOf("the letter that's also a blue bird"),
                'L' to listOf("the letter that's also a train in chicago", "make a loser sign with your fingers, that letter"),
                'O' to listOf("what you do to a debt, also a vowel", "the round letter"),
                'Q' to listOf("the letter that tells actors to go", "the letter that waits in line"),
                'R' to listOf("what a pirate says, but also a letter", "the pirate's favorite letter"),
                'T' to listOf("the letter a golfer puts the ball on", "the letter that's also a drink"),
                'U' to listOf("the shape of a horseshoe magnet", "the letter that's talking about you. yes, you"),
                'X' to listOf("the letter that marks the spot", "the letter on every treasure map"),
                'Y' to listOf("a question, but make it a letter", "the letter that keeps asking questions"),
                'Z' to listOf("the last letter, the british way", "the sleepy letter"),
            )
            val capitalBases = listOf(
                "the letter thats capital in this hint",
                "which letter got the caps lock treatment in this clue",
                "spot the shouty letter in this sentence",
                "one letter in here is yelling at you, which one",
                "the big letter hiding in this clue, spelled out",
            )
            val wordsByFirst = listOf(
                "apple", "banana", "cookie", "dragon", "elephant", "fridge", "guitar", "hamster", "igloo", "jelly",
                "koala", "lemon", "monkey", "noodle", "octopus", "pizza", "queen", "robot", "sandwich", "turtle",
                "unicorn", "volcano", "waffle", "xylophone", "yogurt", "zebra",
            )
            val missingWords = listOf(
                "happy", "banana", "pizza", "jungle", "rocket", "sunny", "puzzle", "galaxy", "wizard", "dragon",
                "kitten", "cheese", "quick", "zombie", "yellow", "vivid", "fox", "jelly", "hello", "mouse",
                "bubble", "monkey", "turtle", "noodle", "waffle", "taco",
            )
            val alphabet = Alphabet.EN.letters
            val templates = listOf<(Char, Rng) -> String?>(
                { letter, rng ->
                    val lower = letter.lowercaseChar()
                    val base = capitalBases.filter { lower in it }.takeIf { it.isNotEmpty() }?.let(rng::pick)
                    base?.map { if (it == lower) letter else it }?.joinToString("")
                        ?.takeIf { clue -> clue.filter { it.isUpperCase() }.toSet() == setOf(letter) }
                },
                { letter, _ ->
                    wordsByFirst.firstOrNull { it.first() == letter.lowercaseChar() }
                        ?.let { "the first letter of '$it'" }
                },
                { letter, rng ->
                    val lower = letter.lowercaseChar()
                    missingWords.filter { lower in it }.takeIf { it.isNotEmpty() }?.let(rng::pick)
                        ?.let { word -> "the letter missing from '${word.replace(lower, '_')}'" }
                },
                { letter, _ ->
                    val i = alphabet.indexOf(letter)
                    if (i > 0) "the letter right after ${alphabet[i - 1]}, spelled out" else null
                },
                { letter, _ ->
                    val i = alphabet.indexOf(letter)
                    if (i < alphabet.lastIndex) "the letter right before ${alphabet[i + 1]}, spelled out" else null
                },
            )
            return TrickSet(Alphabet.EN, spellings, hand, templates)
        }

        // Most russian letter names are 2 letters (бэ, эм, ша), too short for a grid.
        // So the russian set is the 3+ letter ones, plus latin letters by their russian names.
        private fun russian(): TrickSet {
            val spellings = mapOf('Л' to "ЭЛЬ", 'X' to "ИКС", 'Y' to "ИГРЕК", 'Z' to "ЗЕТ")
            val hand = mapOf(
                'Л' to listOf(
                    "буква, которую показывают пальцами неудачнику",
                    "буква из слова «лол», если её произнести",
                    "буква, с которой начинается «лень», вслух",
                ),
                'X' to listOf(
                    "буква, которой обозначают то, что надо найти",
                    "неизвестное из задачки по математике",
                    "латинская буква, похожая на крестик",
                    "буква, которой отмечают клад на карте",
                ),
                'Y' to listOf(
                    "буква, с которой икс всегда ходит парой",
                    "вторая ось графика, как буква",
                    "латинская буква, которая звучит как имя",
                ),
                'Z' to listOf(
                    "последняя буква латиницы",
                    "третья ось, но как буква",
                    "буква, которой заканчивается латинский алфавит",
                ),
            )
            val templates = listOf<(Char, Rng) -> String?>(
                { letter, _ -> if (letter == 'Л') "единственная заглавная буква в слове «моЛоко»" else null },
                { letter, _ -> if (letter == 'Л') "буква, пропущенная в слове «ку_ак»" else null },
            )
            return TrickSet(Alphabet.RU, spellings, hand, templates)
        }
    }
}
