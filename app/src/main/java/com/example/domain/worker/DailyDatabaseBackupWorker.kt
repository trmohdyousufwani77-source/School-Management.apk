package com.example.domain.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.data.local.SchoolDatabase
import com.example.data.local.entities.AuditTrailEntity
import com.example.domain.service.DatabaseMaintenanceService
import java.util.UUID
import java.util.concurrent.TimeUnit

class DailyDatabaseBackupWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val backupFile = DatabaseMaintenanceService.exportDatabaseToExternalStorage(applicationContext)

            // Record audit trail entry for disaster recovery traceability
            val db = SchoolDatabase.getInstance(applicationContext)
            db.auditDao().insertAudit(
                AuditTrailEntity(
                    actionType = "DAILY_BACKUP_EXPORTED",
                    actor = "WorkManager (Scheduled Recovery)",
                    details = "Exported Room DB to external storage: ${backupFile.name} (${backupFile.length() / 1024} KB)"
                )
            )

            Result.success(
                workDataOf(
                    KEY_BACKUP_PATH to backupFile.absolutePath,
                    KEY_BACKUP_SIZE_BYTES to backupFile.length()
                )
            )
        } catch (e: Exception) {
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure(
                    workDataOf(KEY_ERROR_MESSAGE to (e.localizedMessage ?: "External backup failed"))
                )
            }
        }
    }

    companion object {
        const val KEY_BACKUP_PATH = "backup_path"
        const val KEY_BACKUP_SIZE_BYTES = "backup_size_bytes"
        const val KEY_ERROR_MESSAGE = "error_message"
    }
}

object BackupScheduler {
    const val UNIQUE_DAILY_BACKUP_WORK = "daily_room_disaster_recovery_backup"
    const val UNIQUE_IMMEDIATE_BACKUP_WORK = "immediate_room_disaster_recovery_backup"

    fun scheduleDailyDatabaseBackup(
        context: Context,
        replaceExisting: Boolean = false
    ) {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .build()

        val periodicRequest = PeriodicWorkRequestBuilder<DailyDatabaseBackupWorker>(
            24, TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .addTag(UNIQUE_DAILY_BACKUP_WORK)
            .build()

        val policy = if (replaceExisting) {
            ExistingPeriodicWorkPolicy.UPDATE
        } else {
            ExistingPeriodicWorkPolicy.KEEP
        }

        WorkManager.getInstance(context.applicationContext)
            .enqueueUniquePeriodicWork(
                UNIQUE_DAILY_BACKUP_WORK,
                policy,
                periodicRequest
            )
        DatabaseMaintenanceService.setDailyBackupEnabled(context, true)
    }

    fun cancelDailyDatabaseBackup(context: Context) {
        WorkManager.getInstance(context.applicationContext)
            .cancelUniqueWork(UNIQUE_DAILY_BACKUP_WORK)
        DatabaseMaintenanceService.setDailyBackupEnabled(context, false)
    }

    fun triggerImmediateExternalBackup(context: Context): UUID {
        val oneTimeRequest = OneTimeWorkRequestBuilder<DailyDatabaseBackupWorker>()
            .addTag(UNIQUE_IMMEDIATE_BACKUP_WORK)
            .build()

        WorkManager.getInstance(context.applicationContext)
            .enqueueUniqueWork(
                UNIQUE_IMMEDIATE_BACKUP_WORK,
                ExistingWorkPolicy.REPLACE,
                oneTimeRequest
            )
        return oneTimeRequest.id
    }
}
