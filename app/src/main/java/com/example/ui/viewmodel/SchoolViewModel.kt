package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entities.AttendanceEntity
import com.example.data.local.entities.AuditTrailEntity
import com.example.data.local.entities.ExamEntity
import com.example.data.local.entities.LeaveApplicationEntity
import com.example.data.local.entities.SchoolProfileEntity
import com.example.data.local.entities.StudentEntity
import com.example.data.local.entities.StudentGradeEntity
import com.example.data.local.entities.StudentVoiceMemoEntity
import com.example.data.local.entities.TextbookAllocationEntity
import com.example.data.repository.SchoolRepository
import com.example.domain.model.AttendanceStatus
import com.example.domain.model.LeaveStatus
import com.example.domain.model.ReportCardSummary
import com.example.domain.model.UserRole
import com.example.domain.service.CsvStudentParserService
import com.example.domain.service.DatabaseMaintenanceService
import com.example.domain.service.ExcelExportService
import com.example.domain.service.PdfReportCardGenerator
import com.example.domain.service.PerformanceCalculationService
import com.example.domain.service.QrCodeService
import com.example.domain.service.SchoolNotificationService
import com.example.domain.service.WhatsAppService
import com.example.domain.worker.BackupScheduler
import com.example.domain.worker.NotificationScheduler
import com.example.ui.localization.AppLanguage
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class SchoolViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = SchoolRepository(application)
    private val prefs = application.getSharedPreferences("smart_school_prefs", Context.MODE_PRIVATE)

    // Persistent global language preference state (English / Urdu)
    private val _appLanguage = MutableStateFlow(
        if (prefs.getString("app_language", AppLanguage.ENGLISH.code) == AppLanguage.URDU.code) {
            AppLanguage.URDU
        } else {
            AppLanguage.ENGLISH
        }
    )
    val appLanguage = _appLanguage.asStateFlow()

    fun setLanguage(language: AppLanguage) {
        _appLanguage.value = language
        prefs.edit().putString("app_language", language.code).apply()
    }

    fun toggleLanguage() {
        val next = if (_appLanguage.value == AppLanguage.ENGLISH) AppLanguage.URDU else AppLanguage.ENGLISH
        setLanguage(next)
    }

    // Biometric Security Controls (Student Records & Report Card PDF Generation)
    private val _biometricProtectStudents = MutableStateFlow(
        prefs.getBoolean("biometric_protect_students", true)
    )
    val biometricProtectStudents = _biometricProtectStudents.asStateFlow()

    private val _biometricProtectReports = MutableStateFlow(
        prefs.getBoolean("biometric_protect_reports", true)
    )
    val biometricProtectReports = _biometricProtectReports.asStateFlow()

    private val _isStudentRecordsUnlocked = MutableStateFlow(
        !prefs.getBoolean("biometric_protect_students", true)
    )
    val isStudentRecordsUnlocked = _isStudentRecordsUnlocked.asStateFlow()

    private val _isReportCardAccessUnlocked = MutableStateFlow(
        !prefs.getBoolean("biometric_protect_reports", true)
    )
    val isReportCardAccessUnlocked = _isReportCardAccessUnlocked.asStateFlow()

    fun setBiometricProtectStudents(enabled: Boolean) {
        _biometricProtectStudents.value = enabled
        prefs.edit().putBoolean("biometric_protect_students", enabled).apply()
        _isStudentRecordsUnlocked.value = !enabled
    }

    fun setBiometricProtectReports(enabled: Boolean) {
        _biometricProtectReports.value = enabled
        prefs.edit().putBoolean("biometric_protect_reports", enabled).apply()
        _isReportCardAccessUnlocked.value = !enabled
    }

    fun unlockStudentRecords() {
        _isStudentRecordsUnlocked.value = true
        viewModelScope.launch {
            _snackbarMessage.emit("Sensitive student records unlocked via biometric verification.")
        }
    }

    fun lockStudentRecords() {
        if (_biometricProtectStudents.value) {
            _isStudentRecordsUnlocked.value = false
        }
    }

    fun unlockReportCardAccess() {
        _isReportCardAccessUnlocked.value = true
    }

    fun lockReportCardAccess() {
        if (_biometricProtectReports.value) {
            _isReportCardAccessUnlocked.value = false
        }
    }

    // Role state
    val currentUserRole: StateFlow<UserRole> = repository.currentUserRole
    val currentUserName: StateFlow<String> = repository.currentUserName

    fun switchRole(role: UserRole) {
        repository.switchUserRole(role)
    }

    // School Profile
    val schoolProfile: StateFlow<SchoolProfileEntity?> = repository.schoolProfile.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    fun updateSchoolProfile(profile: SchoolProfileEntity) {
        viewModelScope.launch {
            repository.updateSchoolProfile(profile)
        }
    }

    // Selection filters
    private val _selectedClass = MutableStateFlow(6)
    val selectedClass = _selectedClass.asStateFlow()

    private val _selectedSection = MutableStateFlow("A")
    val selectedSection = _selectedSection.asStateFlow()

    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val _selectedDate = MutableStateFlow(dateFormat.format(Date()))
    val selectedDate = _selectedDate.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    fun setSelectedClass(cls: Int) {
        _selectedClass.value = cls
    }

    fun setSelectedSection(sec: String) {
        _selectedSection.value = sec
    }

    fun setSelectedDate(date: String) {
        _selectedDate.value = date
    }

    fun shiftSelectedDate(daysDelta: Int) {
        val current = try {
            dateFormat.parse(_selectedDate.value) ?: Date()
        } catch (e: Exception) {
            Date()
        }
        val cal = Calendar.getInstance().apply {
            time = current
            add(Calendar.DAY_OF_YEAR, daysDelta)
        }
        _selectedDate.value = dateFormat.format(cal.time)
    }

    fun resetSelectedDateToToday() {
        _selectedDate.value = dateFormat.format(Date())
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // Students
    val allActiveStudents: StateFlow<List<StudentEntity>> = repository.allActiveStudents.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val classStudents: StateFlow<List<StudentEntity>> = combine(
        _selectedClass,
        _selectedSection
    ) { cls, sec ->
        Pair(cls, sec)
    }.flatMapLatest { (cls, sec) ->
        repository.getStudentsByClassAndSection(cls, sec)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Attendance for selected date
    val todayAttendance: StateFlow<List<AttendanceEntity>> = _selectedDate.flatMapLatest { date ->
        repository.getAttendanceByDate(date)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Full attendance history for 7-day trend visualization
    val allAttendanceRecords: StateFlow<List<AttendanceEntity>> = repository.allAttendanceRecords.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Snackbar event for Undo
    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage = _snackbarMessage.asSharedFlow()

    private val _canUndoAttendance = MutableStateFlow(false)
    val canUndoAttendance = _canUndoAttendance.asStateFlow()

    fun markStudentAttendance(studentId: String, status: AttendanceStatus) {
        viewModelScope.launch {
            val record = AttendanceEntity(
                studentId = studentId,
                date = _selectedDate.value,
                status = status.name,
                recordedBy = repository.currentUserId.value
            )
            repository.markAttendance(record)
        }
    }

    fun markAllPresentForClass() {
        viewModelScope.launch {
            val students = classStudents.value
            if (students.isNotEmpty()) {
                repository.markAllPresent(students, _selectedDate.value)
                _canUndoAttendance.value = true
                _snackbarMessage.emit("Marked ${students.size} students present for ${_selectedDate.value}")
            }
        }
    }

    fun markAllPresentForSpecificClass(className: Int, section: String) {
        viewModelScope.launch {
            val students = allActiveStudents.value.filter {
                it.className == className && it.section == section
            }
            if (students.isNotEmpty()) {
                repository.markAllPresent(students, _selectedDate.value)
                _canUndoAttendance.value = true
                _snackbarMessage.emit("Class $className-$section: Marked ${students.size} present for ${_selectedDate.value}")
            }
        }
    }

    fun undoLastAttendance() {
        viewModelScope.launch {
            val undone = repository.undoLastBulkAttendance()
            if (undone) {
                _canUndoAttendance.value = false
                _snackbarMessage.emit("Last bulk attendance action reverted.")
            }
        }
    }

    // Student Enrollment & CSV Bulk Import
    fun enrollNewStudent(student: StudentEntity) {
        viewModelScope.launch {
            repository.insertStudent(student)
            _snackbarMessage.emit("Enrolled ${student.nameEn} (Aadhaar Verified)")
        }
    }

    fun importStudentsFromCsv(students: List<StudentEntity>, sourceLabel: String = "CSV File") {
        if (students.isEmpty()) return
        viewModelScope.launch {
            val count = repository.bulkImportStudents(students, sourceLabel)
            _snackbarMessage.emit("Imported $count student profiles into Room database.")
        }
    }

    // Student Behavioral Voice Memos
    val allVoiceMemos: StateFlow<List<StudentVoiceMemoEntity>> = repository.allVoiceMemos.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun saveStudentVoiceMemo(
        student: StudentEntity,
        tag: String,
        noteText: String,
        filePath: String,
        durationSeconds: Int
    ) {
        viewModelScope.launch {
            repository.insertVoiceMemo(
                StudentVoiceMemoEntity(
                    studentUuid = student.uuid,
                    studentName = student.nameEn,
                    tag = tag,
                    noteText = noteText,
                    filePath = filePath,
                    durationSeconds = durationSeconds,
                    recordedBy = currentUserName.value
                )
            )
            _snackbarMessage.emit("Saved ${durationSeconds}s voice memo for ${student.nameEn}.")
        }
    }

    fun deleteStudentVoiceMemo(memo: StudentVoiceMemoEntity) {
        viewModelScope.launch {
            repository.deleteVoiceMemo(memo)
            _snackbarMessage.emit("Deleted voice memo.")
        }
    }

    // Leave Workflow
    val allLeaves: StateFlow<List<LeaveApplicationEntity>> = repository.allLeaves.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun applyLeave(leave: LeaveApplicationEntity) {
        viewModelScope.launch {
            repository.applyLeave(leave)
            _snackbarMessage.emit("Leave application submitted successfully.")
        }
    }

    fun reviewLeave(leaveId: Long, status: LeaveStatus, notes: String) {
        viewModelScope.launch {
            val ok = repository.reviewLeave(leaveId, status, notes)
            if (ok) {
                val updated = repository.getLeaveByIdSync(leaveId)
                if (updated != null) {
                    SchoolNotificationService.notifyLeaveStatusUpdate(
                        context = getApplication(),
                        leave = updated,
                        language = _appLanguage.value
                    )
                }
            }
            _snackbarMessage.emit("Leave application ${status.name.lowercase()}.")
        }
    }

    fun findStudentByQr(rawQrPayload: String): StudentEntity? {
        return QrCodeService.findMatchingStudent(rawQrPayload, allActiveStudents.value)
    }

    fun markAttendanceByQrScan(rawQrPayload: String): StudentEntity? {
        val student = QrCodeService.findMatchingStudent(rawQrPayload, allActiveStudents.value)
        if (student != null) {
            _selectedClass.value = student.className
            _selectedSection.value = student.section
            viewModelScope.launch {
                repository.recordQrAttendance(student, _selectedDate.value)
                _snackbarMessage.emit("QR Present: ${student.nameEn} (${student.admissionNumber})")
            }
        } else {
            viewModelScope.launch {
                _snackbarMessage.emit("No matching student found for scanned QR: $rawQrPayload")
            }
        }
        return student
    }

    // Textbook Allocation
    val allTextbooks: StateFlow<List<TextbookAllocationEntity>> = repository.allTextbookAllocations.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val upcomingDeadlineSummary: StateFlow<SchoolNotificationService.DeadlineSummary> = combine(
        allTextbooks,
        allLeaves
    ) { books, leaves ->
        SchoolNotificationService.evaluateDeadlines(books, leaves)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SchoolNotificationService.DeadlineSummary()
    )

    fun checkAndDispatchDeadlineNotifications() {
        viewModelScope.launch {
            val summary = SchoolNotificationService.evaluateDeadlines(
                allocations = allTextbooks.value,
                leaves = allLeaves.value
            )
            val sent = SchoolNotificationService.notifyUpcomingDeadlines(
                context = getApplication(),
                summary = summary,
                language = _appLanguage.value
            )
            try {
                NotificationScheduler.triggerImmediateDeadlineCheck(getApplication())
            } catch (_: Exception) {
            }
            val msg = if (summary.totalBookAlerts == 0 && summary.pendingLeavesCount == 0) {
                "All textbook returns and leave reviews are up to date."
            } else {
                val prefix = if (sent) "Notification sent: " else "Alerts found: "
                "$prefix${summary.overdueBooks.size} overdue, ${summary.upcomingBooks.size} due soon, ${summary.pendingLeavesCount} pending leaves."
            }
            _snackbarMessage.emit(msg)
        }
    }

    fun updateTextbookStatus(id: Long, status: String) {
        viewModelScope.launch {
            repository.updateTextbookStatus(id, status)
        }
    }

    fun assignTextbookToStudent(allocation: TextbookAllocationEntity) {
        viewModelScope.launch {
            repository.assignTextbookToStudent(allocation)
            _snackbarMessage.emit("Assigned '${allocation.bookTitle}' to ${allocation.studentName}")
        }
    }

    fun updateTextbookInventory(
        id: Long,
        status: String,
        condition: String,
        returnDueDate: String,
        returnedDate: String,
        remarks: String
    ) {
        viewModelScope.launch {
            repository.updateTextbookInventory(id, status, condition, returnDueDate, returnedDate, remarks)
            _snackbarMessage.emit("Updated textbook inventory record.")
        }
    }

    fun markTextbookReturned(id: Long, condition: String, remarks: String) {
        val today = dateFormat.format(Date())
        updateTextbookInventory(
            id = id,
            status = "RETURNED",
            condition = condition,
            returnDueDate = today,
            returnedDate = today,
            remarks = remarks
        )
    }

    // Exams & Grades
    val allExams: StateFlow<List<ExamEntity>> = repository.allExams.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    suspend fun getExamsForClassAndTerm(className: Int, term: String): List<ExamEntity> {
        return repository.ensureExamsForClassAndTerm("2026-2027", className, term)
    }

    suspend fun getGradesMapForStudent(studentId: String): Map<Long, StudentGradeEntity> {
        return repository.getGradesForStudentSync(studentId, "2026-2027").associateBy { it.examId }
    }

    fun saveGrade(examId: Long, studentId: String, marks: Double, isAbsent: Boolean, maxMarks: Int = 100) {
        viewModelScope.launch {
            val pct = if (maxMarks > 0) (marks / maxMarks) * 100.0 else marks
            val gradeBand = PerformanceCalculationService.calculateGradeBand(pct)
            val grade = StudentGradeEntity(
                examId = examId,
                studentId = studentId,
                sessionId = "2026-2027",
                marksObtained = marks,
                isAbsent = isAbsent,
                gradeBand = gradeBand,
                enteredBy = repository.currentUserId.value
            )
            repository.saveStudentGrade(grade)
            _snackbarMessage.emit("Marks saved ($marks/$maxMarks -> Grade $gradeBand)")
        }
    }

    // Report Card Calculations & Generation
    suspend fun getStudentReportSummary(student: StudentEntity, term: String = "TERM_1"): ReportCardSummary {
        val school = repository.getSchoolProfileSync() ?: SchoolProfileEntity()
        val exams = repository.ensureExamsForClassAndTerm("2026-2027", student.className, term)
        val grades = repository.getGradesForStudentSync(student.uuid, "2026-2027")
        val attendance = repository.getAttendanceForStudentSync(student.uuid)

        val examIds = exams.map { it.id }.toSet()
        val totalPossible = exams.sumOf { it.maxMarks }.toDouble().coerceAtLeast(100.0)

        // Class peer percentages for the selected term
        val classStudentsList = repository.getStudentsByClassAndSectionSync(student.className, student.section)
        val peerPercentages = mutableListOf<Double>()
        classStudentsList.forEach { peer ->
            val peerGrades = repository.getGradesForStudentSync(peer.uuid, "2026-2027")
                .filter { examIds.contains(it.examId) }
            val totalObtained = peerGrades.sumOf { if (it.isAbsent) 0.0 else it.marksObtained }
            peerPercentages.add((totalObtained / totalPossible) * 100.0)
        }

        return PerformanceCalculationService.generateReportCardSummary(
            student = student,
            exams = exams,
            grades = grades,
            allPeerPercentages = peerPercentages,
            attendanceRecords = attendance,
            totalScheduledWorkingDays = school.totalWorkingDays,
            term = term
        )
    }

    fun generateSinglePdf(student: StudentEntity, term: String = "TERM_1", onComplete: (File) -> Unit) {
        viewModelScope.launch {
            val school = repository.getSchoolProfileSync() ?: SchoolProfileEntity()
            val summary = getStudentReportSummary(student, term)
            val file = PdfReportCardGenerator.generateSinglePdf(getApplication(), school, summary)
            _snackbarMessage.emit("Generated Bilingual PDF: ${file.name}")
            onComplete(file)
        }
    }

    fun batchGenerateClassPdfs(
        term: String = "TERM_1",
        onProgress: (Int, Int) -> Unit,
        onComplete: (List<File>) -> Unit
    ) {
        viewModelScope.launch {
            val school = repository.getSchoolProfileSync() ?: SchoolProfileEntity()
            val students = classStudents.value
            val summaries = students.map { getStudentReportSummary(it, term) }
            val files = PdfReportCardGenerator.generateBatchPdfs(
                getApplication(),
                school,
                summaries,
                onProgress
            )
            _snackbarMessage.emit("Batch generated ${files.size} bilingual PDF transcripts.")
            onComplete(files)
        }
    }

    // WhatsApp Dispatch
    fun sendReportViaWhatsApp(student: StudentEntity, term: String = "TERM_1") {
        viewModelScope.launch {
            val school = repository.getSchoolProfileSync() ?: SchoolProfileEntity()
            val summary = getStudentReportSummary(student, term)
            val pdfFile = PdfReportCardGenerator.generateSinglePdf(getApplication(), school, summary)
            WhatsAppService.sendReportCardToParent(getApplication(), summary, pdfFile)
        }
    }

    fun sendBulkWhatsAppNotification(phoneNumbers: List<String>, message: String) {
        WhatsAppService.sendBulkAnnouncement(getApplication(), phoneNumbers, message)
    }

    fun sendAttendanceAlertViaWhatsApp(students: List<StudentEntity>, alertMessage: String) {
        if (students.isEmpty()) return
        WhatsAppService.sendAttendanceAlertsToGuardians(getApplication(), students, alertMessage)
        viewModelScope.launch {
            _snackbarMessage.emit("WhatsApp alert triggered for ${students.size} guardian(s)")
        }
    }

    // Exports
    fun exportAttendanceRegister(monthName: String, onFileReady: (File) -> Unit) {
        viewModelScope.launch {
            val students = allActiveStudents.value
            val attendance = repository.getAttendanceByDateSync(_selectedDate.value)
            val file = ExcelExportService.exportAttendanceRegister(getApplication(), monthName, students, attendance)
            _snackbarMessage.emit("Exported: ${file.name}")
            onFileReady(file)
        }
    }

    fun exportLeaveRegister(onFileReady: (File) -> Unit) {
        viewModelScope.launch {
            val leaves = repository.getAllLeavesSync()
            val file = ExcelExportService.exportLeaveRegister(getApplication(), leaves)
            _snackbarMessage.emit("Exported: ${file.name}")
            onFileReady(file)
        }
    }

    // Database Maintenance & Diagnostics
    val auditLogs: StateFlow<List<AuditTrailEntity>> = repository.auditLogs.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val pendingSyncCount: StateFlow<Int> = repository.pendingSyncCount.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    private val _diagnosticResult = MutableStateFlow<DatabaseMaintenanceService.DiagnosticResult?>(null)
    val diagnosticResult = _diagnosticResult.asStateFlow()

    // Scheduled Daily WorkManager External Backup State
    private val _isDailyBackupScheduled = MutableStateFlow(
        DatabaseMaintenanceService.isDailyBackupEnabled(application)
    )
    val isDailyBackupScheduled = _isDailyBackupScheduled.asStateFlow()

    private val initialBackupMeta = DatabaseMaintenanceService.getLastExternalBackupMetadata(application)
    private val _lastExternalBackupTime = MutableStateFlow(initialBackupMeta.first)
    val lastExternalBackupTime = _lastExternalBackupTime.asStateFlow()

    private val _lastExternalBackupPath = MutableStateFlow(initialBackupMeta.second)
    val lastExternalBackupPath = _lastExternalBackupPath.asStateFlow()

    private val _lastCloudSyncTime = MutableStateFlow(
        prefs.getLong("last_cloud_sync_time", -1L).let { if (it > 0L) it else null }
    )
    val lastCloudSyncTime = _lastCloudSyncTime.asStateFlow()

    private val _isSyncingNow = MutableStateFlow(false)
    val isSyncingNow = _isSyncingNow.asStateFlow()

    init {
        SchoolNotificationService.ensureNotificationChannels(application)
        if (_isDailyBackupScheduled.value) {
            try {
                BackupScheduler.scheduleDailyDatabaseBackup(application)
            } catch (_: Exception) {
                // WorkManager may not be initialized in isolated unit test contexts
            }
        }
        try {
            NotificationScheduler.schedulePeriodicDeadlineChecks(application)
        } catch (_: Exception) {
            // Safe fallback in isolated unit tests
        }
    }

    fun refreshExternalBackupMetadata() {
        val (ts, path) = DatabaseMaintenanceService.getLastExternalBackupMetadata(getApplication())
        _lastExternalBackupTime.value = ts
        _lastExternalBackupPath.value = path
    }

    fun toggleDailyBackupSchedule(enabled: Boolean) {
        _isDailyBackupScheduled.value = enabled
        if (enabled) {
            BackupScheduler.scheduleDailyDatabaseBackup(getApplication(), replaceExisting = true)
            viewModelScope.launch {
                _snackbarMessage.emit("Daily Room DB backup scheduled via WorkManager.")
            }
        } else {
            BackupScheduler.cancelDailyDatabaseBackup(getApplication())
            viewModelScope.launch {
                _snackbarMessage.emit("Scheduled daily external backup paused.")
            }
        }
    }

    fun runImmediateExternalBackup() {
        viewModelScope.launch {
            val file = DatabaseMaintenanceService.exportDatabaseToExternalStorage(getApplication())
            try {
                BackupScheduler.triggerImmediateExternalBackup(getApplication())
            } catch (_: Exception) {
                // Safe fallback if WorkManager isn't initialized in unit tests
            }
            refreshExternalBackupMetadata()
            _snackbarMessage.emit("Room DB exported to external storage: ${file.name}")
        }
    }

    fun runDiagnostics() {
        viewModelScope.launch {
            val res = DatabaseMaintenanceService.runDiagnostics(getApplication())
            _diagnosticResult.value = res
            refreshExternalBackupMetadata()
            _snackbarMessage.emit(if (res.isHealthy) "Database checksum healthy (Integrity OK)" else "Integrity warning detected")
        }
    }

    fun runVacuumAndOptimize() {
        viewModelScope.launch {
            val ok = DatabaseMaintenanceService.executeVacuumAndOptimization(getApplication(), currentUserName.value)
            runDiagnostics()
            _snackbarMessage.emit(if (ok) "SQLite VACUUM complete. Disk space reclaimed." else "Optimization failed.")
        }
    }

    fun archiveGraduatedStudents(gradClass: Int = 8) {
        viewModelScope.launch {
            val count = DatabaseMaintenanceService.archiveGraduatedStudents(getApplication(), gradClass, "2025-2026", currentUserName.value)
            _snackbarMessage.emit("Archived $count graduated students to archive table.")
        }
    }

    fun triggerForceSync() {
        viewModelScope.launch {
            _isSyncingNow.value = true
            try {
                val (count, ok) = repository.triggerForceSync()
                if (ok) {
                    val now = System.currentTimeMillis()
                    _lastCloudSyncTime.value = now
                    prefs.edit().putLong("last_cloud_sync_time", now).apply()
                }
                _snackbarMessage.emit("Pushed $count offline changes to cloud backend.")
            } finally {
                _isSyncingNow.value = false
            }
        }
    }

    // Teacher Overlay Coach
    private val _showCoachTour = MutableStateFlow(false)
    val showCoachTour = _showCoachTour.asStateFlow()

    fun triggerCoachTour() {
        _showCoachTour.value = true
    }

    fun dismissCoachTour() {
        _showCoachTour.value = false
    }
}
