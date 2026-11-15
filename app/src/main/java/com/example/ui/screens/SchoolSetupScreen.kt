package com.example.ui.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.SchoolProfileEntity
import com.example.domain.service.BiometricAuthHelper
import com.example.domain.service.BiometricCapability
import com.example.ui.components.BiometricLockDialog
import com.example.ui.localization.AppStrings
import com.example.ui.localization.LocalAppLanguage
import com.example.ui.viewmodel.SchoolViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SchoolSetupScreen(
    viewModel: SchoolViewModel
) {
    val context = LocalContext.current
    val lang = LocalAppLanguage.current

    val profile by viewModel.schoolProfile.collectAsState()
    val lastBackupTime by viewModel.lastExternalBackupTime.collectAsState()
    val lastBackupPath by viewModel.lastExternalBackupPath.collectAsState()
    val isDailyBackupScheduled by viewModel.isDailyBackupScheduled.collectAsState()
    val lastCloudSyncTime by viewModel.lastCloudSyncTime.collectAsState()
    val pendingSyncCount by viewModel.pendingSyncCount.collectAsState()
    val isSyncingNow by viewModel.isSyncingNow.collectAsState()
    val biometricProtectStudents by viewModel.biometricProtectStudents.collectAsState()
    val biometricProtectReports by viewModel.biometricProtectReports.collectAsState()

    val biometricCapability = remember(context) { BiometricAuthHelper.checkCapability(context) }
    val timeFormatter = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US) }

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Backup, Sync & Security, 1 = School Profile
    var showTestBiometricDialog by remember { mutableStateOf(false) }

    val initial = profile ?: SchoolProfileEntity()
    var nameEn by remember(profile) { mutableStateOf(initial.schoolNameEn) }
    var nameUr by remember(profile) { mutableStateOf(initial.schoolNameUr) }
    var udiseCode by remember(profile) { mutableStateOf(initial.udiseCode) }
    var village by remember(profile) { mutableStateOf(initial.villageOrTown) }
    var block by remember(profile) { mutableStateOf(initial.block) }
    var district by remember(profile) { mutableStateOf(initial.district) }
    var state by remember(profile) { mutableStateOf(initial.state) }
    var pin by remember(profile) { mutableStateOf(initial.pinCode) }
    var hmName by remember(profile) { mutableStateOf(initial.headmasterName) }
    var hmPhone by remember(profile) { mutableStateOf(initial.headmasterPhone) }
    var hmEmail by remember(profile) { mutableStateOf(initial.headmasterEmail) }
    var session by remember(profile) { mutableStateOf(initial.currentSession) }
    var workingDays by remember(profile) { mutableStateOf(initial.totalWorkingDays.toString()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { Spacer(modifier = Modifier.height(4.dp)) }

        // Top Settings Tabs: Security/Backup/Sync vs School Profile
        item {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            ) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Text(
                                AppStrings.get("settings_backup_sync_tab", lang),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        modifier = Modifier.testTag("tab_settings_backup_sync")
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Text(
                                AppStrings.get("settings_school_profile_tab", lang),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        },
                        modifier = Modifier.testTag("tab_school_profile")
                    )
                }
            }
        }

        if (selectedTab == 0) {
            // 1. Database Backup & External Storage Export Card
            item {
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_backup_card"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Backup,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = AppStrings.get("last_external_backup", lang),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = if (lastBackupTime != null) {
                                        timeFormatter.format(Date(lastBackupTime!!))
                                    } else {
                                        AppStrings.get("no_backup_yet", lang)
                                    },
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = if (lastBackupTime != null) Color(0xFF155724) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.testTag("settings_last_backup_time")
                                )
                            }
                        }

                        if (!lastBackupPath.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FolderOpen,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = File(lastBackupPath!!).name,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

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
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = AppStrings.get("scheduled_daily_backup", lang),
                                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                                    )
                                    Text(
                                        text = if (isDailyBackupScheduled) {
                                            AppStrings.get("backup_active_label", lang)
                                        } else {
                                            AppStrings.get("backup_paused_label", lang)
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Switch(
                                checked = isDailyBackupScheduled,
                                onCheckedChange = { viewModel.toggleDailyBackupSchedule(it) },
                                modifier = Modifier.testTag("settings_daily_backup_switch")
                            )
                        }

                        Button(
                            onClick = { viewModel.runImmediateExternalBackup() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_export_external_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(AppStrings.get("export_external_btn", lang))
                        }
                    }
                }
            }

            // 2. Cloud Sync Card
            item {
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_sync_card"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudSync,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = AppStrings.get("last_cloud_sync", lang),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = if (lastCloudSyncTime != null) {
                                        timeFormatter.format(Date(lastCloudSyncTime!!))
                                    } else {
                                        AppStrings.get("never_synced", lang)
                                    },
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.testTag("settings_last_sync_time")
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (pendingSyncCount > 0) Color(0xFFFFF3CD) else Color(0xFFD4EDDA)
                            ) {
                                Text(
                                    text = if (pendingSyncCount > 0) "$pendingSyncCount pending" else "Synced",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (pendingSyncCount > 0) Color(0xFF856404) else Color(0xFF155724),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        OutlinedButton(
                            onClick = { viewModel.triggerForceSync() },
                            enabled = !isSyncingNow,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_force_sync_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isSyncingNow) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Synchronizing...")
                            } else {
                                Icon(imageVector = Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(AppStrings.get("force_sync", lang))
                            }
                        }
                    }
                }
            }

            // 3. Biometric Protection Card (androidx.biometric)
            item {
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_biometric_card"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(MaterialTheme.colorScheme.tertiaryContainer, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Fingerprint,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = AppStrings.get("biometric_protection_title", lang),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                Text(
                                    text = when (biometricCapability) {
                                        BiometricCapability.AVAILABLE -> AppStrings.get("biometric_status_ready", lang)
                                        BiometricCapability.NONE_ENROLLED -> AppStrings.get("biometric_status_not_enrolled", lang)
                                        BiometricCapability.HARDWARE_UNAVAILABLE -> AppStrings.get("biometric_status_unavailable", lang)
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (biometricCapability == BiometricCapability.AVAILABLE) Color(0xFF155724) else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.testTag("settings_biometric_status")
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = AppStrings.get("biometric_protect_students", lang),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Text(
                                    text = "Mask UIDAI Aadhaar tokens & guardian phone numbers until unlocked",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = biometricProtectStudents,
                                onCheckedChange = { viewModel.setBiometricProtectStudents(it) },
                                modifier = Modifier.testTag("toggle_biometric_students")
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = AppStrings.get("biometric_protect_reports", lang),
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Text(
                                    text = "Require Fingerprint / Face Unlock before generating or sharing report cards",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = biometricProtectReports,
                                onCheckedChange = { viewModel.setBiometricProtectReports(it) },
                                modifier = Modifier.testTag("toggle_biometric_reports")
                            )
                        }

                        OutlinedButton(
                            onClick = { showTestBiometricDialog = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("settings_test_biometric_btn"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Security, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Verify Fingerprint / Face Unlock")
                        }
                    }
                }
            }
        } else {
            // School Profile & UDISE+ Configuration Tab
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Institutional Identity & UDISE+",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        OutlinedTextField(
                            value = nameEn,
                            onValueChange = { nameEn = it },
                            label = { Text("School Name (English)") },
                            modifier = Modifier.fillMaxWidth().testTag("setup_name_en")
                        )

                        OutlinedTextField(
                            value = nameUr,
                            onValueChange = { nameUr = it },
                            label = { Text("اسکول کا نام (اردو)") },
                            modifier = Modifier.fillMaxWidth().testTag("setup_name_ur")
                        )

                        OutlinedTextField(
                            value = udiseCode,
                            onValueChange = { udiseCode = it },
                            label = { Text("UDISE+ / School Code") },
                            modifier = Modifier.fillMaxWidth().testTag("setup_udise_code")
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = village,
                                onValueChange = { village = it },
                                label = { Text("Town / Village") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = block,
                                onValueChange = { block = it },
                                label = { Text("Block") },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = district,
                                onValueChange = { district = it },
                                label = { Text("District") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = state,
                                onValueChange = { state = it },
                                label = { Text("State / UT") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = pin,
                                onValueChange = { pin = it },
                                label = { Text("PIN") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Headmaster / Authority Details",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )

                        OutlinedTextField(
                            value = hmName,
                            onValueChange = { hmName = it },
                            label = { Text("Headmaster / Principal Name") },
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = hmPhone,
                                onValueChange = { hmPhone = it },
                                label = { Text("Official Phone") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = hmEmail,
                                onValueChange = { hmEmail = it },
                                label = { Text("Official Email") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Academic Calendar & Configuration",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = session,
                                onValueChange = { session = it },
                                label = { Text("Academic Session") },
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = workingDays,
                                onValueChange = { workingDays = it },
                                label = { Text("Scheduled Working Days") },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Text(
                            text = "Grades & Classes Allocation: Primary (Classes 1–5), Middle (Classes 6–8) with Sections A, B, C.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                Button(
                    onClick = {
                        val updated = initial.copy(
                            schoolNameEn = nameEn,
                            schoolNameUr = nameUr,
                            udiseCode = udiseCode,
                            villageOrTown = village,
                            block = block,
                            district = district,
                            state = state,
                            pinCode = pin,
                            headmasterName = hmName,
                            headmasterPhone = hmPhone,
                            headmasterEmail = hmEmail,
                            currentSession = session,
                            totalWorkingDays = workingDays.toIntOrNull() ?: 220
                        )
                        viewModel.updateSchoolProfile(updated)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("save_school_profile_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save School Profile")
                }
            }
        }

        item { Spacer(modifier = Modifier.height(30.dp)) }
    }

    if (showTestBiometricDialog) {
        BiometricLockDialog(
            actionTitle = "Test Biometric Authentication",
            onSuccess = {
                viewModel.unlockStudentRecords()
                viewModel.unlockReportCardAccess()
                showTestBiometricDialog = false
            },
            onDismiss = { showTestBiometricDialog = false }
        )
    }
}
