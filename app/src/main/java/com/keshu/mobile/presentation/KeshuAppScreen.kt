package com.keshu.mobile.presentation

import android.content.Intent
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.viewmodel.compose.viewModel
import com.keshu.mobile.data.AppContainer
import com.keshu.mobile.data.local.ExamAvailability
import com.keshu.mobile.data.local.entity.ClassSessionEntity
import com.keshu.mobile.data.local.entity.CourseEntity
import com.keshu.mobile.data.local.entity.CourseStatus
import com.keshu.mobile.data.local.entity.ExamEntity
import com.keshu.mobile.data.local.entity.TaskEntity
import com.keshu.mobile.domain.model.CreditTree
import com.keshu.mobile.domain.model.CourseAdvice
import com.keshu.mobile.domain.model.CourseRecommendation
import com.keshu.mobile.domain.model.LargeGroupProgress
import com.keshu.mobile.domain.model.SmallGroupProgress
import com.keshu.mobile.presentation.dashboard.DashboardUiState
import com.keshu.mobile.presentation.dashboard.DashboardViewModel
import com.keshu.mobile.presentation.dashboard.DashboardViewModelFactory
import com.keshu.mobile.presentation.sync.ManualSyncActivity
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
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
                Crossfade(
                    targetState = selectedTab,
                    label = "main-tab",
                    modifier = Modifier.weight(1f),
                ) { tab ->
                    ScreenShell(constrainWidth = tab != MainTab.Schedule) {
                when (tab) {
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

@Composable
private fun HomePage(
    state: DashboardUiState,
    currentWeek: Int?,
    onAddTask: (String, Long, Boolean) -> Unit,
    onUpdateTask: (TaskEntity, String, Long, Boolean) -> Unit,
    onSetTaskDone: (TaskEntity, Boolean) -> Unit,
    onDeleteTask: (TaskEntity) -> Unit,
    addTaskRequestId: Long,
    onAddTaskRequestConsumed: (Long) -> Unit,
    examAvailability: ExamAvailability?,
    onRequestNotificationPermission: ((Boolean) -> Unit) -> Unit,
) {
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<TaskEntity?>(null) }
    var deletingTask by remember { mutableStateOf<TaskEntity?>(null) }
    LaunchedEffect(addTaskRequestId) {
        if (addTaskRequestId != 0L) {
            showAddTaskDialog = true
            onAddTaskRequestConsumed(addTaskRequestId)
        }
    }
    val todayDayOfWeek = remember { LocalDate.now().dayOfWeek.value }
    val todaySessions = remember(state.schedule.classSessions, currentWeek, todayDayOfWeek) {
        state.schedule.classSessions
            .filter { it.dayOfWeek == todayDayOfWeek && it.occursInWeek(currentWeek) }
            .sortedBy { it.startSection }
    }
    val nextSession = remember(todaySessions) {
        val currentMinutes = java.time.LocalTime.now().let { it.hour * 60 + it.minute }
        todaySessions.firstOrNull { session ->
            scheduleSectionTime(session.endSection)
                ?.second
                ?.split(":")
                ?.let { parts -> parts[0].toInt() * 60 + parts[1].toInt() >= currentMinutes }
                ?: true
        }
    }
    val laterSessions = remember(todaySessions, nextSession) {
        todaySessions.filterNot { it.id == nextSession?.id }
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 116.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
            item {
                HeroCard(
                    title = if (nextSession == null) "今日安排" else "下一节课",
                    value = nextSession?.courseName ?: "今天没有更多课程",
                    unit = "",
                    caption = nextSession?.let { session ->
                        val start = scheduleSectionTime(session.startSection)?.first.orEmpty()
                        val end = scheduleSectionTime(session.endSection)?.second.orEmpty()
                        listOf("$start–$end".trim('–'), session.location)
                            .filter { it.isNotBlank() }
                            .joinToString(" · ")
                    } ?: "可以安排复习、运动或休息",
                    progress = if (nextSession == null) 0f else 1f,
                )
            }
            item { SectionHeader("今日剩余课程", currentWeek?.let { "第 $it 周" } ?: "按当天显示") }
            if (laterSessions.isEmpty()) {
                item { EmptyCard("后面没有课程", "今天剩余时间可以自由安排。") }
            } else {
                items(laterSessions, key = { it.id }) { session -> TodayCourseRow(session) }
            }
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("待办任务", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "${state.schedule.tasks.count { !it.done }} 项待完成",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Button(onClick = { showAddTaskDialog = true }, shape = RoundedCornerShape(14.dp)) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("添加")
                    }
                }
            }
            if (state.schedule.tasks.isEmpty()) {
                item { EmptyCard("暂无待办", "点击右侧“添加”，创建你的第一个待办。") }
            } else {
                items(state.schedule.tasks, key = { it.id }) { task ->
                    TaskRow(
                        task = task,
                        onDoneChange = { onSetTaskDone(task, it) },
                        onEdit = { editingTask = task },
                        onDelete = { deletingTask = task },
                    )
                }
            }
            item {
                SectionHeader(
                    "考试安排",
                    if (state.schedule.exams.isEmpty() && examAvailability != null) {
                        "当前不可查看"
                    } else {
                        "${state.schedule.exams.size} 场"
                    },
                )
            }
            if (state.schedule.exams.isEmpty()) {
                item {
                    if (examAvailability != null) {
                        EmptyCard("考试安排当前不可查看", examAvailability.detailMessage())
                    } else {
                        EmptyCard("暂无考试安排", "同步教务数据后，考试时间和地点会显示在这里。")
                    }
                }
            } else {
                items(state.schedule.exams, key = { it.id }) { exam -> ExamCard(exam) }
            }
    }
    if (showAddTaskDialog) {
        TaskEditorDialog(
            task = null,
            onDismiss = { showAddTaskDialog = false },
            onSave = { title, dueAtMillis, remindersEnabled ->
                if (remindersEnabled) {
                    onRequestNotificationPermission { granted ->
                        onAddTask(title, dueAtMillis, granted)
                        showAddTaskDialog = false
                    }
                } else {
                    onAddTask(title, dueAtMillis, false)
                    showAddTaskDialog = false
                }
            },
        )
    }
    editingTask?.let { task ->
        TaskEditorDialog(
            task = task,
            onDismiss = { editingTask = null },
            onSave = { title, dueAtMillis, remindersEnabled ->
                if (remindersEnabled) {
                    onRequestNotificationPermission { granted ->
                        onUpdateTask(task, title, dueAtMillis, granted)
                        editingTask = null
                    }
                } else {
                    onUpdateTask(task, title, dueAtMillis, false)
                    editingTask = null
                }
            },
        )
    }
    deletingTask?.let { task ->
        AlertDialog(
            onDismissRequest = { deletingTask = null },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteTask(task)
                    deletingTask = null
                }) { Text("删除") }
            },
            dismissButton = { TextButton(onClick = { deletingTask = null }) { Text("取消") } },
            title = { Text("删除待办") },
            text = { Text("确定删除“${task.title}”吗？") },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskEditorDialog(
    task: TaskEntity?,
    onDismiss: () -> Unit,
    onSave: (String, Long, Boolean) -> Unit,
) {
    var title by remember(task?.id) { mutableStateOf(task?.title.orEmpty()) }
    var saving by remember(task?.id) { mutableStateOf(false) }
    var dueDate by remember(task?.id) {
        mutableStateOf(
            task?.let { java.time.Instant.ofEpochMilli(it.dueAtMillis).atZone(ZoneId.systemDefault()).toLocalDate() }
                ?: LocalDate.now().plusDays(1),
        )
    }
    var showDatePicker by remember { mutableStateOf(false) }
    var remindersEnabled by remember(task?.id) { mutableStateOf(task?.remindersEnabled ?: true) }
    val dueAtMillis = dueDate.atTime(23, 59).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                enabled = !saving && title.isNotBlank() && (task != null || dueAtMillis > System.currentTimeMillis()),
                onClick = {
                    if (!saving) {
                        saving = true
                        onSave(title, dueAtMillis, remindersEnabled)
                    }
                },
            ) { Text(if (task == null) "添加" else "保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
        title = { Text(if (task == null) "添加待办任务" else "编辑待办任务") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("任务标题") },
                    singleLine = true,
                )
                Button(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("截止日期 ${dueDate.format(DateTimeFormatter.ISO_LOCAL_DATE)}")
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("截止提醒", style = MaterialTheme.typography.titleSmall)
                        Text(
                            "在截止前 24 小时和 2 小时通知",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = remindersEnabled, onCheckedChange = { remindersEnabled = it })
                }
            }
        },
    )
    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = dueDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let {
                        dueDate = java.time.Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate()
                    }
                    showDatePicker = false
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("取消") } },
        ) { DatePicker(state = pickerState) }
    }
}

@Composable
private fun CreditPage(state: DashboardUiState) {
    val tree = state.creditTree
    var includeTakingProjection by remember { mutableStateOf(false) }
    var expandedLargeGroups by remember { mutableStateOf(setOf<String>()) }
    var expandedSmallGroups by remember { mutableStateOf(setOf<String>()) }
    val earnedCredits = tree?.graduation?.earnedCredits ?: 0.0
    val takingCredits = state.creditSummary.takingCredits
    val displayedCredits = earnedCredits + if (includeTakingProjection) takingCredits else 0.0
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 116.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            CreditOverview(
                earnedCredits = earnedCredits,
                takingCredits = takingCredits,
                displayedCredits = displayedCredits,
                requiredCredits = tree?.graduation?.requiredCredits,
                includeTakingProjection = includeTakingProjection,
                onProjectionChange = { includeTakingProjection = it },
            )
        }
        item { SectionHeader("学分树", "点开模块查看课程") }
        if (tree == null) {
            item { EmptyCard("暂无学分数据", "同步培养方案和详细课程后，学分进度会显示在这里。") }
        } else {
            items(tree.largeGroups, key = { it.id }) { group ->
                LargeGroupCard(
                    group = group,
                    expanded = group.id in expandedLargeGroups,
                    expandedSmallGroups = expandedSmallGroups,
                    onToggle = { expandedLargeGroups = expandedLargeGroups.toggle(group.id) },
                    onToggleSmallGroup = { groupId -> expandedSmallGroups = expandedSmallGroups.toggle(groupId) },
                )
            }
        }
    }
}

@Composable
private fun CreditOverview(
    earnedCredits: Double,
    takingCredits: Double,
    displayedCredits: Double,
    requiredCredits: Double?,
    includeTakingProjection: Boolean,
    onProjectionChange: (Boolean) -> Unit,
) {
    val remainingCredits = requiredCredits?.let { (it - displayedCredits).coerceAtLeast(0.0) }
    val progress = if (requiredCredits == null || requiredCredits <= 0.0) {
        0f
    } else {
        (displayedCredits / requiredCredits).toFloat().coerceIn(0f, 1f)
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    if (includeTakingProjection) "预计毕业要求进度" else "毕业要求进度",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        requiredCredits?.let { displayedCredits.formatCredit() } ?: "--",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        requiredCredits?.let { " / ${it.formatCredit()} 学分" } ?: " 学分",
                        modifier = Modifier.padding(start = 6.dp, bottom = 4.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(99.dp)),
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            BoxWithConstraints(Modifier.fillMaxWidth()) {
                val stackMetrics = maxWidth < 300.dp || LocalDensity.current.fontScale >= 1.3f
                if (stackMetrics) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        CreditMetricRow("已获得", earnedCredits.formatCredit())
                        CreditMetricRow("本学期", takingCredits.formatCredit())
                        CreditMetricRow("仍需", remainingCredits?.formatCredit() ?: "--")
                    }
                } else {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        CreditMetric("已获得", earnedCredits.formatCredit())
                        CreditMetric("本学期", takingCredits.formatCredit())
                        CreditMetric("仍需", remainingCredits?.formatCredit() ?: "--")
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.65f))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = includeTakingProjection,
                        role = Role.Switch,
                        onValueChange = onProjectionChange,
                    ),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("计入本学期课程", style = MaterialTheme.typography.titleSmall)
                    Text(
                        "假设在修课程全部通过",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = includeTakingProjection, onCheckedChange = null)
            }
        }
    }
}

@Composable
private fun CreditMetric(label: String, value: String) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun CreditMetricRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun SchedulePage(
    state: DashboardUiState,
    semesterStartDate: String,
    onSemesterStartDateChange: (String) -> Unit,
    onSessionSave: (ClassSessionEntity) -> Unit,
) {
    var selectedTerm by remember { mutableStateOf<String?>(null) }
    var selectedWeek by remember { mutableStateOf<Int?>(null) }
    var selectedSession by remember { mutableStateOf<ClassSessionEntity?>(null) }
    var showWeekPicker by remember { mutableStateOf(false) }
    val drawerState = rememberDrawerState(initialValue = androidx.compose.material3.DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val currentWeek = remember(semesterStartDate) { weekFromSemesterStart(semesterStartDate) }
    val terms = remember(state.schedule.classSessions) {
        state.schedule.classSessions.map { it.term }.distinct().sortedDescending()
    }
    val activeTerm = selectedTerm?.takeIf { it in terms } ?: terms.firstOrNull()
    val termSessions = state.schedule.classSessions
        .filter { activeTerm == null || it.term == activeTerm }
        .sortedWith(compareBy<ClassSessionEntity> { it.dayOfWeek }.thenBy { it.startSection })
    val weekOptions = remember(termSessions, currentWeek) {
        val maxWeek = termSessions.flatMap { it.weekNumbers() }.maxOrNull()?.coerceAtLeast(20) ?: 20
        (1..maxWeek.coerceAtLeast(currentWeek ?: 1)).toList()
    }
    val activeWeek = selectedWeek?.takeIf { it in weekOptions } ?: currentWeek?.takeIf { it in weekOptions } ?: weekOptions.firstOrNull()
    val displayedSessions = remember(termSessions, activeWeek) {
        termSessions.filter { it.occursInWeek(activeWeek) }
    }
    val selectAdjacentWeek: (Int) -> Unit = { offset ->
        val currentIndex = weekOptions.indexOf(activeWeek)
        weekOptions.getOrNull(currentIndex + offset)?.let { selectedWeek = it }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.fillMaxWidth(0.9f).widthIn(max = 360.dp),
                drawerContainerColor = MaterialTheme.colorScheme.surface,
                drawerContentColor = MaterialTheme.colorScheme.onSurface,
            ) {
                LazyColumn(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    item {
                        Text("课表设置", style = MaterialTheme.typography.headlineSmall)
                        Text(
                            "管理开学日期和学期课表",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    item {
                        SemesterStartCard(
                            value = semesterStartDate,
                            currentWeek = currentWeek,
                            onValueChange = onSemesterStartDateChange,
                        )
                    }
                    if (terms.isNotEmpty()) {
                        item { Text("学期课表", style = MaterialTheme.typography.titleMedium) }
                        items(terms, key = { it }) { term ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(role = Role.Button) { selectedTerm = term },
                                shape = RoundedCornerShape(14.dp),
                                color = if (term == activeTerm) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                                border = BorderStroke(
                                    1.dp,
                                    if (term == activeTerm) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.outline
                                    },
                                ),
                            ) {
                                Text(
                                    term,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (term == activeTerm) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                )
                            }
                        }
                    }
                }
            }
        },
    ) {
        Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            ScheduleTopHeader(
                activeWeek = activeWeek,
                activeTerm = activeTerm,
                onOpenDrawer = { scope.launch { drawerState.open() } },
                onSelectWeek = { showWeekPicker = true },
            )
            if (termSessions.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "暂无课程，完成课表同步后会显示在这里",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                ScheduleWeekGrid(
                    modifier = Modifier.weight(1f),
                    sessions = displayedSessions,
                    selectedWeek = activeWeek,
                    semesterStartDate = semesterStartDate,
                    onPreviousWeek = { selectAdjacentWeek(-1) },
                    onNextWeek = { selectAdjacentWeek(1) },
                    onSessionClick = { selectedSession = it },
                )
            }
        }
    }
    if (showWeekPicker) {
        ScheduleWeekPickerDialog(
            weeks = weekOptions,
            selectedWeek = activeWeek,
            onSelected = {
                selectedWeek = it
                showWeekPicker = false
            },
            onDismiss = { showWeekPicker = false },
        )
    }
    selectedSession?.let { session ->
        ClassSessionDetailDialog(
            session = session,
            onDismiss = { selectedSession = null },
            onSave = {
                onSessionSave(it)
                selectedSession = null
            },
        )
    }
}

@Composable
private fun ExamPage(
    state: DashboardUiState,
    examAvailability: ExamAvailability?,
    onScheduleAlarms: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 116.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (state.schedule.exams.isEmpty()) {
            item {
                if (examAvailability != null) {
                    ExamUnavailablePanel(examAvailability)
                } else {
                    EmptyCard("暂无考试", "同步考试安排后会显示倒计时、地点和座位。")
                }
            }
        } else {
            item {
                ExamSummaryBar(
                    count = state.schedule.exams.size,
                    onScheduleAlarms = onScheduleAlarms,
                )
            }
            item { SectionHeader("考试列表", "时间、地点与座位") }
            items(state.schedule.exams.sortedBy { it.startsAtMillis }, key = { it.id }) { exam ->
                ExamCard(exam)
            }
        }
    }
}

@Composable
private fun ExamUnavailablePanel(availability: ExamAvailability) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            IconBubble(Icons.Default.Event)
            Text("考试安排当前不可查看", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                availability.detailMessage(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.65f),
            ) {
                Text(
                    "这不是 0 场考试。开放查看后重新同步即可，现有课表与培养方案数据不会被清除。",
                    modifier = Modifier.padding(14.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }
    }
}

@Composable
private fun ExamSummaryBar(count: Int, onScheduleAlarms: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("$count 场考试", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("可为即将到来的考试创建本地通知", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(onClick = onScheduleAlarms, shape = RoundedCornerShape(14.dp)) {
                Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("提醒")
            }
        }
    }
}

@Composable
private fun AdvisorIntro() {
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        IconBubble(Icons.Default.AutoAwesome)
        Text("从培养方案缺口开始", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(
            "顾问会读取设备内的培养方案、已修和在修课程，生成带理由的选课建议。结果仅供规划参考。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(7.dp))
            Text("API Key 不会写入本地存储", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AdvisorPage(state: DashboardUiState, onRequestAdvice: (String) -> Unit) {
    var apiKey by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 116.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            AdvisorIntro()
        }
        item {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("连接 DeepSeek", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("DeepSeek API Key") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                    )
                    Text(
                        "API Key 仅用于本次请求，不会保存在本地。点击生成后，培养方案、已修与在修课程和本地筛选结果会发送给 DeepSeek。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(
                        onClick = { onRequestAdvice(apiKey.trim()) },
                        enabled = apiKey.isNotBlank() && !state.advising,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        if (state.advising) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        } else {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(if (state.advising) "生成中" else "生成建议")
                    }
                }
            }
        }
        if (state.error != null) {
            item { EmptyCard("生成失败", state.error.orEmpty()) }
        }
        val advice = state.advice
        if (advice == null) {
            item { EmptyCard("尚未生成建议", "填写 DeepSeek API Key 后直接生成，系统会自动计入已修与在修课程。") }
        } else {
            item { AdviceSummaryCard(advice) }
            if (advice.recommendations.isEmpty()) {
                item { EmptyCard("暂无可推荐课程", "当前没有符合培养方案缺口的未修课程，请检查详细课程数据。") }
            } else {
                item { SectionHeader("推荐课程", "${advice.recommendations.size} 门") }
                items(advice.recommendations, key = { "${it.courseCode}-${it.groupName}" }) { recommendation ->
                    RecommendationCard(recommendation)
                }
            }
        }
    }
}

@Composable
private fun CourseStatusRow(course: CourseEntity, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
    ) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(course.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(
                    listOfNotNull(course.code.takeIf { it.isNotBlank() }, course.term, "${course.credit.formatCredit()} 学分").joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Surface(
                shape = RoundedCornerShape(99.dp),
                color = course.status.statusColor().copy(alpha = 0.16f),
            ) {
                Text(
                    course.status.statusLabel(),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = course.status.statusColor(),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SemesterStartCard(
    value: String,
    currentWeek: Int?,
    onValueChange: (String) -> Unit,
) {
    var showPicker by remember { mutableStateOf(false) }
    val selectedMillis = remember(value) {
        runCatching {
            LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        }.getOrNull()
    }
    val pickerState = rememberDatePickerState(initialSelectedDateMillis = selectedMillis)
    ElevatedCard(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("开学日期", style = MaterialTheme.typography.titleMedium)
                    Text(
                        currentWeek?.let { "$value · 当前第 $it 周" } ?: value,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Button(
                    onClick = { showPicker = true },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                ) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("选择日期")
                }
            }
        }
    }
    if (showPicker) {
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            val date = java.time.Instant.ofEpochMilli(millis)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()
                            onValueChange(date.format(DateTimeFormatter.ISO_LOCAL_DATE))
                        }
                        showPicker = false
                    },
                ) {
                    Text("确定")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("取消") }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
private fun ScheduleTopHeader(
    activeWeek: Int?,
    activeTerm: String?,
    onOpenDrawer: () -> Unit,
    onSelectWeek: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        BoxWithConstraints {
            val showSwipeHint = maxWidth >= 420.dp && LocalDensity.current.fontScale < 1.3f
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.statusBars)
                    .padding(start = 10.dp, end = 18.dp, top = 14.dp, bottom = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surfaceVariant) {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(
                            Icons.Default.Menu,
                            contentDescription = "打开课表侧栏",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(role = Role.Button, onClick = onSelectWeek)
                        .semantics {
                            contentDescription = activeWeek?.let { "选择周数，当前第 $it 周" } ?: "选择周数"
                        },
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            activeWeek?.let { "第${it}周" } ?: "课表",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Icon(
                            Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        activeTerm ?: "暂无学期",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (showSwipeHint) {
                    Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.primaryContainer) {
                        Text(
                            "左右滑动切周",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScheduleWeekPickerDialog(
    weeks: List<Int>,
    selectedWeek: Int?,
    onSelected: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("取消") } },
        title = { Text("选择周数") },
        text = {
            LazyColumn(modifier = Modifier.height(420.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(weeks, key = { it }) { week ->
                    Surface(
                        modifier = Modifier.fillMaxWidth().clickable(role = Role.Button) { onSelected(week) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (week == selectedWeek) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                    ) {
                        Text(
                            "第 $week 周",
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            color = if (week == selectedWeek) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        },
    )
}

@Composable
private fun ScheduleWeekGrid(
    modifier: Modifier = Modifier,
    sessions: List<ClassSessionEntity>,
    selectedWeek: Int?,
    semesterStartDate: String,
    onPreviousWeek: () -> Unit,
    onNextWeek: () -> Unit,
    onSessionClick: (ClassSessionEntity) -> Unit,
) {
    val maxSection = sessions.maxOfOrNull { it.endSection }?.coerceAtLeast(12) ?: 12
    val weekDates = remember(semesterStartDate, selectedWeek) { scheduleWeekDates(semesterStartDate, selectedWeek) }
    val dayNames = listOf("一", "二", "三", "四", "五", "六", "日")
    val sectionHeight = 68.dp
    val today = remember { LocalDate.now() }
    val verticalScroll = rememberScrollState()
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .pointerInput(selectedWeek) {
                var totalDrag = 0f
                detectHorizontalDragGestures(
                    onDragStart = { totalDrag = 0f },
                    onDragEnd = {
                        if (totalDrag > 60f) onPreviousWeek()
                        if (totalDrag < -60f) onNextWeek()
                    },
                ) { _, dragAmount ->
                    totalDrag += dragAmount
                }
            },
    ) {
        val timeWidth = 58.dp
        val dayWidth = ((maxWidth - timeWidth) / 7).coerceAtLeast(38.dp)
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(76.dp)
                    .background(MaterialTheme.colorScheme.surface),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.width(timeWidth),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        weekDates.firstOrNull()?.monthValue?.toString().orEmpty(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text("月", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                dayNames.forEachIndexed { index, name ->
                    val date = weekDates.getOrNull(index)
                    ScheduleDayHeader(
                        dayName = name,
                        dayNumber = date?.dayOfMonth?.toString().orEmpty(),
                        selected = date == today,
                        width = dayWidth,
                    )
                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(verticalScroll)
                    .background(MaterialTheme.colorScheme.surface),
            ) {
                Box(Modifier.fillMaxWidth().requiredHeight(sectionHeight * maxSection)) {
                    Column(Modifier.width(timeWidth)) {
                        (1..maxSection).forEach { section ->
                            ScheduleTimeCell(section, sectionHeight)
                        }
                    }
                    Row(Modifier.padding(start = timeWidth)) {
                        dayNames.indices.forEach { dayIndex ->
                            Box(Modifier.width(dayWidth).requiredHeight(sectionHeight * maxSection)) {
                                (1..maxSection).forEach { section ->
                                    ScheduleEmptySlot(sectionHeight, dayWidth, section)
                                }
                                sessions
                                    .filter { it.dayOfWeek == dayIndex + 1 }
                                    .forEach { session ->
                                        ScheduleSessionBlock(
                                            session = session,
                                            dayWidth = dayWidth,
                                            sectionHeight = sectionHeight,
                                            onClick = { onSessionClick(session) },
                                        )
                                    }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ScheduleDayHeader(
    dayName: String,
    dayNumber: String,
    selected: Boolean,
    width: androidx.compose.ui.unit.Dp,
) {
    Column(
        modifier = Modifier.width(width),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            dayName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Surface(
            modifier = Modifier.size(38.dp),
            shape = RoundedCornerShape(9.dp),
            color = if (selected) MaterialTheme.colorScheme.primary else Color.Transparent,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    dayNumber,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ScheduleTimeCell(section: Int, sectionHeight: androidx.compose.ui.unit.Dp) {
    Box(modifier = Modifier.width(64.dp).height(sectionHeight).padding(top = 10.dp), contentAlignment = Alignment.TopCenter) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                section.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            scheduleSectionTime(section)?.let { times ->
                Text(times.first, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(times.second, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ScheduleEmptySlot(
    sectionHeight: androidx.compose.ui.unit.Dp,
    dayWidth: androidx.compose.ui.unit.Dp,
    section: Int,
) {
    Surface(
        modifier = Modifier
            .offset(y = sectionHeight * (section - 1))
            .width(dayWidth)
            .height(sectionHeight),
        color = Color.Transparent,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.72f)),
    ) {}
}

@Composable
private fun ScheduleSessionBlock(
    session: ClassSessionEntity,
    dayWidth: androidx.compose.ui.unit.Dp,
    sectionHeight: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
) {
    val color = session.courseCode.scheduleCourseColor()
    val span = (session.endSection - session.startSection + 1).coerceAtLeast(1)
    Surface(
        modifier = Modifier
            .offset(y = sectionHeight * (session.startSection - 1))
            .width(dayWidth)
            .height(sectionHeight * span - 8.dp)
            .padding(horizontal = 2.dp, vertical = 4.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics {
                contentDescription = buildString {
                    append(session.courseName)
                    append("，周${session.dayOfWeek}第${session.startSection}至${session.endSection}节")
                    if (session.location.isNotBlank()) append("，${session.location}")
                }
            },
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.18f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.48f)),
    ) {
        Column(Modifier.padding(horizontal = 5.dp, vertical = 7.dp)) {
            Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(99.dp)).background(color))
            Spacer(Modifier.height(6.dp))
            Text(
                session.courseName,
                fontSize = 10.sp,
                lineHeight = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(5.dp))
            Text(
                session.location.ifBlank { "教室待定" },
                fontSize = 9.sp,
                lineHeight = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun HeroCard(
    title: String,
    value: String,
    unit: String,
    caption: String,
    progress: Float,
    action: (@Composable () -> Unit)? = null,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shadowElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    value,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                )
                if (unit.isNotBlank()) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        unit,
                        modifier = Modifier.padding(bottom = 4.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(99.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
            Text(
                caption,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (action != null) action()
        }
    }
}

@Composable
private fun SectionHeader(title: String, subtitle: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(subtitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun TodayCourseRow(session: ClassSessionEntity) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = session.courseCode.courseColor().copy(alpha = 0.16f),
        border = BorderStroke(1.dp, session.courseCode.courseColor().copy(alpha = 0.28f)),
    ) {
        MiniInfoRow(
            icon = Icons.Default.CalendarMonth,
            title = session.courseName,
            subtitle = "第 ${session.startSection}-${session.endSection} 节 · ${session.location}",
            modifier = Modifier.fillMaxWidth().padding(14.dp),
        )
    }
}

@Composable
private fun LargeGroupCard(
    group: LargeGroupProgress,
    expanded: Boolean,
    expandedSmallGroups: Set<String>,
    onToggle: () -> Unit,
    onToggleSmallGroup: (String) -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onToggle) {
                    Icon(
                        if (expanded) Icons.Default.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = if (expanded) "收起${group.name}" else "展开${group.name}",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(group.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${group.earnedCredits.formatCredit()} / ${group.requiredCredits.formatCredit()} 学分", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("${group.metSmallGroupCount}/${group.smallGroupCount}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
            LinearProgressIndicator(
                progress = { group.progressFraction() },
                modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(99.dp)),
            )
            if (expanded) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.65f))
                group.childGroups.forEach { child ->
                    SmallGroupTree(
                        group = child,
                        expandedSmallGroups = expandedSmallGroups,
                        onToggle = onToggleSmallGroup,
                    )
                }
            }
        }
    }
}

@Composable
private fun SmallGroupTree(
    group: SmallGroupProgress,
    expandedSmallGroups: Set<String>,
    onToggle: (String) -> Unit,
) {
    val expanded = group.id in expandedSmallGroups
    val hasChildren = group.childGroups.isNotEmpty() || group.courses.isNotEmpty()
    Column(
        modifier = Modifier.padding(start = ((group.depth - 1).coerceAtLeast(0) * 14).dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { if (hasChildren) onToggle(group.id) },
                    enabled = hasChildren,
                ) {
                    Icon(
                        if (expanded) Icons.Default.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = if (!hasChildren) null else if (expanded) "收起${group.name}" else "展开${group.name}",
                        tint = if (hasChildren) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(group.name, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        "${group.earnedCredits.formatCredit()} / ${group.requiredCredits.formatCredit()} 学分 · ${group.courses.size} 门课程",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    if (group.met) "达标" else "差 ${group.gapCredits.formatCredit()}",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (group.met) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                )
        }
        if (expanded) {
            group.childGroups.forEach { child ->
                SmallGroupTree(
                    group = child,
                    expandedSmallGroups = expandedSmallGroups,
                    onToggle = onToggle,
                )
            }
            group.courses
                .sortedWith(compareBy<CourseEntity> { it.status.sortOrder() }.thenBy { it.name })
                .forEach { course ->
                    CourseStatusRow(
                        course = course,
                        modifier = Modifier.padding(start = 18.dp),
                    )
                }
        }
    }
}

@Composable
private fun ClassSessionCard(session: ClassSessionEntity, onClick: () -> Unit = {}) {
    ElevatedCard(
        modifier = Modifier
            .clickable(role = Role.Button, onClick = onClick)
            .semantics {
                contentDescription = buildString {
                    append(session.courseName)
                    append("，周${session.dayOfWeek}第${session.startSection}至${session.endSection}节")
                    if (session.location.isNotBlank()) append("，${session.location}")
                    append("，点击查看或编辑详情")
                }
            },
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f)),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                TimePill("周${session.dayOfWeek}", "${session.startSection}-${session.endSection}")
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(session.courseName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(session.location, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            MiniInfoRow(Icons.Default.LocationOn, "点击查看或编辑详情", "${session.weeksText} · 第 ${session.startSection}-${session.endSection} 节")
        }
    }
}

@Composable
private fun FilterChipRow(
    values: List<String>,
    selected: String?,
    label: (String) -> String,
    onSelected: (String) -> Unit,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(values, key = { it }) { value ->
            FilterChip(
                selected = selected == value,
                onClick = { onSelected(value) },
                label = { Text(label(value), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        }
    }
}

@Composable
private fun WeekPickerRow(
    weeks: List<Int>,
    selectedWeek: Int?,
    onWeekSelected: (Int) -> Unit,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(weeks, key = { it }) { week ->
            FilterChip(
                selected = selectedWeek == week,
                onClick = { onWeekSelected(week) },
                label = { Text("第 $week 周") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        }
    }
}

@Composable
private fun ClassSessionDetailDialog(
    session: ClassSessionEntity,
    onDismiss: () -> Unit,
    onSave: (ClassSessionEntity) -> Unit,
) {
    var term by remember(session.id) { mutableStateOf(session.term) }
    var courseName by remember(session.id) { mutableStateOf(session.courseName) }
    var teacher by remember(session.id) { mutableStateOf(session.teacher) }
    var weeksText by remember(session.id) { mutableStateOf(session.weeksText) }
    var dayOfWeek by remember(session.id) { mutableStateOf(session.dayOfWeek.toString()) }
    var startSection by remember(session.id) { mutableStateOf(session.startSection.toString()) }
    var endSection by remember(session.id) { mutableStateOf(session.endSection.toString()) }
    var location by remember(session.id) { mutableStateOf(session.location) }
    var className by remember(session.id) { mutableStateOf(session.className) }
    val parsedDay = dayOfWeek.toIntOrNull()?.coerceIn(1, 7)
    val parsedStart = startSection.toIntOrNull()?.coerceAtLeast(1)
    val parsedEnd = endSection.toIntOrNull()?.coerceAtLeast(parsedStart ?: 1)

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = courseName.isNotBlank() && parsedDay != null && parsedStart != null && parsedEnd != null,
                onClick = {
                    onSave(
                        session.copy(
                            term = term.trim().ifBlank { session.term },
                            courseName = courseName.trim(),
                            teacher = teacher.trim(),
                            weeksText = weeksText.trim().ifBlank { "全周" },
                            dayOfWeek = parsedDay ?: session.dayOfWeek,
                            startSection = parsedStart ?: session.startSection,
                            endSection = parsedEnd ?: session.endSection,
                            location = location.trim(),
                            className = className.trim(),
                        ),
                    )
                },
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("课程详情")
            }
        },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item { OutlinedTextField(courseName, { courseName = it }, modifier = Modifier.fillMaxWidth(), label = { Text("课程名称") }, singleLine = true) }
                item { OutlinedTextField(location, { location = it }, modifier = Modifier.fillMaxWidth(), label = { Text("教室地点") }, singleLine = true) }
                item { OutlinedTextField(term, { term = it }, modifier = Modifier.fillMaxWidth(), label = { Text("学期") }, singleLine = true) }
                item { OutlinedTextField(weeksText, { weeksText = it }, modifier = Modifier.fillMaxWidth(), label = { Text("周次说明") }, singleLine = true) }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(dayOfWeek, { dayOfWeek = it.filter(Char::isDigit).take(1) }, modifier = Modifier.weight(1f), label = { Text("星期") }, singleLine = true)
                        OutlinedTextField(startSection, { startSection = it.filter(Char::isDigit).take(2) }, modifier = Modifier.weight(1f), label = { Text("开始") }, singleLine = true)
                        OutlinedTextField(endSection, { endSection = it.filter(Char::isDigit).take(2) }, modifier = Modifier.weight(1f), label = { Text("结束") }, singleLine = true)
                    }
                }
                item { OutlinedTextField(teacher, { teacher = it }, modifier = Modifier.fillMaxWidth(), label = { Text("教师") }, singleLine = true) }
                item { OutlinedTextField(className, { className = it }, modifier = Modifier.fillMaxWidth(), label = { Text("教学班") }, singleLine = true) }
            }
        },
    )
}

@Composable
private fun ExamCard(exam: ExamEntity) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBubble(Icons.Default.Event)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(exam.courseName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(exam.startsAtMillis.formatDateTime(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            MiniInfoRow(Icons.Default.LocationOn, exam.location, exam.seatNo?.let { "座位 $it" } ?: "暂无座位号")
        }
    }
}

@Composable
private fun TaskRow(
    task: TaskEntity,
    onDoneChange: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (task.done) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.72f)),
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = task.done,
                onCheckedChange = onDoneChange,
                modifier = Modifier.semantics {
                    contentDescription = if (task.done) {
                        "将${task.title}标记为未完成"
                    } else {
                        "将${task.title}标记为已完成"
                    }
                },
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(role = Role.Button, onClick = onEdit)
                    .semantics { contentDescription = "编辑任务${task.title}" }
                    .padding(horizontal = 6.dp, vertical = 8.dp),
            ) {
                Text(
                    task.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = if (task.done) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (task.done) TextDecoration.LineThrough else TextDecoration.None,
                )
                Text(
                    "截止 ${task.dueAtMillis.formatDate()} · 点击编辑",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "删除任务${task.title}", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun AdviceSummaryCard(advice: CourseAdvice) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            MiniInfoRow(Icons.Default.AutoAwesome, "DeepSeek 建议", "基于培养方案、已修与在修课程")
            Text(advice.summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@Composable
private fun RecommendationCard(recommendation: CourseRecommendation) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(recommendation.courseName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        listOf(recommendation.courseCode, recommendation.groupName, "${recommendation.credit.formatCredit()} 学分")
                            .filter { it.isNotBlank() }
                            .joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Surface(shape = RoundedCornerShape(99.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
                    Text(
                        recommendation.priority,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }
            Text(recommendation.reason, style = MaterialTheme.typography.bodyMedium)
            Text(recommendation.feedback, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun EmptyCard(title: String, subtitle: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun MiniInfoRow(icon: ImageVector, title: String, subtitle: String, modifier: Modifier = Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        IconBubble(icon, small = true)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun IconBubble(icon: ImageVector, small: Boolean = false) {
    Surface(
        modifier = Modifier.size(if (small) 36.dp else 44.dp),
        shape = RoundedCornerShape(if (small) 12.dp else 15.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(if (small) 18.dp else 22.dp))
        }
    }
}

@Composable
private fun TimePill(day: String, sections: String) {
    Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(day, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
            Text(sections, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSecondaryContainer, fontWeight = FontWeight.Bold)
        }
    }
}

private fun Double.formatCredit(): String {
    return if (this % 1.0 == 0.0) toInt().toString() else String.format(Locale.CHINA, "%.1f", this)
}

private fun Long.formatDateTime(): String {
    return SimpleDateFormat("MM月dd日 HH:mm", Locale.CHINA).format(Date(this))
}

private fun Long.formatDate(): String {
    return SimpleDateFormat("MM月dd日", Locale.CHINA).format(Date(this))
}

private fun defaultSemesterStartDate(): String {
    val today = LocalDate.now()
    val year = today.year
    return if (today.monthValue >= 8) "$year-09-01" else "$year-03-01"
}

private fun weekFromSemesterStart(value: String): Int? {
    val start = runCatching { LocalDate.parse(value.trim(), DateTimeFormatter.ISO_LOCAL_DATE) }.getOrNull() ?: return null
    val days = ChronoUnit.DAYS.between(start, LocalDate.now())
    return if (days < 0) 1 else (days / 7 + 1).toInt().coerceAtLeast(1)
}

private fun scheduleWeekDates(semesterStartDate: String, selectedWeek: Int?): List<LocalDate> {
    val start = runCatching { LocalDate.parse(semesterStartDate.trim(), DateTimeFormatter.ISO_LOCAL_DATE) }
        .getOrNull()
        ?: LocalDate.now()
    val weekStart = start
        .plusWeeks(((selectedWeek ?: 1) - 1).toLong())
        .minusDays((start.dayOfWeek.value - 1).toLong())
    return (0..6).map { weekStart.plusDays(it.toLong()) }
}

private fun scheduleSectionTime(section: Int): Pair<String, String>? {
    return when (section) {
        1 -> "08:30" to "09:15"
        2 -> "09:25" to "10:10"
        3 -> "10:30" to "11:15"
        4 -> "11:25" to "12:10"
        5 -> "12:20" to "13:05"
        6 -> "14:00" to "14:45"
        7 -> "14:55" to "15:40"
        8 -> "15:50" to "16:35"
        9 -> "16:45" to "17:30"
        10 -> "19:00" to "19:45"
        11 -> "19:55" to "20:40"
        12 -> "20:50" to "21:35"
        else -> null
    }
}

private fun ClassSessionEntity.occursInWeek(week: Int?): Boolean {
    return week == null || week in weekNumbers()
}

private fun ClassSessionEntity.weekNumbers(): List<Int> {
    val text = weeksText.trim()
    if (text.isBlank() || text.contains("全周")) return (1..20).toList()
    val ranges = Regex("""(\d{1,2})(?:\s*[-~－至]\s*(\d{1,2}))?""").findAll(text).toList()
    if (ranges.isEmpty()) return (1..20).toList()
    val oddOnly = text.contains("单")
    val evenOnly = text.contains("双")
    return ranges
        .flatMap { match ->
            val start = match.groupValues[1].toInt()
            val end = match.groupValues.getOrNull(2)?.takeIf { it.isNotBlank() }?.toInt() ?: start
            (start.coerceAtMost(end)..start.coerceAtLeast(end)).toList()
        }
        .filter { it in 1..30 }
        .filter { !oddOnly || it % 2 == 1 }
        .filter { !evenOnly || it % 2 == 0 }
        .distinct()
        .sorted()
}

private fun CreditTree.allCourses(): List<CourseEntity> {
    return largeGroups
        .flatMap { it.allGroups }
        .flatMap { it.courses }
        .distinctBy { it.id }
}

private fun CourseStatus.statusLabel(): String {
    return when (this) {
        CourseStatus.PASSED -> "已通过"
        CourseStatus.TAKING -> "在修"
        CourseStatus.FAILED -> "未通过"
        CourseStatus.NOT_TAKEN -> "未修"
    }
}

private fun CourseStatus.sortOrder(): Int {
    return when (this) {
        CourseStatus.TAKING -> 0
        CourseStatus.FAILED -> 1
        CourseStatus.NOT_TAKEN -> 2
        CourseStatus.PASSED -> 3
    }
}

private fun CourseStatus.statusColor(): Color {
    return when (this) {
        CourseStatus.PASSED -> Color(0xFF168A4A)
        CourseStatus.TAKING -> Color(0xFF0F6FBD)
        CourseStatus.FAILED -> Color(0xFFB3261E)
        CourseStatus.NOT_TAKEN -> Color(0xFF6B7280)
    }
}

private fun String.courseColor(): Color {
    val colors = listOf(
        Color(0xFF0F6FBD),
        Color(0xFF168A4A),
        Color(0xFFB45F06),
        Color(0xFF6D4C9F),
        Color(0xFF00838F),
        Color(0xFFC2410C),
    )
    return colors[kotlin.math.abs(hashCode()) % colors.size]
}

private fun String.scheduleCourseColor(): Color {
    val colors = listOf(
        Color(0xFF79A9F5),
        Color(0xFF62C7A0),
        Color(0xFFE0AA6E),
        Color(0xFFAE92E8),
        Color(0xFF69BDD0),
        Color(0xFFE0848D),
        Color(0xFF9AB36B),
    )
    return colors[kotlin.math.abs(hashCode()) % colors.size]
}

private fun Set<String>.toggle(value: String): Set<String> {
    return if (value in this) this - value else this + value
}

private fun com.keshu.mobile.domain.model.GraduationProgress.progressFraction(): Float {
    return if (requiredCredits <= 0.0) 0f else (earnedCredits / requiredCredits).toFloat().coerceIn(0f, 1f)
}

private fun com.keshu.mobile.domain.model.GraduationProgress.progressPercent(): String {
    return "${(progressFraction() * 100).toInt()}%"
}

private fun LargeGroupProgress.progressFraction(): Float {
    return if (requiredCredits <= 0.0) 0f else (earnedCredits / requiredCredits).toFloat().coerceIn(0f, 1f)
}
