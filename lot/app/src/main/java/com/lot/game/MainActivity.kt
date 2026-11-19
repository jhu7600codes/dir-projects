package com.lot.game

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

sealed interface Screen {
    data object Home : Screen
    data object Badges : Screen
    data object Player : Screen
    data object Scratch : Screen
    data object TvPick : Screen
    data object TvShow : Screen
    data object Host : Screen
    data object Stealer : Screen
    data object Claim : Screen
    data object Bingo : Screen
    data object Dumpster : Screen
    data object Derby : Screen
    data object Items : Screen
    data object Slots : Screen
    data object Campaign : Screen
    data object Boss : Screen
    data object Rain : Screen
    data object Hilo : Screen
    data object Claw : Screen
    data object Flight : Screen
    data object Tower : Screen
    data object Profile : Screen
    data object Creator : Screen
    data object CustomPlay : Screen
    data object MagicBall : Screen
    data object Locker : Screen
    data object Inspector : Screen
}

class Nav {
    val stack = mutableStateListOf<Screen>(Screen.Home)
    val current get() = stack.last()
    fun go(s: Screen) { stack.add(s) }
    fun back() { if (stack.size > 1) stack.removeAt(stack.lastIndex) }
    /** replace the top screen (used for "play again" loops) */
    fun swap(s: Screen) { stack[stack.lastIndex] = s }
}

class MainActivity : ComponentActivity() {
    private val vm: GameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val fx = Fx(applicationContext)
        setContent {
            LotTheme {
                CompositionLocalProvider(LocalFx provides fx) {
                    App(vm, fx)
                }
            }
        }
    }
}

@Composable
fun App(vm: GameViewModel, fx: Fx) {
    val state by vm.state.collectAsState()
    LaunchedEffect(state.sound, state.haptics) {
        fx.soundOn = state.sound
        fx.hapticsOn = state.haptics
    }
    LaunchedEffect(state.outfitHat, state.outfitShirt, state.outfitGlasses) { YouDude = youDude(state) }
    val nav = remember { Nav() }
    BackHandler(nav.stack.size > 1) { nav.back() }

    Box(
        Modifier
            .fillMaxSize()
            .background(Bg)
            .systemBarsPadding()
    ) {
        if (vm.loaded) {
            AnimatedContent(
                nav.current,
                transitionSpec = {
                    (slideInHorizontally(tween(260)) { it / 4 } + fadeIn(tween(260))) togetherWith
                        (slideOutHorizontally(tween(200)) { -it / 4 } + fadeOut(tween(200)))
                },
                label = "nav",
            ) { screen ->
                when (screen) {
                    Screen.Home -> HomeScreen(vm, state, nav)
                    Screen.Badges -> BadgesScreen(state, nav)
                    Screen.Player -> PlayerScreen(vm, state, nav)
                    Screen.Scratch -> ScratchScreen(vm, state, nav)
                    Screen.TvPick -> TvPickScreen(vm, state, nav)
                    Screen.TvShow -> TvShowScreen(vm, state, nav)
                    Screen.Host -> HostScreen(vm, state, nav)
                    Screen.Stealer -> StealerScreen(vm, state, nav)
                    Screen.Claim -> ClaimScreen(vm, state, nav)
                    Screen.Bingo -> BingoScreen(vm, state, nav)
                    Screen.Dumpster -> DumpsterScreen(vm, state, nav)
                    Screen.Derby -> DerbyScreen(vm, state, nav)
                    Screen.Items -> ItemsScreen(vm, state, nav)
                    Screen.Slots -> SlotsScreen(vm, state, nav)
                    Screen.Campaign -> CampaignScreen(vm, state, nav)
                    Screen.Boss -> BossScreen(vm, state, nav)
                    Screen.Rain -> RainScreen(vm, state, nav)
                    Screen.Hilo -> HiloScreen(vm, state, nav)
                    Screen.Claw -> ClawScreen(vm, state, nav)
                    Screen.Flight -> FlightScreen(vm, state, nav)
                    Screen.Tower -> TowerScreen(vm, state, nav)
                    Screen.Profile -> ProfileScreen(vm, state, nav)
                    Screen.Creator -> CreatorScreen(vm, state, nav)
                    Screen.CustomPlay -> CustomPlayScreen(vm, state, nav)
                    Screen.MagicBall -> MagicBallScreen(vm, state, nav)
                    Screen.Locker -> LockerScreen(vm, state, nav)
                    Screen.Inspector -> InspectorScreen(vm, state, nav)
                }
            }
        }
        Box(Modifier.align(Alignment.TopCenter)) {
            BadgePopup(vm.badgeQueue.firstOrNull()) { vm.popBadge() }
        }
    }
}

/** shows the tutorial the first time a mode is opened, plus a way to reopen it */
@Composable
fun ModeTutorial(
    vm: GameViewModel, state: GameState, mode: String, title: String, pages: List<String>,
    forceOpen: Boolean, countsAsMode: Boolean = true, onClosed: () -> Unit,
) {
    LaunchedEffect(mode) { if (countsAsMode) vm.markPlayed(mode) }
    if (mode !in state.tutorialsSeen || forceOpen) {
        TutorialDialog(title, pages) {
            vm.markTutorial(mode)
            onClosed()
        }
    }
}
