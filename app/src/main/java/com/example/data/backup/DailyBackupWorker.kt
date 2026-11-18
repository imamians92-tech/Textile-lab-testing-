package com.example.data.backup

import android.content.Context
import androidx.work.Configuration
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.TextileLabApp
import java.util.concurrent.TimeUnit

class DailyBackupWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? TextileLabApp ?: return Result.failure()
        val outcome = app.container.backupManager.createBackup(
            backupType = "AUTO_DAILY",
            createdBy = "DailyScheduler"
        )
        return if (outcome.isSuccess) Result.success() else Result.retry()
    }

    companion object {
        private const val UNIQUE_WORK_NAME = "textile_lab_daily_sqlite_backup"

        fun schedule(context: Context) {
            runCatching {
                if (!WorkManager.isInitialized()) {
                    WorkManager.initialize(
                        context.applicationContext,
                        Configuration.Builder().build()
                    )
                }
                val request = PeriodicWorkRequestBuilder<DailyBackupWorker>(24, TimeUnit.HOURS)
                    .build()
                WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
                    UNIQUE_WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
                )
            }
        }
    }
}
