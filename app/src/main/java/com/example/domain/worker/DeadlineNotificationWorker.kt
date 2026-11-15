package com.example.domain.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.data.local.SchoolDatabase
import com.example.domain.service.SchoolNotificationService
import com.example.ui.localization.AppLanguage
import java.util.UUID
import java.util.concurrent.TimeUnit

class DeadlineNotificationWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val db = SchoolDatabase.getInstance(applicationContext)
            val allocations = db.textbookDao().getAllAllocationsSync()
            val leaves = db.leaveDao().getAllLeavesSync()
            val prefs = applicationContext.getSharedPreferences("smart_school_prefs", Context.MODE_PRIVATE)
            val lang = if (prefs.getString("app_language", AppLanguage.ENGLISH.code) == AppLanguage.URDU.code) {
                AppLanguage.URDU
            } else {
                AppLanguage.ENGLISH
            }

            val summary = SchoolNotificationService.evaluateDeadlines(allocations, leaves)
            val dispatched = SchoolNotificationService.notifyUpcomingDeadlines(
                context = applicationContext,
                summary = summary,
                language = lang
            )

            Result.success(
                workDataOf(
                    KEY_OVERDUE_COUNT to summary.overdueBooks.size,
                    KEY_UPCOMING_COUNT to summary.upcomingBooks.size,
                    KEY_PENDING_LEAVES to summary.pendingLeavesCount,
                    KEY_NOTIFICATION_SENT to dispatched
                )
            )
        } catch (e: Exception) {
            Result.failure(
                workDataOf(KEY_ERROR to (e.localizedMessage ?: "Deadline check failed"))
            )
        }
    }

    companion object {
        const val KEY_OVERDUE_COUNT = "overdue_count"
        const val KEY_UPCOMING_COUNT = "upcoming_count"
        const val KEY_PENDING_LEAVES = "pending_leaves"
        const val KEY_NOTIFICATION_SENT = "notification_sent"
        const val KEY_ERROR = "error"
    }
}

object NotificationScheduler {
    const val UNIQUE_DEADLINE_CHECK_WORK = "daily_school_deadline_notifications"
    const val UNIQUE_IMMEDIATE_DEADLINE_WORK = "immediate_school_deadline_notifications"

    fun schedulePeriodicDeadlineChecks(context: Context) {
        val request = PeriodicWorkRequestBuilder<DeadlineNotificationWorker>(
            12, TimeUnit.HOURS
        )
            .addTag(UNIQUE_DEADLINE_CHECK_WORK)
            .build()

        WorkManager.getInstance(context.applicationContext)
            .enqueueUniquePeriodicWork(
                UNIQUE_DEADLINE_CHECK_WORK,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
    }

    fun triggerImmediateDeadlineCheck(context: Context): UUID {
        val request = OneTimeWorkRequestBuilder<DeadlineNotificationWorker>()
            .addTag(UNIQUE_IMMEDIATE_DEADLINE_WORK)
            .build()

        WorkManager.getInstance(context.applicationContext)
            .enqueueUniqueWork(
                UNIQUE_IMMEDIATE_DEADLINE_WORK,
                ExistingWorkPolicy.REPLACE,
                request
            )
        return request.id
    }
}
