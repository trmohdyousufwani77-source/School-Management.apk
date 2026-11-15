package com.example.ui.screens

import android.Manifest
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AssignmentReturn
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
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
import com.example.data.local.entities.StudentEntity
import com.example.data.local.entities.TextbookAllocationEntity
import com.example.domain.model.TextbookCondition
import com.example.domain.service.SchoolNotificationService
import com.example.ui.components.QrScannerDialog
import com.example.ui.components.TextbookDistributionChart
import com.example.ui.localization.AppLanguage
import com.example.ui.localization.AppStrings
import com.example.ui.localization.LocalAppLanguage
import com.example.ui.viewmodel.SchoolViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TextbookRegisterScreen(
    viewModel: SchoolViewModel
) {
    val context = LocalContext.current
    val lang = LocalAppLanguage.current
    val allAllocations by viewModel.allTextbooks.collectAsState()
    val allStudents by viewModel.allActiveStudents.collectAsState()
    val deadlineSummary by viewModel.upcomingDeadlineSummary.collectAsState()

    var selectedClassFilter by remember { mutableIntStateOf(0) } // 0 = All classes
    var selectedStatusFilter by remember { mutableStateOf("ALL") } // ALL, RECEIVED, OVERDUE, RETURNED, NOT_AVAILABLE, NOT_RECEIVED
    var selectedConditionFilter by remember { mutableStateOf("ALL") } // ALL, NEW, GOOD, FAIR, DAMAGED, LOST
    var searchQuery by remember { mutableStateOf("") }

    var showAssignDialog by remember { mutableStateOf(false) }
    var qrPreselectedStudent by remember { mutableStateOf<StudentEntity?>(null) }
    var showQrScannerDialog by remember { mutableStateOf(false) }
    var editingAllocation by remember { mutableStateOf<TextbookAllocationEntity?>(null) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.checkAndDispatchDeadlineNotifications()
        } else {
            Toast.makeText(context, "Notification permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    val todayStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()) }

    fun isOverdue(item: TextbookAllocationEntity): Boolean {
        return item.status == "RECEIVED" &&
            item.returnDueDate.isNotBlank() &&
            item.returnDueDate < todayStr
    }

    val filteredAllocations = remember(
        allAllocations,
        selectedClassFilter,
        selectedStatusFilter,
        selectedConditionFilter,
        searchQuery,
        todayStr
    ) {
        val q = searchQuery.trim().lowercase(Locale.US)
        allAllocations.filter { item ->
            val matchesClass = selectedClassFilter == 0 || item.className == selectedClassFilter
            val matchesStatus = when (selectedStatusFilter) {
                "ALL" -> true
                "OVERDUE" -> isOverdue(item)
                else -> item.status == selectedStatusFilter
            }
            val matchesCondition = selectedConditionFilter == "ALL" || item.bookCondition == selectedConditionFilter
            val matchesSearch = q.isEmpty() ||
                item.studentName.lowercase(Locale.US).contains(q) ||
                item.bookTitle.lowercase(Locale.US).contains(q) ||
                item.subject.lowercase(Locale.US).contains(q) ||
                item.bookCode.lowercase(Locale.US).contains(q)
            matchesClass && matchesStatus && matchesCondition && matchesSearch
        }
    }

    val assignedCount = allAllocations.count { it.status == "RECEIVED" }
    val overdueCount = allAllocations.count { isOverdue(it) }
    val fairDamagedCount = allAllocations.count {
        it.bookCondition == "FAIR" || it.bookCondition == "DAMAGED" || it.bookCondition == "LOST"
    }
    val returnedCount = allAllocations.count { it.status == "RETURNED" }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // Header & KPI Strip
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = AppStrings.get("tb_inventory_title", lang),
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = AppStrings.get("tb_inventory_subtitle", lang),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            InventoryKpiChip(
                                label = AppStrings.get("tb_assigned_count", lang),
                                count = assignedCount,
                                containerColor = Color(0xFFD4EDDA),
                                textColor = Color(0xFF155724),
                                modifier = Modifier.weight(1f).testTag("kpi_tb_assigned")
                            )
                            InventoryKpiChip(
                                label = AppStrings.get("tb_overdue_count", lang),
                                count = overdueCount,
                                containerColor = Color(0xFFF8D7DA),
                                textColor = Color(0xFF721C24),
                                modifier = Modifier.weight(1f).testTag("kpi_tb_overdue")
                            )
                            InventoryKpiChip(
                                label = AppStrings.get("tb_damaged_count", lang),
                                count = fairDamagedCount,
                                containerColor = Color(0xFFFFF3CD),
                                textColor = Color(0xFF856404),
                                modifier = Modifier.weight(1f).testTag("kpi_tb_damaged")
                            )
                            InventoryKpiChip(
                                label = AppStrings.get("tb_returned_count", lang),
                                count = returnedCount,
                                containerColor = Color(0xFFD1ECF1),
                                textColor = Color(0xFF0C5460),
                                modifier = Modifier.weight(1f).testTag("kpi_tb_returned")
                            )
                        }
                    }
                }
            }

            // Distribution chart
            item {
                TextbookDistributionChart(allocations = allAllocations)
            }

            // QR Checkout & Deadline Push Notification Quick Actions
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { showQrScannerDialog = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("textbook_qr_scan_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = AppStrings.get("qr_scan_btn", lang),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(AppStrings.get("qr_scan_btn", lang), fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                !SchoolNotificationService.hasNotificationPermission(context)
                            ) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            } else {
                                viewModel.checkAndDispatchDeadlineNotifications()
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("textbook_check_deadlines_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = AppStrings.get("notif_check_deadlines", lang),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        val alertCount = deadlineSummary.totalBookAlerts
                        Text(
                            text = if (alertCount > 0) "Notify ($alertCount due)" else AppStrings.get("notif_check_deadlines", lang),
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                    }
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(AppStrings.get("tb_search_hint", lang), fontSize = 13.sp) },
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
                        .testTag("textbook_search_input")
                )
            }

            // Class, Status/Overdue, and Condition Filters
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedClassFilter == 0,
                            onClick = { selectedClassFilter = 0 },
                            label = { Text(AppStrings.get("all_classes", lang)) },
                            modifier = Modifier.testTag("tb_filter_class_all")
                        )
                        (1..8).forEach { cls ->
                            FilterChip(
                                selected = selectedClassFilter == cls,
                                onClick = { selectedClassFilter = cls },
                                label = { Text("${AppStrings.get("class_prefix", lang)} $cls") },
                                modifier = Modifier.testTag("tb_filter_class_$cls")
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val statusOptions = listOf(
                            "ALL" to "All",
                            "RECEIVED" to AppStrings.get("tb_assigned_count", lang),
                            "OVERDUE" to AppStrings.get("tb_filter_overdue", lang),
                            "RETURNED" to AppStrings.get("tb_returned_count", lang),
                            "NOT_AVAILABLE" to "Shortage",
                            "NOT_RECEIVED" to "Pending Issue"
                        )
                        statusOptions.forEach { (code, label) ->
                            FilterChip(
                                selected = selectedStatusFilter == code,
                                onClick = { selectedStatusFilter = code },
                                label = { Text(label) },
                                modifier = Modifier.testTag("tb_filter_status_$code")
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedConditionFilter == "ALL",
                            onClick = { selectedConditionFilter = "ALL" },
                            label = { Text(AppStrings.get("tb_all_conditions", lang)) },
                            modifier = Modifier.testTag("tb_filter_cond_ALL")
                        )
                        TextbookCondition.entries.forEach { cond ->
                            FilterChip(
                                selected = selectedConditionFilter == cond.name,
                                onClick = { selectedConditionFilter = cond.name },
                                label = { Text(if (lang == AppLanguage.URDU) cond.labelUr else cond.labelEn) },
                                modifier = Modifier.testTag("tb_filter_cond_${cond.name}")
                            )
                        }
                    }
                }
            }

            // Inventory Records List
            if (filteredAllocations.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No textbook inventory records match current filters.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(filteredAllocations, key = { it.id }) { allocation ->
                    val overdue = isOverdue(allocation)
                    var menuExpanded by remember { mutableStateOf(false) }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("textbook_item_${allocation.id}"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
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
                                        Icon(
                                            imageVector = Icons.Default.Book,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "${allocation.subject}: ${allocation.bookTitle}",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${allocation.studentName} • ${AppStrings.get("class_prefix", lang)} ${allocation.className}-${allocation.section}" +
                                            if (allocation.bookCode.isNotBlank()) " • ${allocation.bookCode}" else "",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                Box {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = when (allocation.status) {
                                            "RECEIVED" -> Color(0xFFD4EDDA)
                                            "NOT_AVAILABLE" -> Color(0xFFF8D7DA)
                                            "NOT_RECEIVED" -> Color(0xFFFFF3CD)
                                            "RETURNED" -> Color(0xFFD1ECF1)
                                            else -> Color(0xFFE2E3E5)
                                        },
                                        modifier = Modifier
                                            .clickable { menuExpanded = true }
                                            .testTag("tb_status_chip_${allocation.id}")
                                    ) {
                                        Text(
                                            text = allocation.status.replace("_", " "),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = when (allocation.status) {
                                                "RECEIVED" -> Color(0xFF155724)
                                                "NOT_AVAILABLE" -> Color(0xFF721C24)
                                                "NOT_RECEIVED" -> Color(0xFF856404)
                                                "RETURNED" -> Color(0xFF0C5460)
                                                else -> Color(0xFF383D41)
                                            },
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }

                                    DropdownMenu(
                                        expanded = menuExpanded,
                                        onDismissRequest = { menuExpanded = false }
                                    ) {
                                        listOf(
                                            "RECEIVED" to "Assigned / Issued",
                                            "RETURNED" to "Returned to School",
                                            "NOT_RECEIVED" to "Pending Issue",
                                            "NOT_AVAILABLE" to "Not Available in Inventory"
                                        ).forEach { (statusCode, statusLabel) ->
                                            DropdownMenuItem(
                                                text = { Text(statusLabel) },
                                                onClick = {
                                                    viewModel.updateTextbookStatus(allocation.id, statusCode)
                                                    menuExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Condition & Return Due Date Metadata Strip
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                // Condition Pill
                                val condEnum = TextbookCondition.entries.firstOrNull { it.name == allocation.bookCondition }
                                    ?: TextbookCondition.GOOD
                                val (condBg, condFg) = when (condEnum) {
                                    TextbookCondition.NEW -> Color(0xFFE8F5E9) to Color(0xFF1B5E20)
                                    TextbookCondition.GOOD -> Color(0xFFE3F2FD) to Color(0xFF0D47A1)
                                    TextbookCondition.FAIR -> Color(0xFFFFF8E1) to Color(0xFFE65100)
                                    TextbookCondition.DAMAGED, TextbookCondition.LOST -> Color(0xFFFFEBEE) to Color(0xFFB71C1C)
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = condBg
                                ) {
                                    Text(
                                        text = "${AppStrings.get("tb_condition_label", lang)}: ${if (lang == AppLanguage.URDU) condEnum.labelUr else condEnum.labelEn}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = condFg,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }

                                // Return Due Date / Returned Date Pill
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (overdue) Color(0xFFFFEBEE) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = if (overdue) Icons.Default.Warning else Icons.Default.CalendarMonth,
                                            contentDescription = null,
                                            tint = if (overdue) Color(0xFFC62828) else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        val dateLabel = if (allocation.status == "RETURNED" && allocation.returnedDate.isNotBlank()) {
                                            "${AppStrings.get("tb_returned_on_label", lang)}: ${allocation.returnedDate}"
                                        } else {
                                            "${AppStrings.get("tb_due_date_label", lang)}: ${allocation.returnDueDate}"
                                        }
                                        Text(
                                            text = dateLabel,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = if (overdue) Color(0xFFC62828) else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                if (overdue) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFFD32F2F)
                                    ) {
                                        Text(
                                            text = AppStrings.get("tb_overdue_badge", lang),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }

                            if (allocation.remarks.isNotBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "${AppStrings.get("tb_remarks", lang)}: ${allocation.remarks}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = { editingAllocation = allocation },
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.testTag("edit_textbook_btn_${allocation.id}")
                                ) {
                                    Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(AppStrings.get("tb_edit_inventory", lang), fontSize = 11.sp)
                                }

                                if (allocation.status == "RECEIVED") {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Button(
                                        onClick = {
                                            viewModel.markTextbookReturned(
                                                id = allocation.id,
                                                condition = allocation.bookCondition,
                                                remarks = "Returned on $todayStr (${allocation.bookCondition})"
                                            )
                                        },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                        modifier = Modifier.testTag("return_textbook_btn_${allocation.id}")
                                    ) {
                                        Icon(imageVector = Icons.Default.AssignmentReturn, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(AppStrings.get("tb_mark_returned", lang), fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(84.dp)) }
        }

        // Assign Textbook FAB
        ExtendedFloatingActionButton(
            onClick = { showAssignDialog = true },
            icon = { Icon(imageVector = Icons.Default.Add, contentDescription = "Assign Book") },
            text = { Text(AppStrings.get("tb_assign_book", lang)) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(18.dp)
                .testTag("assign_textbook_fab")
        )
    }

    if (showAssignDialog) {
        AssignTextbookDialog(
            students = allStudents,
            initialStudent = qrPreselectedStudent,
            onDismiss = {
                showAssignDialog = false
                qrPreselectedStudent = null
            },
            onAssign = { allocation ->
                viewModel.assignTextbookToStudent(allocation)
                showAssignDialog = false
                qrPreselectedStudent = null
            }
        )
    }

    if (showQrScannerDialog) {
        QrScannerDialog(
            title = AppStrings.get("qr_scan_textbook_title", lang),
            students = allStudents,
            onDismiss = { showQrScannerDialog = false },
            onQrScanned = { rawCode ->
                val matched = viewModel.findStudentByQr(rawCode)
                if (matched != null) {
                    searchQuery = matched.nameEn
                    qrPreselectedStudent = matched
                    showQrScannerDialog = false
                    showAssignDialog = true
                } else {
                    Toast.makeText(context, "No student matched QR: $rawCode", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    if (editingAllocation != null) {
        EditTextbookInventoryDialog(
            allocation = editingAllocation!!,
            onDismiss = { editingAllocation = null },
            onSave = { status, condition, dueDate, returnedDate, remarks ->
                viewModel.updateTextbookInventory(
                    id = editingAllocation!!.id,
                    status = status,
                    condition = condition,
                    returnDueDate = dueDate,
                    returnedDate = returnedDate,
                    remarks = remarks
                )
                editingAllocation = null
            }
        )
    }
}

@Composable
private fun InventoryKpiChip(
    label: String,
    count: Int,
    containerColor: Color,
    textColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = containerColor
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$count",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = textColor
            )
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = textColor,
                maxLines = 1
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AssignTextbookDialog(
    students: List<StudentEntity>,
    initialStudent: StudentEntity? = null,
    onDismiss: () -> Unit,
    onAssign: (TextbookAllocationEntity) -> Unit
) {
    val lang = LocalAppLanguage.current
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    val todayStr = remember { dateFormat.format(Date()) }
    val defaultDueStr = remember {
        Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 90) }.let { dateFormat.format(it.time) }
    }

    var selectedStudent by remember(students, initialStudent) {
        mutableStateOf(initialStudent ?: students.firstOrNull())
    }
    var studentMenuExpanded by remember { mutableStateOf(false) }

    var subject by remember { mutableStateOf("Science") }
    var bookTitle by remember { mutableStateOf("JKBOSE General Science - Class ${selectedStudent?.className ?: 6}") }
    var bookCode by remember { mutableStateOf("JK-${selectedStudent?.className ?: 6}A-SCI-${selectedStudent?.rollNumber ?: 1}") }
    var selectedCondition by remember { mutableStateOf("NEW") }
    var returnDueDate by remember { mutableStateOf(defaultDueStr) }
    var remarks by remember { mutableStateOf("Assigned from school textbook inventory") }
    var showDueDatePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(AppStrings.get("tb_assign_book", lang)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                ExposedDropdownMenuBox(
                    expanded = studentMenuExpanded,
                    onExpandedChange = { studentMenuExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedStudent?.let { "${it.nameEn} (Class ${it.className}-${it.section}, Roll ${it.rollNumber})" }
                            ?: "No active students",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(AppStrings.get("tb_select_student", lang)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = studentMenuExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                            .testTag("assign_tb_student_dropdown")
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
                                    bookTitle = "JKBOSE $subject - Class ${stu.className}"
                                    bookCode = "JK-${stu.className}${stu.section}-${subject.take(3).uppercase(Locale.US)}-${stu.rollNumber}"
                                    studentMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = subject,
                        onValueChange = {
                            subject = it
                            selectedStudent?.let { s ->
                                bookTitle = "JKBOSE $it - Class ${s.className}"
                            }
                        },
                        label = { Text(AppStrings.get("tb_subject", lang)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("assign_tb_subject_input")
                    )
                    OutlinedTextField(
                        value = bookCode,
                        onValueChange = { bookCode = it },
                        label = { Text(AppStrings.get("tb_book_code", lang)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("assign_tb_code_input")
                    )
                }

                OutlinedTextField(
                    value = bookTitle,
                    onValueChange = { bookTitle = it },
                    label = { Text(AppStrings.get("tb_book_title", lang)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("assign_tb_title_input")
                )

                Text(
                    text = AppStrings.get("tb_condition_label", lang),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("NEW", "GOOD", "FAIR", "DAMAGED").forEach { cond ->
                        FilterChip(
                            selected = selectedCondition == cond,
                            onClick = { selectedCondition = cond },
                            label = { Text(cond) },
                            modifier = Modifier.testTag("assign_tb_cond_$cond")
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = returnDueDate,
                        onValueChange = { returnDueDate = it },
                        label = { Text(AppStrings.get("tb_due_date_label", lang)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("assign_tb_due_date_input")
                    )
                    OutlinedButton(
                        onClick = { showDueDatePicker = true },
                        modifier = Modifier.testTag("assign_tb_pick_due_date_btn")
                    ) {
                        Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = "Pick due date")
                    }
                }

                OutlinedTextField(
                    value = remarks,
                    onValueChange = { remarks = it },
                    label = { Text(AppStrings.get("tb_remarks", lang)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val stu = selectedStudent
                    if (stu != null && bookTitle.isNotBlank() && subject.isNotBlank()) {
                        onAssign(
                            TextbookAllocationEntity(
                                className = stu.className,
                                section = stu.section,
                                subject = subject.trim(),
                                bookTitle = bookTitle.trim(),
                                studentId = stu.uuid,
                                studentName = stu.nameEn,
                                status = "RECEIVED",
                                distributionDate = todayStr,
                                returnDueDate = returnDueDate.trim().ifEmpty { defaultDueStr },
                                returnedDate = "",
                                bookCondition = selectedCondition,
                                bookCode = bookCode.trim(),
                                remarks = remarks.trim()
                            )
                        )
                    }
                },
                enabled = selectedStudent != null && bookTitle.isNotBlank(),
                modifier = Modifier.testTag("confirm_assign_textbook_btn")
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

    if (showDueDatePicker) {
        val pickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDueDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            returnDueDate = dateFormat.format(Date(millis))
                        }
                        showDueDatePicker = false
                    }
                ) { Text(AppStrings.get("confirm", lang)) }
            },
            dismissButton = {
                TextButton(onClick = { showDueDatePicker = false }) { Text(AppStrings.get("cancel", lang)) }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun EditTextbookInventoryDialog(
    allocation: TextbookAllocationEntity,
    onDismiss: () -> Unit,
    onSave: (status: String, condition: String, dueDate: String, returnedDate: String, remarks: String) -> Unit
) {
    val lang = LocalAppLanguage.current
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    val todayStr = remember { dateFormat.format(Date()) }

    var status by remember { mutableStateOf(allocation.status) }
    var condition by remember { mutableStateOf(allocation.bookCondition) }
    var dueDate by remember { mutableStateOf(allocation.returnDueDate) }
    var returnedDate by remember { mutableStateOf(allocation.returnedDate) }
    var remarks by remember { mutableStateOf(allocation.remarks) }
    var showDueDatePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${allocation.bookTitle} (${allocation.studentName})") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Status",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("RECEIVED", "RETURNED", "NOT_RECEIVED", "NOT_AVAILABLE").forEach { st ->
                        FilterChip(
                            selected = status == st,
                            onClick = {
                                status = st
                                if (st == "RETURNED" && returnedDate.isBlank()) {
                                    returnedDate = todayStr
                                }
                            },
                            label = { Text(st.replace("_", " ")) },
                            modifier = Modifier.testTag("edit_tb_status_$st")
                        )
                    }
                }

                Text(
                    text = AppStrings.get("tb_condition_label", lang),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TextbookCondition.entries.forEach { cond ->
                        FilterChip(
                            selected = condition == cond.name,
                            onClick = { condition = cond.name },
                            label = { Text(if (lang == AppLanguage.URDU) cond.labelUr else cond.labelEn) },
                            modifier = Modifier.testTag("edit_tb_cond_${cond.name}")
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = dueDate,
                        onValueChange = { dueDate = it },
                        label = { Text(AppStrings.get("tb_due_date_label", lang)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f).testTag("edit_tb_due_date_input")
                    )
                    OutlinedButton(onClick = { showDueDatePicker = true }) {
                        Icon(imageVector = Icons.Default.CalendarMonth, contentDescription = "Pick due date")
                    }
                }

                if (status == "RETURNED") {
                    OutlinedTextField(
                        value = returnedDate,
                        onValueChange = { returnedDate = it },
                        label = { Text(AppStrings.get("tb_returned_on_label", lang)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("edit_tb_returned_date_input")
                    )
                }

                OutlinedTextField(
                    value = remarks,
                    onValueChange = { remarks = it },
                    label = { Text(AppStrings.get("tb_remarks", lang)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(status, condition, dueDate.trim(), returnedDate.trim(), remarks.trim()) },
                modifier = Modifier.testTag("save_tb_inventory_btn")
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

    if (showDueDatePicker) {
        val pickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDueDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            dueDate = dateFormat.format(Date(millis))
                        }
                        showDueDatePicker = false
                    }
                ) { Text(AppStrings.get("confirm", lang)) }
            },
            dismissButton = {
                TextButton(onClick = { showDueDatePicker = false }) { Text(AppStrings.get("cancel", lang)) }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}
