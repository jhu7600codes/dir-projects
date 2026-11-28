package app.krala.core

import java.time.Duration
import java.time.LocalDateTime
import kotlin.random.Random

/**
 * When krala texts first. Never at night (00:00 to 07:00): a time that lands there moves to a
 * random moment the same morning. Pure, so it's unit tested.
 */
object Daytime {
    const val NIGHT_END_HOUR = 7

    fun isNight(t: LocalDateTime) = t.hour < NIGHT_END_HOUR

    /** hours between messages for a frequency setting, null when off */
    fun gapHours(mode: String): ClosedFloatingPointRange<Double>? = when (mode) {
        "rare" -> 16.0..34.0
        "sometimes" -> 5.0..12.0
        else -> null
    }

    fun next(now: LocalDateTime, mode: String, rnd: Random = Random.Default): LocalDateTime? {
        val gap = gapHours(mode) ?: return null
        var t = now.plusMinutes((rnd.nextDouble(gap.start, gap.endInclusive) * 60).toLong())
        if (isNight(t)) {
            // the same morning, somewhere between 08:00 and 12:00
            t = t.toLocalDate().atTime(8, 0).plusMinutes(rnd.nextLong(0, 240))
        }
        return t
    }

    fun delay(now: LocalDateTime, mode: String, rnd: Random = Random.Default): Duration? =
        next(now, mode, rnd)?.let { Duration.between(now, it) }
}
