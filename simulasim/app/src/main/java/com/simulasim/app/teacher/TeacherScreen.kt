package com.simulasim.app.teacher

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.simulasim.app.ui.Accent
import com.simulasim.app.ui.Blue
import com.simulasim.app.ui.Green
import com.simulasim.app.ui.Orange
import com.simulasim.app.ui.Red
import com.simulasim.app.ui.RowGap
import com.simulasim.app.ui.SimButton
import com.simulasim.app.ui.SimCard
import com.simulasim.app.ui.StatBar
import com.simulasim.app.ui.Surface1
import com.simulasim.app.ui.Surface2
import com.simulasim.app.ui.TextDim
import com.simulasim.app.ui.TextMain
import com.simulasim.app.ui.TopBar
import com.simulasim.app.ui.Vspace
import com.simulasim.app.ui.Yellow

@Composable
fun TeacherScreen(onBack: () -> Unit) {
    val vm: TeacherViewModel = viewModel()
    Column(Modifier.fillMaxSize()) {
        TopBar("teacher sim", onBack) {
            if (vm.phase != TPhase.INTRO && vm.phase != TPhase.GAME_OVER) {
                Text("$%.0f".format(vm.money), color = Green, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 12.dp))
            }
        }
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            when (vm.phase) {
                TPhase.INTRO -> Intro(vm)
                TPhase.CLASS -> ClassView(vm)
                TPhase.BREAK -> BreakView(vm)
                TPhase.GRADING -> GradingView(vm)
                TPhase.DAY_END -> DayEndView(vm)
                TPhase.WEEK_END -> WeekEndView(vm)
                TPhase.GAME_OVER -> GameOverView(vm)
            }
            Vspace(24)
        }
    }

    vm.event?.let { ev ->
        AlertDialog(
            onDismissRequest = {},
            containerColor = Surface1,
            title = { Text(ev.title, color = TextMain, fontWeight = FontWeight.Bold) },
            text = { Text(ev.text, color = TextMain) },
            confirmButton = {
                Column(horizontalAlignment = Alignment.End) {
                    ev.choices.forEach { c ->
                        TextButton(onClick = { vm.choose(c) }) { Text(c.label, color = Accent, fontWeight = FontWeight.SemiBold) }
                    }
                }
            },
        )
    }
}

@Composable
private fun Intro(vm: TeacherViewModel) {
    SimCard(Modifier.fillMaxWidth()) {
        Text("🍎", fontSize = 48.sp)
        Text("welcome to school 1337", fontWeight = FontWeight.Black, fontSize = 22.sp, color = TextMain)
        Vspace(8)
        Text(
            "you're the new teacher. 12 kids, 5 periods a day, ${WEEKS_PER_TERM} weeks until summer.\n\n" +
                "• each period has $TURNS_PER_PERIOD actions. teach, run games, quiz, joke, yell, or tap a kid to deal with them\n" +
                "• kids misbehave based on their personality and mood. it spreads\n" +
                "• the principal shows up at the worst times\n" +
                "• between periods: coffee, gossip, nap, or grade homework\n" +
                "• friday = exam. bad results + bad rep = fired\n" +
                "• energy hits 0 = burnout. patience hits 0 = you go viral (bad)",
            color = TextDim, fontSize = 14.sp,
        )
        Vspace(14)
        SimButton("start the term", Modifier.fillMaxWidth(), color = Yellow) { vm.newGame() }
        if (vm.bestWeeks > 0) {
            Vspace(8)
            Text("best run: survived ${vm.bestWeeks} week${if (vm.bestWeeks > 1) "s" else ""}", color = TextDim, fontSize = 12.sp)
        }
    }
}

@Composable
private fun Header(vm: TeacherViewModel) {
    SimCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("week ${vm.week} · ${DAYS[vm.day]}", color = TextDim, fontSize = 12.sp)
                Text("period ${vm.period + 1}/$PERIODS_PER_DAY · ${vm.subject}", color = TextMain, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(vm.clock, color = Yellow, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text("${TURNS_PER_PERIOD - vm.turn} actions left", color = TextDim, fontSize = 11.sp)
            }
        }
        Vspace(10)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatBar("energy", (vm.energy / 100).toFloat(), Green, Modifier.weight(1f))
            StatBar("patience", (vm.patience / 100).toFloat(), Blue, Modifier.weight(1f))
        }
        Vspace(6)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatBar("reputation", (vm.rep / 100).toFloat(), Yellow, Modifier.weight(1f))
            StatBar("discipline", vm.discipline.toFloat(), Orange, Modifier.weight(1f), valueText = "${(vm.discipline * 100).toInt()}%")
        }
        Vspace(6)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatBar("class knowledge", (vm.avgKnowledge / 100).toFloat(), Accent, Modifier.weight(1f))
            StatBar("class mood", (vm.avgMood / 100).toFloat(), Color(0xFFFF6FB5), Modifier.weight(1f))
        }
    }
}

@Composable
private fun ClassView(vm: TeacherViewModel) {
    var selected by remember { mutableStateOf<Int?>(null) }
    Header(vm)
    Vspace(10)

    // the board
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF1F3B2E))
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text("~ ${vm.subject} ~", color = Color(0xFFE8F5E9), fontWeight = FontWeight.Medium)
    }
    Vspace(10)

    vm.students.chunked(4).forEach { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            row.forEach { s -> Desk(s, Modifier.weight(1f)) { selected = s.id } }
        }
        Vspace(6)
    }
    Vspace(6)

    Row(horizontalArrangement = RowGap) {
        SimButton("teach", Modifier.weight(1f), color = Accent) { vm.teach() }
        SimButton("fun activity", Modifier.weight(1f), color = Green) { vm.funActivity() }
        SimButton("pop quiz", Modifier.weight(1f), color = Yellow, enabled = !vm.quizzedThisPeriod) { vm.quiz() }
    }
    Vspace(6)
    Row(horizontalArrangement = RowGap) {
        SimButton("joke", Modifier.weight(1f), color = Blue) { vm.joke() }
        SimButton("yell QUIET", Modifier.weight(1f), color = Red) { vm.yell() }
        SimButton("video", Modifier.weight(1f), color = Surface2) { vm.video() }
    }
    Vspace(12)
    Text("what's happening", color = TextDim, fontSize = 12.sp)
    Vspace(4)
    vm.log.take(6).forEachIndexed { i, line ->
        Text(
            "› $line",
            color = if (i == 0) TextMain else TextDim,
            fontSize = 13.sp,
            modifier = Modifier.padding(vertical = 2.dp),
        )
    }

    vm.students.find { it.id == selected }?.let { s ->
        StudentDialog(s, vm) { selected = null }
    }
}

@Composable
private fun Desk(s: Student, modifier: Modifier, onClick: () -> Unit) {
    val moodColor = when {
        s.mood >= 65 -> Green
        s.mood >= 35 -> Yellow
        else -> Red
    }
    val bg = when {
        s.state == SState.GONE -> Surface1.copy(alpha = 0.4f)
        s.state.bad -> Red.copy(alpha = 0.16f)
        s.state == SState.HAND -> Blue.copy(alpha = 0.2f)
        else -> Surface1
    }
    Column(
        modifier
            .aspectRatio(0.82f)
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(2.dp, moodColor.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
            .clickable(enabled = s.state != SState.GONE, onClick = onClick)
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(s.state.icon, fontSize = 24.sp)
        Text(s.name, color = TextMain, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(
            if (s.front) "front row" else s.state.label,
            color = TextDim, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun StudentDialog(s: Student, vm: TeacherViewModel, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Surface1,
        title = { Text("${s.state.icon} ${s.name}", color = TextMain, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("${s.trait.label} · ${s.state.label}", color = TextDim, fontSize = 13.sp)
                Vspace(10)
                StatBar("knowledge", (s.knowledge / 100).toFloat(), Accent)
                Vspace(6)
                StatBar("mood", (s.mood / 100).toFloat(), Green)
                if (s.grades.isNotEmpty()) {
                    Vspace(8)
                    Text("grades: " + s.grades.joinToString(" ") { letter(it.toDouble()) }, color = TextDim, fontSize = 12.sp)
                }
                Vspace(14)
                val actions = buildList {
                    add(Triple("call on them", Accent) { vm.callOn(s.id) })
                    if (s.state.bad) add(Triple(handleLabel(s.state), Orange) { vm.handle(s.id) })
                    add(Triple("praise", Green) { vm.praise(s.id) })
                    if (!s.front) add(Triple("move to front row", Blue) { vm.moveFront(s.id) })
                    add(Triple("send to principal", Red) { vm.sendOut(s.id) })
                }
                actions.forEach { (label, color, run) ->
                    SimButton(label, Modifier.fillMaxWidth(), color = color) { run(); onDismiss() }
                    Vspace(4)
                }
                Text("each costs 1 action", color = TextDim, fontSize = 11.sp)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("never mind", color = TextDim) } },
    )
}

private fun handleLabel(s: SState) = when (s) {
    SState.SLEEP -> "wake them up"
    SState.PHONE -> "take their phone"
    SState.TALK -> "tell them to stop talking"
    SState.PRANK -> "stop the prank"
    SState.FIGHT -> "break up the argument"
    else -> "deal with it"
}

@Composable
private fun BreakView(vm: TeacherViewModel) {
    Header(vm)
    Vspace(10)
    SimCard(Modifier.fillMaxWidth()) {
        Text("🔔 break", fontWeight = FontWeight.Black, fontSize = 22.sp, color = TextMain)
        Text(vm.periodSummary, color = TextDim, fontSize = 13.sp)
        Vspace(4)
        Text("10 minutes. pick one:", color = TextDim, fontSize = 13.sp)
        Vspace(12)
        SimButton("☕ coffee  (+25 energy, -\$3)", Modifier.fillMaxWidth(), color = Orange) { vm.breakChoice(0) }
        Vspace(6)
        SimButton("📝 grade homework  (rep, if you're accurate)", Modifier.fillMaxWidth(), color = Accent) { vm.breakChoice(1) }
        Vspace(6)
        SimButton("🗣 teacher's lounge gossip  (+25 patience)", Modifier.fillMaxWidth(), color = Blue) { vm.breakChoice(2) }
        Vspace(6)
        SimButton("🚗 nap in the car  (+18 energy, might be late)", Modifier.fillMaxWidth(), color = Surface2) { vm.breakChoice(3) }
    }
}

@Composable
private fun GradingView(vm: TeacherViewModel) {
    val p = vm.papers.getOrNull(vm.paperIndex) ?: return
    SimCard(Modifier.fillMaxWidth()) {
        Text("grading homework ${vm.paperIndex + 1}/${vm.papers.size}", color = TextDim, fontSize = 13.sp)
        Vspace(4)
        Text("is the answer correct?", color = TextMain, fontWeight = FontWeight.Bold, fontSize = 18.sp)
    }
    Vspace(12)
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFFF7F3E8))
            .padding(24.dp)
    ) {
        Column {
            Text("${p.student}'s homework", color = Color(0xFF555555), fontSize = 13.sp)
            Vspace(16)
            Text("${p.question} = ${p.shown}", color = Color(0xFF1A237E), fontSize = 34.sp, fontWeight = FontWeight.Bold)
            Vspace(8)
            Text("(i did this in 2 minutes on the bus)", color = Color(0xFF888888), fontSize = 11.sp)
        }
    }
    Vspace(12)
    Row(horizontalArrangement = RowGap) {
        SimButton("✗ wrong", Modifier.weight(1f).height(56.dp), color = Red) { vm.grade(false) }
        SimButton("✓ correct", Modifier.weight(1f).height(56.dp), color = Green) { vm.grade(true) }
    }
    vm.lastGradeFeedback?.let {
        Vspace(10)
        Text(if (it == "right") "✓ good call" else it, color = if (it == "right") Green else Red, fontSize = 14.sp)
    }
    Vspace(6)
    Text("graded right so far: ${vm.gradedRight}", color = TextDim, fontSize = 12.sp)
}

@Composable
private fun DayEndView(vm: TeacherViewModel) {
    SimCard(Modifier.fillMaxWidth()) {
        Text("🏠 ${DAYS[vm.day]} is over", fontWeight = FontWeight.Black, fontSize = 22.sp, color = TextMain)
        Vspace(6)
        Text("you got paid \$95. teacher salary moment.", color = TextDim, fontSize = 13.sp)
        Vspace(10)
        StatBar("class knowledge", (vm.avgKnowledge / 100).toFloat(), Accent)
        Vspace(6)
        StatBar("class mood", (vm.avgMood / 100).toFloat(), Color(0xFFFF6FB5))
        Vspace(6)
        StatBar("reputation", (vm.rep / 100).toFloat(), Yellow)
        Vspace(14)
        Text(if (vm.day == DAYS.size - 1) "tonight, then the weekly exam:" else "how do you spend the evening?", color = TextMain, fontWeight = FontWeight.SemiBold)
        Vspace(8)
        SimButton("😴 sleep 9 hours  (full energy)", Modifier.fillMaxWidth(), color = Blue) { vm.eveningChoice(0) }
        Vspace(6)
        SimButton("📚 plan tomorrow's lessons  (+35% teaching, 80 energy)", Modifier.fillMaxWidth(), color = Accent) { vm.eveningChoice(1) }
        Vspace(6)
        SimButton("💸 private tutoring  (+\$60, 65 energy)", Modifier.fillMaxWidth(), color = Green) { vm.eveningChoice(2) }
    }
}

@Composable
private fun WeekEndView(vm: TeacherViewModel) {
    SimCard(Modifier.fillMaxWidth()) {
        Text("📄 week ${vm.week} exam results", fontWeight = FontWeight.Black, fontSize = 22.sp, color = TextMain)
        Vspace(4)
        Text("class average: ${vm.lastExamAvg.toInt()}% (${letter(vm.lastExamAvg)})", color = if (vm.lastExamAvg >= 60) Green else Red, fontWeight = FontWeight.Bold)
        Vspace(10)
        vm.students.sortedByDescending { it.grades.lastOrNull() ?: 0 }.forEach { s ->
            val g = s.grades.lastOrNull() ?: 0
            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                Text(s.name, color = TextMain, modifier = Modifier.weight(1f), fontSize = 14.sp)
                Text(s.trait.label, color = TextDim, fontSize = 12.sp, modifier = Modifier.weight(1f))
                Text("$g%  ${letter(g.toDouble())}", color = if (g >= 60) Green else Red, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
        }
        Vspace(12)
        Text("principal's verdict: reputation now ${vm.rep.toInt()}", color = Yellow, fontWeight = FontWeight.SemiBold)
        Text(
            when {
                vm.rep < 15 -> "\"come see me in my office.\""
                vm.rep < 40 -> "\"we need to talk about your... methods.\""
                vm.rep < 70 -> "\"keep it up, i guess.\""
                else -> "\"the parents love you. don't let it go to your head.\""
            },
            color = TextDim, fontSize = 13.sp,
        )
        Vspace(14)
        SimButton(if (vm.week >= WEEKS_PER_TERM || vm.rep < 15) "see what happens" else "on to week ${vm.week + 1}", Modifier.fillMaxWidth(), color = Yellow) { vm.continueAfterExam() }
    }
}

@Composable
private fun GameOverView(vm: TeacherViewModel) {
    SimCard(Modifier.fillMaxWidth()) {
        Text(vm.gameOverTitle, fontWeight = FontWeight.Black, fontSize = 26.sp, color = if (vm.gameOverTitle.contains("fired")) Red else Yellow)
        Vspace(8)
        Text(vm.gameOverText, color = TextMain, fontSize = 15.sp)
        Vspace(16)
        SimButton("new term", Modifier.fillMaxWidth(), color = Accent) { vm.newGame() }
    }
}
