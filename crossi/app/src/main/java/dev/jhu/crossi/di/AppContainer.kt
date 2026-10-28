package dev.jhu.crossi.di

import android.content.Context
import dev.jhu.crossi.data.ContentRepository
import dev.jhu.crossi.data.CrossiDatabase
import dev.jhu.crossi.data.GameRepository
import dev.jhu.crossi.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Manual DI. Viewmodels pull what they need from [dev.jhu.crossi.CrossiApp.container]. */
class AppContainer(context: Context) {
    /** Lives as long as the app: background puzzle generation, saves that must outlive a screen. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val database = CrossiDatabase.create(context)
    val games = GameRepository(database)
    val settings = SettingsRepository(context)
    val content = ContentRepository(context, appScope)
}
