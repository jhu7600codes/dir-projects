package com.simulasim.app.teacher

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import kotlin.random.Random

enum class Trait(val label: String) {
    NERD("nerd"), CLOWN("class clown"), SLEEPY("sleepy"), REBEL("rebel"),
    PHONE("phone addict"), CHATTY("chatterbox"), NORMAL("normal kid"),
}

enum class SState(val icon: String, val label: String, val bad: Boolean) {
    OK("🙂", "listening", false),
    HAND("✋", "hand up", false),
    SLEEP("😴", "sleeping", true),
    PHONE("📱", "on phone", true),
    TALK("💬", "talking", true),
    PRANK("🤡", "pranking", true),
    FIGHT("😡", "arguing", true),
    GONE("🚪", "at principal's office", false),
}

data class Student(
    val id: Int,
    val name: String,
    val trait: Trait,
    val knowledge: Double,
    val mood: Double,
    val state: SState = SState.OK,
    val front: Boolean = false,
    val handTurns: Int = 0,
    val grades: List<Int> = emptyList(),
)

enum class TPhase { INTRO, CLASS, BREAK, GRADING, DAY_END, WEEK_END, GAME_OVER }

class Choice(val label: String, val run: () -> Unit)
class TEvent(val title: String, val text: String, val choices: List<Choice>)

data class Paper(val student: String, val question: String, val shown: Int, val correct: Int)

const val TURNS_PER_PERIOD = 8
const val PERIODS_PER_DAY = 5
const val WEEKS_PER_TERM = 4

val DAYS = listOf("monday", "tuesday", "wednesday", "thursday", "friday")
private val SUBJECTS = listOf("math", "history", "science", "english", "geography", "literature", "physics", "biology")

private val NAMES = listOf(
    "Artyom", "Sasha", "Masha", "Dima", "Liza", "Kirill", "Vova", "Nastya", "Egor", "Polina",
    "Maks", "Sonya", "Timur", "Alina", "Gosha", "Vika", "Lyosha", "Katya", "Danya", "Yulia",
    "Misha", "Dasha", "Nikita", "Vera", "Stas", "Ksyusha", "Ilya", "Zhenya", "Roma", "Lera",
)

class TeacherViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = app.getSharedPreferences("teacher", 0)

    var phase by mutableStateOf(TPhase.INTRO); private set
    val students = mutableStateListOf<Student>()
    val log = mutableStateListOf<String>()
    var event by mutableStateOf<TEvent?>(null); private set

    var week by mutableIntStateOf(1); private set
    var day by mutableIntStateOf(0); private set
    var period by mutableIntStateOf(0); private set
    var turn by mutableIntStateOf(0); private set
    var subjects by mutableStateOf(listOf<String>()); private set

    var energy by mutableDoubleStateOf(100.0); private set
    var patience by mutableDoubleStateOf(100.0); private set
    var rep by mutableDoubleStateOf(50.0); private set
    var money by mutableDoubleStateOf(0.0); private set
    var teachBonus by mutableDoubleStateOf(1.0); private set
    var quizzedThisPeriod by mutableStateOf(false); private set
    var sentToday by mutableIntStateOf(0); private set
    var lastExamAvg by mutableDoubleStateOf(0.0); private set
    var gameOverTitle by mutableStateOf(""); private set
    var gameOverText by mutableStateOf(""); private set
    var periodSummary by mutableStateOf(""); private set
    var bestWeeks by mutableIntStateOf(prefs.getInt("bestWeeks", 0)); private set

    // grading minigame
    val papers = mutableStateListOf<Paper>()
    var paperIndex by mutableIntStateOf(0); private set
    var gradedRight by mutableIntStateOf(0); private set
    var lastGradeFeedback by mutableStateOf<String?>(null); private set

    private var knowledgeAtPeriodStart = 0.0

    val subject get() = subjects.getOrElse(period) { "math" }
    val present get() = students.filter { it.state != SState.GONE }
    val discipline: Double
        get() {
            val p = present
            if (p.isEmpty()) return 1.0
            return p.count { !it.state.bad }.toDouble() / p.size
        }
    val avgKnowledge get() = students.map { it.knowledge }.average().takeIf { !it.isNaN() } ?: 0.0
    val avgMood get() = students.map { it.mood }.average().takeIf { !it.isNaN() } ?: 0.0

    val clock: String
        get() {
            // periods start 8:30, 9:25, 10:20, 11:30 (long lunch... jk it's 10 minutes), 12:25
            val starts = listOf(8 * 60 + 30, 9 * 60 + 25, 10 * 60 + 20, 11 * 60 + 30, 12 * 60 + 25)
            val m = starts.getOrElse(period) { 8 * 60 + 30 } + turn * 45 / TURNS_PER_PERIOD
            return "%d:%02d".format(m / 60, m % 60)
        }

    fun newGame() {
        students.clear()
        val names = NAMES.shuffled().take(12)
        val traits = (listOf(Trait.NERD, Trait.NERD, Trait.CLOWN, Trait.SLEEPY, Trait.REBEL, Trait.PHONE, Trait.PHONE, Trait.CHATTY, Trait.CHATTY) +
            List(3) { Trait.NORMAL }).shuffled()
        names.forEachIndexed { i, n ->
            val t = traits[i]
            students.add(
                Student(
                    id = i, name = n, trait = t,
                    knowledge = when (t) {
                        Trait.NERD -> Random.nextDouble(55.0, 75.0)
                        Trait.REBEL, Trait.PHONE -> Random.nextDouble(15.0, 35.0)
                        else -> Random.nextDouble(25.0, 50.0)
                    },
                    mood = Random.nextDouble(45.0, 75.0),
                )
            )
        }
        week = 1; day = 0; period = 0; turn = 0
        energy = 100.0; patience = 100.0; rep = 50.0; money = 0.0; teachBonus = 1.0
        log.clear()
        startDay()
    }

    private fun startDay() {
        subjects = SUBJECTS.shuffled().take(PERIODS_PER_DAY)
        period = 0
        sentToday = 0
        update { if (it.state == SState.GONE || it.state.bad || it.state == SState.HAND) it.copy(state = SState.OK, handTurns = 0) else it }
        startPeriod()
    }

    private fun startPeriod() {
        turn = 0
        quizzedThisPeriod = false
        knowledgeAtPeriodStart = avgKnowledge
        // a fresh bell doesn't fix everyone
        update {
            if (it.state == SState.GONE) it
            else if (Random.nextDouble() < 0.15 && it.trait != Trait.NERD) it.copy(state = badStateFor(it.trait))
            else it.copy(state = SState.OK, handTurns = 0)
        }
        say("— ${DAYS[day]}, period ${period + 1}: $subject —")
        phase = TPhase.CLASS
    }

    // ---------------- class actions ----------------

    fun teach() = act {
        val attention = discipline
        update { s ->
            if (s.state == SState.GONE) s
            else {
                val mult = (if (s.trait == Trait.NERD) 1.5 else 1.0) * (if (s.front) 1.2 else 1.0) * teachBonus
                val gain = if (s.state.bad) 0.4 else Random.nextDouble(2.5, 5.0) * mult
                s.copy(knowledge = (s.knowledge + gain).coerceAtMost(100.0), mood = s.mood - 1.5)
            }
        }
        energy -= 6
        say(
            when {
                attention > 0.85 -> "you explained $subject. they actually got it. wow"
                attention > 0.5 -> "you taught $subject. about half the class was there mentally"
                else -> "you taught $subject to a room of zombies and phone screens"
            }
        )
        maybeHands()
    }

    fun funActivity() = act {
        update { s ->
            if (s.state == SState.GONE) s
            else s.copy(
                knowledge = (s.knowledge + Random.nextDouble(1.0, 2.5) * teachBonus).coerceAtMost(100.0),
                mood = s.mood + 6,
                state = if (s.state.bad && Random.nextDouble() < 0.55) SState.OK else s.state,
            )
        }
        energy -= 9
        say(listOf(
            "you made a $subject quiz game with teams. even the rebels played",
            "$subject kahoot. someone named themselves \"teacher is mid\"",
            "group activity: build a $subject poster. glitter is everywhere now",
        ).random())
    }

    fun quiz() = act {
        if (quizzedThisPeriod) { say("they already had a quiz this period. mutiny is brewing"); update { it.copy(mood = it.mood - 4) }; return@act }
        quizzedThisPeriod = true
        val scores = mutableListOf<Int>()
        update { s ->
            if (s.state == SState.GONE) s
            else {
                val score = (s.knowledge + Random.nextDouble(-15.0, 15.0) + (if (s.state.bad) -10 else 0)).coerceIn(0.0, 100.0).toInt()
                scores += score
                s.copy(grades = (s.grades + score).takeLast(10), mood = s.mood - (if (score < 50) 10 else 3), state = if (s.state == SState.SLEEP) SState.OK else s.state)
            }
        }
        rep += 2
        energy -= 3
        val avg = if (scores.isEmpty()) 0.0 else scores.average()
        say("pop quiz! average: ${avg.toInt()}% (${letter(avg)}). groans heard from the hallway")
    }

    fun joke() = act {
        energy -= 3
        if (Random.nextDouble() < 0.6 + (avgMood - 50) / 200) {
            update { s -> if (s.state == SState.GONE) s else s.copy(mood = s.mood + 8, state = if (s.state.bad && Random.nextDouble() < 0.35) SState.OK else s.state) }
            patience += 4
            say(listOf(
                "your $subject pun landed. even the rebel laughed",
                "you did the \"why did the chicken cross the road\" bit but for $subject. it killed",
                "you said \"skibidi\" unironically. they lost it",
            ).random())
        } else {
            update { s -> if (s.state == SState.GONE) s else s.copy(mood = s.mood - 2) }
            patience -= 5
            say(listOf("your joke flopped. silence. someone coughed", "\"ok boomer\" — the entire class, in unison", "cringe. they're going to post this").random())
        }
    }

    fun yell() = act {
        var fixed = 0
        update { s ->
            if (s.state.bad) { fixed++; s.copy(state = SState.OK, mood = s.mood - 10) } else if (s.state == SState.GONE) s else s.copy(mood = s.mood - 5)
        }
        energy -= 9
        patience += 6
        say(if (fixed == 0) "you yelled QUIET at a quiet class. now they think you're unhinged" else "you yelled QUIET. $fixed kids snapped back. everyone's a bit scared now")
        if (fixed == 0) rep -= 2
    }

    fun video() = act {
        energy += 10
        update { s ->
            if (s.state == SState.GONE) s
            else s.copy(
                knowledge = (s.knowledge + 1.2).coerceAtMost(100.0),
                mood = s.mood + 3,
                state = if ((s.trait == Trait.SLEEPY && Random.nextDouble() < 0.5) || Random.nextDouble() < 0.12) SState.SLEEP else s.state,
            )
        }
        say(listOf("you put on a $subject video from 2009. the narrator has a soothing voice", "documentary time. you rested your eyes. for a second. or ten", "crash course $subject at 1.5x speed").random())
    }

    // ---------------- student actions ----------------

    fun callOn(id: Int) = act {
        val s = students.find { it.id == id } ?: return@act
        val chance = s.knowledge / 100 + (if (s.state == SState.HAND) 0.3 else 0.0) - (if (s.state.bad) 0.25 else 0.0)
        if (Random.nextDouble() < chance) {
            set(id) { it.copy(mood = it.mood + 7, knowledge = it.knowledge + 2, state = SState.OK, handTurns = 0) }
            update { if (it.state == SState.GONE || it.id == id) it else it.copy(knowledge = (it.knowledge + 0.8).coerceAtMost(100.0)) }
            say("${s.name} nailed the answer. ${if (s.trait == Trait.NERD) "obviously" else "nice"}")
        } else {
            set(id) { it.copy(mood = it.mood - 6, state = SState.OK, handTurns = 0) }
            say(if (s.state.bad) "${s.name} had no idea what the question was. they're paying attention now at least" else "${s.name} got it wrong. you explained it again")
        }
        energy -= 2
    }

    fun handle(id: Int) = act {
        val s = students.find { it.id == id } ?: return@act
        val (line, moodHit) = when (s.state) {
            SState.SLEEP -> "you woke ${s.name} up. there's drool on the desk" to 4.0
            SState.PHONE -> "you confiscated ${s.name}'s phone. they're mourning" to 14.0
            SState.TALK -> "you told ${s.name} to stop talking. they stopped. mid-sentence" to 5.0
            SState.PRANK -> "you caught ${s.name} mid-prank. the whoopee cushion is now yours" to 7.0
            SState.FIGHT -> "you broke up ${s.name}'s argument. it was about minecraft" to 6.0
            else -> "you stared at ${s.name}. they don't know what they did" to 3.0
        }
        set(id) { it.copy(state = SState.OK, mood = it.mood - moodHit) }
        if (s.trait == Trait.REBEL && Random.nextDouble() < 0.35) {
            patience -= 10
            say("$line... and they talked back. \"you can't tell me what to do\"")
        } else say(line)
        energy -= 3
    }

    fun praise(id: Int) = act {
        val s = students.find { it.id == id } ?: return@act
        set(id) { it.copy(mood = it.mood + 12, state = if (it.state.bad && Random.nextDouble() < 0.5) SState.OK else it.state) }
        say(if (s.state.bad) "you praised ${s.name} while they were ${s.state.label}. confusing, but they're happy" else "you praised ${s.name}. they'll remember this forever")
    }

    fun moveFront(id: Int) = act {
        val s = students.find { it.id == id } ?: return@act
        if (s.front) { say("${s.name} is already in the front row"); return@act }
        set(id) { it.copy(front = true, mood = it.mood - 6, state = SState.OK) }
        say("${s.name} got moved to the front row. betrayal in their eyes")
    }

    fun sendOut(id: Int) = act {
        val s = students.find { it.id == id } ?: return@act
        set(id) { it.copy(state = SState.GONE, mood = it.mood - 15) }
        sentToday++
        update { if (it.state == SState.GONE) it else it.copy(mood = it.mood - 1) }
        if (sentToday > 2) {
            rep -= 6
            say("you sent ${s.name} to the principal. that's #$sentToday today. the principal is \"concerned\"")
        } else {
            rep -= 1
            say("${s.name} is going to the principal's office. the class goes very quiet")
            update { if (it.state.bad && Random.nextDouble() < 0.4) it.copy(state = SState.OK) else it }
        }
    }

    // ---------------- turn engine ----------------

    private fun act(block: () -> Unit) {
        if (phase != TPhase.CLASS || event != null) return
        block()
        endTurn()
    }

    private fun endTurn() {
        // hands left hanging get sad
        update {
            when {
                it.state == SState.HAND && it.handTurns >= 1 -> it.copy(state = SState.OK, mood = it.mood - 5, handTurns = 0)
                it.state == SState.HAND -> it.copy(handTurns = it.handTurns + 1)
                else -> it
            }
        }
        // misbehaviour spreads like a cold
        val badCount = present.count { it.state.bad }
        val contagion = 1 + badCount * 0.12
        update { s ->
            if (s.state == SState.GONE) return@update s
            if (s.state.bad) {
                return@update if (Random.nextDouble() < 0.08) s.copy(state = SState.OK) else s.copy(mood = s.mood + 0.5)
            }
            val base = when (s.trait) {
                Trait.NERD -> 0.02; Trait.NORMAL -> 0.06; Trait.SLEEPY -> 0.13
                Trait.CLOWN, Trait.CHATTY -> 0.12; Trait.PHONE -> 0.15; Trait.REBEL -> 0.14
            }
            val p = base * (1.4 - s.mood / 120) * contagion * (if (s.front) 0.55 else 1.0) * (if (energy < 30) 1.3 else 1.0)
            if (Random.nextDouble() < p) s.copy(state = badStateFor(s.trait)) else s
        }
        update { it.copy(mood = it.mood.coerceIn(0.0, 100.0), knowledge = it.knowledge.coerceIn(0.0, 100.0)) }

        energy -= 1.5
        patience -= present.count { it.state.bad } * 1.3
        energy = energy.coerceIn(0.0, 100.0)
        patience = patience.coerceIn(0.0, 100.0)
        rep = rep.coerceIn(0.0, 100.0)

        turn++

        if (energy <= 0) {
            event = TEvent(
                "burnout",
                "you sat down at your desk and just... stared at the wall for the rest of the day. the vice principal took over your classes.",
                listOf(Choice("go home") { rep -= 10; endDay() }),
            )
            return
        }
        if (patience <= 0) {
            rep -= 10
            patience = 40.0
            update { if (it.state == SState.GONE) it else it.copy(state = SState.OK, mood = it.mood - 12) }
            event = TEvent(
                "you snapped",
                "you threw the chalk at the wall and said some things. someone filmed it. it has 40k views on tiktok already.",
                listOf(Choice("deep breaths") {}),
            )
            return
        }

        if (turn >= TURNS_PER_PERIOD) {
            endPeriod()
            return
        }
        if (Random.nextDouble() < 0.2) randomEvent()
    }

    private fun endPeriod() {
        val gained = avgKnowledge - knowledgeAtPeriodStart
        periodSummary = "%s done. class knowledge %+.1f, discipline %d%%".format(subject, gained, (discipline * 100).toInt())
        say("*bell rings* $periodSummary")
        if (period >= PERIODS_PER_DAY - 1) {
            endDay()
        } else {
            phase = TPhase.BREAK
        }
    }

    private fun endDay() {
        event = null
        val salary = 95.0
        money += salary
        phase = TPhase.DAY_END
    }

    fun eveningChoice(which: Int) {
        if (phase != TPhase.DAY_END) return
        when (which) {
            0 -> { energy = 100.0; teachBonus = 1.0 }
            1 -> { energy = 80.0; teachBonus = 1.35 }
            else -> { energy = 65.0; teachBonus = 1.0; money += 60 }
        }
        patience = (patience + 50).coerceAtMost(100.0)
        // they forget a bit overnight. they always do
        update { it.copy(knowledge = (it.knowledge - Random.nextDouble(0.5, 2.0)).coerceAtLeast(0.0), mood = (it.mood + 5).coerceAtMost(100.0)) }
        if (day >= DAYS.size - 1) {
            weekExam()
        } else {
            day++
            startDay()
        }
    }

    private fun weekExam() {
        update {
            val score = (it.knowledge + it.mood / 12 + Random.nextDouble(-8.0, 8.0)).coerceIn(0.0, 100.0).toInt()
            it.copy(grades = (it.grades + score).takeLast(10))
        }
        lastExamAvg = students.map { it.grades.last() }.average()
        rep += (lastExamAvg - 55) / 2 + (avgMood - 50) / 10
        rep = rep.coerceIn(0.0, 100.0)
        if (week > bestWeeks) {
            bestWeeks = week
            prefs.edit().putInt("bestWeeks", week).apply()
        }
        phase = TPhase.WEEK_END
    }

    fun continueAfterExam() {
        if (phase != TPhase.WEEK_END) return
        when {
            rep < 15 -> gameOver(
                "you're fired",
                "the principal called you in on friday at 4:55pm. \"we're going in a different direction.\" you lasted $week week${if (week > 1) "s" else ""}.",
            )
            week >= WEEKS_PER_TERM -> {
                val title = when {
                    rep >= 75 && lastExamAvg >= 70 -> "TEACHER OF THE YEAR"
                    rep >= 50 -> "you survived the term"
                    else -> "you survived... barely"
                }
                gameOver(
                    title,
                    "term over. final exam avg ${lastExamAvg.toInt()}%, reputation ${rep.toInt()}, earned ${"$%.0f".format(money)}. " +
                        if (title == "TEACHER OF THE YEAR") "they gave you a mug. it says \"world's okayest teacher\". you cried a little." else "summer break. you've earned it.",
                )
            }
            else -> {
                week++
                day = 0
                energy = 100.0
                startDay()
            }
        }
    }

    private fun gameOver(title: String, text: String) {
        gameOverTitle = title
        gameOverText = text
        phase = TPhase.GAME_OVER
    }

    // ---------------- breaks ----------------

    fun breakChoice(which: Int) {
        if (phase != TPhase.BREAK) return
        when (which) {
            0 -> { energy = (energy + 25).coerceAtMost(100.0); money -= 3; say("coffee #${Random.nextInt(2, 6)}. your hands are shaking but you're awake") }
            1 -> { startGrading(); return }
            2 -> {
                patience = (patience + 25).coerceAtMost(100.0); energy = (energy + 5).coerceAtMost(100.0)
                say(listOf(
                    "teacher's lounge gossip: the PE teacher is \"dating\" the cafeteria lady",
                    "teacher's lounge: someone microwaved fish again. you bonded over hating them",
                    "teacher's lounge: the math teacher has been here 30 years and has no soul left. relatable",
                ).random())
            }
            else -> {
                energy = (energy + 18).coerceAtMost(100.0)
                if (Random.nextDouble() < 0.25) { rep -= 5; say("you napped in your car and were 6 minutes late. the class was running a bitcoin mine") }
                else say("power nap in the car. 10/10")
            }
        }
        period++
        startPeriod()
    }

    private fun startGrading() {
        papers.clear()
        repeat(6) {
            val s = students.random()
            val (q, correct) = when (Random.nextInt(3)) {
                0 -> { val a = Random.nextInt(3, 13); val b = Random.nextInt(3, 13); "$a × $b" to a * b }
                1 -> { val a = Random.nextInt(12, 99); val b = Random.nextInt(12, 99); "$a + $b" to a + b }
                else -> { val a = Random.nextInt(40, 150); val b = Random.nextInt(5, 39); "$a − $b" to a - b }
            }
            val wrong = Random.nextDouble() < (1 - s.knowledge / 120)
            val shown = if (wrong) correct + listOf(-10, -2, -1, 1, 2, 10).random() else correct
            papers.add(Paper(s.name, q, shown, correct))
        }
        paperIndex = 0
        gradedRight = 0
        lastGradeFeedback = null
        phase = TPhase.GRADING
    }

    fun grade(markedCorrect: Boolean) {
        if (phase != TPhase.GRADING) return
        val p = papers.getOrNull(paperIndex) ?: return
        val isCorrect = p.shown == p.correct
        if (markedCorrect == isCorrect) {
            gradedRight++
            lastGradeFeedback = "right"
        } else {
            lastGradeFeedback = if (isCorrect) "that was correct! ${p.student}'s parents will hear about this" else "wrong, it's ${p.correct}. oops"
        }
        paperIndex++
        if (paperIndex >= papers.size) {
            val wrong = papers.size - gradedRight
            rep += gradedRight * 1.2 - wrong * 2.5
            energy -= 5
            rep = rep.coerceIn(0.0, 100.0)
            say("graded ${papers.size} papers, $gradedRight correctly. ${if (wrong == 0) "flawless" else "$wrong angry emails incoming"}")
            period++
            startPeriod()
        }
    }

    // ---------------- events ----------------

    private fun randomEvent() {
        if (present.isEmpty()) return // you sent literally everyone out. the room is very calm
        val chaos = 1 - discipline
        val all = listOf(
            {
                if (chaos > 0.35) {
                    rep -= 8
                    TEvent("the principal walks in", "...and sees ${present.count { it.state.bad }} kids doing literally anything but learning. they write something in a little notebook.", listOf(Choice("oh no") {}))
                } else {
                    rep += 6
                    TEvent("the principal walks in", "the class is focused. the principal nods slowly. you've never felt more powerful.", listOf(Choice("nice") {}))
                }
            },
            {
                val s = present.random()
                TEvent(
                    "bathroom request", "${s.name}: \"can i go to the bathroom?\" (it's the ${Random.nextInt(3, 7)}th time this week)",
                    listOf(
                        Choice("sure") { set(s.id) { it.copy(mood = it.mood + 5) }; say("${s.name} left. they'll be back in 25 minutes with a snack") },
                        Choice("no, wait") { set(s.id) { it.copy(mood = it.mood - 8, state = SState.TALK) }; say("${s.name} is now complaining loudly about human rights") },
                    ),
                )
            },
            {
                TEvent(
                    "fire alarm", "the fire alarm goes off. it's not a drill. it's also not a fire. someone vaped in the bathroom.",
                    listOf(Choice("evacuate") { energy -= 6; turn = (turn + 2).coerceAtMost(TURNS_PER_PERIOD - 1); update { if (it.state == SState.GONE) it else it.copy(mood = it.mood + 6) }; say("15 minutes outside. the kids loved it") }),
                )
            },
            {
                TEvent(
                    "projector died", "the projector makes a sad noise and turns off. 20 kids stare at you.",
                    listOf(
                        Choice("fix it yourself") {
                            energy -= 8
                            if (Random.nextDouble() < 0.5) { rep += 3; say("you fixed the projector by hitting it. tech god") }
                            else { say("you pressed every button. it's now showing a windows xp error. the class is cheering") ; update { it.copy(mood = it.mood + 3) } }
                        },
                        Choice("call IT") { say("IT said they'll come \"after lunch\". which lunch is unclear") },
                    ),
                )
            },
            {
                val s = present.random()
                TEvent(
                    "birthday cake", "it's ${s.name}'s birthday. their mom sent a giant cake.",
                    listOf(
                        Choice("party time") { turn = (turn + 1).coerceAtMost(TURNS_PER_PERIOD - 1); update { if (it.state == SState.GONE) it else it.copy(mood = it.mood + 10) }; energy += 5; say("cake for everyone. no learning happened. worth it") },
                        Choice("after class") { set(s.id) { it.copy(mood = it.mood - 10) }; say("${s.name} is sad. the cake is sweating in the corner") },
                    ),
                )
            },
            {
                TEvent(
                    "angry parent email", "\"why did my son get a C. he is very gifted. he told me so. — Concerned Parent\"",
                    listOf(
                        Choice("polite reply") { energy -= 5; rep += 3; say("you wrote a polite email. it took 20 minutes and a piece of your soul") },
                        Choice("ignore") { rep -= 4; say("you left them on read. they're calling the school now") },
                        Choice("\"he is not gifted\"") { rep -= 10; patience += 25; say("you hit send. it felt amazing. the principal wants a word") },
                    ),
                )
            },
            {
                val s = present.random()
                TEvent(
                    "\"when will we ever use this?\"", "${s.name} asks why they even need to learn $subject.",
                    listOf(
                        Choice("real answer") { update { if (it.state == SState.GONE) it else it.copy(knowledge = it.knowledge + 1.5) }; energy -= 4; say("you gave a real-world example. ${s.name} nodded. growth") },
                        Choice("\"it's on the exam\"") { update { if (it.state == SState.GONE) it else it.copy(mood = it.mood - 4) }; say("the classic. the class sighs as one") },
                        Choice("\"to become rich\"") { set(s.id) { it.copy(mood = it.mood + 6) }; say("${s.name} is now motivated by money. capitalism wins again") },
                    ),
                )
            },
            {
                TEvent(
                    "a wasp", "a wasp flies in through the window. three kids are screaming. one is trying to befriend it.",
                    listOf(
                        Choice("deal with it") { energy -= 6; rep += 2; say("you escorted the wasp out with a worksheet. heroic") },
                        Choice("evacuate the corner") { update { if (it.state == SState.GONE) it else if (Random.nextDouble() < 0.4) it.copy(state = SState.TALK) else it }; say("the wasp now owns the back corner. the class is chaos") },
                    ),
                )
            },
            {
                TEvent(
                    "tiktok dance", "half the class wants to film a tiktok dance for \"a school project\".",
                    listOf(
                        Choice("join them") { update { if (it.state == SState.GONE) it else it.copy(mood = it.mood + 12) }; if (Random.nextDouble() < 0.5) { rep -= 5; say("you danced. it went viral. the principal saw. mixed reviews") } else say("you danced. it went viral. you're the cool teacher now") },
                        Choice("absolutely not") { update { if (it.state == SState.GONE) it else it.copy(mood = it.mood - 5) }; say("\"you're no fun\" — 14 people") },
                    ),
                )
            },
            {
                val rebels = present.filter { it.trait == Trait.REBEL || it.trait == Trait.CLOWN }
                val s = rebels.randomOrNull() ?: present.random()
                TEvent(
                    "chair tipping", "${s.name} is leaning back on two chair legs. further. further...",
                    listOf(
                        Choice("\"four legs please\"") { set(s.id) { it.copy(mood = it.mood - 2) }; say("${s.name} slammed back down. drama averted") },
                        Choice("let physics teach") {
                            if (Random.nextDouble() < 0.6) { set(s.id) { it.copy(mood = it.mood - 10, knowledge = it.knowledge + 3) }; update { if (it.state == SState.GONE) it else it.copy(mood = it.mood + 4) }; say("${s.name} fell. they're fine. they learned about gravity") }
                            else say("${s.name} balanced perfectly. respect, honestly")
                        },
                    ),
                )
            },
        )
        event = all.random()()
    }

    fun choose(c: Choice) {
        event = null
        c.run()
        energy = energy.coerceIn(0.0, 100.0)
        patience = patience.coerceIn(0.0, 100.0)
        rep = rep.coerceIn(0.0, 100.0)
        update { it.copy(mood = it.mood.coerceIn(0.0, 100.0), knowledge = it.knowledge.coerceIn(0.0, 100.0)) }
    }

    // ---------------- helpers ----------------

    private fun maybeHands() {
        update { s ->
            if (s.state == SState.OK && Random.nextDouble() < (if (s.trait == Trait.NERD) 0.35 else 0.06)) s.copy(state = SState.HAND, handTurns = 0) else s
        }
    }

    private fun badStateFor(t: Trait): SState = when (t) {
        Trait.SLEEPY -> SState.SLEEP
        Trait.PHONE -> SState.PHONE
        Trait.CHATTY -> SState.TALK
        Trait.CLOWN -> if (Random.nextBoolean()) SState.PRANK else SState.TALK
        Trait.REBEL -> listOf(SState.FIGHT, SState.PHONE, SState.PRANK).random()
        Trait.NERD -> SState.TALK
        Trait.NORMAL -> listOf(SState.PHONE, SState.TALK, SState.SLEEP).random()
    }

    private inline fun update(f: (Student) -> Student) {
        for (i in students.indices) students[i] = f(students[i])
    }

    private fun set(id: Int, f: (Student) -> Student) {
        val i = students.indexOfFirst { it.id == id }
        if (i >= 0) students[i] = f(students[i])
    }

    private fun say(s: String) {
        log.add(0, s)
        while (log.size > 40) log.removeAt(log.size - 1)
    }
}

fun letter(score: Double) = when {
    score >= 90 -> "A"
    score >= 80 -> "B"
    score >= 70 -> "C"
    score >= 60 -> "D"
    else -> "F"
}
