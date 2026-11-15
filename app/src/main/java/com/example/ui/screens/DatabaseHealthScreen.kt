package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.domain.service.DatabaseMaintenanceService
import com.example.ui.components.BiometricLockDialog
import com.example.ui.localization.AppStrings
import com.example.ui.localization.LocalAppLanguage
import com.example.ui.viewmodel.SchoolViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DatabaseHealthScreen(
    viewModel: SchoolViewModel
) {
    val context = LocalContext.current
    val lang = LocalAppLanguage.current
    val diagnosticResult by viewModel.diagnosticResult.collectAsState()
    val auditLogs by viewModel.auditLogs.collectAsState()
    val pendingSyncCount by viewModel.pendingSyncCount.collectAsState()
    val isDailyBackupScheduled by viewModel.isDailyBackupScheduled.collectAsState()
    val lastExternalBackupTime by viewModel.lastExternalBackupTime.collectAsState()
    val lastExternalBackupPath by viewModel.lastExternalBackupPath.collectAsState()

    var showBiometricAuthForExport by remember { mutableStateOf(false) }
    var showBiometricAuthForArchive by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.runDiagnostics()
    }

    val dateFormat = SimpleDateFormat("dd MMM, hh:mm:ss a", Locale.US)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { Spacer(modifier = Modifier.height(4.dp)) }

        // Diagnostic Status Card
        item {
            val diag = diagnosticResult
            val isHealthy = diag?.isHealthy ?: true

            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("db_diagnostic_card"),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = if (isHealthy) Color(0xFFD4EDDA).copy(alpha = 0.6f) else Color(0xFFF8D7DA).copy(alpha = 0.6f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isHealthy) Icons.Default.CheckCircle else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (isHealthy) Color(0xFF155724) else Color(0xFF721C24),
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (isHealthy) "SQLite Integrity Check: PASSED" else "Integrity Issues Detected",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (isHealthy) Color(0xFF155724) else Color(0xFF721C24)
                                )
                                Text(
                                    text = "PRAGMA output: ${diag?.integrityOutput ?: "ok"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isHealthy) Color(0xFF155724).copy(alpha = 0.8f) else Color(0xFF721C24).copy(alpha = 0.8f)
                                )
                            }
                        }

                        Button(
                            onClick = { viewModel.runDiagnostics() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isHealthy) Color(0xFF155724) else Color(0xFF721C24)
                            ),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("run_check_button")
                        ) {
                            Text("Recheck", fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Metrics Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("DB SIZE", style = MaterialTheme.typography.labelSmall)
                            Text(
                                "${(diag?.databaseSizeBytes ?: 0L) / 1024} KB",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("STUDENTS", style = MaterialTheme.typography.labelSmall)
                            Text(
                                "${diag?.totalActiveStudents ?: 0}",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("ATTENDANCE", style = MaterialTheme.typography.labelSmall)
                            Text(
                                "${diag?.totalAttendanceRecords ?: 0}",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                }
            }
        }

        // Maintenance Action Buttons
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Database Maintenance & Optimization",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // 1. Run VACUUM & Optimize
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Execute SQLite VACUUM", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                            Text("Reclaims fragmented disk space and rebuilds indices.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Button(
                            onClick = { viewModel.runVacuumAndOptimize() },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("vacuum_database_button")
                        ) {
                            Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("VACUUM", fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 2. Archive Graduated Students
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Archive Class 8 Graduates", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                            Text("Transfers completed academic sessions to archive table.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        OutlinedButton(
                            onClick = { showBiometricAuthForArchive = true },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("archive_students_button")
                        ) {
                            Icon(imageVector = Icons.Default.Archive, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Archive", fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 3. Export Database Backup file
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Export Database Backup (.db)", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                            Text("Export complete SQLite database file for migration.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Button(
                            onClick = { showBiometricAuthForExport = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("export_sqlite_backup_button")
                        ) {
                            Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Export", fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 4. Force Push/Pull Sync
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Bi-directional REST Cloud Sync", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                            Text("$pendingSyncCount offline mutations awaiting server push.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Button(
                            onClick = { viewModel.triggerForceSync() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF27AE60)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("force_cloud_sync_button")
                        ) {
                            Icon(imageVector = Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Force Sync", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Scheduled Daily WorkManager External Backup Card (Disaster Recovery)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("scheduled_backup_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = AppStrings.get("scheduled_daily_backup", lang),
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = AppStrings.get("scheduled_backup_desc", lang),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Switch(
                            checked = isDailyBackupScheduled,
                            onCheckedChange = { enabled ->
                                viewModel.toggleDailyBackupSchedule(enabled)
                            },
                            modifier = Modifier.testTag("daily_backup_schedule_switch")
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isDailyBackupScheduled) Color(0xFFE7F6EE) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = if (isDailyBackupScheduled) {
                                    AppStrings.get("backup_active_label", lang)
                                } else {
                                    AppStrings.get("backup_paused_label", lang)
                                },
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (isDailyBackupScheduled) Color(0xFF0A5C36) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            val formattedTime = lastExternalBackupTime?.let { ts ->
                                dateFormat.format(Date(ts))
                            } ?: AppStrings.get("no_backup_yet", lang)
                            Text(
                                text = "${AppStrings.get("last_external_backup", lang)}: $formattedTime",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            lastExternalBackupPath?.let { path ->
                                Text(
                                    text = path,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = MaterialTheme.colorScheme.outline,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.runImmediateExternalBackup() },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("run_external_backup_now_button")
                        ) {
                            Icon(imageVector = Icons.Default.Storage, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(AppStrings.get("run_backup_now", lang), fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Audit Trail Logs
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "System Audit Trail (${auditLogs.size} Events)",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                    )
                }
                Text(text = "Encrypted Log", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }
        }

        items(auditLogs.take(25), key = { it.id }) { log ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = log.actionType,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = dateFormat.format(Date(log.timestamp)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Actor: ${log.actor}",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                        text = log.details,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item { Spacer(modifier = Modifier.height(30.dp)) }
    }

    // Biometric / PIN Lock for Exporting DB
    if (showBiometricAuthForExport) {
        BiometricLockDialog(
            actionTitle = "Export SQLite Database Backup",
            onSuccess = {
                showBiometricAuthForExport = false
                val backupFile = DatabaseMaintenanceService.exportDatabaseBackup(context)
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", backupFile)
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/octet-stream"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(shareIntent, "Save / Share DB Backup"))
            },
            onDismiss = { showBiometricAuthForExport = false }
        )
    }

    // Biometric / PIN Lock for Archiving Students
    if (showBiometricAuthForArchive) {
        BiometricLockDialog(
            actionTitle = "Archive Graduated Class 8 Students",
            onSuccess = {
                showBiometricAuthForArchive = false
                viewModel.archiveGraduatedStudents(gradClass = 8)
            },
            onDismiss = { showBiometricAuthForArchive = false }
        )
    }
}
