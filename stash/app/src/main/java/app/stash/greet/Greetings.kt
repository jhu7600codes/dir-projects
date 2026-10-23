package app.stash.greet

import java.util.Calendar
import kotlin.random.Random

sealed interface GreetingAction {
    val label: String

    data class PlayArtist(val artist: String) : GreetingAction { override val label = "play $artist" }
    data object OpenDownloads : GreetingAction { override val label = "downloads" }
    data object ShuffleLiked : GreetingAction { override val label = "shuffle liked songs" }
    data object ShuffleDownloads : GreetingAction { override val label = "shuffle offline songs" }
}

/** [title] goes in the big top bar, [message] in the card under it */
data class Greeting(val title: String, val message: String?, val action: GreetingAction? = null)

/**
 * home screen greetings: the title follows the time of day, the line under it follows where
 * you are (when location greetings are on), and both pick from a few variants so it isn't
 * the same sentence every time.
 */
object Greetings {

    fun build(
        hour: Int,
        dayOfWeek: Int,
        place: Place?,
        topArtist: String?,
        random: Random = Random.Default,
    ): Greeting {
        val title = random.pick(timeTitles(hour, dayOfWeek))
        val placed = place?.let { placeLine(it, topArtist, random) }
        if (placed != null) return Greeting(title, placed.first, placed.second)
        return Greeting(title, timeLines(hour, dayOfWeek).let { random.pick(it) })
    }

    fun now(place: Place?, topArtist: String?, random: Random = Random.Default): Greeting {
        val c = Calendar.getInstance()
        return build(c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.DAY_OF_WEEK), place, topArtist, random)
    }

    private fun <T> Random.pick(list: List<T>) = list[nextInt(list.size)]

    private fun weekend(day: Int) = day == Calendar.SATURDAY || day == Calendar.SUNDAY

    private fun timeTitles(hour: Int, day: Int): List<String> = when (hour) {
        in 0..5 -> listOf("late night listening", "still up?", "night owl mode")
        in 6..8 -> if (weekend(day)) listOf("lazy weekend morning", "good morning") else listOf("rise and shine", "good morning")
        in 9..11 -> listOf("good morning", "morning!")
        in 12..13 -> listOf("good afternoon", "lunch time")
        in 14..17 -> listOf("good afternoon", "hey there")
        in 18..21 -> if (day == Calendar.FRIDAY) listOf("it's friday", "good evening") else listOf("good evening", "evening!")
        else -> listOf("good night", "late night listening")
    }

    private fun timeLines(hour: Int, day: Int): List<String> = when (hour) {
        in 0..5 -> listOf(
            "don't wake up the neighborhood!",
            "keep it quiet, everyone's asleep",
            "3am thoughts need a soundtrack",
        )
        in 6..8 -> if (weekend(day)) listOf("no alarm today. take it slow", "pancakes and a playlist?")
        else listOf("something to wake up to?", "coffee first, then music. or both", "let's make the commute better")
        in 9..11 -> listOf("let's get the day going", "focus mode, headphones on", "a good song fixes a slow morning")
        in 12..13 -> listOf("lunch break beats?", "halfway through the day, treat yourself")
        in 14..17 -> listOf("afternoon slump? fix it with a song", "almost done for today", "one more song, then back to work")
        in 18..21 -> if (day == Calendar.FRIDAY) listOf("weekend starts now, turn it up", "friday night, you know what to do")
        else listOf("the day's done, press play", "evening vibes loading", "dinner needs a soundtrack")
        else -> listOf("winding down?", "one more song before bed", "lights low, volume lower")
    }

    private fun placeLine(place: Place, artist: String?, random: Random): Pair<String, GreetingAction?>? = when (place) {
        Place.AIRPORT -> random.pick(
            listOf(
                "pick a song to fly and don't forget to download it!",
                "flight mode soon. download a playlist first!",
                "boarding soon? make sure your music works offline",
            )
        ) to GreetingAction.OpenDownloads
        Place.CAFE -> random.pick(
            listOf(
                "pick a song for the coffee to vibe to :)",
                "coffee and a playlist, name a better duo",
                "a cafe without music is just a room with coffee",
            )
        ) to null
        Place.STATION, Place.TRAIN -> if (artist != null) {
            random.pick(
                listOf(
                    "welcome aboard. should we start with some $artist?",
                    "next stop: $artist?",
                    "window seat and $artist, perfect",
                )
            ) to GreetingAction.PlayArtist(artist)
        } else {
            random.pick(listOf("welcome aboard. pick something for the ride", "next stop: your playlist")) to null
        }
        Place.BUS_STATION -> random.pick(listOf("waiting for the bus? headphones in", "bus ride soundtrack, coming up")) to null
        Place.DRIVING -> random.pick(
            listOf("eyes on the road, we've got the music", "road trip mode", "windows down, volume up")
        ) to GreetingAction.ShuffleDownloads
        Place.WALKING -> random.pick(listOf("walking soundtrack, coming up", "every walk needs a playlist")) to GreetingAction.ShuffleLiked
        Place.GYM -> random.pick(listOf("time to lift. something loud?", "one more rep, one more song", "gym mode on")) to GreetingAction.ShuffleLiked
        Place.PARK -> random.pick(listOf("nice spot for a walk with music", "fresh air and a good song")) to null
        Place.BEACH -> random.pick(listOf("waves and a playlist", "sun's out, music's on")) to null
        Place.LIBRARY -> random.pick(listOf("it's a library, headphones only!", "quiet zone. keep it in your ears")) to null
        Place.SCHOOL -> random.pick(listOf("after class? or during... we won't tell", "survived school today? you've earned a song")) to null
        Place.UNIVERSITY -> random.pick(listOf("lecture over? press play", "study playlist time")) to null
        Place.MALL -> random.pick(listOf("shopping soundtrack loaded", "the mall music is bad, use yours")) to null
        Place.RESTAURANT -> random.pick(listOf("enjoy the food, we'll handle the music", "good food deserves a good song")) to null
        Place.BAR -> random.pick(listOf("turn it up a little", "tonight's soundtrack is ready")) to null
        Place.HOSPITAL -> random.pick(listOf("hope everything's ok. something calm?", "take it easy, here's something soft")) to null
        Place.CINEMA -> random.pick(listOf("movie's about to start, pause the music?", "after the movie, the soundtrack")) to null
        Place.STADIUM -> random.pick(listOf("game day energy", "loud crowd, louder playlist")) to null
    }
}
