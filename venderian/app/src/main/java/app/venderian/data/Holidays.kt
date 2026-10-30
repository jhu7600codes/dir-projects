package app.venderian.data

import java.time.LocalDate
import java.time.MonthDay

/** days the app dresses up for: its colors change and it says something in venderian */
enum class Holiday(
    val greeting: String,
    val english: String,
    /** primary, its container, and the background tint, as argb */
    val primary: Long,
    val container: Long,
    val background: Long,
) {
    NEW_YEAR("romtal bon! yer nov fela!", "happy new year! a happy new year!", 0xFFFFD36B, 0xFF5A4300, 0xFF12131C),
    CHRISTMAS("salmashzas bon!", "merry christmas!", 0xFFFF8A80, 0xFF1B5E20, 0xFF101A12),
    VALENTINE("dey da liva!", "it's the day of love!", 0xFFFF8AB4, 0xFF7A1E48, 0xFF1C1116),
    WOMENS_DAY("dey da vira! tozun!", "women's day! congratulations!", 0xFFF6B7D6, 0xFF6B2C55, 0xFF1A1218),
    APRIL_FOOLS("vedey shakan!", "today: jokes!", 0xFF9CF27C, 0xFF7A2BB8, 0xFF161219),
    VENDERI_DAY("dey da vendëri! fela dey!", "venderian day! a happy day!", 0xFFFFC94D, 0xFF4F378A, 0xFF15111F),
    HALLOWEEN("pimigu! mesan ik es!", "halloween! the ghosts are here!", 0xFFFF9F43, 0xFF4A2500, 0xFF120F0C),
    ;

    companion object {
        fun on(date: LocalDate): Holiday? {
            val d = MonthDay.from(date)
            return when {
                d.monthValue == 12 && d.dayOfMonth == 31 || d.monthValue == 1 && d.dayOfMonth <= 2 -> NEW_YEAR
                // both christmases: the 25th of december, and the 7th of january
                d.monthValue == 12 && d.dayOfMonth in 24..25 || d.monthValue == 1 && d.dayOfMonth == 7 -> CHRISTMAS
                d.monthValue == 2 && d.dayOfMonth == 14 -> VALENTINE
                d.monthValue == 3 && d.dayOfMonth == 8 -> WOMENS_DAY
                d.monthValue == 4 && d.dayOfMonth == 1 -> APRIL_FOOLS
                d.monthValue == 7 && d.dayOfMonth == 15 -> VENDERI_DAY
                d.monthValue == 10 && d.dayOfMonth == 31 -> HALLOWEEN
                else -> null
            }
        }

        /** for screenshots: pretend it's this holiday */
        var override: Holiday? = null

        fun today() = override ?: on(LocalDate.now())
    }
}

/** the line the widget shows under your streak, in venderian with an english translation */
object Taglines {
    fun forStreak(streak: Int, practicedToday: Boolean): Pair<String, String> = when {
        streak == 0 -> "kiro, vedey lerna!" to "please, learn today!"
        !practicedToday -> "vedey lerna, ne perda!" to "learn today, don't lose it!"
        streak == 1 -> "vela! ashir ra vendëri!" to "hello! welcome to venderian!"
        streak == 2 -> "dor dey! tu bon!" to "two days! you're good!"
        streak == 3 -> "tis dey! tu forë!" to "three days! you're strong!"
        streak < 7 -> "ne puchuso!" to "don't stop!"
        streak == 7 -> "ek vek! tu heroë!" to "one week! you're a hero!"
        streak < 14 -> "tu dey tot lerna!" to "you learn every day!"
        streak == 14 -> "dor vek! mol fela!" to "two weeks! very happy!"
        streak < 30 -> "vendëri un kor da tu es." to "venderian is in your heart."
        streak == 30 -> "ek mesë! mol bon!" to "one month! very good!"
        streak < 100 -> "tu vendëri thela pot!" to "you can speak venderian!"
        streak < 365 -> "tu poma da vendëri!" to "you're a venderian champion!"
        else -> "ek yer! tu menisthi!" to "one year! you're a legend!"
    }
}
