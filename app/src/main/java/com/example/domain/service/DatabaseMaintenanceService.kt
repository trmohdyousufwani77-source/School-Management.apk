package com.example.domain.service

import android.content.Context
import com.example.data.local.SchoolDatabase
import com.example.data.local.entities.ArchivedStudentEntity
import com.example.data.local.entities.AuditTrailEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

object DatabaseMaintenanceService {

    data class DiagnosticResult(
        val isHealthy: Boolean,
        val integrityOutput: String,
        val totalActiveStudents: Int,
        val totalAttendanceRecords: Int,
        val databaseSizeBytes: Long,
        val lastVacuumTimestamp: Long?
    )

    suspend fun runDiagnostics(context: Context): DiagnosticResult = withContext(Dispatchers.IO) {
        val db = SchoolDatabase.getInstance(context)
        val (isOk, output) = db.runIntegrityCheck()
        val activeStudents = db.studentDao().getActiveStudentCount()
        val attendanceCount = db.attendanceDao().getTotalAttendanceRecordsCount()

        val dbFile = context.getDatabasePath(SchoolDatabase.DATABASE_NAME)
        val sizeBytes = if (dbFile.exists()) dbFile.length() else 0L

        DiagnosticResult(
            isHealthy = isOk,
            integrityOutput = output,
            totalActiveStudents = activeStudents,
            totalAttendanceRecords = attendanceCount,
            databaseSizeBytes = sizeBytes,
            lastVacuumTimestamp = System.currentTimeMillis()
        )
    }

    suspend fun executeVacuumAndOptimization(context: Context, actor: String): Boolean = withContext(Dispatchers.IO) {
        val db = SchoolDatabase.getInstance(context)
        val success = db.runVacuumMaintenance()
        if (success) {
            db.auditDao().insertAudit(
                AuditTrailEntity(
                    actionType = "DATABASE_VACUUMED",
                    actor = actor,
                    details = "SQLite VACUUM and re-indexing executed successfully."
                )
            )
        }
        success
    }

    suspend fun archiveGraduatedStudents(
        context: Context,
        graduationClass: Int = 8,
        graduationSession: String = "2025-2026",
        actor: String
    ): Int = withContext(Dispatchers.IO) {
        val db = SchoolDatabase.getInstance(context)
        val allStudents = db.studentDao().getAllActiveStudentsSync()
        val toArchive = allStudents.filter { it.className >= graduationClass }

        if (toArchive.isNotEmpty()) {
            val archivedEntities = toArchive.map { s ->
                ArchivedStudentEntity(
                    uuid = s.uuid,
                    admissionNumber = s.admissionNumber,
                    finalRollNumber = s.rollNumber,
                    nameEn = s.nameEn,
                    nameUr = s.nameUr,
                    completedClass = s.className,
                    graduationSession = graduationSession,
                    fatherName = s.fatherName,
                    contact = s.primaryContact,
                    maskedAadhaar = s.maskedAadhaar,
                    archiveReason = "Graduated Class $graduationClass ($graduationSession)"
                )
            }
            db.studentDao().insertArchivedStudents(archivedEntities)
            toArchive.forEach { db.studentDao().deleteStudent(it) }

            db.auditDao().insertAudit(
                AuditTrailEntity(
                    actionType = "STUDENTS_ARCHIVED",
                    actor = actor,
                    details = "Archived ${toArchive.size} students from Class $graduationClass to archived_students table."
                )
            )
        }
        toArchive.size
    }

    private const val PREFS_NAME = "smart_school_prefs"
    const val KEY_LAST_EXTERNAL_BACKUP_TIME = "last_external_backup_time"
    const val KEY_LAST_EXTERNAL_BACKUP_PATH = "last_external_backup_path"
    const val KEY_DAILY_BACKUP_ENABLED = "daily_backup_enabled"
    private const val MAX_RETAINED_BACKUPS = 7

    fun exportDatabaseBackup(context: Context): File {
        val resolvedDbFile = resolveAndCheckpointDbFile(context)
        val backupDir = File(context.filesDir, "backups")
        if (!backupDir.exists()) backupDir.mkdirs()

        val backupFile = File(backupDir, "SmartSchool_Backup_${System.currentTimeMillis()}.db")
        if (resolvedDbFile.exists()) {
            FileInputStream(resolvedDbFile).use { input ->
                FileOutputStream(backupFile).use { output ->
                    input.copyTo(output)
                    output.flush()
                }
            }
        }
        return backupFile
    }

    /**
     * Exports the Room SQLite database to app-specific external storage (zero-permission, Scoped Storage compliant)
     * for disaster recovery, checkpoints WAL first, and retains the newest backups.
     */
    fun exportDatabaseToExternalStorage(context: Context): File {
        val resolvedDbFile = resolveAndCheckpointDbFile(context)
        val externalBackupDir = context.getExternalFilesDir("backups")
            ?: File(context.filesDir, "backups")
        if (!externalBackupDir.exists()) {
            externalBackupDir.mkdirs()
        }

        val timestamp = java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.US)
            .format(java.util.Date())
        val backupFile = File(externalBackupDir, "SmartSchool_Recovery_$timestamp.db")

        if (resolvedDbFile.exists()) {
            FileInputStream(resolvedDbFile).use { input ->
                FileOutputStream(backupFile).use { output ->
                    input.copyTo(output)
                    output.flush()
                }
            }
        }

        pruneOldBackups(externalBackupDir)
        recordExternalBackupMetadata(context, backupFile)
        return backupFile
    }

    private fun resolveAndCheckpointDbFile(context: Context): File {
        val db = SchoolDatabase.getInstance(context)
        var openPath: String? = null
        try {
            val writable = db.openHelper.writableDatabase
            openPath = writable.path
            writable.query("PRAGMA wal_checkpoint(FULL)").use { cursor ->
                cursor.moveToFirst()
            }
        } catch (_: Exception) {
            // Best-effort WAL flush
        }
        val fromOpenHelper = openPath?.takeIf { it.isNotBlank() && it != ":memory:" }?.let { File(it) }
        return if (fromOpenHelper != null && fromOpenHelper.exists()) {
            fromOpenHelper
        } else {
            context.getDatabasePath(SchoolDatabase.DATABASE_NAME)
        }
    }

    fun getLastExternalBackupMetadata(context: Context): Pair<Long?, String?> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val ts = prefs.getLong(KEY_LAST_EXTERNAL_BACKUP_TIME, -1L)
        val path = prefs.getString(KEY_LAST_EXTERNAL_BACKUP_PATH, null)
        return Pair(if (ts > 0L) ts else null, path)
    }

    fun isDailyBackupEnabled(context: Context): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_DAILY_BACKUP_ENABLED, true)
    }

    fun setDailyBackupEnabled(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_DAILY_BACKUP_ENABLED, enabled).apply()
    }

    private fun recordExternalBackupMetadata(context: Context, backupFile: File) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putLong(KEY_LAST_EXTERNAL_BACKUP_TIME, System.currentTimeMillis())
            .putString(KEY_LAST_EXTERNAL_BACKUP_PATH, backupFile.absolutePath)
            .apply()
    }

    private fun pruneOldBackups(backupDir: File) {
        val files = backupDir.listFiles { file ->
            file.isFile && file.name.endsWith(".db")
        }?.sortedByDescending { it.lastModified() } ?: return

        if (files.size > MAX_RETAINED_BACKUPS) {
            files.drop(MAX_RETAINED_BACKUPS).forEach { old ->
                old.delete()
            }
        }
    }
}
