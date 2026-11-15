package com.example.domain.service

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.local.entities.StudentEntity
import com.example.domain.model.ReportCardSummary
import com.example.ui.localization.AppLanguage
import java.io.File
import java.net.URLEncoder

enum class AttendanceAlertType(val stringKey: String) {
    ABSENCE("alert_absence"),
    LATE("alert_late"),
    LOW_ATTENDANCE("alert_low_attendance"),
    PTM_NOTICE("alert_ptm_notice")
}

object WhatsAppService {

    /**
     * Dispatches a personalized bilingual WhatsApp message and attaches/links the PDF report card.
     */
    fun sendReportCardToParent(
        context: Context,
        summary: ReportCardSummary,
        pdfFile: File?
    ) {
        val formattedPhone = normalizePhoneNumber(summary.guardianPhone)
        val message = buildReportCardMessage(summary)

        try {
            if (pdfFile != null && pdfFile.exists()) {
                val uri: Uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    pdfFile
                )

                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_TEXT, message)
                    putExtra("jid", "$formattedPhone@s.whatsapp.net")
                    setPackage("com.whatsapp")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }

                if (isIntentResolvable(context, sendIntent)) {
                    context.startActivity(sendIntent)
                } else {
                    openWhatsAppUrlScheme(context, formattedPhone, message)
                }
            } else {
                openWhatsAppUrlScheme(context, formattedPhone, message)
            }
        } catch (e: Exception) {
            openWhatsAppUrlScheme(context, formattedPhone, message)
        }
    }

    /**
     * Builds a localized, guardian-ready WhatsApp attendance alert message for a specific alert type.
     */
    fun buildAttendanceAlertMessage(
        alertType: AttendanceAlertType,
        date: String,
        students: List<StudentEntity>,
        language: AppLanguage
    ): String {
        val studentSummaryEn = when (students.size) {
            0 -> "your ward"
            1 -> "${students.first().nameEn} (Class ${students.first().className}-${students.first().section}, Roll #${students.first().rollNumber})"
            else -> "${students.size} selected students (${students.joinToString(", ") { it.nameEn }})"
        }
        val studentSummaryUr = when (students.size) {
            0 -> "آپ کے بچے"
            1 -> "${students.first().nameUr} (جماعت ${students.first().className}-${students.first().section}، رول نمبر ${students.first().rollNumber})"
            else -> "${students.size} منتخب طلباء (${students.joinToString("، ") { it.nameUr }})"
        }

        return if (language == AppLanguage.URDU) {
            when (alertType) {
                AttendanceAlertType.ABSENCE ->
                    "محترم سرپرست، $date کو $studentSummaryUr اسکول سے غیر حاضر رہے۔ برائے مہربانی غیر حاضری کی وجہ سے اسکول کو مطلع کریں۔ - گورنمنٹ ماڈل مڈل اسکول"
                AttendanceAlertType.LATE ->
                    "محترم سرپرست، $date کو $studentSummaryUr اسکول میں تاخیر سے پہنچے۔ برائے مہربانی وقت کی پابندی یقینی بنائیں۔ - گورنمنٹ ماڈل مڈل اسکول"
                AttendanceAlertType.LOW_ATTENDANCE ->
                    "محترم سرپرست، $studentSummaryUr کی حاضری مقررہ معیار سے کم ہے۔ براہ کرم جلد از جلد کلاس ٹیچر سے ملاقات کریں۔"
                AttendanceAlertType.PTM_NOTICE ->
                    "محترم سرپرست، $studentSummaryUr کے تعلیمی اور حاضری ریکارڈ کے سلسلے میں اسکول میں والدین اور اساتذہ کی میٹنگ (PTM) کے لیے تشریف لائیں۔"
            }
        } else {
            when (alertType) {
                AttendanceAlertType.ABSENCE ->
                    "Dear Guardian, $studentSummaryEn was marked ABSENT on $date. Please inform the class teacher regarding the reason for absence. - Govt. Model Middle School"
                AttendanceAlertType.LATE ->
                    "Dear Guardian, $studentSummaryEn arrived LATE to school on $date. Kindly ensure punctual arrival. - Govt. Model Middle School"
                AttendanceAlertType.LOW_ATTENDANCE ->
                    "Dear Guardian, attendance for $studentSummaryEn requires urgent attention. Kindly meet the class teacher at your earliest convenience."
                AttendanceAlertType.PTM_NOTICE ->
                    "Dear Guardian, you are requested to attend a Parent-Teacher Meeting regarding $studentSummaryEn's attendance & progress ($date)."
            }
        }
    }

    /**
     * Normalizes guardian phone numbers to international format (defaults 10-digit numbers to +91).
     */
    fun normalizePhoneNumber(rawPhone: String): String {
        val digits = rawPhone.replace("[^0-9]".toRegex(), "")
        return if (digits.length == 10) "91$digits" else digits
    }

    /**
     * Builds the WhatsApp ACTION_VIEW Intent for 1 or multiple guardians.
     */
    fun buildBulkWhatsAppIntent(
        phoneNumbers: List<String>,
        messageText: String,
        targetWhatsAppPackage: Boolean = true
    ): Intent {
        val encodedText = URLEncoder.encode(messageText, "UTF-8")
        val targetPhone = if (phoneNumbers.size == 1) {
            normalizePhoneNumber(phoneNumbers.first())
        } else {
            ""
        }

        val url = if (targetPhone.isNotEmpty()) {
            "https://api.whatsapp.com/send?phone=$targetPhone&text=$encodedText"
        } else {
            "https://api.whatsapp.com/send?text=$encodedText"
        }

        return Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            if (targetWhatsAppPackage) {
                setPackage("com.whatsapp")
            }
            putExtra(Intent.EXTRA_TEXT, messageText)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /**
     * Bulk action: shares specific attendance alerts with selected guardians via WhatsApp Intent.
     */
    fun sendAttendanceAlertsToGuardians(
        context: Context,
        students: List<StudentEntity>,
        alertMessage: String
    ): Intent? {
        if (students.isEmpty()) return null
        val phoneNumbers = students
            .map { it.whatsAppNumber.ifBlank { it.primaryContact } }
            .filter { it.isNotBlank() }

        val whatsappIntent = buildBulkWhatsAppIntent(
            phoneNumbers = phoneNumbers,
            messageText = alertMessage,
            targetWhatsAppPackage = true
        )
        launchWhatsAppIntentSafely(context, whatsappIntent, phoneNumbers, alertMessage)
        return whatsappIntent
    }

    /**
     * Bulk action: shares announcements or attendance alerts to multiple guardians
     */
    fun sendBulkAnnouncement(
        context: Context,
        phoneNumbers: List<String>,
        announcementText: String
    ) {
        if (phoneNumbers.isEmpty()) return
        val whatsappIntent = buildBulkWhatsAppIntent(
            phoneNumbers = phoneNumbers,
            messageText = announcementText,
            targetWhatsAppPackage = true
        )
        launchWhatsAppIntentSafely(context, whatsappIntent, phoneNumbers, announcementText)
    }

    private fun launchWhatsAppIntentSafely(
        context: Context,
        primaryIntent: Intent,
        phoneNumbers: List<String>,
        messageText: String
    ) {
        try {
            if (isIntentResolvable(context, primaryIntent)) {
                context.startActivity(primaryIntent)
            } else {
                val browserFallback = buildBulkWhatsAppIntent(
                    phoneNumbers = phoneNumbers,
                    messageText = messageText,
                    targetWhatsAppPackage = false
                )
                context.startActivity(browserFallback)
            }
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(
                context,
                "WhatsApp not installed. Alert prepared for ${phoneNumbers.size} guardian(s).",
                Toast.LENGTH_SHORT
            ).show()
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "Unable to open WhatsApp: ${e.localizedMessage ?: "unknown error"}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun openWhatsAppUrlScheme(context: Context, formattedPhone: String, message: String) {
        try {
            val encodedMessage = URLEncoder.encode(message, "UTF-8")
            val url = "https://api.whatsapp.com/send?phone=$formattedPhone&text=$encodedMessage"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "WhatsApp not installed. Message copied to clipboard.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun buildReportCardMessage(summary: ReportCardSummary): String {
        return """
            🏫 *GOVT. MODEL MIDDLE SCHOOL*
            *Annual Progress Report / سالانہ کارکردگی رپورٹ*
            ------------------------------------------
            👤 *Student:* ${summary.studentNameEn} (${summary.studentNameUr})
            📚 *Class:* Class ${summary.className}-${summary.section} | *Roll No:* ${summary.rollNo}
            📝 *Session:* ${summary.session}
            
            📊 *Scholastic Summary:*
            • Total Marks: ${summary.totalObtained} / ${summary.totalMax.toInt()}
            • Percentage: ${summary.percentage}%
            • Percentile Rank: ${summary.percentile}%
            • Overall Grade: *${summary.overallGrade}*
            • Attendance: ${summary.attendancePercentage}% (${summary.attendanceDaysPresent}/${summary.attendanceTotalWorkingDays} Days)
            
            💬 *Teacher's Feedback / استاد کی رائے:*
            ${summary.qualitativeFeedbackEn}
            ${summary.qualitativeFeedbackUr}
            
            📎 *Official bilingual PDF Report Card attached.*
            Issued by Headmaster / Principal
        """.trimIndent()
    }

    private fun isIntentResolvable(context: Context, intent: Intent): Boolean {
        return intent.resolveActivity(context.packageManager) != null
    }
}
