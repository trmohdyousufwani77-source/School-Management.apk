package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.local.dao.AttendanceDao
import com.example.data.local.dao.AuditDao
import com.example.data.local.dao.ExamDao
import com.example.data.local.dao.LeaveDao
import com.example.data.local.dao.SchoolDao
import com.example.data.local.dao.StudentDao
import com.example.data.local.dao.TeacherDao
import com.example.data.local.dao.TextbookDao
import com.example.data.local.dao.VoiceMemoDao
import com.example.data.local.entities.ArchivedStudentEntity
import com.example.data.local.entities.AttendanceEntity
import com.example.data.local.entities.AuditTrailEntity
import com.example.data.local.entities.ExamEntity
import com.example.data.local.entities.LeaveApplicationEntity
import com.example.data.local.entities.SchoolProfileEntity
import com.example.data.local.entities.StudentEntity
import com.example.data.local.entities.StudentGradeEntity
import com.example.data.local.entities.StudentVoiceMemoEntity
import com.example.data.local.entities.SyncQueueEntity
import com.example.data.local.entities.TeacherEntity
import com.example.data.local.entities.TextbookAllocationEntity
import androidx.room.migration.Migration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

@Database(
    entities = [
        SchoolProfileEntity::class,
        StudentEntity::class,
        ArchivedStudentEntity::class,
        TeacherEntity::class,
        AttendanceEntity::class,
        LeaveApplicationEntity::class,
        ExamEntity::class,
        StudentGradeEntity::class,
        TextbookAllocationEntity::class,
        AuditTrailEntity::class,
        SyncQueueEntity::class,
        StudentVoiceMemoEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class SchoolDatabase : RoomDatabase() {
    abstract fun schoolDao(): SchoolDao
    abstract fun studentDao(): StudentDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun teacherDao(): TeacherDao
    abstract fun leaveDao(): LeaveDao
    abstract fun examDao(): ExamDao
    abstract fun textbookDao(): TextbookDao
    abstract fun auditDao(): AuditDao
    abstract fun voiceMemoDao(): VoiceMemoDao

    fun runIntegrityCheck(): Pair<Boolean, String> {
        return try {
            val cursor = openHelper.readableDatabase.query("PRAGMA integrity_check")
            var result = "ok"
            if (cursor.moveToFirst()) {
                result = cursor.getString(0)
            }
            cursor.close()
            Pair(result.equals("ok", ignoreCase = true), result)
        } catch (e: Exception) {
            Pair(false, e.localizedMessage ?: "Check failed")
        }
    }

    fun runVacuumMaintenance(): Boolean {
        return try {
            openHelper.writableDatabase.execSQL("VACUUM")
            true
        } catch (e: Exception) {
            false
        }
    }

    companion object {
        const val DATABASE_NAME = "smart_school_manager.db"

        @Volatile
        private var INSTANCE: SchoolDatabase? = null

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `student_voice_memos` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `studentUuid` TEXT NOT NULL,
                        `studentName` TEXT NOT NULL,
                        `tag` TEXT NOT NULL,
                        `noteText` TEXT NOT NULL,
                        `filePath` TEXT NOT NULL,
                        `durationSeconds` INTEGER NOT NULL,
                        `recordedBy` TEXT NOT NULL,
                        `recordedTimestamp` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): SchoolDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SchoolDatabase::class.java,
                    DATABASE_NAME
                )
                    .addMigrations(MIGRATION_2_3)
                    .addCallback(DatabasePrepopulateCallback(context.applicationContext))
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        @androidx.annotation.VisibleForTesting
        fun resetInstance() {
            synchronized(this) {
                try {
                    INSTANCE?.close()
                } catch (_: Exception) {
                }
                INSTANCE = null
            }
        }
    }

    private class DatabasePrepopulateCallback(private val context: Context) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            CoroutineScope(Dispatchers.IO).launch {
                val database = getInstance(context)
                prepopulateInitialData(database)
            }
        }

        private suspend fun prepopulateInitialData(db: SchoolDatabase) {
            // 1. School Profile
            val school = SchoolProfileEntity()
            db.schoolDao().insertOrUpdate(school)

            // 2. Teachers Roster
            val teachers = listOf(
                TeacherEntity(
                    employeeId = "HM01",
                    name = "Mohd. Yousuf Wani",
                    designation = "Headmaster",
                    contactPhone = "+91 94190 12345",
                    email = "hm.sopore@jk.gov.in",
                    assignedClasses = "Class 7-A, Class 8-A",
                    assignedSubjects = "Urdu Literature, Social Science",
                    employmentStatus = "Active",
                    clQuotaTotal = 12,
                    clQuotaUsed = 2,
                    mlQuotaTotal = 10,
                    mlQuotaUsed = 0,
                    elQuotaTotal = 15,
                    elQuotaUsed = 0
                ),
                TeacherEntity(
                    employeeId = "T01",
                    name = "Farooq Ahmad Lone",
                    designation = "TGT Science & Math",
                    contactPhone = "+91 94191 23456",
                    email = "farooq.t01@gmail.com",
                    assignedClasses = "Class 5-A, Class 6-A, Class 7-A",
                    assignedSubjects = "General Science, Mathematics",
                    employmentStatus = "Active",
                    clQuotaTotal = 12,
                    clQuotaUsed = 3,
                    mlQuotaTotal = 10,
                    mlQuotaUsed = 1,
                    elQuotaTotal = 15,
                    elQuotaUsed = 0
                ),
                TeacherEntity(
                    employeeId = "T02",
                    name = "Nasreen Akhtar",
                    designation = "PRT General & Languages",
                    contactPhone = "+91 94192 34567",
                    email = "nasreen.p02@gmail.com",
                    assignedClasses = "Class 1-A, Class 2-A, Class 3-A",
                    assignedSubjects = "English, EVS, Urdu",
                    employmentStatus = "Active",
                    clQuotaTotal = 12,
                    clQuotaUsed = 1,
                    mlQuotaTotal = 10,
                    mlQuotaUsed = 0,
                    elQuotaTotal = 15,
                    elQuotaUsed = 0
                ),
                TeacherEntity(
                    employeeId = "T03",
                    name = "Showkat Hussain Dar",
                    designation = "TGT Social Studies & Hindi",
                    contactPhone = "+91 94193 45678",
                    email = "showkat.tgt@gmail.com",
                    assignedClasses = "Class 6-A, Class 8-A",
                    assignedSubjects = "Social Science, Hindi",
                    employmentStatus = "Active",
                    clQuotaTotal = 12,
                    clQuotaUsed = 4,
                    mlQuotaTotal = 10,
                    mlQuotaUsed = 2,
                    elQuotaTotal = 15,
                    elQuotaUsed = 0
                )
            )
            db.teacherDao().insertTeachers(teachers)

            // 3. Students across Classes 1–8 with UIDAI Compliance guardrails
            val sampleStudents = listOf(
                // Class 1
                StudentEntity(
                    uuid = UUID.randomUUID().toString(),
                    admissionNumber = "ADM-2026-101",
                    rollNumber = 1,
                    nameEn = "Areeba Fatima",
                    nameUr = "اریبہ فاطمہ",
                    className = 1,
                    section = "A",
                    gender = "Female",
                    dob = "2020-05-12",
                    category = "GENERAL",
                    fatherName = "Ghulam Hassan",
                    motherName = "Hameeda Begum",
                    primaryContact = "+91 94190 77112",
                    whatsAppNumber = "+91 94190 77112",
                    residentialAddress = "Noorbagh, Sopore",
                    status = "ACTIVE",
                    aadhaarReferenceToken = "UID-REF-9182-01",
                    maskedAadhaar = "XXXX-XXXX-8421",
                    aadhaarStatus = "VERIFIED",
                    consentRecorded = true
                ),
                StudentEntity(
                    uuid = UUID.randomUUID().toString(),
                    admissionNumber = "ADM-2026-102",
                    rollNumber = 2,
                    nameEn = "Mohammad Ayaan",
                    nameUr = "محمد ایان",
                    className = 1,
                    section = "A",
                    gender = "Male",
                    dob = "2020-08-20",
                    category = "OBC",
                    fatherName = "Bashir Ahmad",
                    motherName = "Rubeena Bano",
                    primaryContact = "+91 94190 77113",
                    whatsAppNumber = "+91 94190 77113",
                    residentialAddress = "Model Town, Sopore",
                    status = "ACTIVE",
                    aadhaarReferenceToken = "UID-REF-9182-02",
                    maskedAadhaar = "XXXX-XXXX-3312",
                    aadhaarStatus = "VERIFIED",
                    consentRecorded = true
                ),
                // Class 3
                StudentEntity(
                    uuid = UUID.randomUUID().toString(),
                    admissionNumber = "ADM-2026-301",
                    rollNumber = 1,
                    nameEn = "Zainab Jan",
                    nameUr = "زینب جان",
                    className = 3,
                    section = "A",
                    gender = "Female",
                    dob = "2018-03-14",
                    category = "GENERAL",
                    fatherName = "Manzoor Ahmad",
                    motherName = "Shahzada Begum",
                    primaryContact = "+91 94190 77114",
                    whatsAppNumber = "+91 94190 77114",
                    residentialAddress = "Arampora, Sopore",
                    status = "ACTIVE",
                    aadhaarReferenceToken = "UID-REF-9182-03",
                    maskedAadhaar = "XXXX-XXXX-9104",
                    aadhaarStatus = "VERIFIED",
                    consentRecorded = true
                ),
                StudentEntity(
                    uuid = UUID.randomUUID().toString(),
                    admissionNumber = "ADM-2026-302",
                    rollNumber = 2,
                    nameEn = "Faizan Shabir",
                    nameUr = "فیضان شبیر",
                    className = 3,
                    section = "A",
                    gender = "Male",
                    dob = "2018-06-25",
                    category = "GENERAL",
                    fatherName = "Shabir Ahmad Mir",
                    motherName = "Parveena Akhtar",
                    primaryContact = "+91 94190 77115",
                    whatsAppNumber = "+91 94190 77115",
                    residentialAddress = "Kralteng, Sopore",
                    status = "ACTIVE",
                    aadhaarReferenceToken = "UID-REF-9182-04",
                    maskedAadhaar = "XXXX-XXXX-6721",
                    aadhaarStatus = "VERIFIED",
                    consentRecorded = true
                ),
                StudentEntity(
                    uuid = UUID.randomUUID().toString(),
                    admissionNumber = "ADM-2026-303",
                    rollNumber = 3,
                    nameEn = "Saima Mushtaq",
                    nameUr = "صائمہ مشتاق",
                    className = 3,
                    section = "A",
                    gender = "Female",
                    dob = "2018-11-04",
                    category = "ST",
                    fatherName = "Mushtaq Ahmad Dar",
                    motherName = "Fehmida Begum",
                    primaryContact = "+91 94190 77116",
                    whatsAppNumber = "+91 94190 77116",
                    residentialAddress = "Botingoo, Zaingeer",
                    status = "ACTIVE",
                    aadhaarReferenceToken = "UID-REF-9182-05",
                    maskedAadhaar = "XXXX-XXXX-1156",
                    aadhaarStatus = "PENDING",
                    consentRecorded = true
                ),
                // Class 5
                StudentEntity(
                    uuid = UUID.randomUUID().toString(),
                    admissionNumber = "ADM-2026-501",
                    rollNumber = 1,
                    nameEn = "Huzaif Tariq",
                    nameUr = "حذیف طارق",
                    className = 5,
                    section = "A",
                    gender = "Male",
                    dob = "2016-02-18",
                    category = "GENERAL",
                    fatherName = "Tariq Ahmad Bhat",
                    motherName = "Naseema Akhtar",
                    primaryContact = "+91 94190 77117",
                    whatsAppNumber = "+91 94190 77117",
                    residentialAddress = "Chinkipora, Sopore",
                    status = "ACTIVE",
                    aadhaarReferenceToken = "UID-REF-9182-06",
                    maskedAadhaar = "XXXX-XXXX-7890",
                    aadhaarStatus = "VERIFIED",
                    consentRecorded = true
                ),
                StudentEntity(
                    uuid = UUID.randomUUID().toString(),
                    admissionNumber = "ADM-2026-502",
                    rollNumber = 2,
                    nameEn = "Mehak Zahoor",
                    nameUr = "مہک ظہور",
                    className = 5,
                    section = "A",
                    gender = "Female",
                    dob = "2016-09-09",
                    category = "OBC",
                    fatherName = "Zahoor Ahmad Khan",
                    motherName = "Suraya Bano",
                    primaryContact = "+91 94190 77118",
                    whatsAppNumber = "+91 94190 77118",
                    residentialAddress = "Badambagh, Sopore",
                    status = "ACTIVE",
                    aadhaarReferenceToken = "UID-REF-9182-07",
                    maskedAadhaar = "XXXX-XXXX-4512",
                    aadhaarStatus = "VERIFIED",
                    consentRecorded = true
                ),
                // Class 6
                StudentEntity(
                    uuid = UUID.randomUUID().toString(),
                    admissionNumber = "ADM-2026-601",
                    rollNumber = 1,
                    nameEn = "Danish Farooq",
                    nameUr = "دانش فاروق",
                    className = 6,
                    section = "A",
                    gender = "Male",
                    dob = "2015-01-10",
                    category = "GENERAL",
                    fatherName = "Farooq Ahmad Rather",
                    motherName = "Safia Begum",
                    primaryContact = "+91 94190 77119",
                    whatsAppNumber = "+91 94190 77119",
                    residentialAddress = "Adipora, Zaingeer",
                    status = "ACTIVE",
                    aadhaarReferenceToken = "UID-REF-9182-08",
                    maskedAadhaar = "XXXX-XXXX-9934",
                    aadhaarStatus = "VERIFIED",
                    consentRecorded = true
                ),
                StudentEntity(
                    uuid = UUID.randomUUID().toString(),
                    admissionNumber = "ADM-2026-602",
                    rollNumber = 2,
                    nameEn = "Insha Bashir",
                    nameUr = "انشاء بشیر",
                    className = 6,
                    section = "A",
                    gender = "Female",
                    dob = "2015-04-16",
                    category = "SC",
                    fatherName = "Bashir Ahmad Malik",
                    motherName = "Shakeela Bano",
                    primaryContact = "+91 94190 77120",
                    whatsAppNumber = "+91 94190 77120",
                    residentialAddress = "Seer Jagir, Sopore",
                    status = "ACTIVE",
                    aadhaarReferenceToken = "UID-REF-9182-09",
                    maskedAadhaar = "XXXX-XXXX-2145",
                    aadhaarStatus = "EXEMPTION",
                    consentRecorded = true
                ),
                // Class 8
                StudentEntity(
                    uuid = UUID.randomUUID().toString(),
                    admissionNumber = "ADM-2026-801",
                    rollNumber = 1,
                    nameEn = "Muzamil Nazir",
                    nameUr = "مزمل نذیر",
                    className = 8,
                    section = "A",
                    gender = "Male",
                    dob = "2013-07-22",
                    category = "GENERAL",
                    fatherName = "Nazir Ahmad War",
                    motherName = "Rafiqa Begum",
                    primaryContact = "+91 94190 77121",
                    whatsAppNumber = "+91 94190 77121",
                    residentialAddress = "Warpora, Sopore",
                    status = "ACTIVE",
                    aadhaarReferenceToken = "UID-REF-9182-10",
                    maskedAadhaar = "XXXX-XXXX-5582",
                    aadhaarStatus = "VERIFIED",
                    consentRecorded = true
                ),
                StudentEntity(
                    uuid = UUID.randomUUID().toString(),
                    admissionNumber = "ADM-2026-802",
                    rollNumber = 2,
                    nameEn = "Sadia Amin",
                    nameUr = "سادیہ امین",
                    className = 8,
                    section = "A",
                    gender = "Female",
                    dob = "2013-10-11",
                    category = "GENERAL",
                    fatherName = "Mohammad Amin Mir",
                    motherName = "Khalida Parveen",
                    primaryContact = "+91 94190 77122",
                    whatsAppNumber = "+91 94190 77122",
                    residentialAddress = "Muslim Peer, Sopore",
                    status = "ACTIVE",
                    aadhaarReferenceToken = "UID-REF-9182-11",
                    maskedAadhaar = "XXXX-XXXX-9023",
                    aadhaarStatus = "VERIFIED",
                    consentRecorded = true
                )
            )
            db.studentDao().insertStudents(sampleStudents)

            // 4. Prepopulate Attendance for past 14 days and today
            val calendar = Calendar.getInstance()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val todayStr = dateFormat.format(calendar.time)
            val attendanceList = mutableListOf<AttendanceEntity>()

            // For today
            sampleStudents.forEachIndexed { idx, student ->
                val status = when (idx) {
                    2 -> "LATE"
                    4 -> "ABSENT"
                    8 -> "LEAVE"
                    else -> "PRESENT"
                }
                attendanceList.add(
                    AttendanceEntity(
                        studentId = student.uuid,
                        date = todayStr,
                        status = status,
                        recordedBy = "HM01",
                        remarks = if (status == "LATE") "Bus delayed" else ""
                    )
                )
            }

            // For previous 10 working days
            for (dayOffset in 1..10) {
                val cal = Calendar.getInstance()
                cal.add(Calendar.DAY_OF_YEAR, -dayOffset)
                // Skip Sundays
                if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) continue
                val pastDate = dateFormat.format(cal.time)
                sampleStudents.forEachIndexed { sIdx, student ->
                    val status = if ((sIdx + dayOffset) % 7 == 0) "ABSENT" else "PRESENT"
                    attendanceList.add(
                        AttendanceEntity(
                            studentId = student.uuid,
                            date = pastDate,
                            status = status,
                            recordedBy = "HM01"
                        )
                    )
                }
            }
            db.attendanceDao().insertOrUpdateAttendance(attendanceList)

            // 5. Prepopulate Exams & Subject Grades across Classes 1–8 for TERM_1
            val classSubjects = listOf(
                Triple("Urdu Literature", "اردو ادب", "2026-10-10"),
                Triple("Mathematics", "ریاضی", "2026-10-12"),
                Triple("General Science", "سائنس", "2026-10-14"),
                Triple("English Language", "انگریزی", "2026-10-16"),
                Triple("Social Studies", "سماجی علوم", "2026-10-18")
            )
            val examsByClass = mutableMapOf<Int, List<Long>>()
            for (cls in 1..8) {
                val classExams = classSubjects.map { (en, ur, dt) ->
                    val subjEn = if (cls <= 5 && en == "General Science") "Environmental Studies (EVS)" else en
                    val subjUr = if (cls <= 5 && en == "General Science") "ماحولیات" else ur
                    ExamEntity(
                        sessionId = "2026-2027",
                        term = "TERM_1",
                        className = cls,
                        subjectNameEn = subjEn,
                        subjectNameUr = subjUr,
                        maxMarks = 100,
                        passMarks = 33,
                        examDate = dt
                    )
                }
                examsByClass[cls] = db.examDao().insertExams(classExams)
            }

            // Sample grades for Class 6 (Danish Farooq & Insha Bashir), Class 3 (Zainab Jan), Class 8 (Muzamil Nazir)
            val danish = sampleStudents.first { it.rollNumber == 1 && it.className == 6 }
            val insha = sampleStudents.first { it.rollNumber == 2 && it.className == 6 }
            val zainab = sampleStudents.first { it.rollNumber == 1 && it.className == 3 }
            val muzamil = sampleStudents.first { it.rollNumber == 1 && it.className == 8 }

            val c6Exams = examsByClass[6].orEmpty()
            val c3Exams = examsByClass[3].orEmpty()
            val c8Exams = examsByClass[8].orEmpty()

            val grades = buildList {
                if (c6Exams.size >= 5) {
                    add(StudentGradeEntity(examId = c6Exams[0], studentId = danish.uuid, sessionId = "2026-2027", marksObtained = 88.0, gradeBand = "A2", remarks = "Excellent reading skills"))
                    add(StudentGradeEntity(examId = c6Exams[1], studentId = danish.uuid, sessionId = "2026-2027", marksObtained = 94.0, gradeBand = "A1", remarks = "Outstanding conceptual clarity"))
                    add(StudentGradeEntity(examId = c6Exams[2], studentId = danish.uuid, sessionId = "2026-2027", marksObtained = 82.0, gradeBand = "A2", remarks = "Good practical aptitude"))
                    add(StudentGradeEntity(examId = c6Exams[3], studentId = danish.uuid, sessionId = "2026-2027", marksObtained = 76.0, gradeBand = "B1", remarks = "Grammar improving"))
                    add(StudentGradeEntity(examId = c6Exams[4], studentId = danish.uuid, sessionId = "2026-2027", marksObtained = 85.0, gradeBand = "A2", remarks = "Very good understanding of history"))

                    add(StudentGradeEntity(examId = c6Exams[0], studentId = insha.uuid, sessionId = "2026-2027", marksObtained = 92.0, gradeBand = "A1", remarks = "Brilliant calligraphy & poetry"))
                    add(StudentGradeEntity(examId = c6Exams[1], studentId = insha.uuid, sessionId = "2026-2027", marksObtained = 81.0, gradeBand = "A2", remarks = "Consistent problem solver"))
                    add(StudentGradeEntity(examId = c6Exams[2], studentId = insha.uuid, sessionId = "2026-2027", marksObtained = 79.0, gradeBand = "B1", remarks = "Active lab participation"))
                    add(StudentGradeEntity(examId = c6Exams[3], studentId = insha.uuid, sessionId = "2026-2027", marksObtained = 88.0, gradeBand = "A2", remarks = "Fluent expression"))
                    add(StudentGradeEntity(examId = c6Exams[4], studentId = insha.uuid, sessionId = "2026-2027", marksObtained = 90.0, gradeBand = "A1", remarks = "Extremely well prepared"))
                }
                if (c3Exams.size >= 5) {
                    add(StudentGradeEntity(examId = c3Exams[0], studentId = zainab.uuid, sessionId = "2026-2027", marksObtained = 91.0, gradeBand = "A1", remarks = "Neat handwriting"))
                    add(StudentGradeEntity(examId = c3Exams[1], studentId = zainab.uuid, sessionId = "2026-2027", marksObtained = 86.0, gradeBand = "A2", remarks = "Quick mental math"))
                    add(StudentGradeEntity(examId = c3Exams[2], studentId = zainab.uuid, sessionId = "2026-2027", marksObtained = 84.0, gradeBand = "A2", remarks = "Curious learner"))
                    add(StudentGradeEntity(examId = c3Exams[3], studentId = zainab.uuid, sessionId = "2026-2027", marksObtained = 80.0, gradeBand = "B1", remarks = "Good vocabulary"))
                    add(StudentGradeEntity(examId = c3Exams[4], studentId = zainab.uuid, sessionId = "2026-2027", marksObtained = 87.0, gradeBand = "A2", remarks = "Active in class"))
                }
                if (c8Exams.size >= 5) {
                    add(StudentGradeEntity(examId = c8Exams[0], studentId = muzamil.uuid, sessionId = "2026-2027", marksObtained = 78.0, gradeBand = "B1", remarks = "Good essay writing"))
                    add(StudentGradeEntity(examId = c8Exams[1], studentId = muzamil.uuid, sessionId = "2026-2027", marksObtained = 89.0, gradeBand = "A2", remarks = "Strong algebra skills"))
                    add(StudentGradeEntity(examId = c8Exams[2], studentId = muzamil.uuid, sessionId = "2026-2027", marksObtained = 85.0, gradeBand = "A2", remarks = "Well-documented lab work"))
                    add(StudentGradeEntity(examId = c8Exams[3], studentId = muzamil.uuid, sessionId = "2026-2027", marksObtained = 74.0, gradeBand = "B1", remarks = "Steady progress"))
                    add(StudentGradeEntity(examId = c8Exams[4], studentId = muzamil.uuid, sessionId = "2026-2027", marksObtained = 83.0, gradeBand = "A2", remarks = "Thoughtful analysis"))
                }
            }
            if (grades.isNotEmpty()) {
                db.examDao().insertGrades(grades)
            }

            // 6. Prepopulate Textbook Inventory Allocations with Condition & Return Due Dates
            val allocations = mutableListOf<TextbookAllocationEntity>()
            val subjectsList = listOf("Urdu", "English", "Mathematics", "EVS/Science", "Social Studies")
            sampleStudents.forEachIndexed { stuIdx, student ->
                subjectsList.forEachIndexed { sIdx, subj ->
                    val status = when {
                        student.className == 8 && subj == "Social Studies" -> "NOT_AVAILABLE"
                        student.rollNumber == 2 && subj == "Mathematics" -> "NOT_RECEIVED"
                        student.className == 6 && student.rollNumber == 2 && subj == "Urdu" -> "RETURNED"
                        else -> "RECEIVED"
                    }
                    val condition = when {
                        status == "NOT_AVAILABLE" || status == "NOT_RECEIVED" -> "NEW"
                        status == "RETURNED" -> "GOOD"
                        (stuIdx + sIdx) % 9 == 0 -> "DAMAGED"
                        (stuIdx + sIdx) % 5 == 0 -> "FAIR"
                        (stuIdx + sIdx) % 3 == 0 -> "NEW"
                        else -> "GOOD"
                    }
                    // Include a couple past-due dates so teachers immediately see overdue return tracking
                    val dueDate = when {
                        status == "RECEIVED" && student.className == 6 && subj == "English" -> "2026-10-30"
                        status == "RECEIVED" && student.className == 5 && subj == "Urdu" -> "2026-11-01"
                        else -> "2027-03-15"
                    }
                    val returnedDate = if (status == "RETURNED") "2026-11-05" else ""
                    val subjCode = subj.take(3).uppercase(Locale.US)
                    allocations.add(
                        TextbookAllocationEntity(
                            className = student.className,
                            section = student.section,
                            subject = subj,
                            bookTitle = "JKBOSE $subj Text - Class ${student.className}",
                            studentId = student.uuid,
                            studentName = student.nameEn,
                            status = status,
                            distributionDate = "2026-04-12",
                            returnDueDate = dueDate,
                            returnedDate = returnedDate,
                            bookCondition = condition,
                            bookCode = "JK-${student.className}${student.section}-$subjCode-${student.rollNumber}",
                            remarks = when {
                                status == "NOT_AVAILABLE" -> "State procurement requisition sent"
                                condition == "DAMAGED" -> "Binding loose - repair or replace on return"
                                status == "RETURNED" -> "Checked in with library clerk"
                                else -> "Free Govt. Distribution"
                            }
                        )
                    )
                }
            }
            db.textbookDao().insertAllocations(allocations)

            // 7. Prepopulate Student/Parent Digital Leave Requests & Staff Leave Applications
            val saima = sampleStudents.first { it.admissionNumber == "ADM-2026-303" }
            val huzaif = sampleStudents.first { it.admissionNumber == "ADM-2026-501" }
            val leaves = listOf(
                LeaveApplicationEntity(
                    applicantId = saima.admissionNumber,
                    applicantName = saima.nameEn,
                    applicantRole = "PARENT",
                    studentUuid = saima.uuid,
                    className = saima.className,
                    section = saima.section,
                    guardianName = saima.fatherName,
                    guardianPhone = saima.primaryContact,
                    leaveType = "MEDICAL",
                    startDate = "2026-11-15",
                    endDate = "2026-11-16",
                    daysCount = 2,
                    reason = "High fever and throat infection; doctor advised 2 days rest.",
                    attachmentReceiptPath = "doctor_note_saima.jpg",
                    status = "PENDING",
                    reviewNotes = "",
                    reviewedBy = ""
                ),
                LeaveApplicationEntity(
                    applicantId = zainab.admissionNumber,
                    applicantName = zainab.nameEn,
                    applicantRole = "STUDENT",
                    studentUuid = zainab.uuid,
                    className = zainab.className,
                    section = zainab.section,
                    guardianName = zainab.fatherName,
                    guardianPhone = zainab.primaryContact,
                    leaveType = "FAMILY_EVENT",
                    startDate = "2026-11-18",
                    endDate = "2026-11-18",
                    daysCount = 1,
                    reason = "Attending elder sister's convocation ceremony in Srinagar.",
                    attachmentReceiptPath = "",
                    status = "PENDING",
                    reviewNotes = "",
                    reviewedBy = ""
                ),
                LeaveApplicationEntity(
                    applicantId = danish.admissionNumber,
                    applicantName = danish.nameEn,
                    applicantRole = "PARENT",
                    studentUuid = danish.uuid,
                    className = danish.className,
                    section = danish.section,
                    guardianName = danish.fatherName,
                    guardianPhone = danish.primaryContact,
                    leaveType = "CASUAL",
                    startDate = "2026-11-04",
                    endDate = "2026-11-05",
                    daysCount = 2,
                    reason = "Outstation family visit to Kupwara.",
                    attachmentReceiptPath = "",
                    status = "APPROVED",
                    reviewNotes = "Approved by class teacher; classwork shared on WhatsApp.",
                    reviewedBy = "Farooq Ahmad Lone (Teacher)",
                    reviewedTimestamp = System.currentTimeMillis() - 86400000L * 8
                ),
                LeaveApplicationEntity(
                    applicantId = huzaif.admissionNumber,
                    applicantName = huzaif.nameEn,
                    applicantRole = "STUDENT",
                    studentUuid = huzaif.uuid,
                    className = huzaif.className,
                    section = huzaif.section,
                    guardianName = huzaif.fatherName,
                    guardianPhone = huzaif.primaryContact,
                    leaveType = "CASUAL",
                    startDate = "2026-11-12",
                    endDate = "2026-11-13",
                    daysCount = 2,
                    reason = "Personal outing during school assessment week.",
                    attachmentReceiptPath = "",
                    status = "REJECTED",
                    reviewNotes = "Mathematics unit test scheduled; attendance mandatory.",
                    reviewedBy = "Farooq Ahmad Lone (Teacher)",
                    reviewedTimestamp = System.currentTimeMillis() - 86400000L * 3
                ),
                LeaveApplicationEntity(
                    applicantId = "T01",
                    applicantName = "Farooq Ahmad Lone",
                    applicantRole = "TEACHER",
                    leaveType = "CASUAL",
                    startDate = "2026-11-08",
                    endDate = "2026-11-09",
                    daysCount = 2,
                    reason = "Family wedding ceremony in Baramulla",
                    attachmentReceiptPath = "invitation_card.jpg",
                    status = "PENDING",
                    reviewNotes = "",
                    reviewedBy = ""
                ),
                LeaveApplicationEntity(
                    applicantId = "T02",
                    applicantName = "Nasreen Akhtar",
                    applicantRole = "TEACHER",
                    leaveType = "MEDICAL",
                    startDate = "2026-10-22",
                    endDate = "2026-10-24",
                    daysCount = 3,
                    reason = "Severe viral bronchitis with medical prescription",
                    attachmentReceiptPath = "prescription_slip.jpg",
                    status = "APPROVED",
                    reviewNotes = "Sanctioned as per medical fitness certificate",
                    reviewedBy = "Mohd. Yousuf Wani (HM)",
                    reviewedTimestamp = System.currentTimeMillis() - 86400000L * 15
                )
            )
            leaves.forEach { db.leaveDao().insertLeave(it) }

            // 8. Audit Trail initial entry
            db.auditDao().insertAudit(
                AuditTrailEntity(
                    actionType = "SYSTEM_INITIALIZATION",
                    actor = "Mohd. Yousuf Wani (Headmaster)",
                    details = "Smart School Manager v1.0 initial seed initialized with UDISE+ 01010408201"
                )
            )
        }
    }
}
