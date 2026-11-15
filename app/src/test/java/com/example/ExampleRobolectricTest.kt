package com.example

import android.app.Application
import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.SchoolDatabase
import com.example.data.local.entities.AttendanceEntity
import com.example.data.local.entities.StudentEntity
import android.content.Intent
import androidx.work.ListenableWorker
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import com.example.data.local.entities.ExamEntity
import com.example.data.local.entities.LeaveApplicationEntity
import com.example.data.local.entities.SchoolProfileEntity
import com.example.data.local.entities.StudentGradeEntity
import com.example.data.local.entities.TextbookAllocationEntity
import com.example.domain.service.AttendanceAlertType
import com.example.domain.service.CsvStudentParserService
import com.example.domain.service.PdfReportCardGenerator
import com.example.domain.service.PerformanceCalculationService
import com.example.domain.service.WhatsAppService
import com.example.domain.worker.BackupScheduler
import com.example.domain.worker.DailyDatabaseBackupWorker
import com.example.ui.components.DailyAttendanceRegisterCard
import com.example.ui.components.WeeklyAttendanceTrendChart
import com.example.ui.localization.AppLanguage
import com.example.ui.localization.AppStrings
import com.example.ui.localization.LocalAppLanguage
import com.example.ui.screens.LeaveManagementScreen
import com.example.ui.screens.TextbookRegisterScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.SchoolViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import android.app.NotificationManager
import com.example.domain.service.PdfPrintHelper
import com.example.domain.service.QrCodeService
import com.example.domain.service.SchoolNotificationService
import com.example.domain.worker.DeadlineNotificationWorker
import com.example.ui.components.PdfPreviewDialog
import com.example.data.local.entities.StudentVoiceMemoEntity
import com.example.domain.service.BiometricAuthHelper
import com.example.domain.service.DatabaseMaintenanceService
import com.example.domain.service.VoiceMemoAudioService
import com.example.ui.components.StudentVoiceMemosDialog
import com.example.ui.screens.SchoolSetupScreen
import com.example.ui.screens.StudentsScreen
import org.robolectric.Shadows
import org.robolectric.shadows.ShadowApplication
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Smart School Manager", appName)
  }

  @Test
  fun `verify grade band calculations`() {
    assertEquals("A1", PerformanceCalculationService.calculateGradeBand(95.0))
    assertEquals("A2", PerformanceCalculationService.calculateGradeBand(85.0))
    assertEquals("B1", PerformanceCalculationService.calculateGradeBand(75.0))
    assertEquals("B2", PerformanceCalculationService.calculateGradeBand(65.0))
    assertEquals("C1", PerformanceCalculationService.calculateGradeBand(55.0))
    assertEquals("D", PerformanceCalculationService.calculateGradeBand(38.0))
    assertEquals("E", PerformanceCalculationService.calculateGradeBand(25.0))
  }

  @Test
  fun `room database stores students and enforces idempotent attendance composite key offline`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = Room.inMemoryDatabaseBuilder(context, SchoolDatabase::class.java)
      .allowMainThreadQueries()
      .build()

    try {
      val student = StudentEntity(
        uuid = "uuid-test-1",
        admissionNumber = "ADM-TEST-01",
        rollNumber = 1,
        nameEn = "Zainab Jan",
        nameUr = "زینب جان",
        className = 4,
        section = "A",
        gender = "Female",
        dob = "2017-04-12",
        category = "GENERAL",
        fatherName = "Manzoor Ahmad",
        motherName = "Shahzada",
        primaryContact = "+919419011111",
        whatsAppNumber = "+919419011111",
        residentialAddress = "Sopore",
        aadhaarReferenceToken = "UID-TEST-1",
        maskedAadhaar = "XXXX-XXXX-4321",
        aadhaarStatus = "VERIFIED"
      )
      db.studentDao().insertStudent(student)

      val fetched = db.studentDao().getStudentByUuid("uuid-test-1")
      assertEquals("Zainab Jan", fetched?.nameEn)

      // Insert ABSENT, then overwrite same (studentId, date) with PRESENT
      db.attendanceDao().insertSingleAttendance(
        AttendanceEntity(studentId = "uuid-test-1", date = "2026-11-13", status = "ABSENT")
      )
      db.attendanceDao().insertSingleAttendance(
        AttendanceEntity(studentId = "uuid-test-1", date = "2026-11-13", status = "PRESENT")
      )

      val records = db.attendanceDao().getAttendanceByDateSync("2026-11-13")
      assertEquals(1, records.size)
      assertEquals("PRESENT", records.first().status)
    } finally {
      db.close()
    }
  }

  @Test
  fun `global language preference persists across viewmodel instances`() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val vm1 = SchoolViewModel(app)
    vm1.setLanguage(AppLanguage.URDU)
    assertEquals(AppLanguage.URDU, vm1.appLanguage.value)

    val vm2 = SchoolViewModel(app)
    assertEquals(AppLanguage.URDU, vm2.appLanguage.value)

    // Restore English
    vm2.setLanguage(AppLanguage.ENGLISH)
    assertEquals(AppLanguage.ENGLISH, vm2.appLanguage.value)
  }

  @Test
  fun `daily attendance register card filters primary and middle classes opens datepicker and supports urdu`() {
    val sampleStudents = listOf(
      StudentEntity(
        uuid = "stu-1",
        admissionNumber = "ADM-101",
        rollNumber = 1,
        nameEn = "Areeba Fatima",
        nameUr = "اریبہ فاطمہ",
        className = 3,
        section = "A",
        gender = "Female",
        dob = "2018-03-14",
        category = "GENERAL",
        fatherName = "Ghulam Hassan",
        motherName = "Hameeda",
        primaryContact = "+919419000001",
        whatsAppNumber = "+919419000001",
        residentialAddress = "Sopore",
        aadhaarReferenceToken = "UID-1",
        maskedAadhaar = "XXXX-XXXX-1234",
        aadhaarStatus = "VERIFIED"
      ),
      StudentEntity(
        uuid = "stu-2",
        admissionNumber = "ADM-601",
        rollNumber = 1,
        nameEn = "Danish Farooq",
        nameUr = "دانش فاروق",
        className = 6,
        section = "A",
        gender = "Male",
        dob = "2015-01-10",
        category = "GENERAL",
        fatherName = "Farooq Ahmad",
        motherName = "Safia",
        primaryContact = "+919419000002",
        whatsAppNumber = "+919419000002",
        residentialAddress = "Zaingeer",
        aadhaarReferenceToken = "UID-2",
        maskedAadhaar = "XXXX-XXXX-5678",
        aadhaarStatus = "VERIFIED"
      )
    )
    val sampleAttendance = listOf(
      AttendanceEntity(studentId = "stu-1", date = "2026-11-13", status = "ABSENT")
    )

    var markAllCalledFor: Pair<Int, String>? = null
    var fullRegisterCalledFor: Pair<Int, String>? = null

    composeTestRule.setContent {
      CompositionLocalProvider(
        LocalAppLanguage provides AppLanguage.ENGLISH,
        LocalLayoutDirection provides AppLanguage.ENGLISH.layoutDirection
      ) {
        MyApplicationTheme {
          Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            DailyAttendanceRegisterCard(
              allStudents = sampleStudents,
              attendanceRecords = sampleAttendance,
              selectedDate = "2026-11-13",
              canUndo = false,
              onShiftDate = {},
              onSelectDate = {},
              onResetToToday = {},
              onMarkStudentStatus = { _, _ -> },
              onMarkAllPresentForClass = { cls, sec -> markAllCalledFor = Pair(cls, sec) },
              onUndoLastBulk = {},
              onOpenFullRegister = { cls, sec -> fullRegisterCalledFor = Pair(cls, sec) }
            )
          }
        }
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("daily_attendance_register_dashboard_card").assertIsDisplayed()

    // Open DatePickerDialog from trigger
    composeTestRule.onNodeWithTag("dashboard_date_picker_trigger").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("dashboard_date_picker_dialog").assertIsDisplayed()
    composeTestRule.onNodeWithTag("date_picker_cancel_btn").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("class_register_card_3_A").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("class_register_card_6_A").performScrollTo().assertIsDisplayed()

    // Filter to Primary (1-5): Class 3-A visible, Class 6-A hidden
    composeTestRule.onNodeWithTag("filter_division_primary").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("class_register_card_3_A").performScrollTo().assertIsDisplayed()
    composeTestRule.onNodeWithTag("class_register_card_6_A").assertDoesNotExist()

    // Expand Class 3-A to verify inline student register and open full register
    composeTestRule.onNodeWithTag("class_register_card_3_A").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("Areeba Fatima").performScrollTo().assertIsDisplayed()

    composeTestRule.onNodeWithTag("quick_mark_all_3_A").performScrollTo().performClick()
    assertEquals(Pair(3, "A"), markAllCalledFor)

    composeTestRule.onNodeWithTag("open_full_register_3_A").performScrollTo().performClick()
    assertEquals(Pair(3, "A"), fullRegisterCalledFor)

    // Filter to Middle (6-8): Class 6-A visible, Class 3-A hidden
    composeTestRule.onNodeWithTag("filter_division_middle").performScrollTo().performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("class_register_card_6_A").performScrollTo().assertIsDisplayed()
    assertTrue(fullRegisterCalledFor != null)
    assertEquals("روزانہ جماعتی حاضری رجسٹر", AppStrings.get("daily_class_registers", AppLanguage.URDU))
  }

  @Test
  fun `bulk selection checkboxes open specific alert dialog and trigger whatsapp callback`() {
    val sampleStudents = listOf(
      StudentEntity(
        uuid = "stu-1",
        admissionNumber = "ADM-101",
        rollNumber = 1,
        nameEn = "Areeba Fatima",
        nameUr = "اریبہ فاطمہ",
        className = 3,
        section = "A",
        gender = "Female",
        dob = "2018-03-14",
        category = "GENERAL",
        fatherName = "Ghulam Hassan",
        motherName = "Hameeda",
        primaryContact = "9419000001",
        whatsAppNumber = "9419000001",
        residentialAddress = "Sopore",
        aadhaarReferenceToken = "UID-1",
        maskedAadhaar = "XXXX-XXXX-1234",
        aadhaarStatus = "VERIFIED"
      ),
      StudentEntity(
        uuid = "stu-2",
        admissionNumber = "ADM-102",
        rollNumber = 2,
        nameEn = "Umar Bashir",
        nameUr = "عمر بشیر",
        className = 3,
        section = "A",
        gender = "Male",
        dob = "2018-06-11",
        category = "GENERAL",
        fatherName = "Bashir Ahmad",
        motherName = "Naseema",
        primaryContact = "9419000002",
        whatsAppNumber = "9419000002",
        residentialAddress = "Sopore",
        aadhaarReferenceToken = "UID-2",
        maskedAadhaar = "XXXX-XXXX-5678",
        aadhaarStatus = "VERIFIED"
      )
    )
    val sampleAttendance = listOf(
      AttendanceEntity(studentId = "stu-1", date = "2026-11-14", status = "ABSENT"),
      AttendanceEntity(studentId = "stu-2", date = "2026-11-14", status = "PRESENT")
    )

    var sentStudents: List<StudentEntity> = emptyList()
    var sentMessage = ""

    composeTestRule.setContent {
      CompositionLocalProvider(
        LocalAppLanguage provides AppLanguage.ENGLISH,
        LocalLayoutDirection provides AppLanguage.ENGLISH.layoutDirection
      ) {
        MyApplicationTheme {
          Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            DailyAttendanceRegisterCard(
              allStudents = sampleStudents,
              attendanceRecords = sampleAttendance,
              selectedDate = "2026-11-14",
              canUndo = false,
              onShiftDate = {},
              onSelectDate = {},
              onResetToToday = {},
              onMarkStudentStatus = { _, _ -> },
              onMarkAllPresentForClass = { _, _ -> },
              onUndoLastBulk = {},
              onOpenFullRegister = { _, _ -> },
              onSendWhatsAppAlert = { selected, msg ->
                sentStudents = selected
                sentMessage = msg
              }
            )
          }
        }
      }
    }

    composeTestRule.waitForIdle()
    // Expand Class 3-A
    composeTestRule.onNodeWithTag("class_register_card_3_A").performScrollTo().performClick()
    composeTestRule.waitForIdle()

    // Select individual student via bulk selection checkbox
    composeTestRule.onNodeWithTag("dashboard_student_checkbox_1").performScrollTo().performClick()
    composeTestRule.waitForIdle()

    // Bulk WhatsApp bar appears
    composeTestRule.onNodeWithTag("dashboard_bulk_whatsapp_bar", useUnmergedTree = true)
      .performScrollTo()
      .assertIsDisplayed()
    composeTestRule.onNodeWithTag("dashboard_send_whatsapp_alert_btn").performScrollTo().performClick()
    composeTestRule.waitForIdle()

    // Specific alert dialog opens -> select Late Arrival alert -> confirm send
    composeTestRule.onNodeWithTag("whatsapp_alert_dialog").assertIsDisplayed()
    composeTestRule.onNodeWithTag("alert_type_chip_LATE").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("confirm_whatsapp_alert_send_btn").performClick()
    composeTestRule.waitForIdle()

    assertEquals(1, sentStudents.size)
    assertEquals("stu-1", sentStudents.first().uuid)
    assertTrue(sentMessage.contains("Areeba Fatima"))
    assertTrue(sentMessage.contains("LATE"))
  }

  @Test
  fun `whatsapp service builds expected intent uri for single and multiple guardians`() {
    val singleIntent = WhatsAppService.buildBulkWhatsAppIntent(
      phoneNumbers = listOf("9419000001"),
      messageText = "Absence alert test"
    )
    assertEquals(Intent.ACTION_VIEW, singleIntent.action)
    assertEquals("com.whatsapp", singleIntent.`package`)
    assertTrue((singleIntent.flags and Intent.FLAG_ACTIVITY_NEW_TASK) != 0)
    val singleUri = singleIntent.data.toString()
    assertTrue(singleUri.startsWith("https://api.whatsapp.com/send?phone=919419000001&text="))

    val multiIntent = WhatsAppService.buildBulkWhatsAppIntent(
      phoneNumbers = listOf("9419000001", "9419000002"),
      messageText = "Low attendance alert"
    )
    val multiUri = multiIntent.data.toString()
    assertTrue(multiUri.startsWith("https://api.whatsapp.com/send?text="))

    val urMsg = WhatsAppService.buildAttendanceAlertMessage(
      alertType = AttendanceAlertType.ABSENCE,
      date = "2026-11-14",
      students = emptyList(),
      language = AppLanguage.URDU
    )
    assertTrue(urMsg.contains("غیر حاضر"))
  }

  @Test
  fun `daily backup worker exports room database to external storage and schedules periodic work`() = runBlocking {
    SchoolDatabase.resetInstance()
    val context = ApplicationProvider.getApplicationContext<Context>()
    WorkManagerTestInitHelper.initializeTestWorkManager(context)

    // Ensure Room DB file exists first
    SchoolDatabase.getInstance(context).openHelper.writableDatabase

    val worker = TestListenableWorkerBuilder<DailyDatabaseBackupWorker>(context).build()
    val result = worker.doWork()
    assertTrue(result is ListenableWorker.Result.Success)

    val outputPath = (result as ListenableWorker.Result.Success)
      .outputData
      .getString(DailyDatabaseBackupWorker.KEY_BACKUP_PATH)
    assertNotNull(outputPath)
    val backupFile = File(outputPath!!)
    assertTrue(backupFile.exists())
    assertTrue(backupFile.length() > 0L)

    BackupScheduler.scheduleDailyDatabaseBackup(context, replaceExisting = true)
    val workInfos = WorkManager.getInstance(context)
      .getWorkInfosForUniqueWork(BackupScheduler.UNIQUE_DAILY_BACKUP_WORK)
      .get()
    assertTrue(workInfos.isNotEmpty())
    assertEquals(WorkInfo.State.ENQUEUED, workInfos.first().state)
  }

  @Test
  fun `csv parser parses standardized student csv with quoted fields and imports into room`() = runBlocking {
    val csv = """
      admission_number,roll_number,name_en,name_ur,class_name,section,gender,dob,category,father_name,mother_name,primary_contact,whatsapp_number,residential_address,academic_session
      ADM-2026-401,1,Ayesha Tariq,عائشہ طارق,4,A,Female,2017-04-12,GENERAL,Tariq Ahmad,Rukhsana,9419088101,9419088101,"Model Town, Sopore",2026-2027
      ADM-2026-402,2,Bilal Yousuf,بلال یوسف,4,A,Male,2017-09-21,OBC,Yousuf Bhat,Shaheena,9419088102,9419088102,"Noorbagh, Sopore",2026-2027
      ADM-BAD-999,1,Invalid Class Student,غلط,9,A,Male,2012-01-01,GENERAL,Parent,Mother,9419088199,9419088199,Sopore,2026-2027
    """.trimIndent()

    val parsed = CsvStudentParserService.parse(csv)
    assertEquals(3, parsed.totalRowsProcessed)
    assertEquals(2, parsed.validStudents.size)
    assertEquals(1, parsed.errors.size)
    assertEquals("Model Town, Sopore", parsed.validStudents.first().residentialAddress)
    assertFalse(parsed.validStudents.first().consentRecorded)

    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = Room.inMemoryDatabaseBuilder(context, SchoolDatabase::class.java)
      .allowMainThreadQueries()
      .build()
    try {
      db.studentDao().insertStudents(parsed.validStudents)
      val class4Students = db.studentDao().getStudentsByClassAndSectionSync(4, "A")
      assertEquals(2, class4Students.size)
      assertEquals("Ayesha Tariq", class4Students.first().nameEn)
    } finally {
      db.close()
    }
  }

  @Test
  fun `weekly attendance trend chart renders 7 school days and inspects peak absenteeism`() {
    val sampleStudents = listOf(
      StudentEntity(
        uuid = "stu-w1",
        admissionNumber = "ADM-1",
        rollNumber = 1,
        nameEn = "Areeba Fatima",
        nameUr = "اریبہ فاطمہ",
        className = 3,
        section = "A",
        gender = "Female",
        dob = "2018-03-14",
        category = "GENERAL",
        fatherName = "Ghulam Hassan",
        motherName = "Hameeda",
        primaryContact = "9419000001",
        whatsAppNumber = "9419000001",
        residentialAddress = "Sopore",
        aadhaarReferenceToken = "UID-1",
        maskedAadhaar = "XXXX-XXXX-1234",
        aadhaarStatus = "VERIFIED"
      ),
      StudentEntity(
        uuid = "stu-w2",
        admissionNumber = "ADM-2",
        rollNumber = 2,
        nameEn = "Umar Bashir",
        nameUr = "عمر بشیر",
        className = 3,
        section = "A",
        gender = "Male",
        dob = "2018-06-11",
        category = "GENERAL",
        fatherName = "Bashir Ahmad",
        motherName = "Naseema",
        primaryContact = "9419000002",
        whatsAppNumber = "9419000002",
        residentialAddress = "Sopore",
        aadhaarReferenceToken = "UID-2",
        maskedAadhaar = "XXXX-XXXX-5678",
        aadhaarStatus = "VERIFIED"
      )
    )

    val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val cal = Calendar.getInstance()
    cal.set(2026, Calendar.NOVEMBER, 14) // Saturday
    val refDate = fmt.format(cal.time)
    val records = mutableListOf<AttendanceEntity>()
    for (offset in 0..6) {
      val c = (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -offset) }
      if (c.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) continue
      val d = fmt.format(c.time)
      records.add(AttendanceEntity(studentId = "stu-w1", date = d, status = if (offset == 2) "ABSENT" else "PRESENT"))
      records.add(AttendanceEntity(studentId = "stu-w2", date = d, status = "PRESENT"))
    }

    composeTestRule.setContent {
      CompositionLocalProvider(
        LocalAppLanguage provides AppLanguage.ENGLISH,
        LocalLayoutDirection provides AppLanguage.ENGLISH.layoutDirection
      ) {
        MyApplicationTheme {
          WeeklyAttendanceTrendChart(
            allStudents = sampleStudents,
            attendanceRecords = records,
            referenceDate = refDate
          )
        }
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("weekly_attendance_trend_card").assertIsDisplayed()
    composeTestRule.onNodeWithTag("weekly_trend_line_canvas").assertIsDisplayed()
    composeTestRule.onNodeWithTag("weekly_peak_absent_pill").assertIsDisplayed()
    composeTestRule.onNodeWithTag("trend_selected_day_detail").assertIsDisplayed()

    composeTestRule.onNodeWithTag("trend_day_chip_0").performClick()
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("trend_selected_day_detail").assertIsDisplayed()
  }

  @Test
  fun `textbook inventory tracks student assignment condition return due date and overdue status`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = Room.inMemoryDatabaseBuilder(context, SchoolDatabase::class.java)
      .allowMainThreadQueries()
      .build()

    try {
      val id = db.textbookDao().insertAllocation(
        TextbookAllocationEntity(
          className = 6,
          section = "A",
          subject = "Mathematics",
          bookTitle = "JKBOSE Mathematics - Class 6",
          studentId = "stu-tb-1",
          studentName = "Danish Farooq",
          status = "RECEIVED",
          distributionDate = "2026-04-12",
          returnDueDate = "2026-10-30",
          bookCondition = "FAIR",
          bookCode = "JK-6A-MAT-1",
          remarks = "Cover slightly worn"
        )
      )
      val saved = db.textbookDao().getAllocationById(id)
      assertNotNull(saved)
      assertEquals("FAIR", saved!!.bookCondition)
      assertEquals("2026-10-30", saved.returnDueDate)

      db.textbookDao().updateInventoryDetails(
        id = id,
        status = "RETURNED",
        condition = "GOOD",
        returnDueDate = "2026-10-30",
        returnedDate = "2026-11-15",
        remarks = "Rebound and returned"
      )
      val updated = db.textbookDao().getAllocationById(id)!!
      assertEquals("RETURNED", updated.status)
      assertEquals("GOOD", updated.bookCondition)
      assertEquals("2026-11-15", updated.returnedDate)
    } finally {
      db.close()
    }
  }

  @Test
  fun `student and parent digital leave requests persist in room and support teacher status tracking`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = Room.inMemoryDatabaseBuilder(context, SchoolDatabase::class.java)
      .allowMainThreadQueries()
      .build()

    try {
      val leaveId = db.leaveDao().insertLeave(
        LeaveApplicationEntity(
          applicantId = "ADM-2026-303",
          applicantName = "Saima Mushtaq",
          applicantRole = "PARENT",
          studentUuid = "stu-saima",
          className = 3,
          section = "A",
          guardianName = "Mushtaq Ahmad Dar",
          guardianPhone = "+91 94190 77116",
          leaveType = "MEDICAL",
          startDate = "2026-11-15",
          endDate = "2026-11-16",
          daysCount = 2,
          reason = "High fever",
          status = "PENDING"
        )
      )

      val pendingList = db.leaveDao().getLeavesByStatus("PENDING").first()
      assertEquals(1, pendingList.size)
      assertEquals("PARENT", pendingList.first().applicantRole)

      val loaded = db.leaveDao().getLeaveById(leaveId)!!
      db.leaveDao().updateLeave(
        loaded.copy(
          status = "APPROVED",
          reviewedBy = "Farooq Ahmad Lone (Teacher)",
          reviewNotes = "Medical rest granted"
        )
      )

      val approvedList = db.leaveDao().getLeavesByStatus("APPROVED").first()
      assertEquals(1, approvedList.size)
      assertEquals("Saima Mushtaq", approvedList.first().applicantName)
      assertEquals("Farooq Ahmad Lone (Teacher)", approvedList.first().reviewedBy)
    } finally {
      db.close()
    }
  }

  @Test
  fun `bilingual report card generator fetches grades from room and exports valid english urdu pdf`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = Room.inMemoryDatabaseBuilder(context, SchoolDatabase::class.java)
      .allowMainThreadQueries()
      .build()

    try {
      val student = StudentEntity(
        uuid = "stu-rc-1",
        admissionNumber = "ADM-2026-601",
        rollNumber = 1,
        nameEn = "Danish Farooq",
        nameUr = "دانش فاروق",
        className = 6,
        section = "A",
        gender = "Male",
        dob = "2015-01-10",
        category = "GENERAL",
        fatherName = "Farooq Ahmad",
        motherName = "Safia Begum",
        primaryContact = "9419077119",
        whatsAppNumber = "9419077119",
        residentialAddress = "Zaingeer",
        aadhaarReferenceToken = "UID-601",
        maskedAadhaar = "XXXX-XXXX-9934",
        aadhaarStatus = "VERIFIED"
      )
      db.studentDao().insertStudent(student)

      val examIds = db.examDao().insertExams(
        listOf(
          ExamEntity(sessionId = "2026-2027", term = "TERM_1", className = 6, subjectNameEn = "Urdu Literature", subjectNameUr = "اردو ادب", maxMarks = 100),
          ExamEntity(sessionId = "2026-2027", term = "TERM_1", className = 6, subjectNameEn = "Mathematics", subjectNameUr = "ریاضی", maxMarks = 100)
        )
      )
      db.examDao().insertGrades(
        listOf(
          StudentGradeEntity(examId = examIds[0], studentId = student.uuid, sessionId = "2026-2027", marksObtained = 88.0, gradeBand = "A2"),
          StudentGradeEntity(examId = examIds[1], studentId = student.uuid, sessionId = "2026-2027", marksObtained = 94.0, gradeBand = "A1")
        )
      )
      db.attendanceDao().insertSingleAttendance(
        AttendanceEntity(studentId = student.uuid, date = "2026-11-14", status = "PRESENT")
      )

      val fetchedExams = db.examDao().getExamsForClassAndTermSync("2026-2027", 6, "TERM_1")
      val fetchedGrades = db.examDao().getGradesForStudentSync(student.uuid, "2026-2027")
      val fetchedAttendance = db.attendanceDao().getAttendanceForStudentSync(student.uuid)

      val summary = PerformanceCalculationService.generateReportCardSummary(
        student = student,
        exams = fetchedExams,
        grades = fetchedGrades,
        allPeerPercentages = listOf(80.0, 91.0),
        attendanceRecords = fetchedAttendance,
        totalScheduledWorkingDays = 1,
        term = "TERM_1"
      )

      assertEquals(2, summary.subjectScores.size)
      assertEquals("اردو ادب", summary.subjectScores[0].subjectNameUr)
      assertEquals(91.0, summary.percentage, 0.01)
      assertEquals("A1", summary.overallGrade)
      assertTrue(summary.qualitativeFeedbackUr.isNotBlank())

      val pdfFile = PdfReportCardGenerator.generateSinglePdf(
        context = context,
        school = SchoolProfileEntity(),
        summary = summary
      )
      assertTrue(pdfFile.exists())
      assertTrue(pdfFile.length() > 500L)
      val header = pdfFile.readBytes().take(5).toByteArray().toString(Charsets.US_ASCII)
      assertEquals("%PDF-", header)
    } finally {
      db.close()
    }
  }

  @Test
  fun `qr code service encodes parses matches student ids and generates qr bitmap`() {
    val student = StudentEntity(
      uuid = "uuid-qr-601",
      admissionNumber = "ADM-2026-601",
      rollNumber = 1,
      nameEn = "Danish Farooq",
      nameUr = "دانش فاروق",
      className = 6,
      section = "A",
      gender = "Male",
      dob = "2015-01-10",
      category = "GENERAL",
      fatherName = "Farooq Ahmad",
      motherName = "Safia Begum",
      primaryContact = "9419077119",
      whatsAppNumber = "9419077119",
      residentialAddress = "Zaingeer",
      aadhaarReferenceToken = "UID-601",
      maskedAadhaar = "XXXX-XXXX-9934",
      aadhaarStatus = "VERIFIED"
    )

    val payload = QrCodeService.encodeStudentQrPayload(student)
    assertEquals("SSM_STU|ADM-2026-601|uuid-qr-601", payload)

    assertEquals(student, QrCodeService.findMatchingStudent(payload, listOf(student)))
    assertEquals(student, QrCodeService.findMatchingStudent("ADM-2026-601", listOf(student)))
    assertEquals(student, QrCodeService.findMatchingStudent("{\"admissionNumber\":\"ADM-2026-601\"}", listOf(student)))

    val qrBitmap = QrCodeService.generateQrBitmap(payload, 256)
    assertNotNull(qrBitmap)
    assertEquals(256, qrBitmap!!.width)
    assertEquals(256, qrBitmap.height)
  }

  @Test
  fun `local push notifications evaluate upcoming deadlines dispatch leave status alerts and run worker`() = runBlocking {
    val app = ApplicationProvider.getApplicationContext<Application>()
    Shadows.shadowOf(app).grantPermissions(android.Manifest.permission.POST_NOTIFICATIONS)
    SchoolNotificationService.ensureNotificationChannels(app)

    val notifManager = app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val shadowNotif = Shadows.shadowOf(notifManager)
    assertTrue(shadowNotif.notificationChannels.size >= 2)

    val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val todayCal = Calendar.getInstance()
    val todayStr = fmt.format(todayCal.time)
    val twoDaysLater = (todayCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 2) }.let { fmt.format(it.time) }
    val pastDue = (todayCal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -5) }.let { fmt.format(it.time) }

    val books = listOf(
      TextbookAllocationEntity(
        id = 1, className = 6, section = "A", subject = "Mathematics",
        bookTitle = "Math 6", studentId = "s1", studentName = "Danish Farooq",
        status = "RECEIVED", distributionDate = "2026-04-01", returnDueDate = pastDue
      ),
      TextbookAllocationEntity(
        id = 2, className = 3, section = "A", subject = "Science",
        bookTitle = "Sci 3", studentId = "s2", studentName = "Zainab Jan",
        status = "RECEIVED", distributionDate = "2026-04-01", returnDueDate = twoDaysLater
      )
    )
    val leave = LeaveApplicationEntity(
      id = 9,
      applicantId = "ADM-2026-303",
      applicantName = "Saima Mushtaq",
      applicantRole = "PARENT",
      leaveType = "MEDICAL",
      startDate = todayStr,
      endDate = todayStr,
      daysCount = 1,
      reason = "Fever",
      status = "APPROVED",
      reviewNotes = "Rest approved"
    )

    val summary = SchoolNotificationService.evaluateDeadlines(books, listOf(leave), todayStr)
    assertEquals(1, summary.overdueBooks.size)
    assertEquals(1, summary.upcomingBooks.size)

    assertTrue(SchoolNotificationService.notifyLeaveStatusUpdate(app, leave, AppLanguage.ENGLISH))
    assertTrue(SchoolNotificationService.notifyUpcomingDeadlines(app, summary, AppLanguage.ENGLISH))
    assertTrue(shadowNotif.allNotifications.size >= 2)

    val worker = TestListenableWorkerBuilder<DeadlineNotificationWorker>(app).build()
    val workerResult = worker.doWork()
    assertTrue(workerResult is ListenableWorker.Result.Success)
  }

  @Test
  fun `in app pdf previewer displays generated report card with print share and whatsapp actions`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val student = StudentEntity(
      uuid = "stu-preview-1",
      admissionNumber = "ADM-2026-601",
      rollNumber = 1,
      nameEn = "Danish Farooq",
      nameUr = "دانش فاروق",
      className = 6,
      section = "A",
      gender = "Male",
      dob = "2015-01-10",
      category = "GENERAL",
      fatherName = "Farooq Ahmad",
      motherName = "Safia Begum",
      primaryContact = "9419077119",
      whatsAppNumber = "9419077119",
      residentialAddress = "Zaingeer",
      aadhaarReferenceToken = "UID-601",
      maskedAadhaar = "XXXX-XXXX-9934",
      aadhaarStatus = "VERIFIED"
    )
    val summary = PerformanceCalculationService.generateReportCardSummary(
      student = student,
      exams = listOf(ExamEntity(id = 1, sessionId = "2026-2027", term = "TERM_1", className = 6, subjectNameEn = "Mathematics", subjectNameUr = "ریاضی", maxMarks = 100)),
      grades = listOf(StudentGradeEntity(examId = 1, studentId = student.uuid, sessionId = "2026-2027", marksObtained = 90.0, gradeBand = "A1")),
      allPeerPercentages = listOf(90.0),
      attendanceRecords = emptyList(),
      totalScheduledWorkingDays = 10,
      term = "TERM_1"
    )
    val pdfFile = PdfReportCardGenerator.generateSinglePdf(context, SchoolProfileEntity(), summary)

    var closed = false
    var whatsAppClicked = false

    composeTestRule.setContent {
      CompositionLocalProvider(
        LocalAppLanguage provides AppLanguage.ENGLISH,
        LocalLayoutDirection provides AppLanguage.ENGLISH.layoutDirection
      ) {
        MyApplicationTheme {
          PdfPreviewDialog(
            pdfFile = pdfFile,
            onDismiss = { closed = true },
            onWhatsAppClick = { whatsAppClicked = true }
          )
        }
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("in_app_pdf_preview_dialog").assertIsDisplayed()
    composeTestRule.onNodeWithTag("pdf_preview_print_btn").assertIsDisplayed()
    composeTestRule.onNodeWithTag("pdf_preview_share_btn").assertIsDisplayed()
    composeTestRule.onNodeWithTag("pdf_preview_whatsapp_btn").performClick()
    assertTrue(whatsAppClicked)

    composeTestRule.onNodeWithTag("pdf_preview_close_btn").performClick()
    assertTrue(closed)
  }

  @Test
  fun `biometric lock protects sensitive student records and unlocks on verification`() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    // Ensure biometric capability check completes cleanly under Robolectric
    assertNotNull(BiometricAuthHelper.checkCapability(app))

    val viewModel = SchoolViewModel(app)
    viewModel.setBiometricProtectStudents(true)
    assertTrue(viewModel.biometricProtectStudents.value)
    assertFalse(viewModel.isStudentRecordsUnlocked.value)

    composeTestRule.setContent {
      CompositionLocalProvider(
        LocalAppLanguage provides AppLanguage.ENGLISH,
        LocalLayoutDirection provides AppLanguage.ENGLISH.layoutDirection
      ) {
        MyApplicationTheme {
          StudentsScreen(
            viewModel = viewModel,
            onNavigateToExams = {}
          )
        }
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("student_records_locked_banner").assertIsDisplayed()
    composeTestRule.onNodeWithTag("unlock_student_records_btn").performClick()
    composeTestRule.waitForIdle()

    composeTestRule.onNodeWithTag("biometric_lock_dialog").assertIsDisplayed()
    composeTestRule.onNodeWithTag("biometric_fingerprint_button").performClick()
    composeTestRule.waitForIdle()

    assertTrue(viewModel.isStudentRecordsUnlocked.value)
    composeTestRule.onNodeWithTag("lock_student_records_btn").assertIsDisplayed()
  }

  @Test
  fun `settings screen displays last backup time triggers external export and cloud sync`() = runBlocking {
    SchoolDatabase.resetInstance()
    val app = ApplicationProvider.getApplicationContext<Application>()
    app.getSharedPreferences("smart_school_prefs", Context.MODE_PRIVATE).edit().clear().apply()
    WorkManagerTestInitHelper.initializeTestWorkManager(app)
    SchoolDatabase.getInstance(app).openHelper.writableDatabase

    val viewModel = SchoolViewModel(app)

    composeTestRule.setContent {
      CompositionLocalProvider(
        LocalAppLanguage provides AppLanguage.ENGLISH,
        LocalLayoutDirection provides AppLanguage.ENGLISH.layoutDirection
      ) {
        MyApplicationTheme {
          SchoolSetupScreen(viewModel = viewModel)
        }
      }
    }

    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithTag("settings_backup_card").assertIsDisplayed()
    composeTestRule.onNodeWithTag("settings_last_backup_time").assertIsDisplayed()
    assertEquals(null, viewModel.lastExternalBackupTime.value)

    // Trigger immediate external storage backup export from Settings screen
    composeTestRule.onNodeWithTag("settings_export_external_btn").performClick()
    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      viewModel.lastExternalBackupTime.value != null
    }
    val (_, backupPath) = DatabaseMaintenanceService.getLastExternalBackupMetadata(app)
    assertNotNull(backupPath)
    assertTrue(File(backupPath!!).exists())

    // Trigger immediate cloud sync from Settings screen
    composeTestRule.onNodeWithTag("settings_force_sync_btn").performScrollTo().performClick()
    composeTestRule.waitUntil(timeoutMillis = 5_000) {
      viewModel.lastCloudSyncTime.value != null
    }
    assertTrue(viewModel.lastCloudSyncTime.value!! > 0L)
  }

  @Test
  fun `student behavioral voice memos persist in room and render in voice memos dialog`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = Room.inMemoryDatabaseBuilder(context, SchoolDatabase::class.java)
      .allowMainThreadQueries()
      .build()

    try {
      val student = StudentEntity(
        uuid = "stu-memo-1",
        admissionNumber = "ADM-2026-601",
        rollNumber = 1,
        nameEn = "Danish Farooq",
        nameUr = "دانش فاروق",
        className = 6,
        section = "A",
        gender = "Male",
        dob = "2015-01-10",
        category = "GENERAL",
        fatherName = "Farooq Ahmad",
        motherName = "Safia Begum",
        primaryContact = "9419077119",
        whatsAppNumber = "9419077119",
        residentialAddress = "Zaingeer",
        aadhaarReferenceToken = "UID-601",
        maskedAadhaar = "XXXX-XXXX-9934",
        aadhaarStatus = "VERIFIED"
      )
      db.studentDao().insertStudent(student)

      val fakeAudioFile = File(context.filesDir, "voice_memos/test_memo.m4a").apply {
        parentFile?.mkdirs()
        writeBytes(byteArrayOf(0x01, 0x02, 0x03))
      }

      val memoId = db.voiceMemoDao().insertVoiceMemo(
        StudentVoiceMemoEntity(
          studentUuid = student.uuid,
          studentName = student.nameEn,
          tag = "Behavioral",
          noteText = "Helped classmate with science lab apparatus.",
          filePath = fakeAudioFile.absolutePath,
          durationSeconds = 14,
          recordedBy = "Farooq Ahmad Lone (Teacher)"
        )
      )

      val savedMemos = db.voiceMemoDao().getVoiceMemosForStudentSync(student.uuid)
      assertEquals(1, savedMemos.size)
      assertEquals("Behavioral", savedMemos.first().tag)
      assertEquals("00:14", VoiceMemoAudioService.formatDuration(savedMemos.first().durationSeconds))

      var deletedMemo: StudentVoiceMemoEntity? = null
      composeTestRule.setContent {
        CompositionLocalProvider(
          LocalAppLanguage provides AppLanguage.ENGLISH,
          LocalLayoutDirection provides AppLanguage.ENGLISH.layoutDirection
        ) {
          MyApplicationTheme {
            StudentVoiceMemosDialog(
              student = student,
              memos = savedMemos,
              onSaveMemo = { _, _, _, _ -> },
              onDeleteMemo = { deletedMemo = it },
              onDismiss = {}
            )
          }
        }
      }

      composeTestRule.waitForIdle()
      composeTestRule.onNodeWithTag("student_voice_memos_dialog").assertIsDisplayed()
      composeTestRule.onNodeWithTag("voice_memo_record_btn").assertIsDisplayed()
      composeTestRule.onNodeWithTag("voice_memo_card_$memoId").assertIsDisplayed()
      composeTestRule.onNodeWithTag("play_voice_memo_$memoId").assertIsDisplayed()
      composeTestRule.onNodeWithTag("delete_voice_memo_$memoId").performClick()
      assertEquals(memoId, deletedMemo?.id)
    } finally {
      db.close()
    }
  }
}

