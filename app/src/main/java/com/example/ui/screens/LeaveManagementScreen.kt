package com.example.ui.screens

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import com.example.data.local.entities.LeaveApplicationEntity
import com.example.data.local.entities.StudentEntity
import com.example.domain.model.LeaveApplicantRole
import com.example.domain.model.LeaveStatus
import com.example.domain.model.UserRole
import com.example.domain.service.SchoolNotificationService
import com.example.ui.components.BiometricLockDialog
import com.example.ui.localization.AppLanguage
import com.example.ui.localization.AppStrings
import com.example.ui.localization.LocalAppLanguage
import com.example.ui.viewmodel.SchoolViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@Composable
fun LeaveManagementScreen(
    viewModel: SchoolViewModel
) {
    val context = LocalContext.current
    val lang = LocalAppLanguage.current
    val userRole by viewModel.currentUserRole.collectAsState()
    val allLeaves by viewModel.allLeaves.collectAsState()
    val allStudents by viewModel.allActiveStudents.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) } // 0 = Student & Parent Digital Leaves, 1 = Staff Quotas & Leaves
    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, PENDING, APPROVED, REJECTED
    var searchQuery by remember { mutableStateOf("") }
    var showApplyModal by remember { mutableStateOf(false) }

    var notificationsGranted by remember {
        mutableStateOf(SchoolNotificationService.hasNotificationPermission(context))
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        notificationsGranted = granted
        if (granted) {
            viewModel.checkAndDispatchDeadlineNotifications()
        } else {
            Toast.makeText(context, "Push notifications permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    // For Headmaster biometric approval on Teacher leaves
    var pendingAuthTeacherLeaveId by remember { mutableStateOf<Long?>(null) }
    // For Teacher/HM review remarks dialog on Student/Parent leaves
    var pendingStudentReview by remember { mutableStateOf<Pair<LeaveApplicationEntity, LeaveStatus>?>(null) }

    val studentParentLeaves = remember(allLeaves) {
        allLeaves.filter { it.applicantRole == "STUDENT" || it.applicantRole == "PARENT" }
    }
    val staffLeaves = remember(allLeaves) {
        allLeaves.filter { it.applicantRole == "TEACHER" }
    }

    val activeTabLeaves = if (selectedTab == 0) studentParentLeaves else staffLeaves
    val filteredLeaves = remember(activeTabLeaves, selectedFilter, searchQuery) {
        val q = searchQuery.trim().lowercase(Locale.US)
        activeTabLeaves.filter { item ->
            val matchesStatus = selectedFilter == "ALL" || item.status == selectedFilter
            val matchesQuery = q.isEmpty() ||
                item.applicantName.lowercase(Locale.US).contains(q) ||
                item.guardianName.lowercase(Locale.US).contains(q) ||
                item.applicantId.lowercase(Locale.US).contains(q) ||
                item.reason.lowercase(Locale.US).contains(q)
            matchesStatus && matchesQuery
        }
    }

    val pendingCount = studentParentLeaves.count { it.status == "PENDING" }
    val approvedCount = studentParentLeaves.count { it.status == "APPROVED" }
    val rejectedCount = studentParentLeaves.count { it.status == "REJECTED" }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // Tab Switcher
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
                                    "${AppStrings.get("lv_student_parent_tab", lang)} (${studentParentLeaves.size})",
                                    fontSize = 12.sp
                                )
                            },
                            modifier = Modifier.testTag("tab_student_parent_leaves")
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = {
                                Text(
                                    "${AppStrings.get("lv_staff_tab", lang)} (${staffLeaves.size})",
                                    fontSize = 12.sp
                                )
                            },
                            modifier = Modifier.testTag("tab_staff_leaves")
                        )
                    }
                }
            }

            // Status Tracking Summary Strip (Student/Parent Tab)
            if (selectedTab == 0) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        LeaveStatusKpiCard(
                            label = AppStrings.get("lv_pending_badge", lang),
                            count = pendingCount,
                            bgColor = Color(0xFFFFF3CD),
                            fgColor = Color(0xFF856404),
                            modifier = Modifier.weight(1f).testTag("leave_kpi_pending")
                        )
                        LeaveStatusKpiCard(
                            label = AppStrings.get("lv_approved_badge", lang),
                            count = approvedCount,
                            bgColor = Color(0xFFD4EDDA),
                            fgColor = Color(0xFF155724),
                            modifier = Modifier.weight(1f).testTag("leave_kpi_approved")
                        )
                        LeaveStatusKpiCard(
                            label = AppStrings.get("lv_rejected_badge", lang),
                            count = rejectedCount,
                            bgColor = Color(0xFFF8D7DA),
                            fgColor = Color(0xFF721C24),
                            modifier = Modifier.weight(1f).testTag("leave_kpi_rejected")
                        )
                    }
                }
            }

            // Search & Filter Bar + Export
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search student, guardian, or reason...", fontSize = 13.sp) },
                        leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(imageVector = Icons.Default.Close, contentDescription = "Clear")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("leave_search_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("ALL", "PENDING", "APPROVED", "REJECTED").forEach { f ->
                                FilterChip(
                                    selected = selectedFilter == f,
                                    onClick = { selectedFilter = f },
                                    label = { Text(f) },
                                    modifier = Modifier.testTag("leave_filter_$f")
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        OutlinedButton(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !notificationsGranted) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    viewModel.checkAndDispatchDeadlineNotifications()
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("leave_enable_notifications_btn")
                        ) {
                            Icon(imageVector = Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (notificationsGranted) AppStrings.get("notif_alerts_enabled", lang) else AppStrings.get("notif_enable_alerts", lang),
                                fontSize = 11.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        OutlinedButton(
                            onClick = { viewModel.exportLeaveRegister {} },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("export_leave_register_btn")
                        ) {
                            Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Export", fontSize = 11.sp)
                        }
                    }
                }
            }

            // Leave Cards List
            if (filteredLeaves.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No leave requests found in this category.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(filteredLeaves, key = { it.id }) { leave ->
                    LeaveRequestCard(
                        leave = leave,
                        lang = lang,
                        userRole = userRole,
                        onApproveStudentLeave = {
                            pendingStudentReview = leave to LeaveStatus.APPROVED
                        },
                        onRejectStudentLeave = {
                            pendingStudentReview = leave to LeaveStatus.REJECTED
                        },
                        onApproveTeacherLeave = {
                            pendingAuthTeacherLeaveId = leave.id
                        },
                        onRejectTeacherLeave = {
                            viewModel.reviewLeave(leave.id, LeaveStatus.REJECTED, "Not permissible as per duty schedule")
                        }
                    )
                }
            }

            // Staff Quota Rules & Roster on Staff Tab
            if (selectedTab == 1) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Annual Quota Rules (J&K Govt. Education Dept)",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("• Casual Leave (CL): 12 days per calendar year with full pay.", style = MaterialTheme.typography.bodySmall)
                            Text("• Medical Leave (ML): 10 days on production of registered medical certificate.", style = MaterialTheme.typography.bodySmall)
                            Text("• Earned Leave (EL): 15 days accumulated quota.", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                items(
                    listOf(
                        Triple("Farooq Ahmad Lone (TGT)", "CL: 9/12 left | ML: 9/10 left", "Classes 5-A, 6-A"),
                        Triple("Nasreen Akhtar (PRT)", "CL: 11/12 left | ML: 7/10 left", "Classes 1-A, 3-A"),
                        Triple("Showkat Hussain Dar (TGT)", "CL: 8/12 left | ML: 8/10 left", "Classes 6-A, 8-A")
                    )
                ) { (name, quota, classes) ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                Text(text = classes, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(text = quota, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(84.dp)) }
        }

        // FAB to submit a digital leave request (Student / Parent / Staff)
        ExtendedFloatingActionButton(
            onClick = { showApplyModal = true },
            icon = { Icon(imageVector = Icons.Default.Add, contentDescription = "Submit Leave") },
            text = { Text(AppStrings.get("lv_submit_digital", lang)) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(18.dp)
                .testTag("apply_leave_fab")
        )
    }

    // Digital Leave Request Dialog
    if (showApplyModal) {
        SubmitDigitalLeaveDialog(
            students = allStudents,
            defaultRole = if (selectedTab == 0) LeaveApplicantRole.PARENT else LeaveApplicantRole.TEACHER,
            onDismiss = { showApplyModal = false },
            onSubmit = { leave ->
                viewModel.applyLeave(leave)
                selectedTab = if (leave.applicantRole == "TEACHER") 1 else 0
                selectedFilter = "ALL"
                showApplyModal = false
            }
        )
    }

    // Teacher / Headmaster Review Notes Dialog for Student & Parent Leaves
    if (pendingStudentReview != null) {
        val (leave, targetStatus) = pendingStudentReview!!
        LeaveReviewNoteDialog(
            leave = leave,
            targetStatus = targetStatus,
            onDismiss = { pendingStudentReview = null },
            onConfirm = { notes ->
                viewModel.reviewLeave(leave.id, targetStatus, notes)
                pendingStudentReview = null
            }
        )
    }

    // Biometric / PIN Lock for Headmaster Staff Leave approval
    if (pendingAuthTeacherLeaveId != null) {
        BiometricLockDialog(
            actionTitle = "Approve Staff Leave Request #$pendingAuthTeacherLeaveId",
            onSuccess = {
                val leaveId = pendingAuthTeacherLeaveId!!
                pendingAuthTeacherLeaveId = null
                viewModel.reviewLeave(leaveId, LeaveStatus.APPROVED, "Sanctioned as per entitlement")
            },
            onDismiss = { pendingAuthTeacherLeaveId = null }
        )
    }
}

@Composable
private fun LeaveStatusKpiCard(
    label: String,
    count: Int,
    bgColor: Color,
    fgColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = bgColor
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$count",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = fgColor
            )
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = fgColor
            )
        }
    }
}

@Composable
private fun LeaveRequestCard(
    leave: LeaveApplicationEntity,
    lang: AppLanguage,
    userRole: UserRole,
    onApproveStudentLeave: () -> Unit,
    onRejectStudentLeave: () -> Unit,
    onApproveTeacherLeave: () -> Unit,
    onRejectTeacherLeave: () -> Unit
) {
    val isStudentOrParent = leave.applicantRole == "STUDENT" || leave.applicantRole == "PARENT"

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("leave_card_${leave.id}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = leave.applicantName,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = when (leave.applicantRole) {
                                    "PARENT" -> AppStrings.get("lv_role_parent", lang)
                                    "STUDENT" -> AppStrings.get("lv_role_student", lang)
                                    else -> AppStrings.get("lv_role_teacher", lang)
                                },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    val subtitle = if (isStudentOrParent && leave.className > 0) {
                        "Class ${leave.className}-${leave.section} | Adm: ${leave.applicantId} | ${leave.leaveType}"
                    } else {
                        "ID: ${leave.applicantId} | Type: ${leave.leaveType}"
                    }
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )

                    if (isStudentOrParent && leave.guardianName.isNotBlank()) {
                        Text(
                            text = "Guardian: ${leave.guardianName}" +
                                if (leave.guardianPhone.isNotBlank()) " (${leave.guardianPhone})" else "",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = when (leave.status) {
                        "APPROVED" -> Color(0xFFD4EDDA)
                        "REJECTED" -> Color(0xFFF8D7DA)
                        else -> Color(0xFFFFF3CD)
                    },
                    modifier = Modifier.testTag("leave_status_badge_${leave.id}")
                ) {
                    Text(
                        text = leave.status,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (leave.status) {
                            "APPROVED" -> Color(0xFF155724)
                            "REJECTED" -> Color(0xFF721C24)
                            else -> Color(0xFF856404)
                        },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 3-step Digital Status Tracker
            LeaveStatusTrackerBar(status = leave.status, lang = lang)

            Spacer(modifier = Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${leave.startDate} → ${leave.endDate} (${leave.daysCount} ${AppStrings.get("lv_days_suffix", lang)})",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = leave.reason,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (leave.attachmentReceiptPath.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AttachFile,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Attachment: ${leave.attachmentReceiptPath}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Teacher & Headmaster Review Actions for Student/Parent Leaves
            if (isStudentOrParent && leave.status == "PENDING") {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onRejectStudentLeave,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("reject_leave_btn_${leave.id}")
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(AppStrings.get("lv_reject", lang), fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onApproveStudentLeave,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF27AE60)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("approve_leave_btn_${leave.id}")
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(AppStrings.get("lv_approve", lang), fontSize = 12.sp)
                    }
                }
            } else if (!isStudentOrParent && userRole == UserRole.HEADMASTER && leave.status == "PENDING") {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = onRejectTeacherLeave,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("reject_leave_btn_${leave.id}")
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(AppStrings.get("lv_reject", lang), fontSize = 12.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onApproveTeacherLeave,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF27AE60)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("approve_leave_btn_${leave.id}")
                    ) {
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Approve & Sanction", fontSize = 12.sp)
                    }
                }
            } else if (leave.reviewedBy.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Reviewed by ${leave.reviewedBy}: ${leave.reviewNotes}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun LeaveStatusTrackerBar(status: String, lang: AppLanguage) {
    val isDecisionMade = status == "APPROVED" || status == "REJECTED"
    val decisionColor = when (status) {
        "APPROVED" -> Color(0xFF27AE60)
        "REJECTED" -> Color(0xFFC62828)
        else -> Color(0xFFE67E22)
    }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TrackerStep(
                label = AppStrings.get("lv_status_submitted", lang),
                active = true,
                color = Color(0xFF27AE60)
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(2.dp)
                    .padding(horizontal = 4.dp)
                    .background(Color(0xFF27AE60))
            )
            TrackerStep(
                label = AppStrings.get("lv_status_review", lang),
                active = true,
                color = if (isDecisionMade) Color(0xFF27AE60) else Color(0xFFE67E22)
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(2.dp)
                    .padding(horizontal = 4.dp)
                    .background(if (isDecisionMade) decisionColor else Color.LightGray)
            )
            TrackerStep(
                label = when (status) {
                    "APPROVED" -> AppStrings.get("lv_approved_badge", lang)
                    "REJECTED" -> AppStrings.get("lv_rejected_badge", lang)
                    else -> AppStrings.get("lv_status_decision", lang)
                },
                active = isDecisionMade,
                color = if (isDecisionMade) decisionColor else Color.Gray
            )
        }
    }
}

@Composable
private fun TrackerStep(
    label: String,
    active: Boolean,
    color: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        Icon(
            imageVector = if (active) Icons.Default.CheckCircle else Icons.Default.HourglassTop,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(13.dp)
        )
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
            color = color
        )
    }
}

@Composable
private fun LeaveReviewNoteDialog(
    leave: LeaveApplicationEntity,
    targetStatus: LeaveStatus,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val lang = LocalAppLanguage.current
    val isApproving = targetStatus == LeaveStatus.APPROVED
    var notes by remember {
        mutableStateOf(
            if (isApproving) "Approved by class teacher." else "Please attend scheduled class / assessment."
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (isApproving) "${AppStrings.get("lv_approve", lang)}: ${leave.applicantName}"
                else "${AppStrings.get("lv_reject", lang)}: ${leave.applicantName}"
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "${leave.startDate} → ${leave.endDate} (${leave.daysCount} days)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(AppStrings.get("lv_review_notes", lang)) },
                    modifier = Modifier.fillMaxWidth().testTag("leave_review_notes_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(notes.trim()) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isApproving) Color(0xFF27AE60) else MaterialTheme.colorScheme.error
                ),
                modifier = Modifier.testTag("confirm_leave_review_btn")
            ) {
                Text(AppStrings.get("confirm", lang))
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text(AppStrings.get("cancel", lang))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun SubmitDigitalLeaveDialog(
    students: List<StudentEntity>,
    defaultRole: LeaveApplicantRole,
    onDismiss: () -> Unit,
    onSubmit: (LeaveApplicationEntity) -> Unit
) {
    val lang = LocalAppLanguage.current
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    val todayStr = remember { dateFormat.format(Date()) }

    var applicantRole by remember { mutableStateOf(defaultRole) }
    var selectedStudent by remember(students) { mutableStateOf(students.firstOrNull()) }
    var studentMenuExpanded by remember { mutableStateOf(false) }

    var guardianName by remember(selectedStudent) { mutableStateOf(selectedStudent?.fatherName ?: "") }
    var leaveType by remember { mutableStateOf("MEDICAL") }
    var startDate by remember { mutableStateOf(todayStr) }
    var endDate by remember { mutableStateOf(todayStr) }
    var reason by remember { mutableStateOf("") }
    var attachmentNote by remember { mutableStateOf("") }

    var activeDatePickerTarget by remember { mutableStateOf<String?>(null) } // "START" or "END"

    val computedDays = remember(startDate, endDate) {
        try {
            val s = dateFormat.parse(startDate)
            val e = dateFormat.parse(endDate)
            if (s != null && e != null) {
                val diff = TimeUnit.MILLISECONDS.toDays(e.time - s.time).toInt() + 1
                diff.coerceIn(1, 60)
            } else 1
        } catch (_: Exception) {
            1
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(AppStrings.get("lv_submit_title", lang)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = AppStrings.get("lv_submitted_by", lang),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    LeaveApplicantRole.entries.forEach { role ->
                        FilterChip(
                            selected = applicantRole == role,
                            onClick = { applicantRole = role },
                            label = { Text(if (lang == AppLanguage.URDU) role.labelUr else role.labelEn) },
                            modifier = Modifier.testTag("submit_leave_role_${role.name}")
                        )
                    }
                }

                if (applicantRole == LeaveApplicantRole.STUDENT || applicantRole == LeaveApplicantRole.PARENT) {
                    ExposedDropdownMenuBox(
                        expanded = studentMenuExpanded,
                        onExpandedChange = { studentMenuExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedStudent?.let { "${it.nameEn} (Class ${it.className}-${it.section})" }
                                ?: "Select student",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(AppStrings.get("tb_select_student", lang)) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = studentMenuExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                                .testTag("submit_leave_student_dropdown")
                        )
                        ExposedDropdownMenu(
                            expanded = studentMenuExpanded,
                            onDismissRequest = { studentMenuExpanded = false }
                        ) {
                            students.forEach { stu ->
                                DropdownMenuItem(
                                    text = { Text("${stu.nameEn} • Class ${stu.className}-${stu.section} (Roll ${stu.rollNumber})") },
                                    onClick = {
                                        selectedStudent = stu
                                        guardianName = stu.fatherName
                                        studentMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    if (applicantRole == LeaveApplicantRole.PARENT) {
                        OutlinedTextField(
                            value = guardianName,
                            onValueChange = { guardianName = it },
                            label = { Text("Parent / Guardian Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("submit_leave_guardian_input")
                        )
                    }
                }

                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val types = if (applicantRole == LeaveApplicantRole.TEACHER) {
                        listOf("CASUAL", "MEDICAL", "EARNED")
                    } else {
                        listOf("MEDICAL", "CASUAL", "FAMILY_EVENT", "EMERGENCY")
                    }
                    types.forEach { type ->
                        FilterChip(
                            selected = leaveType == type,
                            onClick = { leaveType = type },
                            label = { Text(type.replace("_", " ")) },
                            modifier = Modifier.testTag("submit_leave_type_$type")
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startDate,
                        onValueChange = { startDate = it },
                        label = { Text("From") },
                        trailingIcon = {
                            IconButton(onClick = { activeDatePickerTarget = "START" }) {
                                Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = "Pick start date")
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("submit_leave_start_date")
                    )
                    OutlinedTextField(
                        value = endDate,
                        onValueChange = { endDate = it },
                        label = { Text("To ($computedDays d)") },
                        trailingIcon = {
                            IconButton(onClick = { activeDatePickerTarget = "END" }) {
                                Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = "Pick end date")
                            }
                        },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("submit_leave_end_date")
                    )
                }

                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Reason for Leave") },
                    singleLine = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(86.dp)
                        .testTag("submit_leave_reason_input")
                )

                OutlinedTextField(
                    value = attachmentNote,
                    onValueChange = { attachmentNote = it },
                    label = { Text("Attachment / Medical Certificate Note (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val newLeave = if (applicantRole == LeaveApplicantRole.TEACHER) {
                        LeaveApplicationEntity(
                            applicantId = "T01",
                            applicantName = "Farooq Ahmad Lone",
                            applicantRole = "TEACHER",
                            leaveType = leaveType,
                            startDate = startDate.trim(),
                            endDate = endDate.trim(),
                            daysCount = computedDays,
                            reason = reason.trim(),
                            attachmentReceiptPath = attachmentNote.trim(),
                            status = "PENDING"
                        )
                    } else {
                        val stu = selectedStudent ?: return@Button
                        LeaveApplicationEntity(
                            applicantId = stu.admissionNumber,
                            applicantName = stu.nameEn,
                            applicantRole = applicantRole.name,
                            studentUuid = stu.uuid,
                            className = stu.className,
                            section = stu.section,
                            guardianName = guardianName.trim().ifEmpty { stu.fatherName },
                            guardianPhone = stu.primaryContact,
                            leaveType = leaveType,
                            startDate = startDate.trim(),
                            endDate = endDate.trim(),
                            daysCount = computedDays,
                            reason = reason.trim(),
                            attachmentReceiptPath = attachmentNote.trim(),
                            status = "PENDING"
                        )
                    }
                    onSubmit(newLeave)
                },
                enabled = reason.isNotBlank() && (applicantRole == LeaveApplicantRole.TEACHER || selectedStudent != null),
                modifier = Modifier.testTag("submit_leave_application_button")
            ) {
                Text(AppStrings.get("lv_submit_digital", lang))
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text(AppStrings.get("cancel", lang)) }
        }
    )

    if (activeDatePickerTarget != null) {
        val pickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { activeDatePickerTarget = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            val picked = dateFormat.format(Date(millis))
                            if (activeDatePickerTarget == "START") {
                                startDate = picked
                                if (endDate < picked) endDate = picked
                            } else {
                                endDate = picked
                            }
                        }
                        activeDatePickerTarget = null
                    }
                ) { Text(AppStrings.get("confirm", lang)) }
            },
            dismissButton = {
                TextButton(onClick = { activeDatePickerTarget = null }) { Text(AppStrings.get("cancel", lang)) }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}
