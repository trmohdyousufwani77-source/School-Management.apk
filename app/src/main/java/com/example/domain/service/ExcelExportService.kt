package com.example.domain.service

import android.content.Context
import com.example.data.local.entities.AttendanceEntity
import com.example.data.local.entities.LeaveApplicationEntity
import com.example.data.local.entities.StudentEntity
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets

object ExcelExportService {

    /**
     * Exports monthly attendance register as Excel-compatible CSV with UTF-8 BOM
     */
    fun exportAttendanceRegister(
        context: Context,
        monthName: String,
        students: List<StudentEntity>,
        attendanceRecords: List<AttendanceEntity>
    ): File {
        val dir = File(context.filesDir, "reports")
        if (!dir.exists()) dir.mkdirs()

        val file = File(dir, "Attendance_Register_${monthName.replace(" ", "_")}.csv")
        val writer = OutputStreamWriter(FileOutputStream(file), StandardCharsets.UTF_8)

        // Write UTF-8 BOM so MS Excel displays Unicode & Urdu characters properly
        writer.write("\uFEFF")

        // Header
        writer.write("Roll No,Admission No,Student Name (En),Student Name (Ur),Class,Section,Masked Aadhaar,Present Days,Absent Days,Late Days,Attendance %\n")

        val attendanceByStudent = attendanceRecords.groupBy { it.studentId }

        students.forEach { student ->
            val records = attendanceByStudent[student.uuid] ?: emptyList()
            val presentCount = records.count { it.status == "PRESENT" }
            val absentCount = records.count { it.status == "ABSENT" }
            val lateCount = records.count { it.status == "LATE" }
            val total = records.size
            val pct = if (total > 0) String.format("%.1f", (presentCount.toDouble() / total) * 100.0) else "0.0"

            val line = listOf(
                student.rollNumber.toString(),
                escapeCsv(student.admissionNumber),
                escapeCsv(student.nameEn),
                escapeCsv(student.nameUr),
                student.className.toString(),
                escapeCsv(student.section),
                escapeCsv(student.maskedAadhaar),
                presentCount.toString(),
                absentCount.toString(),
                lateCount.toString(),
                "$pct%"
            ).joinToString(",")

            writer.write(line + "\n")
        }

        writer.flush()
        writer.close()
        return file
    }

    /**
     * Exports staff leave register as Excel-compatible CSV
     */
    fun exportLeaveRegister(
        context: Context,
        leaves: List<LeaveApplicationEntity>
    ): File {
        val dir = File(context.filesDir, "reports")
        if (!dir.exists()) dir.mkdirs()

        val file = File(dir, "Staff_Leave_Register_${System.currentTimeMillis()}.csv")
        val writer = OutputStreamWriter(FileOutputStream(file), StandardCharsets.UTF_8)
        writer.write("\uFEFF")

        writer.write("Application ID,Applicant ID,Applicant Role,Applicant Name,Class,Leave Type,Start Date,End Date,Days Count,Reason,Status,Reviewed By\n")

        leaves.forEach { leave ->
            val classLabel = if (leave.className > 0) "${leave.className}-${leave.section}" else "Staff"
            val line = listOf(
                leave.id.toString(),
                escapeCsv(leave.applicantId),
                escapeCsv(leave.applicantRole),
                escapeCsv(leave.applicantName),
                escapeCsv(classLabel),
                escapeCsv(leave.leaveType),
                escapeCsv(leave.startDate),
                escapeCsv(leave.endDate),
                leave.daysCount.toString(),
                escapeCsv(leave.reason),
                escapeCsv(leave.status),
                escapeCsv(leave.reviewedBy.ifEmpty { "Pending" })
            ).joinToString(",")
            writer.write(line + "\n")
        }

        writer.flush()
        writer.close()
        return file
    }

    private fun escapeCsv(value: String): String {
        return if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }
}
