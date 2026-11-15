package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.QueryBuilder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.example.data.local.entities.AttendanceEntity
import com.example.data.local.entities.StudentEntity
import com.example.domain.model.AttendanceStatus
import com.example.domain.service.AttendanceAlertType
import com.example.ui.components.AttendanceHeatmap
import com.example.ui.components.AttendanceWhatsAppAlertDialog
import com.example.ui.components.QrScannerDialog
import com.example.ui.components.WeeklyAttendanceTrendChart
import com.example.ui.localization.AppLanguage
import com.example.ui.localization.AppStrings
import com.example.ui.localization.LocalAppLanguage
import com.example.ui.viewmodel.SchoolViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AttendanceScreen(
    viewModel: SchoolViewModel
) {
    val context = LocalContext.current
    val lang = LocalAppLanguage.current
    val selectedClass by viewModel.selectedClass.collectAsState()
    val selectedSection by viewModel.selectedSection.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val students by viewModel.classStudents.collectAsState()
    val allStudents by viewModel.allActiveStudents.collectAsState()
    val attendanceRecords by viewModel.todayAttendance.collectAsState()
    val allAttendanceRecords by viewModel.allAttendanceRecords.collectAsState()
    val canUndo by viewModel.canUndoAttendance.collectAsState()

    var expandedStudentUuid by remember { mutableStateOf<String?>(null) }
    var classMenuExpanded by remember { mutableStateOf(false) }
    var sectionMenuExpanded by remember { mutableStateOf(false) }
    var selectedStudentUuids by remember { mutableStateOf(setOf<String>()) }
    var showWhatsAppAlertDialog by remember { mutableStateOf(false) }
    var showQrScannerDialog by remember { mutableStateOf(false) }

    val attendanceMap = remember(attendanceRecords) {
        attendanceRecords.associateBy { it.studentId }
    }
    val selectedStudents = remember(students, selectedStudentUuids) {
        students.filter { selectedStudentUuids.contains(it.uuid) }
    }
    val allSelected = students.isNotEmpty() && selectedStudents.size == students.size
    val absentOrLateUuids = remember(students, attendanceMap) {
        students.filter {
            val st = attendanceMap[it.uuid]?.status
            st == "ABSENT" || st == "LATE"
        }.map { it.uuid }
    }

    val presentCount = students.count { attendanceMap[it.uuid]?.status == "PRESENT" }
    val absentCount = students.count { attendanceMap[it.uuid]?.status == "ABSENT" }
    val lateCount = students.count { attendanceMap[it.uuid]?.status == "LATE" }
    val leaveCount = students.count { attendanceMap[it.uuid]?.status == "LEAVE" }
    val totalCount = students.size
    val attendancePct = if (totalCount > 0) (presentCount.toDouble() / totalCount.toDouble()) * 100.0 else 0.0

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { Spacer(modifier = Modifier.height(4.dp)) }

        // Top Class & Section Selectors Row
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Class Selector (1 to 8)
                        ExposedDropdownMenuBox(
                            expanded = classMenuExpanded,
                            onExpandedChange = { classMenuExpanded = it },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = "Class $selectedClass",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Class") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = classMenuExpanded) },
                                modifier = Modifier
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                    .fillMaxWidth()
                                    .testTag("class_selector_dropdown")
                            )
                            ExposedDropdownMenu(
                                expanded = classMenuExpanded,
                                onDismissRequest = { classMenuExpanded = false }
                            ) {
                                (1..8).forEach { cls ->
                                    DropdownMenuItem(
                                        text = { Text("Class $cls (جماعت $cls)") },
                                        onClick = {
                                            viewModel.setSelectedClass(cls)
                                            classMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Section Selector (A, B, C)
                        ExposedDropdownMenuBox(
                            expanded = sectionMenuExpanded,
                            onExpandedChange = { sectionMenuExpanded = it },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = "Section $selectedSection",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Section") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sectionMenuExpanded) },
                                modifier = Modifier
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                    .fillMaxWidth()
                                    .testTag("section_selector_dropdown")
                            )
                            ExposedDropdownMenu(
                                expanded = sectionMenuExpanded,
                                onDismissRequest = { sectionMenuExpanded = false }
                            ) {
                                listOf("A", "B", "C").forEach { sec ->
                                    DropdownMenuItem(
                                        text = { Text("Section '$sec'") },
                                        onClick = {
                                            viewModel.setSelectedSection(sec)
                                            sectionMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Date & Metric strip
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = selectedDate,
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                            )
                        }

                        Text(
                            text = "Rate: ${String.format("%.1f", attendancePct)}% ($presentCount/$totalCount)",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (attendancePct >= 75.0) Color(0xFF27AE60) else Color(0xFFE67E22)
                        )
                    }
                }
            }
        }

        // Action Toolbar (Mark All Present, QR Scan, Undo, Export Register)
        item {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.markAllPresentForClass() },
                    modifier = Modifier.testTag("mark_all_present_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = AppStrings.get("mark_all_present", lang), fontSize = 12.sp)
                }

                Button(
                    onClick = { showQrScannerDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    ),
                    modifier = Modifier.testTag("attendance_qr_scan_btn"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.QrCodeScanner, contentDescription = AppStrings.get("qr_scan_btn", lang), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(text = AppStrings.get("qr_scan_btn", lang), fontSize = 12.sp)
                }

                if (canUndo) {
                    OutlinedButton(
                        onClick = { viewModel.undoLastAttendance() },
                        modifier = Modifier.testTag("undo_attendance_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.Undo, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Undo", fontSize = 12.sp)
                    }
                }

                OutlinedButton(
                    onClick = {
                        viewModel.exportAttendanceRegister("Class_${selectedClass}_${selectedSection}") { _ -> }
                    },
                    modifier = Modifier.testTag("export_attendance_excel_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "Excel", fontSize = 12.sp)
                }
            }
        }

        // Bulk Selection Controls & WhatsApp Alert Bar
        if (students.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    selectedStudentUuids = if (allSelected) {
                                        emptySet()
                                    } else {
                                        students.map { it.uuid }.toSet()
                                    }
                                }
                                .padding(end = 8.dp)
                        ) {
                            Checkbox(
                                checked = allSelected,
                                onCheckedChange = { checked ->
                                    selectedStudentUuids = if (checked) {
                                        students.map { it.uuid }.toSet()
                                    } else {
                                        emptySet()
                                    }
                                },
                                modifier = Modifier.testTag("attendance_select_all_checkbox")
                            )
                            Text(
                                text = AppStrings.get("select_all", lang),
                                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        if (absentOrLateUuids.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFFDECEA),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        selectedStudentUuids = selectedStudentUuids + absentOrLateUuids
                                    }
                                    .testTag("attendance_select_absent_late_btn")
                            ) {
                                Text(
                                    text = "${AppStrings.get("select_absent_late", lang)} (${absentOrLateUuids.size})",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFC62828),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    AnimatedVisibility(visible = selectedStudents.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFE7F6EE),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("attendance_bulk_whatsapp_bar")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${selectedStudents.size} ${AppStrings.get("selected_count_suffix", lang)}",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF0A5C36)
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    TextButton(
                                        onClick = { selectedStudentUuids = emptySet() },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.testTag("attendance_clear_selection_btn")
                                    ) {
                                        Text(
                                            text = AppStrings.get("clear_selection", lang),
                                            fontSize = 12.sp,
                                            color = Color(0xFF0A5C36)
                                        )
                                    }
                                    Button(
                                        onClick = { showWhatsAppAlertDialog = true },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF128C7E),
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        modifier = Modifier.testTag("attendance_send_whatsapp_alert_btn")
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Send,
                                            contentDescription = AppStrings.get("whatsapp_alert_btn", lang),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = AppStrings.get("whatsapp_alert_btn", lang),
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Student Dual-Register Roster
        if (students.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No students enrolled in Class $selectedClass-$selectedSection yet.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(students, key = { it.uuid }) { student ->
                val currentStatus = attendanceMap[student.uuid]?.status ?: "ABSENT"
                val isExpanded = expandedStudentUuid == student.uuid
                val isSelected = selectedStudentUuids.contains(student.uuid)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("student_attendance_row_${student.rollNumber}"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) Color(0xFFE7F6EE) else MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 4.dp, end = 10.dp, top = 10.dp, bottom = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { checked ->
                                    selectedStudentUuids = if (checked) {
                                        selectedStudentUuids + student.uuid
                                    } else {
                                        selectedStudentUuids - student.uuid
                                    }
                                },
                                colors = CheckboxDefaults.colors(
                                    checkedColor = Color(0xFF128C7E),
                                    checkmarkColor = Color.White
                                ),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("attendance_student_checkbox_${student.rollNumber}")
                            )

                            // Roll No & Name
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        expandedStudentUuid = if (isExpanded) null else student.uuid
                                    },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${student.rollNumber}",
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "${student.nameEn} (${student.nameUr})",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Adm: ${student.admissionNumber} | UID: ${student.maskedAadhaar}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Individual Toggle Pills (P, A, L, LV)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                StatusPill(
                                    label = "P",
                                    selected = currentStatus == "PRESENT",
                                    color = Color(0xFF27AE60),
                                    onClick = { viewModel.markStudentAttendance(student.uuid, AttendanceStatus.PRESENT) }
                                )
                                StatusPill(
                                    label = "A",
                                    selected = currentStatus == "ABSENT",
                                    color = Color(0xFFE74C3C),
                                    onClick = { viewModel.markStudentAttendance(student.uuid, AttendanceStatus.ABSENT) }
                                )
                                StatusPill(
                                    label = "L",
                                    selected = currentStatus == "LATE",
                                    color = Color(0xFFF39C12),
                                    onClick = { viewModel.markStudentAttendance(student.uuid, AttendanceStatus.LATE) }
                                )
                                StatusPill(
                                    label = "LV",
                                    selected = currentStatus == "LEAVE",
                                    color = Color(0xFF3498DB),
                                    onClick = { viewModel.markStudentAttendance(student.uuid, AttendanceStatus.LEAVE) }
                                )

                                IconButton(
                                    onClick = { expandedStudentUuid = if (isExpanded) null else student.uuid },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                        contentDescription = "Heatmap",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Expandable Calendar Attendance Heatmap
                        AnimatedVisibility(visible = isExpanded) {
                            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                AttendanceHeatmap(
                                    studentName = student.nameEn,
                                    attendanceList = attendanceRecords.filter { it.studentId == student.uuid }
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            WeeklyAttendanceTrendChart(
                allStudents = allStudents,
                attendanceRecords = allAttendanceRecords,
                referenceDate = selectedDate
            )
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }

    if (showWhatsAppAlertDialog && selectedStudents.isNotEmpty()) {
        val statuses = selectedStudents.mapNotNull { attendanceMap[it.uuid]?.status }
        val defaultType = when {
            statuses.any { it == "ABSENT" } -> AttendanceAlertType.ABSENCE
            statuses.any { it == "LATE" } -> AttendanceAlertType.LATE
            else -> AttendanceAlertType.PTM_NOTICE
        }
        AttendanceWhatsAppAlertDialog(
            selectedStudents = selectedStudents,
            selectedDate = selectedDate,
            initialAlertType = defaultType,
            onDismiss = { showWhatsAppAlertDialog = false },
            onSendAlert = { _, message ->
                viewModel.sendAttendanceAlertViaWhatsApp(selectedStudents, message)
                showWhatsAppAlertDialog = false
                selectedStudentUuids = emptySet()
            }
        )
    }

    if (showQrScannerDialog) {
        QrScannerDialog(
            title = AppStrings.get("qr_scan_attendance_title", lang),
            students = allStudents,
            onDismiss = { showQrScannerDialog = false },
            onQrScanned = { rawCode ->
                val matched = viewModel.markAttendanceByQrScan(rawCode)
                if (matched != null) {
                    showQrScannerDialog = false
                }
            }
        )
    }
}

@Composable
private fun StatusPill(
    label: String,
    selected: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(if (selected) color else color.copy(alpha = 0.12f))
            .clickable(onClick = onClick)
            .testTag("attendance_pill_$label"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) Color.White else color
        )
    }
}
