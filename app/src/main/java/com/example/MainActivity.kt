package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.fragment.app.FragmentActivity
import com.example.domain.service.SchoolNotificationService
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.domain.model.UserRole
import com.example.ui.components.TeacherCoachOverlay
import com.example.ui.localization.AppLanguage
import com.example.ui.localization.AppStrings
import com.example.ui.localization.LocalAppLanguage
import com.example.ui.screens.AttendanceScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DatabaseHealthScreen
import com.example.ui.screens.DocumentManagerScreen
import com.example.ui.screens.ExamReportCardScreen
import com.example.ui.screens.LeaveManagementScreen
import com.example.ui.screens.SchoolSetupScreen
import com.example.ui.screens.StudentsScreen
import com.example.ui.screens.TextbookRegisterScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.SchoolViewModel

const val ROUTE_DASHBOARD = "dashboard"
const val ROUTE_ATTENDANCE = "attendance"
const val ROUTE_STUDENTS = "students"
const val ROUTE_LEAVES = "leaves"
const val ROUTE_TEXTBOOKS = "textbooks"
const val ROUTE_EXAMS = "exams"
const val ROUTE_DOCUMENTS = "documents"
const val ROUTE_DB_HEALTH = "db_health"
const val ROUTE_SETUP = "setup"

class MainActivity : FragmentActivity() {
    private val pendingNavRoute = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingNavRoute.value = intent?.getStringExtra(SchoolNotificationService.EXTRA_NAV_ROUTE)
        setContent {
            val viewModel: SchoolViewModel = viewModel()
            val currentLanguage by viewModel.appLanguage.collectAsState()
            val navExtra by pendingNavRoute

            CompositionLocalProvider(
                LocalAppLanguage provides currentLanguage,
                LocalLayoutDirection provides currentLanguage.layoutDirection
            ) {
                MyApplicationTheme {
                    MainScreenContent(
                        viewModel = viewModel,
                        requestedRoute = navExtra,
                        onRequestedRouteHandled = { pendingNavRoute.value = null }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingNavRoute.value = intent.getStringExtra(SchoolNotificationService.EXTRA_NAV_ROUTE)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreenContent(
    viewModel: SchoolViewModel,
    requestedRoute: String? = null,
    onRequestedRouteHandled: () -> Unit = {}
) {
    val lang = LocalAppLanguage.current
    var currentScreen by remember { mutableStateOf(ROUTE_DASHBOARD) }
    LaunchedEffect(requestedRoute) {
        if (!requestedRoute.isNullOrBlank()) {
            currentScreen = requestedRoute
            onRequestedRouteHandled()
        }
    }
    val userRole by viewModel.currentUserRole.collectAsState()
    val allLeaves by viewModel.allLeaves.collectAsState()
    val pendingLeavesCount = if (userRole == UserRole.HEADMASTER) {
        allLeaves.count { it.status == "PENDING" }
    } else {
        allLeaves.count { it.status == "PENDING" && (it.applicantRole == "STUDENT" || it.applicantRole == "PARENT") }
    }
    val showCoachTour by viewModel.showCoachTour.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    // Handle back button for secondary screens
    if (currentScreen != ROUTE_DASHBOARD) {
        BackHandler {
            currentScreen = ROUTE_DASHBOARD
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = AppStrings.get("app_title", lang),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = AppStrings.get("primary_middle_edition", lang),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    if (currentScreen != ROUTE_DASHBOARD) {
                        IconButton(
                            onClick = { currentScreen = ROUTE_DASHBOARD },
                            modifier = Modifier.testTag("nav_back_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back"
                            )
                        }
                    } else {
                        IconButton(
                            onClick = { currentScreen = ROUTE_SETUP },
                            modifier = Modifier.testTag("school_logo_nav_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.School,
                                contentDescription = "School Setup",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                actions = {
                    // Language Switch Button (English <-> اردو)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier
                            .clickable { viewModel.toggleLanguage() }
                            .padding(end = 6.dp)
                            .testTag("language_toggle_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Translate,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (lang == AppLanguage.URDU) "English" else "اردو",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    // Documents action
                    IconButton(
                        onClick = { currentScreen = ROUTE_DOCUMENTS },
                        modifier = Modifier.testTag("nav_documents_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Folder,
                            contentDescription = "Documents",
                            tint = if (currentScreen == ROUTE_DOCUMENTS) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Setup action
                    IconButton(
                        onClick = { currentScreen = ROUTE_SETUP },
                        modifier = Modifier.testTag("nav_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = if (currentScreen == ROUTE_SETUP) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp,
                modifier = Modifier.testTag("main_bottom_nav_bar")
            ) {
                NavigationBarItem(
                    selected = currentScreen == ROUTE_DASHBOARD,
                    onClick = { currentScreen = ROUTE_DASHBOARD },
                    icon = { Icon(imageVector = Icons.Default.School, contentDescription = "Dashboard") },
                    label = { Text(AppStrings.get("dashboard", lang), fontSize = 10.sp) },
                    modifier = Modifier.testTag("nav_item_dashboard")
                )
                NavigationBarItem(
                    selected = currentScreen == ROUTE_ATTENDANCE,
                    onClick = { currentScreen = ROUTE_ATTENDANCE },
                    icon = { Icon(imageVector = Icons.Default.Today, contentDescription = "Attendance") },
                    label = { Text(AppStrings.get("attendance", lang), fontSize = 10.sp) },
                    modifier = Modifier.testTag("nav_item_attendance")
                )
                NavigationBarItem(
                    selected = currentScreen == ROUTE_STUDENTS,
                    onClick = { currentScreen = ROUTE_STUDENTS },
                    icon = { Icon(imageVector = Icons.Default.Person, contentDescription = "Students") },
                    label = { Text(AppStrings.get("students", lang), fontSize = 10.sp) },
                    modifier = Modifier.testTag("nav_item_students")
                )
                NavigationBarItem(
                    selected = currentScreen == ROUTE_LEAVES,
                    onClick = { currentScreen = ROUTE_LEAVES },
                    icon = {
                        if (pendingLeavesCount > 0) {
                            BadgedBox(badge = { Badge { Text("$pendingLeavesCount") } }) {
                                Icon(imageVector = Icons.Default.Assignment, contentDescription = "Leaves")
                            }
                        } else {
                            Icon(imageVector = Icons.Default.Assignment, contentDescription = "Leaves")
                        }
                    },
                    label = { Text(AppStrings.get("leaves", lang), fontSize = 10.sp) },
                    modifier = Modifier.testTag("nav_item_leaves")
                )
                NavigationBarItem(
                    selected = currentScreen == ROUTE_EXAMS,
                    onClick = { currentScreen = ROUTE_EXAMS },
                    icon = { Icon(imageVector = Icons.Default.Book, contentDescription = "Exams") },
                    label = { Text(AppStrings.get("exams_reports", lang), fontSize = 10.sp) },
                    modifier = Modifier.testTag("nav_item_exams")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                ROUTE_DASHBOARD -> DashboardScreen(
                    viewModel = viewModel,
                    onNavigateTo = { screen -> currentScreen = screen }
                )
                ROUTE_ATTENDANCE -> AttendanceScreen(viewModel = viewModel)
                ROUTE_STUDENTS -> StudentsScreen(
                    viewModel = viewModel,
                    onNavigateToExams = { currentScreen = ROUTE_EXAMS }
                )
                ROUTE_LEAVES -> LeaveManagementScreen(viewModel = viewModel)
                ROUTE_TEXTBOOKS -> TextbookRegisterScreen(viewModel = viewModel)
                ROUTE_EXAMS -> ExamReportCardScreen(
                    viewModel = viewModel,
                    onNavigateToDocuments = { currentScreen = ROUTE_DOCUMENTS }
                )
                ROUTE_DOCUMENTS -> DocumentManagerScreen()
                ROUTE_DB_HEALTH -> DatabaseHealthScreen(viewModel = viewModel)
                ROUTE_SETUP -> SchoolSetupScreen(viewModel = viewModel)
            }

            // Interactive Coach Tour Overlay
            TeacherCoachOverlay(
                visible = showCoachTour,
                onDismiss = { viewModel.dismissCoachTour() }
            )
        }
    }
}
