package com.example.domain.model

enum class UserRole(val displayNameEn: String, val displayNameUr: String) {
    HEADMASTER("Headmaster / Admin", "ہیڈ ماسٹر / ایڈمن"),
    TEACHER("Assigned Teacher", "مقررہ استاد")
}

enum class AttendanceStatus(val code: String, val labelEn: String, val labelUr: String) {
    PRESENT("P", "Present", "حاضر"),
    ABSENT("A", "Absent", "غیر حاضر"),
    LATE("L", "Late", "دیر سے"),
    LEAVE("LV", "Approved Leave", "رخصت منظور")
}

enum class LeaveType(val labelEn: String, val labelUr: String, val annualQuota: Int) {
    CASUAL("Casual Leave (CL)", "اتفاقی رخصت", 12),
    MEDICAL("Medical Leave (ML)", "طبی رخصت", 10),
    EARNED("Earned Leave (EL)", "حق رخصت", 15)
}

enum class LeaveStatus(val labelEn: String, val labelUr: String) {
    PENDING("Pending Approval", "زیر التواء منظوری"),
    APPROVED("Approved", "منظور شدہ"),
    REJECTED("Rejected", "مسترد شدہ")
}

enum class AadhaarVerificationStatus(val labelEn: String, val labelUr: String) {
    VERIFIED("Verified", "تصدیق شدہ"),
    PENDING("Pending Verification", "زیر تصدیق"),
    FAILED("Verification Failed", "تصدیق ناکام"),
    EXEMPTION("Exemption / Alt ID", "استثنیٰ / متبادل شناختی کارڈ")
}

enum class TextbookStatus(val labelEn: String, val labelUr: String) {
    RECEIVED("Assigned / Issued", "جاری شدہ"),
    NOT_AVAILABLE("Not Available in School", "اسکول میں دستیاب نہیں"),
    NOT_RECEIVED("Pending Issue", "جاری ہونا باقی"),
    RETURNED("Returned", "واپس شدہ")
}

enum class TextbookCondition(val labelEn: String, val labelUr: String) {
    NEW("New", "نئی"),
    GOOD("Good", "اچھی"),
    FAIR("Fair", "درمیانی"),
    DAMAGED("Damaged", "خراب"),
    LOST("Lost", "گمشدہ")
}

enum class LeaveApplicantRole(val labelEn: String, val labelUr: String) {
    STUDENT("Student", "طالب علم"),
    PARENT("Parent / Guardian", "والدین / سرپرست"),
    TEACHER("Teacher / Staff", "استاد / عملہ")
}

enum class StudentCategory {
    GENERAL, SC, ST, OBC
}

enum class AssessmentType(val titleEn: String, val titleUr: String, val maxMarks: Int, val weightagePercent: Int) {
    UNIT_TEST_1("Unit Test 1", "یونٹ ٹیسٹ ۱", 25, 10),
    UNIT_TEST_2("Unit Test 2", "یونٹ ٹیسٹ ۲", 25, 10),
    TERM_1("Term 1 Examination", "پہلی ششماہی امتحان", 50, 30),
    TERM_2("Term 2 / Final Examination", "سالانہ امتحان", 100, 50)
}
