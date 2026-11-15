package com.example.data.repository

import android.content.Context
import com.example.data.local.SchoolDatabase
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
import com.example.domain.service.VoiceMemoAudioService
import com.example.domain.model.AttendanceStatus
import com.example.domain.model.LeaveStatus
import com.example.domain.model.UserRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class SchoolRepository(private val context: Context) {
    private val database = SchoolDatabase.getInstance(context)
    private val schoolDao = database.schoolDao()
    private val studentDao = database.studentDao()
    private val attendanceDao = database.attendanceDao()
    private val teacherDao = database.teacherDao()
    private val leaveDao = database.leaveDao()
    private val examDao = database.examDao()
    private val textbookDao = database.textbookDao()
    private val auditDao = database.auditDao()
    private val voiceMemoDao = database.voiceMemoDao()

    // Active session state (JWT claim-based simulation)
    private val _currentUserRole = MutableStateFlow(UserRole.HEADMASTER)
    val currentUserRole = _currentUserRole.asStateFlow()

    private val _currentUserId = MutableStateFlow("HM01")
    val currentUserId = _currentUserId.asStateFlow()

    private val _currentUserName = MutableStateFlow("Mohd. Yousuf Wani")
    val currentUserName = _currentUserName.asStateFlow()

    private val _authToken = MutableStateFlow("jwt.mock.claim.headmaster.2026")
    val authToken = _authToken.asStateFlow()

    // Last attendance bulk action backup for "Undo" feature
    private var lastBulkAttendanceBackup: List<AttendanceEntity>? = null
    private var lastBulkDate: String? = null

    fun switchUserRole(role: UserRole) {
        _currentUserRole.value = role
        if (role == UserRole.HEADMASTER) {
            _currentUserId.value = "HM01"
            _currentUserName.value = "Mohd. Yousuf Wani (HM)"
            _authToken.value = "jwt.token.headmaster.${UUID.randomUUID().toString().take(8)}"
        } else {
            _currentUserId.value = "T01"
            _currentUserName.value = "Farooq Ahmad Lone (Teacher)"
            _authToken.value = "jwt.token.teacher.${UUID.randomUUID().toString().take(8)}"
        }
    }

    // School Profile
    val schoolProfile: Flow<SchoolProfileEntity?> = schoolDao.getSchoolProfile()
    suspend fun getSchoolProfileSync() = schoolDao.getSchoolProfileSync()
    suspend fun updateSchoolProfile(profile: SchoolProfileEntity) = schoolDao.insertOrUpdate(profile)

    // Students
    val allActiveStudents: Flow<List<StudentEntity>> = studentDao.getAllActiveStudents()
    suspend fun getAllActiveStudentsSync() = studentDao.getAllActiveStudentsSync()

    fun getStudentsByClassAndSection(className: Int, section: String): Flow<List<StudentEntity>> {
        return studentDao.getStudentsByClassAndSection(className, section)
    }

    suspend fun getStudentsByClassAndSectionSync(className: Int, section: String): List<StudentEntity> {
        return studentDao.getStudentsByClassAndSectionSync(className, section)
    }

    fun searchStudents(query: String): Flow<List<StudentEntity>> {
        return if (query.isBlank()) {
            studentDao.getAllActiveStudents()
        } else {
            studentDao.searchStudents(query.trim())
        }
    }

    suspend fun insertStudent(student: StudentEntity) {
        studentDao.insertStudent(student)
        auditDao.insertAudit(
            AuditTrailEntity(
                actionType = "STUDENT_ENROLLED",
                actor = _currentUserName.value,
                targetId = student.admissionNumber,
                details = "Enrolled ${student.nameEn} in Class ${student.className}-${student.section}. UIDAI Token: ${student.aadhaarReferenceToken}"
            )
        )
        // Add to offline sync queue
        auditDao.insertSyncQueue(
            SyncQueueEntity(
                entityType = "Student",
                entityId = student.uuid,
                changeType = "INSERT",
                payloadJson = "{\"name\":\"${student.nameEn}\", \"adm\":\"${student.admissionNumber}\"}"
            )
        )
    }

    suspend fun bulkImportStudents(students: List<StudentEntity>, sourceLabel: String = "CSV Import"): Int {
        if (students.isEmpty()) return 0
        studentDao.insertStudents(students)
        auditDao.insertAudit(
            AuditTrailEntity(
                actionType = "STUDENTS_CSV_IMPORTED",
                actor = _currentUserName.value,
                targetId = sourceLabel,
                details = "Bulk imported ${students.size} student profiles into Room database from $sourceLabel."
            )
        )
        auditDao.insertSyncQueue(
            SyncQueueEntity(
                entityType = "Student",
                entityId = "csv-import-${System.currentTimeMillis()}",
                changeType = "BULK_IMPORT",
                payloadJson = "{\"count\":${students.size}, \"source\":\"$sourceLabel\"}"
            )
        )
        return students.size
    }

    suspend fun updateStudent(student: StudentEntity) {
        studentDao.updateStudent(student)
    }

    // Attendance Operations
    val allAttendanceRecords: Flow<List<AttendanceEntity>> = attendanceDao.getAllAttendanceFlow()
    fun getAttendanceByDate(date: String): Flow<List<AttendanceEntity>> = attendanceDao.getAttendanceByDate(date)
    suspend fun getAttendanceByDateSync(date: String): List<AttendanceEntity> = attendanceDao.getAttendanceByDateSync(date)

    fun getAttendanceForStudent(studentId: String): Flow<List<AttendanceEntity>> = attendanceDao.getAttendanceForStudent(studentId)
    suspend fun getAttendanceForStudentSync(studentId: String): List<AttendanceEntity> = attendanceDao.getAttendanceForStudentSync(studentId)

    suspend fun markAttendance(record: AttendanceEntity) {
        attendanceDao.insertSingleAttendance(record)
    }

    suspend fun recordQrAttendance(student: StudentEntity, date: String) {
        val record = AttendanceEntity(
            studentId = student.uuid,
            date = date,
            status = AttendanceStatus.PRESENT.name,
            recordedBy = _currentUserId.value,
            remarks = "QR ID Verified"
        )
        attendanceDao.insertSingleAttendance(record)
        auditDao.insertAudit(
            AuditTrailEntity(
                actionType = "QR_ATTENDANCE_MARKED",
                actor = _currentUserName.value,
                targetId = student.admissionNumber,
                details = "QR scan marked ${student.nameEn} (Class ${student.className}-${student.section}) PRESENT for $date"
            )
        )
    }

    suspend fun markAllPresent(
        students: List<StudentEntity>,
        date: String
    ) {
        // Save previous state for Undo
        val existing = attendanceDao.getAttendanceByDateSync(date)
        val studentIds = students.map { it.uuid }.toSet()
        lastBulkAttendanceBackup = existing.filter { studentIds.contains(it.studentId) }
        lastBulkDate = date

        val newRecords = students.map { s ->
            AttendanceEntity(
                studentId = s.uuid,
                date = date,
                status = AttendanceStatus.PRESENT.name,
                recordedBy = _currentUserId.value,
                remarks = "1-Tap Bulk Verified"
            )
        }
        attendanceDao.insertOrUpdateAttendance(newRecords)

        auditDao.insertAudit(
            AuditTrailEntity(
                actionType = "ATTENDANCE_BULK_MARKED",
                actor = _currentUserName.value,
                details = "Marked ${newRecords.size} students as PRESENT for date $date"
            )
        )

        auditDao.insertSyncQueue(
            SyncQueueEntity(
                entityType = "Attendance",
                entityId = "$date-bulk-${students.firstOrNull()?.className ?: 0}",
                changeType = "BULK_UPDATE",
                payloadJson = "{\"date\":\"$date\", \"count\":${newRecords.size}}"
            )
        )
    }

    suspend fun undoLastBulkAttendance(): Boolean {
        val backup = lastBulkAttendanceBackup
        val date = lastBulkDate
        if (backup != null && date != null) {
            // Restore backup records or remove if they were newly created
            if (backup.isNotEmpty()) {
                attendanceDao.insertOrUpdateAttendance(backup)
            }
            auditDao.insertAudit(
                AuditTrailEntity(
                    actionType = "ATTENDANCE_REVERTED",
                    actor = _currentUserName.value,
                    details = "Reverted last bulk attendance submission for $date"
                )
            )
            lastBulkAttendanceBackup = null
            lastBulkDate = null
            return true
        }
        return false
    }

    // Teachers
    val allTeachers: Flow<List<TeacherEntity>> = teacherDao.getAllTeachers()
    suspend fun getTeacherById(id: String) = teacherDao.getTeacherById(id)
    suspend fun updateTeacher(teacher: TeacherEntity) = teacherDao.updateTeacher(teacher)

    // Leave Workflow
    val allLeaves: Flow<List<LeaveApplicationEntity>> = leaveDao.getAllLeaves()
    suspend fun getAllLeavesSync() = leaveDao.getAllLeavesSync()
    suspend fun getLeaveByIdSync(leaveId: Long) = leaveDao.getLeaveById(leaveId)

    suspend fun applyLeave(leave: LeaveApplicationEntity): Long {
        val id = leaveDao.insertLeave(leave)
        auditDao.insertAudit(
            AuditTrailEntity(
                actionType = if (leave.applicantRole == "TEACHER") "LEAVE_APPLIED" else "STUDENT_LEAVE_SUBMITTED",
                actor = if (leave.applicantRole == "PARENT" && leave.guardianName.isNotBlank()) {
                    "${leave.guardianName} (Parent of ${leave.applicantName})"
                } else {
                    "${leave.applicantName} (${leave.applicantRole})"
                },
                targetId = leave.applicantId,
                details = "Submitted ${leave.daysCount} days ${leave.leaveType} leave (${leave.startDate} to ${leave.endDate})"
            )
        )
        return id
    }

    suspend fun reviewLeave(
        leaveId: Long,
        status: LeaveStatus,
        reviewNotes: String
    ): Boolean {
        val leave = leaveDao.getLeaveById(leaveId) ?: return false
        val updated = leave.copy(
            status = status.name,
            reviewNotes = reviewNotes,
            reviewedBy = _currentUserName.value,
            reviewedTimestamp = System.currentTimeMillis()
        )
        leaveDao.updateLeave(updated)

        // If approved and applicant is a teacher, deduct staff quota
        if (status == LeaveStatus.APPROVED && leave.applicantRole == "TEACHER") {
            val teacher = teacherDao.getTeacherById(leave.applicantId)
            if (teacher != null) {
                val updatedTeacher = when (leave.leaveType) {
                    "CASUAL" -> teacher.copy(clQuotaUsed = teacher.clQuotaUsed + leave.daysCount)
                    "MEDICAL" -> teacher.copy(mlQuotaUsed = teacher.mlQuotaUsed + leave.daysCount)
                    "EARNED" -> teacher.copy(elQuotaUsed = teacher.elQuotaUsed + leave.daysCount)
                    else -> teacher
                }
                teacherDao.updateTeacher(updatedTeacher)
            }
        }

        auditDao.insertAudit(
            AuditTrailEntity(
                actionType = if (status == LeaveStatus.APPROVED) "LEAVE_APPROVED" else "LEAVE_REJECTED",
                actor = _currentUserName.value,
                targetId = leave.applicantId,
                details = "${status.name} ${leave.applicantRole.lowercase(Locale.US)} leave #${leave.id} (${leave.applicantName}) for ${leave.daysCount} days"
            )
        )
        return true
    }

    // Exams & Grades
    val allExams: Flow<List<ExamEntity>> = examDao.getAllExams()

    fun getExamsForClass(sessionId: String, className: Int): Flow<List<ExamEntity>> {
        return examDao.getExamsForClass(sessionId, className)
    }

    suspend fun getExamsForClassSync(sessionId: String, className: Int): List<ExamEntity> {
        return examDao.getExamsForClassSync(sessionId, className)
    }

    suspend fun ensureExamsForClassAndTerm(
        sessionId: String,
        className: Int,
        term: String
    ): List<ExamEntity> {
        val existing = examDao.getExamsForClassAndTermSync(sessionId, className, term)
        if (existing.isNotEmpty()) return existing

        val maxMarks = when (term) {
            "UNIT_TEST_1", "UNIT_TEST_2" -> 50
            else -> 100
        }
        val passMarks = (maxMarks * 0.33).toInt().coerceAtLeast(1)
        val examDate = when (term) {
            "UNIT_TEST_1" -> "2026-08-15"
            "TERM_1" -> "2026-10-15"
            else -> "2027-03-05"
        }
        val subjects = listOf(
            "Urdu Literature" to "اردو ادب",
            "Mathematics" to "ریاضی",
            (if (className <= 5) "Environmental Studies (EVS)" else "General Science") to (if (className <= 5) "ماحولیات" else "سائنس"),
            "English Language" to "انگریزی",
            "Social Studies" to "سماجی علوم"
        )
        val created = subjects.map { (en, ur) ->
            ExamEntity(
                sessionId = sessionId,
                term = term,
                className = className,
                subjectNameEn = en,
                subjectNameUr = ur,
                maxMarks = maxMarks,
                passMarks = passMarks,
                examDate = examDate
            )
        }
        examDao.insertExams(created)
        return examDao.getExamsForClassAndTermSync(sessionId, className, term)
    }

    fun getGradesForStudent(studentId: String, sessionId: String): Flow<List<StudentGradeEntity>> {
        return examDao.getGradesForStudent(studentId, sessionId)
    }

    suspend fun getGradesForStudentSync(studentId: String, sessionId: String): List<StudentGradeEntity> {
        return examDao.getGradesForStudentSync(studentId, sessionId)
    }

    suspend fun saveStudentGrade(grade: StudentGradeEntity) {
        examDao.insertGrade(grade)
    }

    // Textbooks
    val allTextbookAllocations: Flow<List<TextbookAllocationEntity>> = textbookDao.getAllAllocations()

    fun getTextbookAllocationsForClass(className: Int): Flow<List<TextbookAllocationEntity>> {
        return textbookDao.getAllocationsForClass(className)
    }

    fun getTextbookAllocationsForClassAndSection(className: Int, section: String): Flow<List<TextbookAllocationEntity>> {
        return textbookDao.getAllocationsForClassAndSection(className, section)
    }

    suspend fun updateTextbookStatus(id: Long, status: String) {
        val current = textbookDao.getAllocationById(id)
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val returnedDate = if (status == "RETURNED") (current?.returnedDate?.ifBlank { today } ?: today) else ""
        textbookDao.updateInventoryDetails(
            id = id,
            status = status,
            condition = current?.bookCondition ?: "GOOD",
            returnDueDate = current?.returnDueDate ?: "2027-03-15",
            returnedDate = returnedDate,
            remarks = current?.remarks ?: ""
        )
    }

    suspend fun assignTextbookToStudent(allocation: TextbookAllocationEntity): Long {
        val id = textbookDao.insertAllocation(allocation)
        auditDao.insertAudit(
            AuditTrailEntity(
                actionType = "TEXTBOOK_ASSIGNED",
                actor = _currentUserName.value,
                targetId = allocation.studentId,
                details = "Assigned '${allocation.bookTitle}' (${allocation.bookCondition}, due ${allocation.returnDueDate}) to ${allocation.studentName}"
            )
        )
        return id
    }

    suspend fun updateTextbookInventory(
        id: Long,
        status: String,
        condition: String,
        returnDueDate: String,
        returnedDate: String,
        remarks: String
    ) {
        textbookDao.updateInventoryDetails(id, status, condition, returnDueDate, returnedDate, remarks)
        val item = textbookDao.getAllocationById(id)
        if (item != null) {
            auditDao.insertAudit(
                AuditTrailEntity(
                    actionType = "TEXTBOOK_INVENTORY_UPDATED",
                    actor = _currentUserName.value,
                    targetId = item.studentId,
                    details = "Updated '${item.bookTitle}' for ${item.studentName}: status=$status, condition=$condition, due=$returnDueDate"
                )
            )
        }
    }

    // Behavioral Voice Memos
    val allVoiceMemos: Flow<List<StudentVoiceMemoEntity>> = voiceMemoDao.getAllVoiceMemos()

    suspend fun insertVoiceMemo(memo: StudentVoiceMemoEntity): Long {
        val withAuthor = if (memo.recordedBy == "Teacher" || memo.recordedBy.isBlank()) {
            memo.copy(recordedBy = _currentUserName.value)
        } else {
            memo
        }
        val id = voiceMemoDao.insertVoiceMemo(withAuthor)
        auditDao.insertAudit(
            AuditTrailEntity(
                actionType = "VOICE_MEMO_RECORDED",
                actor = _currentUserName.value,
                targetId = memo.studentUuid,
                details = "Attached ${memo.durationSeconds}s '${memo.tag}' voice observation memo to ${memo.studentName}"
            )
        )
        return id
    }

    suspend fun deleteVoiceMemo(memo: StudentVoiceMemoEntity) {
        voiceMemoDao.deleteVoiceMemoById(memo.id)
        VoiceMemoAudioService.deleteMemoFile(memo.filePath)
        auditDao.insertAudit(
            AuditTrailEntity(
                actionType = "VOICE_MEMO_DELETED",
                actor = _currentUserName.value,
                targetId = memo.studentUuid,
                details = "Deleted voice observation memo #${memo.id} for ${memo.studentName}"
            )
        )
    }

    // Audit Trail
    val auditLogs: Flow<List<AuditTrailEntity>> = auditDao.getAllAuditLogs()
    val pendingSyncCount: Flow<Int> = auditDao.getPendingSyncCountFlow()

    suspend fun triggerForceSync(): Pair<Int, Boolean> = withContext(Dispatchers.IO) {
        val pendingCount = auditDao.getPendingSyncCount()
        // Simulate synchronization with secure REST API gateway
        kotlinx.coroutines.delay(1200)
        auditDao.markAllSynced()
        auditDao.purgeSyncedItems()

        auditDao.insertAudit(
            AuditTrailEntity(
                actionType = "SYNC_TRIGGERED",
                actor = _currentUserName.value,
                details = "Force synchronization complete. Pushed $pendingCount offline records to REST server."
            )
        )
        Pair(pendingCount, true)
    }
}
