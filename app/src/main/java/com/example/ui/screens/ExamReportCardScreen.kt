package com.example.ui.screens

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.data.local.entities.ExamEntity
import com.example.data.local.entities.StudentEntity
import com.example.domain.model.ReportCardSummary
import com.example.ui.components.BiometricLockDialog
import com.example.ui.components.PdfPreviewDialog
import com.example.ui.localization.AppLanguage
import com.example.ui.localization.AppStrings
import com.example.ui.localization.LocalAppLanguage
import com.example.ui.viewmodel.SchoolViewModel
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun ExamReportCardScreen(
    viewModel: SchoolViewModel,
    onNavigateToDocuments: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val lang = LocalAppLanguage.current
    val students by viewModel.classStudents.collectAsState()
    val selectedClass by viewModel.selectedClass.collectAsState()
    val selectedSection by viewModel.selectedSection.collectAsState()
    val biometricProtectReports by viewModel.biometricProtectReports.collectAsState()
    val isReportCardAccessUnlocked by viewModel.isReportCardAccessUnlocked.collectAsState()
    val needsReportBiometric = biometricProtectReports && !isReportCardAccessUnlocked

    var selectedTerm by remember { mutableStateOf("TERM_1") }
    var editingStudentForGrade by remember { mutableStateOf<StudentEntity?>(null) }
    var previewSummary by remember { mutableStateOf<ReportCardSummary?>(null) }

    var isBatchGenerating by remember { mutableStateOf(false) }
    var batchProgress by remember { mutableStateOf(Pair(0, 0)) }
    var lastExportedPdfName by remember { mutableStateOf<String?>(null) }
    var previewPdfFile by remember { mutableStateOf<Pair<File, StudentEntity?>?>(null) }
    var pendingReportBiometricAction by remember { mutableStateOf<Pair<String, () -> Unit>?>(null) }

    fun runWithReportCardBiometric(actionLabel: String, action: () -> Unit) {
        if (needsReportBiometric) {
            pendingReportBiometricAction = actionLabel to action
        } else {
            action()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item { Spacer(modifier = Modifier.height(4.dp)) }

        // Assessment Term & Class Selector Header
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = AppStrings.get("rc_engine_title", lang),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = "${AppStrings.get("rc_engine_subtitle", lang)} | 2026-2027",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                    )

                    if (biometricProtectReports) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            onClick = {
                                if (needsReportBiometric) {
                                    pendingReportBiometricAction = "Authorize Report Card PDF Access" to {}
                                } else {
                                    viewModel.lockReportCardAccess()
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            color = if (needsReportBiometric) Color(0xFFFFF3CD) else Color(0xFFD4EDDA),
                            modifier = Modifier.testTag("rc_biometric_status_chip")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (needsReportBiometric) Icons.Default.Lock else Icons.Default.LockOpen,
                                    contentDescription = null,
                                    tint = if (needsReportBiometric) Color(0xFF856404) else Color(0xFF155724),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (needsReportBiometric) {
                                        AppStrings.get("rc_protected_locked", lang)
                                    } else {
                                        AppStrings.get("rc_protected_unlocked", lang)
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (needsReportBiometric) Color(0xFF856404) else Color(0xFF155724)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        (1..8).forEach { cls ->
                            FilterChip(
                                selected = selectedClass == cls,
                                onClick = { viewModel.setSelectedClass(cls) },
                                label = { Text("${AppStrings.get("class_prefix", lang)} $cls") },
                                modifier = Modifier.testTag("rc_class_chip_$cls")
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("UNIT_TEST_1", "TERM_1", "TERM_2").forEach { term ->
                            FilterChip(
                                selected = selectedTerm == term,
                                onClick = { selectedTerm = term },
                                label = { Text(term.replace("_", " ")) },
                                modifier = Modifier.testTag("rc_term_chip_$term")
                            )
                        }
                    }

                    if (lastExportedPdfName != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFFD4EDDA),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Saved Bilingual PDF: $lastExportedPdfName",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = Color(0xFF155724),
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        // Batch PDF Generation Action Card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("batch_generate_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${AppStrings.get("batch_generate_class", lang)} ($selectedClass-$selectedSection)",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                            )
                            Text(
                                text = "Exports ${students.size} bilingual (EN/UR) PDFs from Room DB (${selectedTerm.replace("_", " ")}).",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = {
                                runWithReportCardBiometric("Batch Generate Class $selectedClass-$selectedSection Report Cards") {
                                    isBatchGenerating = true
                                    viewModel.batchGenerateClassPdfs(
                                        term = selectedTerm,
                                        onProgress = { cur, tot -> batchProgress = Pair(cur, tot) },
                                        onComplete = { files ->
                                            isBatchGenerating = false
                                            lastExportedPdfName = "${files.size} PDFs generated"
                                            onNavigateToDocuments()
                                        }
                                    )
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.surface,
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !isBatchGenerating && students.isNotEmpty(),
                            modifier = Modifier.testTag("batch_generate_pdfs_button")
                        ) {
                            Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Generate All", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (isBatchGenerating) {
                        Spacer(modifier = Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { if (batchProgress.second > 0) batchProgress.first.toFloat() / batchProgress.second.toFloat() else 0f },
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Generating bilingual transcript ${batchProgress.first} of ${batchProgress.second}...",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }

        // Student Marks & Report Cards Roster
        if (students.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${AppStrings.get("no_students_class", lang)} (Class $selectedClass-$selectedSection)",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(students, key = { it.uuid }) { student ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("student_grade_row_${student.rollNumber}"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.5.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${student.rollNumber}",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "${student.nameEn} (${student.nameUr})",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                    )
                                    Text(
                                        text = "Class ${student.className}-${student.section} | Adm: ${student.admissionNumber}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Actions Row (Marks entry, Preview, Direct Bilingual PDF, WhatsApp)
                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                IconButton(
                                    onClick = { editingStudentForGrade = student },
                                    modifier = Modifier.testTag("enter_marks_btn_${student.rollNumber}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = AppStrings.get("rc_enter_marks", lang),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            previewSummary = viewModel.getStudentReportSummary(student, selectedTerm)
                                        }
                                    },
                                    modifier = Modifier.testTag("preview_report_card_btn_${student.rollNumber}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Visibility,
                                        contentDescription = AppStrings.get("rc_preview_bilingual", lang),
                                        tint = MaterialTheme.colorScheme.secondary
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        runWithReportCardBiometric("Generate PDF Report Card (${student.nameEn})") {
                                            viewModel.generateSinglePdf(student, selectedTerm) { file ->
                                                lastExportedPdfName = file.name
                                                previewPdfFile = file to student
                                            }
                                        }
                                    },
                                    modifier = Modifier.testTag("export_pdf_btn_${student.rollNumber}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PictureAsPdf,
                                        contentDescription = AppStrings.get("rc_export_bilingual_pdf", lang),
                                        tint = Color(0xFFD32F2F)
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        runWithReportCardBiometric("Share Report Card PDF (${student.nameEn})") {
                                            viewModel.sendReportViaWhatsApp(student, selectedTerm)
                                        }
                                    },
                                    modifier = Modifier.testTag("whatsapp_dispatch_btn_${student.rollNumber}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Send,
                                        contentDescription = AppStrings.get("share_whatsapp", lang),
                                        tint = Color(0xFF25D366)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(30.dp)) }
    }

    // Room-backed Enter / Edit Marks Dialog
    if (editingStudentForGrade != null) {
        val s = editingStudentForGrade!!
        EnterMarksDialog(
            student = s,
            term = selectedTerm,
            viewModel = viewModel,
            onDismiss = { editingStudentForGrade = null },
            onSave = { marksList ->
                marksList.forEach { (exam, marks) ->
                    viewModel.saveGrade(exam.id, s.uuid, marks, false, exam.maxMarks)
                }
                editingStudentForGrade = null
            }
        )
    }

    // Bilingual Preview Report Card Dialog
    if (previewSummary != null) {
        val summary = previewSummary!!
        ReportCardPreviewDialog(
            summary = summary,
            onDismiss = { previewSummary = null },
            onGeneratePdf = {
                val student = students.firstOrNull { it.uuid == summary.studentUuid }
                previewSummary = null
                if (student != null) {
                    runWithReportCardBiometric("Export Bilingual PDF (${student.nameEn})") {
                        viewModel.generateSinglePdf(student, selectedTerm) { file ->
                            lastExportedPdfName = file.name
                            previewPdfFile = file to student
                        }
                    }
                }
            },
            onShareWhatsApp = {
                val student = students.firstOrNull { it.uuid == summary.studentUuid }
                previewSummary = null
                if (student != null) {
                    runWithReportCardBiometric("Share Report Card PDF (${student.nameEn})") {
                        viewModel.sendReportViaWhatsApp(student, selectedTerm)
                    }
                }
            }
        )
    }

    // In-App PDF Page Previewer (with Print, Share, and WhatsApp)
    if (previewPdfFile != null) {
        val (file, student) = previewPdfFile!!
        PdfPreviewDialog(
            pdfFile = file,
            onDismiss = { previewPdfFile = null },
            onWhatsAppClick = if (student != null) {
                { viewModel.sendReportViaWhatsApp(student, selectedTerm) }
            } else null
        )
    }

    // Biometric Verification Gate for Report Card Generation
    if (pendingReportBiometricAction != null) {
        val (title, action) = pendingReportBiometricAction!!
        BiometricLockDialog(
            actionTitle = title,
            onSuccess = {
                viewModel.unlockReportCardAccess()
                pendingReportBiometricAction = null
                action()
            },
            onDismiss = { pendingReportBiometricAction = null }
        )
    }
}

@Composable
private fun EnterMarksDialog(
    student: StudentEntity,
    term: String,
    viewModel: SchoolViewModel,
    onDismiss: () -> Unit,
    onSave: (List<Pair<ExamEntity, Double>>) -> Unit
) {
    val lang = LocalAppLanguage.current
    var exams by remember { mutableStateOf<List<ExamEntity>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    val marksInputs = remember { mutableStateMapOf<Long, String>() }

    LaunchedEffect(student.uuid, student.className, term) {
        isLoading = true
        val loadedExams = viewModel.getExamsForClassAndTerm(student.className, term)
        val existingGrades = viewModel.getGradesMapForStudent(student.uuid)
        exams = loadedExams
        loadedExams.forEach { exam ->
            val existing = existingGrades[exam.id]
            marksInputs[exam.id] = existing?.marksObtained?.toInt()?.toString()
                ?: (exam.maxMarks * 0.8).toInt().toString()
        }
        isLoading = false
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("${AppStrings.get("rc_enter_marks", lang)}: ${student.nameEn}")
                Text(
                    text = "${student.nameUr} • Class ${student.className}-${student.section} (${term.replace("_", " ")})",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        text = {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    exams.forEachIndexed { idx, exam ->
                        val valStr = marksInputs[exam.id] ?: ""
                        OutlinedTextField(
                            value = valStr,
                            onValueChange = { marksInputs[exam.id] = it },
                            label = {
                                Text("${exam.subjectNameEn} (${exam.subjectNameUr}) [/${exam.maxMarks}]")
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("marks_input_subject_$idx")
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val result = exams.map { exam ->
                        val parsed = (marksInputs[exam.id]?.toDoubleOrNull() ?: 0.0)
                            .coerceIn(0.0, exam.maxMarks.toDouble())
                        exam to parsed
                    }
                    onSave(result)
                },
                enabled = !isLoading && exams.isNotEmpty(),
                modifier = Modifier.testTag("save_marks_submit_button")
            ) {
                Text(AppStrings.get("rc_save_marks", lang))
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text(AppStrings.get("cancel", lang)) }
        }
    )
}

@Composable
private fun ReportCardPreviewDialog(
    summary: ReportCardSummary,
    onDismiss: () -> Unit,
    onGeneratePdf: () -> Unit,
    onShareWhatsApp: () -> Unit
) {
    val lang = LocalAppLanguage.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text("Bilingual Report Card | رپورٹ کارڈ")
                Text(
                    text = summary.term.replace("_", " "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.testTag("bilingual_report_preview_list"),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text(
                        text = "${summary.studentNameEn} (${summary.studentNameUr})",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = "Class ${summary.className}-${summary.section} | Roll: ${summary.rollNo} | Adm: ${summary.admissionNo}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Attendance / حاضری: ${summary.attendanceDaysPresent}/${summary.attendanceTotalWorkingDays} (${summary.attendancePercentage}%)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                item {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("PERCENTAGE / فیصد", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp)
                                Text("${summary.percentage}%", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("PERCENTILE", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp)
                                Text("${summary.percentile}%", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold))
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("GRADE / گریڈ", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp)
                                Text(summary.overallGrade, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }

                items(summary.subjectScores) { subj ->
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = subj.subjectName,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold)
                                )
                                Text(
                                    text = subj.subjectNameUr,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(
                                text = "${subj.marksObtained.toInt()}/${subj.maxMarks} [${subj.gradeBand}]",
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                        HorizontalDivider(modifier = Modifier.padding(top = 4.dp), thickness = 0.5.dp)
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Remarks / استاد کی رائے:\n• ${summary.qualitativeFeedbackEn}\n• ${summary.qualitativeFeedbackUr}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onGeneratePdf,
                modifier = Modifier.testTag("dialog_generate_pdf_button")
            ) {
                Icon(imageVector = Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (lang == AppLanguage.URDU) "پی ڈی ایف محفوظ کریں" else "Export Bilingual PDF")
            }
        },
        dismissButton = {
            Button(
                onClick = onShareWhatsApp,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                modifier = Modifier.testTag("dialog_whatsapp_share_button")
            ) {
                Icon(imageVector = Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("WhatsApp")
            }
        }
    )
}
