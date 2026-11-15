package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.StudentEntity
import com.example.domain.model.UserRole
import com.example.domain.service.CsvStudentParserService
import com.example.ui.components.BiometricLockDialog
import com.example.ui.components.StudentQrIdCardDialog
import com.example.ui.components.StudentVoiceMemosDialog
import com.example.ui.localization.AppLanguage
import com.example.ui.localization.AppStrings
import com.example.ui.localization.LocalAppLanguage
import com.example.ui.viewmodel.SchoolViewModel
import java.util.UUID

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StudentsScreen(
    viewModel: SchoolViewModel,
    onNavigateToExams: () -> Unit
) {
    val context = LocalContext.current
    val lang = LocalAppLanguage.current
    val userRole by viewModel.currentUserRole.collectAsState()
    val allStudents by viewModel.allActiveStudents.collectAsState()
    val allVoiceMemos by viewModel.allVoiceMemos.collectAsState()
    val biometricProtectStudents by viewModel.biometricProtectStudents.collectAsState()
    val isStudentRecordsUnlocked by viewModel.isStudentRecordsUnlocked.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    val areSensitiveFieldsLocked = biometricProtectStudents && !isStudentRecordsUnlocked

    var selectedClassFilter by remember { mutableIntStateOf(0) } // 0 = all
    val selectedStudentUuids = remember { mutableStateListOf<String>() }
    var showEnrollDialog by remember { mutableStateOf(false) }
    var showBulkMessageDialog by remember { mutableStateOf(false) }
    var showCsvImportDialog by remember { mutableStateOf(false) }
    var showBiometricUnlockDialog by remember { mutableStateOf(false) }
    var pendingUnlockedAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var qrCardStudent by remember { mutableStateOf<StudentEntity?>(null) }
    var voiceMemoStudent by remember { mutableStateOf<StudentEntity?>(null) }

    // Filter students by query and class
    val filteredStudents = remember(allStudents, searchQuery, selectedClassFilter) {
        allStudents.filter { s ->
            val matchesClass = selectedClassFilter == 0 || s.className == selectedClassFilter
            val matchesQuery = searchQuery.isBlank() ||
                    s.nameEn.contains(searchQuery, ignoreCase = true) ||
                    s.nameUr.contains(searchQuery, ignoreCase = true) ||
                    s.admissionNumber.contains(searchQuery, ignoreCase = true) ||
                    s.rollNumber.toString().contains(searchQuery)
            matchesClass && matchesQuery
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // Search Bar & Headmaster CSV Import Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { viewModel.setSearchQuery(it) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("student_search_input"),
                        placeholder = { Text(AppStrings.get("search_hint", lang)) },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Search, contentDescription = null)
                        },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setSearchQuery("") }) {
                                    Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp)
                    )

                    OutlinedButton(
                        onClick = {
                            if (userRole == UserRole.HEADMASTER) {
                                showCsvImportDialog = true
                            } else {
                                Toast.makeText(
                                    context,
                                    AppStrings.get("csv_import_headmaster_only", lang),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 14.dp),
                        modifier = Modifier.testTag("open_csv_import_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileUpload,
                            contentDescription = AppStrings.get("import_csv_btn", lang),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = AppStrings.get("import_csv_btn", lang), fontSize = 12.sp)
                    }
                }
            }

            // Class Filter Chips (All, Class 1 to 8)
            item {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    FilterChip(
                        selected = selectedClassFilter == 0,
                        onClick = { selectedClassFilter = 0 },
                        label = { Text("All (1–8)") }
                    )
                    (1..8).forEach { cls ->
                        FilterChip(
                            selected = selectedClassFilter == cls,
                            onClick = { selectedClassFilter = cls },
                            label = { Text("Class $cls") }
                        )
                    }
                }
            }

            // Biometric Protection Gate Banner for Sensitive Student Records
            if (biometricProtectStudents) {
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (areSensitiveFieldsLocked) Color(0xFFFFF3CD) else Color(0xFFD4EDDA),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("student_records_locked_banner")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = if (areSensitiveFieldsLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                    contentDescription = null,
                                    tint = if (areSensitiveFieldsLocked) Color(0xFF856404) else Color(0xFF155724),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = if (areSensitiveFieldsLocked) {
                                            AppStrings.get("unlock_sensitive_records", lang)
                                        } else {
                                            "Sensitive Records Unlocked (Biometric Session)"
                                        },
                                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                        color = if (areSensitiveFieldsLocked) Color(0xFF856404) else Color(0xFF155724)
                                    )
                                    Text(
                                        text = if (areSensitiveFieldsLocked) {
                                            AppStrings.get("sensitive_records_locked_msg", lang)
                                        } else {
                                            "UIDAI tokens, guardian phone numbers & QR IDs are visible."
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (areSensitiveFieldsLocked) Color(0xFF856404) else Color(0xFF155724)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            if (areSensitiveFieldsLocked) {
                                Button(
                                    onClick = { showBiometricUnlockDialog = true },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("unlock_student_records_btn")
                                ) {
                                    Text(AppStrings.get("biometric_unlock_btn", lang), fontSize = 11.sp)
                                }
                            } else {
                                OutlinedButton(
                                    onClick = { viewModel.lockStudentRecords() },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("lock_student_records_btn")
                                ) {
                                    Text(AppStrings.get("biometric_lock_btn", lang), fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }

            // UIDAI Guardrail Compliance Notice
            item {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "UIDAI Compliance Active: Raw Aadhaar numbers are never stored. Only masked tokens (XXXX-XXXX-1234) & parental consent audit hashes are maintained.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }

            // Bulk Action Toolbar
            if (selectedStudentUuids.isNotEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${selectedStudentUuids.size} selected",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { showBulkMessageDialog = true },
                                    modifier = Modifier.testTag("bulk_whatsapp_action_btn"),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(imageVector = Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("WhatsApp Notice", fontSize = 12.sp)
                                }
                                OutlinedButton(
                                    onClick = { selectedStudentUuids.clear() },
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Clear", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Student Cards
            items(filteredStudents, key = { it.uuid }) { student ->
                val isSelected = selectedStudentUuids.contains(student.uuid)
                val studentMemoCount = allVoiceMemos.count { it.studentUuid == student.uuid }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("student_card_${student.rollNumber}"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { checked ->
                                if (checked) selectedStudentUuids.add(student.uuid) else selectedStudentUuids.remove(student.uuid)
                            },
                            modifier = Modifier.testTag("student_checkbox_${student.rollNumber}")
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${student.nameEn} (${student.nameUr})",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = when (student.aadhaarStatus) {
                                        "VERIFIED" -> Color(0xFFD4EDDA)
                                        "PENDING" -> Color(0xFFFFF3CD)
                                        else -> Color(0xFFE2E3E5)
                                    }
                                ) {
                                    Text(
                                        text = student.aadhaarStatus,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = when (student.aadhaarStatus) {
                                            "VERIFIED" -> Color(0xFF155724)
                                            "PENDING" -> Color(0xFF856404)
                                            else -> Color(0xFF383D41)
                                        },
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Class ${student.className}-${student.section} | Roll: ${student.rollNumber} | Adm: ${student.admissionNumber}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (areSensitiveFieldsLocked) {
                                    "UIDAI Token: XXXX-XXXX-•••• (Biometric Locked)"
                                } else {
                                    "UIDAI Token: ${student.maskedAadhaar} • ${student.primaryContact}"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = if (areSensitiveFieldsLocked) Color(0xFF856404) else MaterialTheme.colorScheme.primary
                            )
                        }

                        // Behavioral Voice Memo button
                        IconButton(
                            onClick = { voiceMemoStudent = student },
                            modifier = Modifier.testTag("student_voice_memo_btn_${student.rollNumber}")
                        ) {
                            if (studentMemoCount > 0) {
                                BadgedBox(badge = { Badge { Text("$studentMemoCount") } }) {
                                    Icon(
                                        imageVector = Icons.Default.Mic,
                                        contentDescription = AppStrings.get("voice_memos_title", lang),
                                        tint = MaterialTheme.colorScheme.tertiary
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = AppStrings.get("voice_memos_title", lang),
                                    tint = MaterialTheme.colorScheme.tertiary
                                )
                            }
                        }

                        // QR ID Card preview button (requires unlock if protected)
                        IconButton(
                            onClick = {
                                if (areSensitiveFieldsLocked) {
                                    pendingUnlockedAction = { qrCardStudent = student }
                                    showBiometricUnlockDialog = true
                                } else {
                                    qrCardStudent = student
                                }
                            },
                            modifier = Modifier.testTag("student_qr_card_btn_${student.rollNumber}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.QrCode2,
                                contentDescription = AppStrings.get("qr_student_card_title", lang),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }

                        // WhatsApp Action button
                        IconButton(
                            onClick = { viewModel.sendReportViaWhatsApp(student) },
                            modifier = Modifier.testTag("student_whatsapp_btn_${student.rollNumber}")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Send WhatsApp",
                                tint = Color(0xFF25D366)
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }

        // Floating Action Button for New Enrollment
        FloatingActionButton(
            onClick = { showEnrollDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp)
                .testTag("enroll_student_fab"),
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Enroll Student")
        }
    }

    // Enroll New Student Modal Dialog
    if (showEnrollDialog) {
        EnrollStudentDialog(
            onDismiss = { showEnrollDialog = false },
            onEnroll = { newStudent ->
                viewModel.enrollNewStudent(newStudent)
                showEnrollDialog = false
            }
        )
    }

    // Bulk Message Dialog
    if (showBulkMessageDialog) {
        BulkMessageDialog(
            recipientCount = selectedStudentUuids.size,
            onDismiss = { showBulkMessageDialog = false },
            onSend = { message ->
                val phoneNumbers = allStudents.filter { selectedStudentUuids.contains(it.uuid) }.map { it.primaryContact }
                viewModel.sendBulkWhatsAppNotification(phoneNumbers, message)
                showBulkMessageDialog = false
                selectedStudentUuids.clear()
            }
        )
    }

    // Headmaster CSV Bulk Import Dialog
    if (showCsvImportDialog) {
        CsvStudentImportDialog(
            lang = lang,
            onDismiss = { showCsvImportDialog = false },
            onImportStudents = { importedStudents, sourceLabel ->
                viewModel.importStudentsFromCsv(importedStudents, sourceLabel)
                showCsvImportDialog = false
            }
        )
    }

    // Student QR ID Card Modal
    if (qrCardStudent != null) {
        StudentQrIdCardDialog(
            student = qrCardStudent!!,
            onDismiss = { qrCardStudent = null }
        )
    }

    // Student Behavioral Voice Memos Dialog
    if (voiceMemoStudent != null) {
        val target = voiceMemoStudent!!
        val studentMemos = allVoiceMemos.filter { it.studentUuid == target.uuid }
        StudentVoiceMemosDialog(
            student = target,
            memos = studentMemos,
            onSaveMemo = { tag, noteText, filePath, duration ->
                viewModel.saveStudentVoiceMemo(
                    student = target,
                    tag = tag,
                    noteText = noteText,
                    filePath = filePath,
                    durationSeconds = duration
                )
            },
            onDeleteMemo = { memo -> viewModel.deleteStudentVoiceMemo(memo) },
            onDismiss = { voiceMemoStudent = null }
        )
    }

    // Biometric Unlock Dialog for Sensitive Student Records
    if (showBiometricUnlockDialog) {
        BiometricLockDialog(
            actionTitle = AppStrings.get("unlock_sensitive_records", lang),
            onSuccess = {
                viewModel.unlockStudentRecords()
                showBiometricUnlockDialog = false
                pendingUnlockedAction?.invoke()
                pendingUnlockedAction = null
            },
            onDismiss = {
                showBiometricUnlockDialog = false
                pendingUnlockedAction = null
            }
        )
    }
}

@Composable
private fun EnrollStudentDialog(
    onDismiss: () -> Unit,
    onEnroll: (StudentEntity) -> Unit
) {
    var nameEn by remember { mutableStateOf("") }
    var nameUr by remember { mutableStateOf("") }
    var className by remember { mutableIntStateOf(6) }
    var section by remember { mutableStateOf("A") }
    var rollNo by remember { mutableStateOf("1") }
    var fatherName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("+91 94190 ") }
    var consentChecked by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Enroll New Student (UIDAI Compliant)")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = nameEn,
                    onValueChange = { nameEn = it },
                    label = { Text("Student Name (English)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("enroll_name_en")
                )
                OutlinedTextField(
                    value = nameUr,
                    onValueChange = { nameUr = it },
                    label = { Text("طالب علم کا نام (Urdu)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("enroll_name_ur")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = className.toString(),
                        onValueChange = { className = it.toIntOrNull() ?: 1 },
                        label = { Text("Class (1-8)") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = section,
                        onValueChange = { section = it },
                        label = { Text("Sec") },
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = rollNo,
                        onValueChange = { rollNo = it },
                        label = { Text("Roll") },
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = fatherName,
                    onValueChange = { fatherName = it },
                    label = { Text("Father's Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it },
                    label = { Text("Guardian WhatsApp") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { consentChecked = !consentChecked }
                ) {
                    Checkbox(checked = consentChecked, onCheckedChange = { consentChecked = it })
                    Text(
                        text = "UIDAI Parental Consent obtained for masked reference token",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (nameEn.isNotBlank() && consentChecked) {
                        val newStudent = StudentEntity(
                            uuid = UUID.randomUUID().toString(),
                            admissionNumber = "ADM-2026-${(100..999).random()}",
                            rollNumber = rollNo.toIntOrNull() ?: 1,
                            nameEn = nameEn,
                            nameUr = nameUr.ifEmpty { nameEn },
                            className = className,
                            section = section,
                            gender = "Male",
                            dob = "2015-05-15",
                            category = "GENERAL",
                            fatherName = fatherName.ifEmpty { "Guardian" },
                            motherName = "Mother",
                            primaryContact = phone,
                            whatsAppNumber = phone,
                            residentialAddress = "Sopore Central",
                            status = "ACTIVE",
                            aadhaarReferenceToken = "UID-REF-${(1000..9999).random()}",
                            maskedAadhaar = "XXXX-XXXX-${(1000..9999).random()}",
                            aadhaarStatus = "VERIFIED",
                            consentRecorded = consentChecked
                        )
                        onEnroll(newStudent)
                    }
                },
                modifier = Modifier.testTag("submit_enroll_button")
            ) {
                Text("Enroll Student")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun BulkMessageDialog(
    recipientCount: Int,
    onDismiss: () -> Unit,
    onSend: (String) -> Unit
) {
    var messageText by remember {
        mutableStateOf("Dear Parent, this is an official notice from Govt. Model Middle School regarding upcoming academic evaluations and school timing.")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Bulk Parent Announcement") },
        text = {
            Column {
                Text(
                    text = "Sending to $recipientCount selected student guardians via WhatsApp.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { messageText = it },
                    label = { Text("Message Template") },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    maxLines = 4
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSend(messageText) },
                modifier = Modifier.testTag("confirm_bulk_send_button")
            ) {
                Text("Dispatch via WhatsApp")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun CsvStudentImportDialog(
    lang: AppLanguage,
    onDismiss: () -> Unit,
    onImportStudents: (List<StudentEntity>, String) -> Unit
) {
    val context = LocalContext.current
    var csvInputText by remember { mutableStateOf("") }
    var selectedFileName by remember { mutableStateOf("Standard Template") }
    var parseResult by remember { mutableStateOf<CsvStudentParserService.CsvParseResult?>(null) }

    val csvFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    val content = stream.bufferedReader(Charsets.UTF_8).readText()
                    csvInputText = content
                    selectedFileName = uri.lastPathSegment?.substringAfterLast('/') ?: "selected_students.csv"
                    parseResult = CsvStudentParserService.parse(content)
                }
            } catch (e: Exception) {
                Toast.makeText(
                    context,
                    "Unable to read CSV: ${e.localizedMessage ?: "error"}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("csv_import_dialog"),
        title = {
            Text(
                text = AppStrings.get("csv_import_title", lang),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Required headers: admission_number, roll_number, name_en, class_name, primary_contact",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            csvFileLauncher.launch(arrayOf("text/*", "text/csv", "text/comma-separated-values"))
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("pick_csv_file_button")
                    ) {
                        Icon(imageVector = Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(AppStrings.get("pick_csv_file", lang), fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            val sample = CsvStudentParserService.generateSampleCsvTemplate()
                            csvInputText = sample
                            selectedFileName = "standard_students_template.csv"
                            parseResult = CsvStudentParserService.parse(sample)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("load_sample_csv_button")
                    ) {
                        Text(AppStrings.get("load_sample_csv", lang), fontSize = 11.sp)
                    }
                }

                OutlinedTextField(
                    value = csvInputText,
                    onValueChange = {
                        csvInputText = it
                        parseResult = if (it.isNotBlank()) CsvStudentParserService.parse(it) else null
                    },
                    label = { Text("CSV Content ($selectedFileName)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .testTag("csv_content_input"),
                    maxLines = 6
                )

                parseResult?.let { res ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (res.validStudents.isNotEmpty()) Color(0xFFE7F6EE) else Color(0xFFFDECEA),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "${AppStrings.get("csv_valid_rows", lang)}: ${res.validStudents.size} / ${res.totalRowsProcessed}",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color(0xFF0A5C36),
                                modifier = Modifier.testTag("csv_valid_count_text")
                            )
                            res.validStudents.take(3).forEach { st ->
                                Text(
                                    text = "• ${st.nameEn} (Class ${st.className}-${st.section}, Roll ${st.rollNumber})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF155724)
                                )
                            }
                            if (res.hasErrors) {
                                Text(
                                    text = "${AppStrings.get("csv_error_rows", lang)}: ${res.errors.size}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFFC62828)
                                )
                                res.errors.take(2).forEach { err ->
                                    Text(
                                        text = "Row ${err.rowNumber}: ${err.reason}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFC62828)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            val validList = parseResult?.validStudents.orEmpty()
            Button(
                onClick = { onImportStudents(validList, selectedFileName) },
                enabled = validList.isNotEmpty(),
                modifier = Modifier.testTag("confirm_csv_import_button")
            ) {
                Text("${AppStrings.get("commit_csv_import", lang)} (${validList.size})")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(AppStrings.get("cancel", lang))
            }
        }
    )
}
