package com.keshu.mobile.presentation

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material.icons.filled.Widgets
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
import androidx.compose.material3.Surface
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.keshu.mobile.data.AppContainer
import com.keshu.mobile.R
import com.keshu.mobile.BuildConfig
import com.keshu.mobile.data.local.ClassTimePreferences
import com.keshu.mobile.data.update.AppUpdate
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
import com.keshu.mobile.widget.WidgetUpdateManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

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
    val uriHandler = LocalUriHandler.current
    val coroutineScope = rememberCoroutineScope()
    val academicSettings = remember { context.getSharedPreferences("academic_settings", 0) }
    val classTimePreferences = remember { ClassTimePreferences(academicSettings) }
    var selectedTab by remember { mutableStateOf(MainTab.Home) }
    var semesterStartDate by remember {
        mutableStateOf(academicSettings.getString("semester_start_date", defaultSemesterStartDate()).orEmpty())
    }
    var examAvailability by remember {
        mutableStateOf(container.examAvailabilityPreferences.getUnavailable())
    }
    var classTimeSettings by remember { mutableStateOf(classTimePreferences.load()) }
    var showPortalPrivacyNotice by remember { mutableStateOf(false) }
    var availableUpdate by remember { mutableStateOf<AppUpdate?>(null) }
    var showWelcomeGuide by remember {
        mutableStateOf(!academicSettings.getBoolean("welcome_guide_v1_acknowledged", false))
    }
    var showScheduleSetupNotice by remember {
        mutableStateOf(!academicSettings.getBoolean("schedule_setup_notice_acknowledged", false))
    }
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
        coroutineScope.launch {
            container.appUpdateChecker.checkIfDue(BuildConfig.VERSION_NAME)?.let {
                availableUpdate = it
            }
        }
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
                                        if (academicSettings.getBoolean("portal_import_notice_v2_acknowledged", false)) {
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
                                classTimeSettings = classTimeSettings,
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
                                classTimeSettings = classTimeSettings,
                                showSetupNotice = showScheduleSetupNotice,
                                onSemesterStartDateChange = {
                                    semesterStartDate = it
                                    academicSettings.edit().putString("semester_start_date", it).apply()
                                },
                                onClassTimeProfileChange = {
                                    classTimePreferences.select(it)
                                    classTimeSettings = classTimePreferences.load()
                                    WidgetUpdateManager.requestUpdate(context)
                                },
                                onCustomClassTimesSave = {
                                    classTimePreferences.saveCustom(it)
                                    classTimeSettings = classTimePreferences.load()
                                    WidgetUpdateManager.requestUpdate(context)
                                },
                                onSetupNoticeDismiss = {
                                    academicSettings.edit()
                                        .putBoolean("schedule_setup_notice_acknowledged", true)
                                        .apply()
                                    showScheduleSetupNotice = false
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
    if (showWelcomeGuide && !launchAddTask) {
        val dismissWelcomeGuide = {
            academicSettings.edit()
                .putBoolean("welcome_guide_v1_acknowledged", true)
                .apply()
            showWelcomeGuide = false
        }
        AlertDialog(
            onDismissRequest = dismissWelcomeGuide,
            confirmButton = {
                Button(onClick = dismissWelcomeGuide) { Text("开始使用") }
            },
            title = {
                Text(
                    "欢迎使用课枢",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    WelcomeGuideStep(
                        icon = Icons.Default.Sync,
                        title = "1. 先同步教务数据",
                        body = "点击首页右上角的“同步”，登录教务系统并开始导入。",
                    )
                    WelcomeGuideStep(
                        icon = Icons.Default.Widgets,
                        title = "2. 把课枢放到桌面",
                        body = "长按手机桌面空白处，点击“小部件”或“添加小部件”，找到“课枢”，再把“课枢待办”或“课枢课程”拖到桌面。不同手机的入口名称可能略有不同。",
                    )
                }
            },
        )
    } else if (availableUpdate != null && !launchAddTask) {
        val update = requireNotNull(availableUpdate)
        AlertDialog(
            onDismissRequest = { availableUpdate = null },
            confirmButton = {
                Button(
                    onClick = {
                        availableUpdate = null
                        uriHandler.openUri(update.releasePageUrl)
                    },
                ) {
                    Text("前往下载")
                }
            },
            dismissButton = {
                TextButton(onClick = { availableUpdate = null }) { Text("稍后") }
            },
            title = { Text("发现新版本 ${update.version}") },
            text = {
                Text("当前版本为 ${BuildConfig.VERSION_NAME}。建议下载并安装最新版本，以获得功能改进和问题修复。")
            },
        )
    }
    if (showPortalPrivacyNotice) {
        AlertDialog(
            onDismissRequest = { showPortalPrivacyNotice = false },
            confirmButton = {
                Button(
                    onClick = {
                        academicSettings.edit().putBoolean("portal_import_notice_v2_acknowledged", true).apply()
                        showPortalPrivacyNotice = false
                        openPortal()
                    },
                ) { Text("继续登录") }
            },
            dismissButton = {
                TextButton(onClick = { showPortalPrivacyNotice = false }) { Text("取消") }
            },
            title = {
                Text(
                    "注意事项！",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error,
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("1. 先登陆学号密码，继续点击登录 Login。")
                    Image(
                        painter = painterResource(R.drawable.login_button_hint),
                        contentDescription = "学校登录页面中的登录 Login 按钮",
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(478f / 90f)
                            .clip(MaterialTheme.shapes.small),
                        contentScale = ContentScale.FillWidth,
                    )
                    Text("2. 为确保学分树正常，请先点击“教务系统 → 学业完成查询 → 重新计算”，等待计算完成。")
                    Text(
                        "登录在学校网页内完成。课枢只读取你主动同步的培养方案、课程、课表和考试信息并保存在本机，不会保存教务密码。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
        )
    }
}

@Composable
private fun WelcomeGuideStep(
    icon: ImageVector,
    title: String,
    body: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Surface(
            modifier = Modifier.size(38.dp),
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(
                body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
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
