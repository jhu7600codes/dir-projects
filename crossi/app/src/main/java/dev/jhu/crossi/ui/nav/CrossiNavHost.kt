package dev.jhu.crossi.ui.nav

import android.net.Uri
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import dev.jhu.crossi.game.GameMode
import dev.jhu.crossi.ui.daily.DailyScreen
import dev.jhu.crossi.ui.freeroam.FreeRoamScreen
import dev.jhu.crossi.ui.game.GameScreen
import dev.jhu.crossi.ui.home.HomeScreen
import dev.jhu.crossi.ui.howto.HowToPlayScreen
import dev.jhu.crossi.ui.levels.LevelsScreen
import dev.jhu.crossi.ui.powerups.PowerUpsScreen
import dev.jhu.crossi.ui.settings.SettingsScreen
import kotlin.reflect.typeOf
import kotlinx.serialization.json.Json

// GameMode is a sealed class, so navigation needs to be told how to put it in a bundle.
private val GameModeNavType = object : NavType<GameMode>(isNullableAllowed = false) {
    override fun get(bundle: Bundle, key: String): GameMode? = bundle.getString(key)?.let(::parseValue)
    override fun parseValue(value: String): GameMode = Json.decodeFromString(value)
    override fun put(bundle: Bundle, key: String, value: GameMode) =
        bundle.putString(key, Json.encodeToString(GameMode.serializer(), value))
    override fun serializeAsValue(value: GameMode): String =
        Uri.encode(Json.encodeToString(GameMode.serializer(), value))
}

@Composable
fun CrossiNavHost(showHowToPlay: Boolean) {
    val nav = rememberNavController()
    val back: () -> Unit = { nav.popBackStack() }
    val play: (GameMode) -> Unit = { nav.navigate(GameRoute(it)) }

    LaunchedEffect(Unit) { if (showHowToPlay) nav.navigate(HowToPlayRoute) }

    NavHost(navController = nav, startDestination = HomeRoute) {
        composable<HomeRoute> {
            HomeScreen(
                onContinue = play,
                onLevels = { nav.navigate(LevelsRoute) },
                onFreeRoam = { nav.navigate(FreeRoamRoute) },
                onDaily = { nav.navigate(DailyRoute) },
                onPowerUps = { nav.navigate(PowerUpsRoute) },
                onSettings = { nav.navigate(SettingsRoute) },
                onHowToPlay = { nav.navigate(HowToPlayRoute) },
            )
        }
        composable<LevelsRoute> { LevelsScreen(onBack = back, onPlay = play) }
        composable<FreeRoamRoute> { FreeRoamScreen(onBack = back, onPlay = play) }
        composable<DailyRoute> { DailyScreen(onBack = back, onPlay = play) }
        composable<PowerUpsRoute> { PowerUpsScreen(onBack = back) }
        composable<SettingsRoute> { SettingsScreen(onBack = back) }
        composable<HowToPlayRoute> { HowToPlayScreen(onDone = back) }
        composable<GameRoute>(typeMap = mapOf(typeOf<GameMode>() to GameModeNavType)) { entry ->
            val route = entry.toRoute<GameRoute>()
            GameScreen(
                mode = route.mode,
                onBack = back,
                onPlayNext = { next ->
                    nav.navigate(GameRoute(next)) { popUpTo<GameRoute> { inclusive = true } }
                },
            )
        }
    }
}
