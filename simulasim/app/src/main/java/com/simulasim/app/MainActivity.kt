package com.simulasim.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.simulasim.app.crypto.CryptoScreen
import com.simulasim.app.extra.BubbleWrapScreen
import com.simulasim.app.extra.HackerScreen
import com.simulasim.app.shake.ShakeLightScreen
import com.simulasim.app.teacher.TeacherScreen
import com.simulasim.app.ui.Bg
import com.simulasim.app.ui.SimTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SimTheme {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Bg)
                        .safeDrawingPadding()
                ) {
                    App()
                }
            }
        }
    }
}

enum class Sim { HOME, CRYPTO, TEACHER, SHAKE, HACKER, BUBBLE }

@Composable
fun App() {
    var sim by rememberSaveable { mutableStateOf(Sim.HOME) }
    val home = { sim = Sim.HOME }
    BackHandler(enabled = sim != Sim.HOME, onBack = home)

    AnimatedContent(targetState = sim, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "sim") { s ->
        when (s) {
            Sim.HOME -> HomeScreen(onOpen = { sim = it })
            Sim.CRYPTO -> CryptoScreen(onBack = home)
            Sim.TEACHER -> TeacherScreen(onBack = home)
            Sim.SHAKE -> ShakeLightScreen(onBack = home)
            Sim.HACKER -> HackerScreen(onBack = home)
            Sim.BUBBLE -> BubbleWrapScreen(onBack = home)
        }
    }
}
