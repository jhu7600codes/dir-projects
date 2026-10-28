package dev.jhu.crossi.di

import android.content.Context

/**
 * Manual DI. Repositories (room, datastore, dictionaries) get wired in here as
 * the milestones land, viewmodels pull them from [dev.jhu.crossi.CrossiApp.container].
 */
class AppContainer(@Suppress("unused") private val context: Context)
