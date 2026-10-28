package dev.jhu.crossi.ui.nav

import dev.jhu.crossi.game.GameMode
import kotlinx.serialization.Serializable

@Serializable data object HomeRoute
@Serializable data object LevelsRoute
@Serializable data object FreeRoamRoute
@Serializable data object DailyRoute
@Serializable data object PowerUpsRoute
@Serializable data object SettingsRoute
@Serializable data object HowToPlayRoute

@Serializable data class GameRoute(val mode: GameMode)
