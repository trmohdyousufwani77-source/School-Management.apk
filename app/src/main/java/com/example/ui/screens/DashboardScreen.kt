package com.example.ui.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Badge
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.UserRole
import com.example.ui.components.AttendanceCircularProgress
import com.example.ui.components.DailyAttendanceRegisterCard
import com.example.ui.components.TextbookDistributionChart
import com.example.ui.components.WeeklyAttendanceTrendChart
import com.example.ui.localization.AppLanguage
import com.example.ui.localization.AppStrings
import com.example.ui.localization.LocalAppLanguage
import com.example.ui.viewmodel.SchoolViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    viewModel: SchoolViewModel,
    onNavigateTo: (String) -> Unit
) {
    val lang = LocalAppLanguage.current
    val schoolProfile by viewModel.schoolProfile.collectAsState()
    val userRole by viewModel.currentUserRole.collectAsState()
    val userName by viewModel.currentUserName.collectAsState()
    val allStudents by viewModel.allActiveStudents.collectAsState()
    val todayAttendance by viewModel.todayAttendance.collectAsState()
    val allAttendanceRecords by viewModel.allAttendanceRecords.collectAsState()
    val allLeaves by viewModel.allLeaves.collectAsState()
    val allTextbooks by viewModel.allTextbooks.collectAsState()
    val pendingSyncCount by viewModel.pendingSyncCount.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val canUndoAttendance by viewModel.canUndoAttendance.collectAsState()

    val pendingLeavesCount = allLeaves.count { it.status == "PENDING" }
    val totalEnrolled = allStudents.size
    val presentCount = todayAttendance.count { it.status == "PRESENT" }
    val absentCount = todayAttendance.count { it.status == "ABSENT" }
    val lateCount = todayAttendance.count { it.status == "LATE" }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }

        // School Header Banner
        item {
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("school_header_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.School,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = if (lang == AppLanguage.URDU) schoolProfile?.schoolNameUr ?: "گورنمنٹ ماڈل اسکول" else schoolProfile?.schoolNameEn ?: "Govt. Model Middle School",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "${AppStrings.get("udise_label", lang)}: ${schoolProfile?.udiseCode ?: "01010408201"} | ${AppStrings.get("session_label", lang)}: ${schoolProfile?.currentSession ?: "2026-2027"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Help / Coach overlay trigger
                        IconButton(
                            onClick = { viewModel.triggerCoachTour() },
                            modifier = Modifier.testTag("dashboard_coach_help_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.HelpOutline,
                                contentDescription = "Guided Tour",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Role & Sync Status Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            tonalElevation = 1.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .clickable {
                                        val nextRole = if (userRole == UserRole.HEADMASTER) UserRole.TEACHER else UserRole.HEADMASTER
                                        viewModel.switchRole(nextRole)
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (lang == AppLanguage.URDU) {
                                        if (userRole == UserRole.HEADMASTER) AppStrings.get("role_headmaster", lang) else AppStrings.get("role_teacher", lang)
                                    } else {
                                        userName
                                    },
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.SwapHoriz,
                                    contentDescription = AppStrings.get("switch_role", lang),
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        // Force Sync Chip
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (pendingSyncCount > 0) Color(0xFFFFF3CD) else Color(0xFFD4EDDA),
                            modifier = Modifier.clickable { viewModel.triggerForceSync() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudSync,
                                    contentDescription = null,
                                    tint = if (pendingSyncCount > 0) Color(0xFF856404) else Color(0xFF155724),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (pendingSyncCount > 0) "$pendingSyncCount ${AppStrings.get("pending_sync_suffix", lang)}" else AppStrings.get("offline_synced", lang),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (pendingSyncCount > 0) Color(0xFF856404) else Color(0xFF155724)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Attendance Gauge Card
        item {
            AttendanceCircularProgress(
                totalEnrolled = totalEnrolled,
                presentCount = presentCount,
                absentCount = absentCount,
                lateCount = lateCount
            )
        }

        // Weekly Attendance Trend Line Chart (7-day Present vs Absenteeism pattern)
        item {
            WeeklyAttendanceTrendChart(
                allStudents = allStudents,
                attendanceRecords = allAttendanceRecords,
                referenceDate = selectedDate
            )
        }

        // Daily Attendance Registers Manager (Primary 1-5 & Middle 6-8)
        item {
            DailyAttendanceRegisterCard(
                allStudents = allStudents,
                attendanceRecords = todayAttendance,
                selectedDate = selectedDate,
                canUndo = canUndoAttendance,
                onShiftDate = { delta -> viewModel.shiftSelectedDate(delta) },
                onSelectDate = { date -> viewModel.setSelectedDate(date) },
                onResetToToday = { viewModel.resetSelectedDateToToday() },
                onMarkStudentStatus = { studentId, status ->
                    viewModel.markStudentAttendance(studentId, status)
                },
                onMarkAllPresentForClass = { cls, sec ->
                    viewModel.markAllPresentForSpecificClass(cls, sec)
                },
                onUndoLastBulk = { viewModel.undoLastAttendance() },
                onOpenFullRegister = { cls, sec ->
                    viewModel.setSelectedClass(cls)
                    viewModel.setSelectedSection(sec)
                    onNavigateTo("attendance")
                },
                onSendWhatsAppAlert = { selectedStudents, message ->
                    viewModel.sendAttendanceAlertViaWhatsApp(selectedStudents, message)
                }
            )
        }

        // Functional Modules Navigation Grid
        item {
            Text(
                text = AppStrings.get("core_modules", lang),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(8.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                maxItemsInEachRow = 2
            ) {
                ModuleCard(
                    title = AppStrings.get("attendance", lang),
                    subtitle = AppStrings.get("sub_attendance", lang),
                    icon = Icons.Default.Today,
                    color = Color(0xFF1E88E5),
                    badgeCount = 0,
                    tagKey = "attendance",
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateTo("attendance") }
                )
                ModuleCard(
                    title = AppStrings.get("students", lang),
                    subtitle = AppStrings.get("sub_students", lang),
                    icon = Icons.Default.Person,
                    color = Color(0xFF43A047),
                    badgeCount = 0,
                    tagKey = "students",
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateTo("students") }
                )
                ModuleCard(
                    title = AppStrings.get("leaves", lang),
                    subtitle = AppStrings.get("sub_leaves", lang),
                    icon = Icons.Default.Assignment,
                    color = Color(0xFFE53935),
                    badgeCount = if (userRole == UserRole.HEADMASTER) pendingLeavesCount else 0,
                    tagKey = "leaves",
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateTo("leaves") }
                )
                ModuleCard(
                    title = AppStrings.get("textbooks", lang),
                    subtitle = AppStrings.get("sub_textbooks", lang),
                    icon = Icons.Default.MenuBook,
                    color = Color(0xFF8E24AA),
                    badgeCount = 0,
                    tagKey = "textbooks",
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateTo("textbooks") }
                )
                ModuleCard(
                    title = AppStrings.get("exams_reports", lang),
                    subtitle = AppStrings.get("sub_exams", lang),
                    icon = Icons.Default.Book,
                    color = Color(0xFFFB8C00),
                    badgeCount = 0,
                    tagKey = "exams",
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateTo("exams") }
                )
                ModuleCard(
                    title = AppStrings.get("db_health", lang),
                    subtitle = AppStrings.get("sub_db_health", lang),
                    icon = Icons.Default.Storage,
                    color = Color(0xFF00ACC1),
                    badgeCount = 0,
                    tagKey = "db_health",
                    modifier = Modifier.weight(1f),
                    onClick = { onNavigateTo("db_health") }
                )
            }
        }

        // Textbook distribution overview widget
        item {
            TextbookDistributionChart(allocations = allTextbooks)
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
private fun ModuleCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    badgeCount: Int,
    tagKey: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag("module_card_$tagKey"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(color.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(22.dp)
                    )
                }

                if (badgeCount > 0) {
                    Badge(containerColor = MaterialTheme.colorScheme.error) {
                        Text("$badgeCount")
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}
