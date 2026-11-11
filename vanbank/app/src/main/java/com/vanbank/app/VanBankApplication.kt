package com.vanbank.app

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.vanbank.app.di.AppContainer
import com.vanbank.app.work.BillPayWorker
import java.util.concurrent.TimeUnit

class VanBankApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.notificationHelper.ensureChannels()
        scheduleBillPayWorker()
    }

    private fun scheduleBillPayWorker() {
        val request = PeriodicWorkRequestBuilder<BillPayWorker>(
            BillPayWorker.REPEAT_INTERVAL, TimeUnit.MINUTES,
        ).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            BillPayWorker.UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }
}
