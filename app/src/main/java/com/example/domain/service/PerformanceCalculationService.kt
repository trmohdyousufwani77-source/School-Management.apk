package com.example.domain.service

import com.example.data.local.entities.AttendanceEntity
import com.example.data.local.entities.ExamEntity
import com.example.data.local.entities.StudentEntity
import com.example.data.local.entities.StudentGradeEntity
import com.example.domain.model.ReportCardSummary
import com.example.domain.model.SubjectScore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

object PerformanceCalculationService {

    fun calculateGradeBand(percentage: Double): String {
        return when {
            percentage >= 91.0 -> "A1"
            percentage >= 81.0 -> "A2"
            percentage >= 71.0 -> "B1"
            percentage >= 61.0 -> "B2"
            percentage >= 51.0 -> "C1"
            percentage >= 41.0 -> "C2"
            percentage >= 33.0 -> "D"
            else -> "E"
        }
    }

    fun getQualitativeFeedbackEn(grade: String): String {
        return when (grade) {
            "A1" -> "Outstanding scholastic excellence! Demonstrates exemplary conceptual understanding and leadership."
            "A2" -> "Excellent performance across all subjects. Shows commendable dedication and inquisitive attitude."
            "B1" -> "Very good academic progress with consistent study habits. Keep striving for top honors."
            "B2" -> "Good performance. Can achieve higher grades with additional focus on problem solving."
            "C1", "C2" -> "Satisfactory effort. Needs targeted revision in core subjects and active classroom participation."
            "D" -> "Fair effort. Requires regular monitoring and homework consistency to strengthen foundational concepts."
            else -> "Needs substantial remedial guidance and regular parent-teacher consultation."
        }
    }

    fun getQualitativeFeedbackUr(grade: String): String {
        return when (grade) {
            "A1" -> "شاندار تعلیمی کارکردگی! مثالی فہم، اعلیٰ اخلاق اور قائدانہ صلاحیتوں کا مظاہرہ۔"
            "A2" -> "تمام مضامین میں بہترین کارکردگی۔ قابل ستائش لگن اور محنت۔"
            "B1" -> "بہت عمدہ تعلیمی پیشرفت اور باقاعدہ مطالعہ۔ محنت جاری رکھیں۔"
            "B2" -> "اچھی کارکردگی۔ تھوڑی مزید توجہ سے مزید بہتر نتائج حاصل ہو سکتے ہیں۔"
            "C1", "C2" -> "اطمینان بخش کوشش۔ بنیادی مضامین اور ریاضی پر خصوصی توجہ درکار ہے۔"
            "D" -> "مناسب کوشش۔ طالب علم کو روزانہ باقاعدہ مطالعے کی ضرورت ہے۔"
            else -> "بنیادی تصورات میں خصوصی رہنمائی اور والدین کی توجہ کی اشد ضرورت ہے۔"
        }
    }

    fun generateReportCardSummary(
        student: StudentEntity,
        exams: List<ExamEntity>,
        grades: List<StudentGradeEntity>,
        allPeerPercentages: List<Double>,
        attendanceRecords: List<AttendanceEntity>,
        totalScheduledWorkingDays: Int,
        term: String = "TERM_1"
    ): ReportCardSummary {
        val termExams = exams.filter { it.term == term }.ifEmpty { exams }
        val examMap = termExams.associateBy { it.id }
        val gradeByExamId = grades.associateBy { it.examId }
        val subjectScores = mutableListOf<SubjectScore>()

        var totalObtained = 0.0
        var totalMax = 0.0

        for (exam in termExams) {
            val grade = gradeByExamId[exam.id]
            if (grade != null) {
                val max = exam.maxMarks
                val obtained = if (grade.isAbsent) 0.0 else grade.marksObtained
                totalObtained += obtained
                totalMax += max
                val subjectPct = if (max > 0) (obtained / max) * 100.0 else 0.0
                val band = calculateGradeBand(subjectPct)
                subjectScores.add(
                    SubjectScore(
                        subjectName = exam.subjectNameEn,
                        subjectNameUr = exam.subjectNameUr,
                        maxMarks = max,
                        marksObtained = obtained,
                        gradeBand = band,
                        remarks = grade.remarks.ifEmpty { if (grade.isAbsent) "Absent" else "Qualified" }
                    )
                )
            } else {
                subjectScores.add(
                    SubjectScore(
                        subjectName = exam.subjectNameEn,
                        subjectNameUr = exam.subjectNameUr,
                        maxMarks = exam.maxMarks,
                        marksObtained = 0.0,
                        gradeBand = "N/A",
                        remarks = "Pending"
                    )
                )
                totalMax += exam.maxMarks
            }
        }

        // Fallback if exams came in via non-matching order or orphaned grades
        if (subjectScores.isEmpty() && grades.isNotEmpty()) {
            for (grade in grades) {
                val exam = examMap[grade.examId]
                if (exam != null) {
                    val max = exam.maxMarks
                    val obtained = if (grade.isAbsent) 0.0 else grade.marksObtained
                    totalObtained += obtained
                    totalMax += max
                    val subjectPct = if (max > 0) (obtained / max) * 100.0 else 0.0
                    val band = calculateGradeBand(subjectPct)
                    subjectScores.add(
                        SubjectScore(
                            subjectName = exam.subjectNameEn,
                            subjectNameUr = exam.subjectNameUr,
                            maxMarks = max,
                            marksObtained = obtained,
                            gradeBand = band,
                            remarks = grade.remarks.ifEmpty { if (grade.isAbsent) "Absent" else "Qualified" }
                        )
                    )
                }
            }
        }

        val percentage = if (totalMax > 0) ((totalObtained / totalMax) * 100.0) else 0.0
        val overallGrade = calculateGradeBand(percentage)

        // Percentile rank calculation: % of peers who scored less than or equal to this student
        val percentile = if (allPeerPercentages.isNotEmpty()) {
            val lowerOrEqualCount = allPeerPercentages.count { it <= percentage }
            ((lowerOrEqualCount.toDouble() / allPeerPercentages.size.toDouble()) * 100.0)
        } else {
            100.0
        }

        // Attendance computation
        val daysPresent = attendanceRecords.count { it.status == "PRESENT" || it.status == "LATE" }
        val workingDays = if (totalScheduledWorkingDays > 0) totalScheduledWorkingDays else maxOf(attendanceRecords.size, 1)
        val attendancePct = if (workingDays > 0) ((daysPresent.toDouble() / workingDays.toDouble()) * 100.0) else 100.0

        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.US)
        val currentDateStr = dateFormat.format(Date())

        return ReportCardSummary(
            studentUuid = student.uuid,
            admissionNo = student.admissionNumber,
            rollNo = student.rollNumber,
            studentNameEn = student.nameEn,
            studentNameUr = student.nameUr,
            className = student.className,
            section = student.section,
            session = student.academicSession,
            fatherName = student.fatherName,
            guardianPhone = student.primaryContact,
            maskedAadhaar = student.maskedAadhaar,
            subjectScores = subjectScores,
            totalObtained = (totalObtained * 10.0).roundToInt() / 10.0,
            totalMax = totalMax,
            percentage = (percentage * 10.0).roundToInt() / 10.0,
            percentile = (percentile * 10.0).roundToInt() / 10.0,
            overallGrade = overallGrade,
            qualitativeFeedbackEn = getQualitativeFeedbackEn(overallGrade),
            qualitativeFeedbackUr = getQualitativeFeedbackUr(overallGrade),
            attendanceDaysPresent = daysPresent,
            attendanceTotalWorkingDays = workingDays,
            attendancePercentage = (attendancePct * 10.0).roundToInt() / 10.0,
            generatedDate = currentDateStr,
            term = term
        )
    }
}
