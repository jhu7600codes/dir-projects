package dev.jhu.crossi.ui.nav

import android.net.Uri
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import dev.jhu.crossi.R
import dev.jhu.crossi.game.GameMode
import dev.jhu.crossi.ui.common.PlaceholderScreen
import dev.jhu.crossi.ui.home.HomeScreen
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
fun CrossiNavHost() {
    val nav = rememberNavController()
    val back: () -> Unit = { nav.popBackStack() }

    NavHost(navController = nav, startDestination = HomeRoute) {
        composable<HomeRoute> {
            HomeScreen(
                onLevels = { nav.navigate(LevelsRoute) },
                onFreeRoam = { nav.navigate(FreeRoamRoute) },
                onDaily = { nav.navigate(DailyRoute) },
                onPowerUps = { nav.navigate(PowerUpsRoute) },
                onSettings = { nav.navigate(SettingsRoute) },
                onHowToPlay = { nav.navigate(HowToPlayRoute) },
            )
        }
        composable<LevelsRoute> { PlaceholderScreen(R.string.levels, back) }
        composable<FreeRoamRoute> { PlaceholderScreen(R.string.free_roam, back) }
        composable<DailyRoute> { PlaceholderScreen(R.string.daily, back) }
        composable<PowerUpsRoute> { PlaceholderScreen(R.string.power_ups, back) }
        composable<SettingsRoute> { PlaceholderScreen(R.string.settings, back) }
        composable<HowToPlayRoute> { PlaceholderScreen(R.string.how_to_play, back) }
        composable<GameRoute>(typeMap = mapOf(typeOf<GameMode>() to GameModeNavType)) { entry ->
            entry.toRoute<GameRoute>()
            PlaceholderScreen(R.string.app_name, back)
        }
    }
}
