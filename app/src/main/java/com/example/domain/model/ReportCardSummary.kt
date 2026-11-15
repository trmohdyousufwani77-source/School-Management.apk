package com.example.domain.model

data class SubjectScore(
    val subjectName: String,
    val subjectNameUr: String,
    val maxMarks: Int,
    val marksObtained: Double,
    val gradeBand: String,
    val remarks: String
)

data class ReportCardSummary(
    val studentUuid: String,
    val admissionNo: String,
    val rollNo: Int,
    val studentNameEn: String,
    val studentNameUr: String,
    val className: Int,
    val section: String,
    val session: String,
    val fatherName: String,
    val guardianPhone: String,
    val maskedAadhaar: String,
    val subjectScores: List<SubjectScore>,
    val totalObtained: Double,
    val totalMax: Double,
    val percentage: Double,
    val percentile: Double,
    val overallGrade: String,
    val qualitativeFeedbackEn: String,
    val qualitativeFeedbackUr: String,
    val attendanceDaysPresent: Int,
    val attendanceTotalWorkingDays: Int,
    val attendancePercentage: Double,
    val generatedDate: String,
    val term: String = "TERM_1"
)
