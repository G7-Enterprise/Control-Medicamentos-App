package com.carlos.controlmedicamentos.backup

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class AutoBackupWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            BackupManager.createAutomaticBackup(applicationContext)
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
