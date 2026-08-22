package com.spy.game.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.spy.game.R
import com.spy.game.ui.components.MascotImage
import com.spy.game.ui.components.SpyBigRedButton
import com.spy.game.ui.components.SpyCard
import com.spy.game.ui.components.SpyPrimaryButton
import com.spy.game.ui.components.SpyTextButton
import com.spy.game.ui.theme.SpyOnSurfaceMuted
import com.spy.game.ui.theme.SpyRed
import kotlinx.coroutines.launch

private const val TOTAL_STEPS = 12

/**
 * A scripted, tap-to-advance walkthrough of one full round -- reveal, hint
 * rounds, the big red button, discussion, voting, and the elimination
 * animation -- played out by the three mascots (Спай, Вотер, and Лейхер,
 * who's the spy this time). Nothing here touches [com.spy.game.viewmodel.GameViewModel];
 * it's a fixed script, not a simulation of the real game engine.
 */
@Composable
fun DemoScreen(onDone: () -> Unit) {
    var stepIndex by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "ДЕМО-ИГРА ${stepIndex + 1}/$TOTAL_STEPS",
                style = MaterialTheme.typography.labelLarge,
                color = SpyOnSurfaceMuted,
            )
            SpyTextButton(text = "Пропустить", onClick = onDone)
        }

        Spacer(Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedContent(
                targetState = stepIndex,
                transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(120)) },
                label = "demo-step",
            ) { step ->
                DemoStep(step)
            }
        }

        Spacer(Modifier.height(20.dp))

        SpyPrimaryButton(
            text = if (stepIndex < TOTAL_STEPS - 1) "Далее" else "Готово",
            onClick = {
                if (stepIndex < TOTAL_STEPS - 1) stepIndex += 1 else onDone()
            },
        )
    }
}

@Composable
private fun DemoStep(step: Int) {
    when (step) {
        0 -> DemoCard(title = "Три игрока садятся играть") {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.Bottom) {
                MascotImage(painter = painterResource(R.drawable.mascot_spiey), size = 72.dp)
                MascotImage(painter = painterResource(R.drawable.mascot_voter), size = 72.dp)
                MascotImage(painter = painterResource(R.drawable.mascot_leicher), size = 72.dp)
            }
            DemoBody("Спай, Вотер и Лейхер играют один быстрый раунд, чтобы показать, как работает игра. (В этой демо-игре шпион — Лейхер, но сами игроки этого пока не знают.)")
        }

        1 -> DemoCard(title = "Спай открывает карту", mascotRes = R.drawable.mascot_spiey) {
            DemoBody("Спай видит слово: «Аэропорт». Все, кроме шпиона, видят одно и то же слово.")
        }

        2 -> DemoCard(title = "Вотер открывает карту", mascotRes = R.drawable.mascot_voter) {
            DemoBody("Вотер тоже видит «Аэропорт» и запоминает его.")
        }

        3 -> DemoCard(title = "Лейхер открывает карту", mascotRes = R.drawable.mascot_leicher) {
            Text(
                "ШПИОН",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Black,
                color = SpyRed,
            )
            DemoBody("Слова нет! Теперь Лейхеру придётся угадывать по чужим подсказкам, не выдав себя.")
        }

        4 -> DemoCard(title = "Раунд 1 — подсказка в 1 слово") {
            DemoBody("Спай: «Самолёты». Вотер: «Очереди». Лейхер, не зная слова, отвечает наугад — «Билеты» — и пока звучит правдоподобно.")
        }

        5 -> DemoCard(title = "Никто пока не уверен — раунд 2") {
            DemoBody("Подсказки становятся длиннее — уже по 2 слова. Спай: «Взлётная полоса». Вотер: «Регистрация багажа». Лейхер начинает путаться в деталях...")
        }

        6 -> DemoCard(title = "У Вотера есть подозрение") {
            SpyBigRedButton(text = "Я знаю,\nкто шпион!", onClick = {})
            DemoBody("Вотер подозревает Лейхера и нажимает большую красную кнопку — начинается 3-минутное обсуждение.")
        }

        7 -> DemoCard(title = "Идёт обсуждение") {
            Text("3:00 → 0:00", style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Black)
            DemoBody("Три минуты все спорят и защищаются. Лейхер путается в объяснениях про регистрацию багажа.")
        }

        8 -> DemoCard(title = "Время вышло — голосование") {
            DemoBody("Каждый голосует за подозреваемого. Спай голосует за Лейхера. Вотер тоже голосует за Лейхера.")
        }

        9 -> DemoEliminationStep()

        10 -> DemoCard(title = "Лейхер был шпионом!", mascotRes = R.drawable.mascot_voter) {
            DemoBody("Мирные жители побеждают — шпион пойман. Игра продолжалась бы дальше, если бы шпион остался неразоблачён.")
        }

        else -> DemoCard(title = "Вот и всё!") {
            DemoBody("Теперь вы знаете, как играть. Нажмите «Готово» и соберите реальных игроков в «Настройке».")
        }
    }
}

@Composable
private fun DemoCard(title: String, mascotRes: Int? = null, content: @Composable () -> Unit) {
    SpyCard(modifier = Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
            if (mascotRes != null) {
                MascotImage(painter = painterResource(mascotRes), size = 88.dp)
                Spacer(Modifier.height(16.dp))
            }
            content()
        }
    }
}

@Composable
private fun DemoBody(text: String) {
    Spacer(Modifier.height(12.dp))
    Text(
        text,
        style = MaterialTheme.typography.bodyLarge,
        color = SpyOnSurfaceMuted,
        textAlign = TextAlign.Center,
    )
}

/** The demo's own version of the elimination "thrown out" animation, playing once when this step is shown. */
@Composable
private fun DemoEliminationStep() {
    val offsetY = remember { Animatable(0f) }
    val rotation = remember { Animatable(0f) }
    val fade = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        launch { offsetY.animateTo(500f, tween(700, easing = FastOutLinearInEasing)) }
        launch { rotation.animateTo(35f, tween(700)) }
        launch { fade.animateTo(0.15f, tween(500, delayMillis = 250)) }
    }

    SpyCard(modifier = Modifier.fillMaxWidth()) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "Голоса подсчитаны",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        translationY = offsetY.value
                        rotationZ = rotation.value
                    }
                    .alpha(fade.value),
            ) {
                MascotImage(painter = painterResource(R.drawable.mascot_leicher), size = 88.dp)
            }
            DemoBody("Лейхер набрал большинство голосов и выбывает из игры...")
        }
    }
}
