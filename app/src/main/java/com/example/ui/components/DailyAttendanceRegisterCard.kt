package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.AttendanceEntity
import com.example.data.local.entities.StudentEntity
import com.example.domain.model.AttendanceStatus
import com.example.domain.service.AttendanceAlertType
import com.example.ui.localization.AppLanguage
import com.example.ui.localization.AppStrings
import com.example.ui.localization.LocalAppLanguage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

enum class SchoolDivisionFilter {
    ALL,
    PRIMARY, // Classes 1-5
    MIDDLE   // Classes 6-8
}

private data class ClassRegisterSummary(
    val className: Int,
    val section: String,
    val isPrimary: Boolean,
    val students: List<StudentEntity>,
    val presentCount: Int,
    val absentCount: Int,
    val lateCount: Int,
    val leaveCount: Int,
    val unmarkedCount: Int
) {
    val totalStudents: Int get() = students.size
    val markedCount: Int get() = presentCount + absentCount + lateCount + leaveCount
    val attendanceRate: Int
        get() = if (totalStudents > 0) (((presentCount + lateCount) * 100f) / totalStudents).toInt() else 0
    val isFullyMarked: Boolean get() = totalStudents > 0 && unmarkedCount == 0
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun DailyAttendanceRegisterCard(
    allStudents: List<StudentEntity>,
    attendanceRecords: List<AttendanceEntity>,
    selectedDate: String,
    canUndo: Boolean,
    onShiftDate: (Int) -> Unit,
    onSelectDate: (String) -> Unit,
    onResetToToday: () -> Unit,
    onMarkStudentStatus: (studentId: String, status: AttendanceStatus) -> Unit,
    onMarkAllPresentForClass: (className: Int, section: String) -> Unit,
    onUndoLastBulk: () -> Unit,
    onOpenFullRegister: (className: Int, section: String) -> Unit,
    onSendWhatsAppAlert: (selectedStudents: List<StudentEntity>, message: String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val lang = LocalAppLanguage.current
    var divisionFilter by remember { mutableStateOf(SchoolDivisionFilter.ALL) }
    var expandedClassKey by remember { mutableStateOf<String?>(null) }
    var showDatePickerDialog by remember { mutableStateOf(false) }
    var selectedStudentUuids by remember { mutableStateOf(setOf<String>()) }
    var alertTargetStudents by remember { mutableStateOf<List<StudentEntity>?>(null) }

    val todayStr = remember {
        SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }
    val isToday = selectedDate == todayStr
    val displayDateFormat = remember(lang) {
        SimpleDateFormat("dd MMM", if (lang == AppLanguage.URDU) Locale("ur") else Locale.US)
    }
    val selectedDisplayDate = remember(selectedDate, isToday, lang) {
        if (isToday) {
            AppStrings.get("today_label", lang)
        } else {
            try {
                val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(selectedDate)
                if (parsed != null) displayDateFormat.format(parsed) else selectedDate.takeLast(5)
            } catch (e: Exception) {
                selectedDate.takeLast(5)
            }
        }
    }

    val attendanceByStudent = remember(attendanceRecords) {
        attendanceRecords.associateBy { it.studentId }
    }

    // Build register summaries for Classes 1–8 across sections (defaulting empty classes to Section A)
    val classSummaries = remember(allStudents, attendanceByStudent, divisionFilter) {
        val classRange = when (divisionFilter) {
            SchoolDivisionFilter.ALL -> 1..8
            SchoolDivisionFilter.PRIMARY -> 1..5
            SchoolDivisionFilter.MIDDLE -> 6..8
        }

        val summaries = mutableListOf<ClassRegisterSummary>()
        for (cls in classRange) {
            val studentsInClass = allStudents.filter { it.className == cls }
            val sections = if (studentsInClass.isEmpty()) {
                listOf("A")
            } else {
                studentsInClass.map { it.section }.distinct().sorted()
            }

            sections.forEach { sec ->
                val secStudents = studentsInClass
                    .filter { it.section == sec }
                    .sortedBy { it.rollNumber }

                var present = 0
                var absent = 0
                var late = 0
                var leave = 0
                var unmarked = 0

                secStudents.forEach { student ->
                    when (attendanceByStudent[student.uuid]?.status) {
                        "PRESENT" -> present++
                        "ABSENT" -> absent++
                        "LATE" -> late++
                        "LEAVE" -> leave++
                        else -> unmarked++
                    }
                }

                summaries.add(
                    ClassRegisterSummary(
                        className = cls,
                        section = sec,
                        isPrimary = cls <= 5,
                        students = secStudents,
                        presentCount = present,
                        absentCount = absent,
                        lateCount = late,
                        leaveCount = leave,
                        unmarkedCount = unmarked
                    )
                )
            }
        }
        summaries
    }

    // Aggregate metrics for the currently selected division
    val divisionTotalStudents = classSummaries.sumOf { it.totalStudents }
    val divisionPresent = classSummaries.sumOf { it.presentCount }
    val divisionAbsent = classSummaries.sumOf { it.absentCount }
    val divisionLate = classSummaries.sumOf { it.lateCount }
    val activeClassesCount = classSummaries.count { it.totalStudents > 0 }
    val completedClassesCount = classSummaries.count { it.isFullyMarked }

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("daily_attendance_register_dashboard_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header & Date Navigator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                MaterialTheme.colorScheme.primaryContainer,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.FactCheck,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = AppStrings.get("daily_class_registers", lang),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "$completedClassesCount / $activeClassesCount ${AppStrings.get("active_registers_marked", lang)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Compact Date Switcher + Material 3 DatePicker Trigger
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.testTag("register_date_switcher")
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { onShiftDate(-1) },
                            modifier = Modifier
                                .size(36.dp)
                                .minimumInteractiveComponentSize()
                                .testTag("register_prev_date_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                contentDescription = "Previous Day",
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Row(
                            modifier = Modifier
                                .clickable { showDatePickerDialog = true }
                                .padding(horizontal = 6.dp, vertical = 8.dp)
                                .testTag("dashboard_date_picker_trigger"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = AppStrings.get("select_date", lang),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = selectedDisplayDate,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        IconButton(
                            onClick = { onShiftDate(1) },
                            modifier = Modifier
                                .size(36.dp)
                                .minimumInteractiveComponentSize()
                                .testTag("register_next_date_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = "Next Day",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Primary / Middle / All Segmented Filter Chips
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                FilterChip(
                    selected = divisionFilter == SchoolDivisionFilter.ALL,
                    onClick = { divisionFilter = SchoolDivisionFilter.ALL },
                    label = { Text(AppStrings.get("division_all", lang), fontSize = 12.sp) },
                    modifier = Modifier.testTag("filter_division_all")
                )
                FilterChip(
                    selected = divisionFilter == SchoolDivisionFilter.PRIMARY,
                    onClick = { divisionFilter = SchoolDivisionFilter.PRIMARY },
                    label = { Text(AppStrings.get("division_primary", lang), fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFD8F3DC),
                        selectedLabelColor = Color(0xFF08331A)
                    ),
                    modifier = Modifier.testTag("filter_division_primary")
                )
                FilterChip(
                    selected = divisionFilter == SchoolDivisionFilter.MIDDLE,
                    onClick = { divisionFilter = SchoolDivisionFilter.MIDDLE },
                    label = { Text(AppStrings.get("division_middle", lang), fontSize = 12.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    modifier = Modifier.testTag("filter_division_middle")
                )
            }

            // Division Summary KPI Strip
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DivisionMiniStat(
                        label = AppStrings.get("total_enrolled", lang),
                        value = divisionTotalStudents.toString(),
                        color = MaterialTheme.colorScheme.primary
                    )
                    DivisionMiniStat(
                        label = AppStrings.get("present_count", lang),
                        value = divisionPresent.toString(),
                        color = Color(0xFF1E824C)
                    )
                    DivisionMiniStat(
                        label = AppStrings.get("absent_count", lang),
                        value = divisionAbsent.toString(),
                        color = Color(0xFFD32F2F)
                    )
                    DivisionMiniStat(
                        label = AppStrings.get("late_short", lang),
                        value = divisionLate.toString(),
                        color = Color(0xFFD35400)
                    )
                }
            }

            // Undo Bulk Action Bar (when available)
            AnimatedVisibility(visible = canUndo) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFFFF3CD),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("dashboard_undo_attendance_bar")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = AppStrings.get("bulk_updated", lang),
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF856404)
                        )
                        TextButton(
                            onClick = onUndoLastBulk,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                            modifier = Modifier.testTag("dashboard_undo_attendance_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Undo,
                                contentDescription = "Undo",
                                tint = Color(0xFF856404),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = AppStrings.get("undo_last", lang),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF856404)
                            )
                        }
                    }
                }
            }

            // Class Register Items List
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                classSummaries.forEach { summary ->
                    val classKey = "${summary.className}-${summary.section}"
                    val isExpanded = expandedClassKey == classKey

                    ClassRegisterRowCard(
                        summary = summary,
                        isExpanded = isExpanded,
                        attendanceByStudent = attendanceByStudent,
                        selectedStudentUuids = selectedStudentUuids,
                        lang = lang,
                        onToggleExpand = {
                            if (summary.totalStudents > 0) {
                                expandedClassKey = if (isExpanded) null else classKey
                            } else {
                                onOpenFullRegister(summary.className, summary.section)
                            }
                        },
                        onMarkAllPresent = {
                            onMarkAllPresentForClass(summary.className, summary.section)
                        },
                        onMarkStudentStatus = onMarkStudentStatus,
                        onToggleStudentSelection = { uuid, checked ->
                            selectedStudentUuids = if (checked) {
                                selectedStudentUuids + uuid
                            } else {
                                selectedStudentUuids - uuid
                            }
                        },
                        onToggleSelectAllInClass = { uuids, selectAll ->
                            selectedStudentUuids = if (selectAll) {
                                selectedStudentUuids + uuids
                            } else {
                                selectedStudentUuids - uuids.toSet()
                            }
                        },
                        onSelectAbsentLateInClass = { uuids ->
                            selectedStudentUuids = selectedStudentUuids + uuids
                        },
                        onClearClassSelection = { uuids ->
                            selectedStudentUuids = selectedStudentUuids - uuids.toSet()
                        },
                        onTriggerWhatsAppAlert = { targetStudents ->
                            alertTargetStudents = targetStudents
                        },
                        onOpenFullRegister = {
                            onOpenFullRegister(summary.className, summary.section)
                        }
                    )
                }
            }
        }
    }

    alertTargetStudents?.let { targets ->
        if (targets.isNotEmpty()) {
            val statuses = targets.mapNotNull { attendanceByStudent[it.uuid]?.status }
            val defaultType = when {
                statuses.any { it == "ABSENT" } -> AttendanceAlertType.ABSENCE
                statuses.any { it == "LATE" } -> AttendanceAlertType.LATE
                else -> AttendanceAlertType.PTM_NOTICE
            }
            AttendanceWhatsAppAlertDialog(
                selectedStudents = targets,
                selectedDate = selectedDate,
                initialAlertType = defaultType,
                onDismiss = { alertTargetStudents = null },
                onSendAlert = { _, message ->
                    onSendWhatsAppAlert(targets, message)
                    selectedStudentUuids = selectedStudentUuids - targets.map { it.uuid }.toSet()
                    alertTargetStudents = null
                }
            )
        }
    }

    if (showDatePickerDialog) {
        val utcFormat = remember {
            SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
        }
        val initialMillis = remember(selectedDate) {
            try {
                utcFormat.parse(selectedDate)?.time ?: System.currentTimeMillis()
            } catch (e: Exception) {
                System.currentTimeMillis()
            }
        }
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

        DatePickerDialog(
            onDismissRequest = { showDatePickerDialog = false },
            modifier = Modifier.testTag("dashboard_date_picker_dialog"),
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            onSelectDate(utcFormat.format(Date(millis)))
                        }
                        showDatePickerDialog = false
                    },
                    modifier = Modifier.testTag("date_picker_confirm_btn")
                ) {
                    Text(AppStrings.get("confirm", lang))
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (!isToday) {
                        TextButton(
                            onClick = {
                                onResetToToday()
                                showDatePickerDialog = false
                            },
                            modifier = Modifier.testTag("date_picker_today_btn")
                        ) {
                            Text(AppStrings.get("today_label", lang))
                        }
                    }
                    TextButton(
                        onClick = { showDatePickerDialog = false },
                        modifier = Modifier.testTag("date_picker_cancel_btn")
                    ) {
                        Text(AppStrings.get("cancel", lang))
                    }
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                title = {
                    Text(
                        text = AppStrings.get("select_date", lang),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(start = 24.dp, top = 16.dp)
                    )
                }
            )
        }
    }
}

@Composable
private fun DivisionMiniStat(
    label: String,
    value: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = color
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

@Composable
private fun ClassRegisterRowCard(
    summary: ClassRegisterSummary,
    isExpanded: Boolean,
    attendanceByStudent: Map<String, AttendanceEntity>,
    selectedStudentUuids: Set<String>,
    lang: AppLanguage,
    onToggleExpand: () -> Unit,
    onMarkAllPresent: () -> Unit,
    onMarkStudentStatus: (studentId: String, status: AttendanceStatus) -> Unit,
    onToggleStudentSelection: (studentUuid: String, checked: Boolean) -> Unit,
    onToggleSelectAllInClass: (studentUuids: List<String>, selectAll: Boolean) -> Unit,
    onSelectAbsentLateInClass: (studentUuids: List<String>) -> Unit,
    onClearClassSelection: (studentUuids: List<String>) -> Unit,
    onTriggerWhatsAppAlert: (selectedStudents: List<StudentEntity>) -> Unit,
    onOpenFullRegister: () -> Unit
) {
    val divisionColor = if (summary.isPrimary) Color(0xFF1E824C) else Color(0xFF1B365D)
    val divisionLabel = if (summary.isPrimary) {
        AppStrings.get("primary_badge", lang)
    } else {
        AppStrings.get("middle_badge", lang)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (isExpanded) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                shape = RoundedCornerShape(14.dp)
            )
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onToggleExpand)
            .testTag("class_register_card_${summary.className}_${summary.section}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (summary.totalStudents == 0) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Class Badge + Title + Division Tag
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(divisionColor.copy(alpha = 0.14f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${summary.className}${summary.section}",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            color = divisionColor
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "${AppStrings.get("class_prefix", lang)} ${summary.className}-${summary.section}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = divisionColor.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = divisionLabel,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = divisionColor,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        if (summary.totalStudents == 0) {
                            Text(
                                text = AppStrings.get("no_students_class", lang),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                text = "${summary.presentCount}P · ${summary.absentCount}A · ${summary.lateCount}L · ${summary.leaveCount}LV  (${summary.totalStudents} ${AppStrings.get("total_short", lang)})",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Right: Attendance % / Action Controls
                if (summary.totalStudents > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        // Mark All Present Quick Button
                        val allPresent = summary.presentCount == summary.totalStudents
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (allPresent) Color(0xFFD4EDDA) else MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier
                                .heightIn(min = 36.dp)
                                .clickable(enabled = !allPresent) { onMarkAllPresent() }
                                .testTag("quick_mark_all_${summary.className}_${summary.section}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (allPresent) Icons.Default.CheckCircle else Icons.Default.DoneAll,
                                    contentDescription = AppStrings.get("mark_all", lang),
                                    tint = if (allPresent) Color(0xFF155724) else MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (allPresent) "${summary.attendanceRate}%" else AppStrings.get("mark_all", lang),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (allPresent) Color(0xFF155724) else MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isExpanded) "Collapse register" else "Expand register",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Open class register",
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Stacked distribution bar for active classes
            if (summary.totalStudents > 0) {
                Spacer(modifier = Modifier.height(8.dp))
                ClassAttendanceStackedBar(summary = summary)
            }

            // Inline Expandable Student Quick-Toggle Register with Bulk Selection & WhatsApp Alert
            AnimatedVisibility(
                visible = isExpanded && summary.totalStudents > 0,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                val classUuids = summary.students.map { it.uuid }
                val selectedInClass = summary.students.filter { selectedStudentUuids.contains(it.uuid) }
                val allInClassSelected = summary.students.isNotEmpty() && selectedInClass.size == summary.students.size
                val absentOrLateUuids = summary.students
                    .filter {
                        val st = attendanceByStudent[it.uuid]?.status
                        st == "ABSENT" || st == "LATE"
                    }
                    .map { it.uuid }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Bulk Selection Controls Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    onToggleSelectAllInClass(classUuids, !allInClassSelected)
                                }
                                .padding(end = 6.dp)
                        ) {
                            Checkbox(
                                checked = allInClassSelected,
                                onCheckedChange = { checked ->
                                    onToggleSelectAllInClass(classUuids, checked)
                                },
                                modifier = Modifier.testTag("dashboard_select_all_checkbox_${summary.className}_${summary.section}")
                            )
                            Text(
                                text = AppStrings.get("select_all", lang),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        if (absentOrLateUuids.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFFDECEA),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onSelectAbsentLateInClass(absentOrLateUuids) }
                                    .testTag("dashboard_select_absent_late_${summary.className}_${summary.section}")
                            ) {
                                Text(
                                    text = "${AppStrings.get("select_absent_late", lang)} (${absentOrLateUuids.size})",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFFC62828),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                )
                            }
                        }
                    }

                    // Bulk WhatsApp Alert Action Bar
                    AnimatedVisibility(visible = selectedInClass.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFE7F6EE),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("dashboard_bulk_whatsapp_bar")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${selectedInClass.size} ${AppStrings.get("selected_count_suffix", lang)}",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF0A5C36)
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    TextButton(
                                        onClick = { onClearClassSelection(classUuids) },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.testTag("dashboard_clear_selection_btn")
                                    ) {
                                        Text(
                                            text = AppStrings.get("clear_selection", lang),
                                            fontSize = 11.sp,
                                            color = Color(0xFF0A5C36)
                                        )
                                    }
                                    Button(
                                        onClick = { onTriggerWhatsAppAlert(selectedInClass) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF128C7E),
                                            contentColor = Color.White
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                        modifier = Modifier.testTag("dashboard_send_whatsapp_alert_btn")
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Send,
                                            contentDescription = AppStrings.get("whatsapp_alert_btn", lang),
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = AppStrings.get("whatsapp_alert_btn", lang),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    summary.students.forEach { student ->
                        val status = attendanceByStudent[student.uuid]?.status ?: "UNMARKED"
                        val isSelected = selectedStudentUuids.contains(student.uuid)
                        InlineStudentAttendanceRow(
                            student = student,
                            currentStatus = status,
                            isSelected = isSelected,
                            onSelectionChange = { checked ->
                                onToggleStudentSelection(student.uuid, checked)
                            },
                            onStatusChange = { newStatus ->
                                onMarkStudentStatus(student.uuid, newStatus)
                            }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        OutlinedButton(
                            onClick = onOpenFullRegister,
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("open_full_register_${summary.className}_${summary.section}")
                        ) {
                            Text(
                                text = AppStrings.get("open_register", lang),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ClassAttendanceStackedBar(summary: ClassRegisterSummary) {
    val total = summary.totalStudents.toFloat().coerceAtLeast(1f)
    val presentF = summary.presentCount / total
    val lateF = summary.lateCount / total
    val leaveF = summary.leaveCount / total
    val absentF = summary.absentCount / total
    val unmarkedF = summary.unmarkedCount / total

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(Color(0xFFECEFF4))
    ) {
        if (presentF > 0f) {
            Box(modifier = Modifier.weight(presentF).height(6.dp).background(Color(0xFF27AE60)))
        }
        if (lateF > 0f) {
            Box(modifier = Modifier.weight(lateF).height(6.dp).background(Color(0xFFF39C12)))
        }
        if (leaveF > 0f) {
            Box(modifier = Modifier.weight(leaveF).height(6.dp).background(Color(0xFF3498DB)))
        }
        if (absentF > 0f) {
            Box(modifier = Modifier.weight(absentF).height(6.dp).background(Color(0xFFE74C3C)))
        }
        if (unmarkedF > 0f) {
            Box(modifier = Modifier.weight(unmarkedF).height(6.dp).background(Color(0xFFD5DCE6)))
        }
    }
}

@Composable
private fun InlineStudentAttendanceRow(
    student: StudentEntity,
    currentStatus: String,
    isSelected: Boolean,
    onSelectionChange: (Boolean) -> Unit,
    onStatusChange: (AttendanceStatus) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isSelected) Color(0xFFE7F6EE) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                RoundedCornerShape(10.dp)
            )
            .padding(start = 2.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = onSelectionChange,
                colors = CheckboxDefaults.colors(
                    checkedColor = Color(0xFF128C7E),
                    checkmarkColor = Color.White
                ),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("dashboard_student_checkbox_${student.rollNumber}")
            )
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${student.rollNumber}",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(
                    text = student.nameEn,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = student.nameUr,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            InlineStatusPill(
                code = "P",
                selected = currentStatus == "PRESENT",
                activeColor = Color(0xFF27AE60),
                onClick = { onStatusChange(AttendanceStatus.PRESENT) }
            )
            InlineStatusPill(
                code = "A",
                selected = currentStatus == "ABSENT",
                activeColor = Color(0xFFE74C3C),
                onClick = { onStatusChange(AttendanceStatus.ABSENT) }
            )
            InlineStatusPill(
                code = "L",
                selected = currentStatus == "LATE",
                activeColor = Color(0xFFF39C12),
                onClick = { onStatusChange(AttendanceStatus.LATE) }
            )
            InlineStatusPill(
                code = "LV",
                selected = currentStatus == "LEAVE",
                activeColor = Color(0xFF3498DB),
                onClick = { onStatusChange(AttendanceStatus.LEAVE) }
            )
        }
    }
}

@Composable
private fun InlineStatusPill(
    code: String,
    selected: Boolean,
    activeColor: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(if (selected) activeColor else activeColor.copy(alpha = 0.12f))
            .clickable(onClick = onClick)
            .testTag("inline_attendance_pill_$code"),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = code,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) Color.White else activeColor
        )
    }
}
