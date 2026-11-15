package com.example.domain.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.entities.LeaveApplicationEntity
import com.example.data.local.entities.TextbookAllocationEntity
import com.example.ui.localization.AppLanguage
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object SchoolNotificationService {

    const val CHANNEL_LEAVE_UPDATES = "ssm_leave_updates"
    const val CHANNEL_DEADLINES = "ssm_upcoming_deadlines"
    const val EXTRA_NAV_ROUTE = "extra_nav_route"

    private const val NOTIF_ID_DEADLINES_BASE = 4100
    private const val NOTIF_ID_LEAVE_BASE = 5100

    data class DeadlineSummary(
        val upcomingBooks: List<TextbookAllocationEntity> = emptyList(),
        val overdueBooks: List<TextbookAllocationEntity> = emptyList(),
        val pendingLeavesCount: Int = 0
    ) {
        val totalBookAlerts: Int get() = upcomingBooks.size + overdueBooks.size
    }

    fun ensureNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return

            val leaveChannel = NotificationChannel(
                CHANNEL_LEAVE_UPDATES,
                "Leave Status Updates",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts parents, students, and teachers when leave requests are approved or rejected."
            }

            val deadlineChannel = NotificationChannel(
                CHANNEL_DEADLINES,
                "Upcoming Deadlines & Returns",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Reminders for upcoming textbook return due dates, assignments, and pending reviews."
            }

            manager.createNotificationChannel(leaveChannel)
            manager.createNotificationChannel(deadlineChannel)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun evaluateDeadlines(
        allocations: List<TextbookAllocationEntity>,
        leaves: List<LeaveApplicationEntity>,
        referenceDateStr: String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()),
        windowDays: Int = 3
    ): DeadlineSummary {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val refDate = try {
            fmt.parse(referenceDateStr) ?: Date()
        } catch (_: Exception) {
            Date()
        }
        val windowEndStr = Calendar.getInstance().apply {
            time = refDate
            add(Calendar.DAY_OF_YEAR, windowDays)
        }.let { fmt.format(it.time) }

        val activeIssued = allocations.filter { it.status == "RECEIVED" && it.returnDueDate.isNotBlank() }
        val overdue = activeIssued.filter { it.returnDueDate < referenceDateStr }
        val upcoming = activeIssued.filter {
            it.returnDueDate >= referenceDateStr && it.returnDueDate <= windowEndStr
        }
        val pendingLeaves = leaves.count { it.status == "PENDING" }

        return DeadlineSummary(
            upcomingBooks = upcoming,
            overdueBooks = overdue,
            pendingLeavesCount = pendingLeaves
        )
    }

    fun notifyLeaveStatusUpdate(
        context: Context,
        leave: LeaveApplicationEntity,
        language: AppLanguage = AppLanguage.ENGLISH
    ): Boolean {
        ensureNotificationChannels(context)
        if (!hasNotificationPermission(context)) return false

        val isApproved = leave.status == "APPROVED"
        val title = if (language == AppLanguage.URDU) {
            if (isApproved) "درخواستِ رخصت منظور ہو گئی" else "درخواستِ رخصت مسترد کر دی گئی"
        } else {
            if (isApproved) "Leave Request Approved" else "Leave Request Rejected"
        }

        val noteSuffix = if (leave.reviewNotes.isNotBlank()) " • ${leave.reviewNotes}" else ""
        val body = if (language == AppLanguage.URDU) {
            "${leave.applicantName} (${leave.startDate} تا ${leave.endDate}) کی رخصت پر فیصلہ: ${leave.status}$noteSuffix"
        } else {
            "${leave.applicantName}'s ${leave.leaveType.lowercase(Locale.US)} leave (${leave.startDate} → ${leave.endDate}) was ${leave.status}$noteSuffix"
        }

        val pendingIntent = buildLaunchIntent(context, "leaves", NOTIF_ID_LEAVE_BASE + leave.id.toInt())

        val notification = NotificationCompat.Builder(context, CHANNEL_LEAVE_UPDATES)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        return try {
            NotificationManagerCompat.from(context).notify(
                NOTIF_ID_LEAVE_BASE + leave.id.toInt(),
                notification
            )
            true
        } catch (_: SecurityException) {
            false
        }
    }

    fun notifyUpcomingDeadlines(
        context: Context,
        summary: DeadlineSummary,
        language: AppLanguage = AppLanguage.ENGLISH
    ): Boolean {
        ensureNotificationChannels(context)
        if (!hasNotificationPermission(context)) return false
        if (summary.totalBookAlerts == 0 && summary.pendingLeavesCount == 0) return false

        val title = if (language == AppLanguage.URDU) {
            "اسکول یاددہانی: آخری تاریخیں اور زیر التواء کام"
        } else {
            "Upcoming Deadlines & School Alerts"
        }

        val parts = mutableListOf<String>()
        if (summary.overdueBooks.isNotEmpty()) {
            val sampleNames = summary.overdueBooks.take(2).joinToString(", ") { "${it.studentName} (${it.subject})" }
            parts.add("${summary.overdueBooks.size} overdue textbook return(s): $sampleNames")
        }
        if (summary.upcomingBooks.isNotEmpty()) {
            val sampleSoon = summary.upcomingBooks.take(2).joinToString(", ") { "${it.studentName} due ${it.returnDueDate}" }
            parts.add("${summary.upcomingBooks.size} textbook(s) due within 3 days: $sampleSoon")
        }
        if (summary.pendingLeavesCount > 0) {
            parts.add("${summary.pendingLeavesCount} pending leave request(s) awaiting review")
        }

        val body = parts.joinToString(" | ")
        val targetRoute = if (summary.totalBookAlerts > 0) "textbooks" else "leaves"
        val pendingIntent = buildLaunchIntent(context, targetRoute, NOTIF_ID_DEADLINES_BASE)

        val notification = NotificationCompat.Builder(context, CHANNEL_DEADLINES)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        return try {
            NotificationManagerCompat.from(context).notify(NOTIF_ID_DEADLINES_BASE, notification)
            true
        } catch (_: SecurityException) {
            false
        }
    }

    private fun buildLaunchIntent(context: Context, route: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_NAV_ROUTE, route)
        }
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
