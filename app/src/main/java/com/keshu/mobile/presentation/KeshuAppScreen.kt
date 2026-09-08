package com.keshu.mobile.presentation

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.keshu.mobile.data.AppContainer
import com.keshu.mobile.presentation.dashboard.DashboardViewModel
import com.keshu.mobile.presentation.dashboard.DashboardViewModelFactory
import com.keshu.mobile.presentation.advisor.AdvisorPage
import com.keshu.mobile.presentation.components.defaultSemesterStartDate
import com.keshu.mobile.presentation.components.weekFromSemesterStart
import com.keshu.mobile.presentation.credit.CreditPage
import com.keshu.mobile.presentation.exam.ExamPage
import com.keshu.mobile.presentation.home.HomePage
import com.keshu.mobile.presentation.schedule.SchedulePage
import com.keshu.mobile.presentation.sync.ManualSyncActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class MainTab(val title: String, val subtitle: String, val icon: ImageVector) {
    Home("今天", "今日概览", Icons.Default.Home),
    Credit("学分进度", "培养方案", Icons.Default.School),
    Schedule("课表", "课程安排", Icons.Default.CalendarMonth),
    Exam("考试安排", "提醒与座位", Icons.Default.Event),
    Advisor("选课顾问", "课程建议", Icons.Default.AutoAwesome),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeshuAppScreen(
    container: AppContainer,
    launchDestination: String? = null,
    launchAddTask: Boolean = false,
    launchRequestId: Long = 0L,
    onAddTaskLaunchConsumed: (Long) -> Unit = {},
    onRequestNotificationPermission: ((Boolean) -> Unit) -> Unit = { it(true) },
) {
    val context = LocalContext.current
    val academicSettings = remember { context.getSharedPreferences("academic_settings", 0) }
    var selectedTab by remember { mutableStateOf(MainTab.Home) }
    var semesterStartDate by remember {
        mutableStateOf(academicSettings.getString("semester_start_date", defaultSemesterStartDate()).orEmpty())
    }
    var examAvailability by remember {
        mutableStateOf(container.examAvailabilityPreferences.getUnavailable())
    }
    var showPortalPrivacyNotice by remember { mutableStateOf(false) }
    val openPortal = {
        context.startActivity(Intent(context, ManualSyncActivity::class.java))
    }
    val viewModel: DashboardViewModel = viewModel(
        factory = DashboardViewModelFactory(
            container.buildCreditTreeUseCase,
            container.observeCreditSummaryUseCase,
            container.observeScheduleUseCase,
            container.observeLocalCourseFeedbackUseCase,
            container.getCourseAdviceUseCase,
            container.addTaskUseCase,
            container.scheduleExamAlarmsUseCase,
            container.classScheduleDao,
        ),
    )
    val state by viewModel.uiState.collectAsState()
    val todayLabel = remember {
        SimpleDateFormat("M月d日 EEEE", Locale.CHINA).format(Date())
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        examAvailability = container.examAvailabilityPreferences.getUnavailable()
    }

    LaunchedEffect(launchRequestId) {
        MainTab.entries.firstOrNull { it.name.equals(launchDestination, ignoreCase = true) }?.let {
            selectedTab = it
        }
    }

    BoxWithConstraints {
        val useNavigationRail = maxWidth >= 720.dp
        Scaffold(
            topBar = {
                if (selectedTab != MainTab.Schedule) {
                    TopAppBar(
                        title = {
                            Column {
                                Text(selectedTab.title, style = MaterialTheme.typography.titleLarge)
                                Text(
                                    if (selectedTab == MainTab.Home) todayLabel else selectedTab.subtitle,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        },
                        actions = {
                            if (selectedTab == MainTab.Home || selectedTab == MainTab.Exam) {
                                TextButton(
                                    onClick = {
                                        if (academicSettings.getBoolean("portal_privacy_notice_acknowledged", false)) {
                                            openPortal()
                                        } else {
                                            showPortalPrivacyNotice = true
                                        }
                                    },
                                ) {
                                    Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("同步")
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ),
                    )
                }
            },
            bottomBar = {
                if (!useNavigationRail) {
                    MainNavigation(selectedTab = selectedTab, onSelect = { selectedTab = it }, useRail = false)
                }
            },
        ) { padding ->
            Row(Modifier.fillMaxSize().padding(padding)) {
                if (useNavigationRail) {
                    MainNavigation(selectedTab = selectedTab, onSelect = { selectedTab = it }, useRail = true)
                }
                Box(Modifier.weight(1f)) {
                    ScreenShell(constrainWidth = selectedTab != MainTab.Schedule) {
                        when (selectedTab) {
                            MainTab.Home -> HomePage(
                                state = state,
                                currentWeek = weekFromSemesterStart(semesterStartDate),
                                onAddTask = viewModel::addTask,
                                onUpdateTask = viewModel::updateTask,
                                onSetTaskDone = viewModel::setTaskDone,
                                onDeleteTask = viewModel::deleteTask,
                                addTaskRequestId = if (launchAddTask) launchRequestId else 0L,
                                onAddTaskRequestConsumed = onAddTaskLaunchConsumed,
                                examAvailability = examAvailability,
                                onRequestNotificationPermission = onRequestNotificationPermission,
                            )
                            MainTab.Credit -> CreditPage(state)
                            MainTab.Schedule -> SchedulePage(
                                state = state,
                                semesterStartDate = semesterStartDate,
                                onSemesterStartDateChange = {
                                    semesterStartDate = it
                                    academicSettings.edit().putString("semester_start_date", it).apply()
                                },
                                onSessionSave = viewModel::updateClassSession,
                            )
                            MainTab.Exam -> ExamPage(
                                state = state,
                                examAvailability = examAvailability,
                                onScheduleAlarms = {
                                    onRequestNotificationPermission { granted ->
                                        if (granted) viewModel.scheduleAlarms()
                                    }
                                },
                            )
                            MainTab.Advisor -> AdvisorPage(state, onRequestAdvice = viewModel::requestAdvice)
                        }
                    }
                }
            }
        }
    }
    if (showPortalPrivacyNotice) {
        AlertDialog(
            onDismissRequest = { showPortalPrivacyNotice = false },
            confirmButton = {
                Button(
                    onClick = {
                        academicSettings.edit().putBoolean("portal_privacy_notice_acknowledged", true).apply()
                        showPortalPrivacyNotice = false
                        openPortal()
                    },
                ) { Text("继续登录") }
            },
            dismissButton = {
                TextButton(onClick = { showPortalPrivacyNotice = false }) { Text("取消") }
            },
            title = { Text("连接暨南大学教务系统") },
            text = {
                Text("登录在学校网页内完成。课枢会读取你主动同步的培养方案、课程、课表和考试信息，并仅保存在本机；不会保存教务密码。你可以稍后在同步页面清除登录 Cookie 和网页存储。")
            },
        )
    }
}

@Composable
private fun MainNavigation(
    selectedTab: MainTab,
    onSelect: (MainTab) -> Unit,
    useRail: Boolean,
) {
    if (useRail) {
        NavigationRail(containerColor = MaterialTheme.colorScheme.surface) {
            MainTab.entries.forEach { tab ->
                NavigationRailItem(
                    selected = selectedTab == tab,
                    onClick = { onSelect(tab) },
                    icon = { Icon(tab.icon, contentDescription = null) },
                    label = { Text(tab.title, maxLines = 1, style = MaterialTheme.typography.labelSmall) },
                )
            }
        }
    } else {
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
        ) {
            MainTab.entries.forEach { tab ->
                NavigationBarItem(
                    selected = selectedTab == tab,
                    onClick = { onSelect(tab) },
                    icon = { Icon(tab.icon, contentDescription = null) },
                    label = { Text(tab.title, maxLines = 1, style = MaterialTheme.typography.labelSmall) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = Color.Transparent,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                )
            }
        }
    }
}

@Composable
private fun ScreenShell(constrainWidth: Boolean, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(if (constrainWidth) Modifier.widthIn(max = 960.dp) else Modifier),
        ) {
            content()
        }
    }
}
