package dev.jhu.crossi

import android.app.Application
import dev.jhu.crossi.di.AppContainer

class CrossiApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
