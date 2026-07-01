package edu.jnu.smartedu.presentation

import android.content.Intent
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
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
import androidx.compose.material.icons.filled.KeyboardArrowRight
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import edu.jnu.smartedu.data.AppContainer
import edu.jnu.smartedu.data.local.entity.ClassSessionEntity
import edu.jnu.smartedu.data.local.entity.CourseEntity
import edu.jnu.smartedu.data.local.entity.CourseStatus
import edu.jnu.smartedu.data.local.entity.ExamEntity
import edu.jnu.smartedu.data.local.entity.TaskEntity
import edu.jnu.smartedu.domain.model.CreditTree
import edu.jnu.smartedu.domain.model.CourseAdvice
import edu.jnu.smartedu.domain.model.CourseRecommendation
import edu.jnu.smartedu.domain.model.LargeGroupProgress
import edu.jnu.smartedu.domain.model.SmallGroupProgress
import edu.jnu.smartedu.presentation.dashboard.DashboardUiState
import edu.jnu.smartedu.presentation.dashboard.DashboardViewModel
import edu.jnu.smartedu.presentation.dashboard.DashboardViewModelFactory
import edu.jnu.smartedu.presentation.sync.ManualSyncActivity
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.launch

private enum class MainTab(val title: String, val subtitle: String, val icon: ImageVector) {
    Home("首页", "今日概览", Icons.Default.Home),
    Credit("学分", "已修学分", Icons.Default.School),
    Schedule("课表", "课程安排", Icons.Default.CalendarMonth),
    Exam("考试", "提醒与座位", Icons.Default.Event),
    Advisor("AI", "选课建议", Icons.Default.AutoAwesome),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JnuSmartEduAppScreen(
    container: AppContainer,
    launchDestination: String? = null,
    launchAddTask: Boolean = false,
    launchRequestId: Long = 0L,
) {
    val context = LocalContext.current
    val academicSettings = remember { context.getSharedPreferences("academic_settings", 0) }
    var selectedTab by remember { mutableStateOf(MainTab.Home) }
    var semesterStartDate by remember {
        mutableStateOf(academicSettings.getString("semester_start_date", defaultSemesterStartDate()).orEmpty())
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

    LaunchedEffect(launchRequestId) {
        MainTab.entries.firstOrNull { it.name.equals(launchDestination, ignoreCase = true) }?.let {
            selectedTab = it
        }
    }

    Scaffold(
        topBar = {
            if (selectedTab != MainTab.Schedule) {
                TopAppBar(
                    modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars),
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                modifier = Modifier.size(42.dp),
                                shape = RoundedCornerShape(15.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        selectedTab.icon,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(21.dp),
                                    )
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(selectedTab.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Text(selectedTab.subtitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.96f)),
                )
            }
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .windowInsetsPadding(WindowInsets.navigationBars),
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.96f),
                tonalElevation = 0.dp,
            ) {
                MainTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = { Icon(tab.icon, contentDescription = tab.title) },
                        label = { Text(tab.title, maxLines = 1) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    )
                }
            }
        },
    ) { padding ->
        Crossfade(targetState = selectedTab, label = "main-tab") { tab ->
            ScreenShell(padding) {
                when (tab) {
                    MainTab.Home -> HomePage(
                        state = state,
                        currentWeek = weekFromSemesterStart(semesterStartDate),
                        onSync = { context.startActivity(Intent(context, ManualSyncActivity::class.java)) },
                        onAddTask = viewModel::addTask,
                        onUpdateTask = viewModel::updateTask,
                        onSetTaskDone = viewModel::setTaskDone,
                        onDeleteTask = viewModel::deleteTask,
                        addTaskRequestId = if (launchAddTask) launchRequestId else 0L,
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
                    MainTab.Exam -> ExamPage(state, onScheduleAlarms = viewModel::scheduleAlarms)
                    MainTab.Advisor -> AdvisorPage(state, onRequestAdvice = viewModel::requestAdvice)
                }
            }
        }
    }
}

@Composable
private fun ScreenShell(padding: PaddingValues, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.12f),
                        MaterialTheme.colorScheme.background,
                    ),
                ),
            )
            .padding(padding),
    ) {
        content()
    }
}

@Composable
private fun HomePage(
    state: DashboardUiState,
    currentWeek: Int?,
    onSync: () -> Unit,
    onAddTask: (String, Long) -> Unit,
    onUpdateTask: (TaskEntity, String, Long) -> Unit,
    onSetTaskDone: (TaskEntity, Boolean) -> Unit,
    onDeleteTask: (TaskEntity) -> Unit,
    addTaskRequestId: Long,
) {
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<TaskEntity?>(null) }
    var deletingTask by remember { mutableStateOf<TaskEntity?>(null) }
    LaunchedEffect(addTaskRequestId) {
        if (addTaskRequestId != 0L) showAddTaskDialog = true
    }
    val tree = state.creditTree
    val todayDayOfWeek = remember { LocalDate.now().dayOfWeek.value }
    val todaySessions = remember(state.schedule.classSessions, currentWeek, todayDayOfWeek) {
        state.schedule.classSessions
            .filter { it.dayOfWeek == todayDayOfWeek && it.occursInWeek(currentWeek) }
            .sortedBy { it.startSection }
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 116.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
            item {
                HeroCard(
                    title = "学习驾驶舱",
                    value = tree?.graduation?.earnedCredits?.formatCredit() ?: "--",
                    unit = "已获学分",
                    caption = tree?.let { "${it.graduation.metLargeGroupCount}/${it.graduation.largeGroupCount} 个模块达标" } ?: "等待同步教务数据",
                    progress = tree?.graduation?.progressFraction() ?: 0f,
                )
            }
            item { SyncEntryCard(onSync) }
            item { SectionHeader("今日的课程内容", currentWeek?.let { "第 $it 周" } ?: "按当天显示") }
            if (todaySessions.isEmpty()) {
                item { EmptyCard("今天休息啦！", "今日没有课程安排。") }
            } else {
                items(todaySessions, key = { it.id }) { session -> TodayCourseRow(session) }
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
            item { SectionHeader("考试安排", "${state.schedule.exams.size} 场") }
            if (state.schedule.exams.isEmpty()) {
                item { EmptyCard("暂无考试安排", "同步教务数据后，考试时间和地点会显示在这里。") }
            } else {
                items(state.schedule.exams, key = { it.id }) { exam -> ExamCard(exam) }
            }
    }
    if (showAddTaskDialog) {
        TaskEditorDialog(
            task = null,
            onDismiss = { showAddTaskDialog = false },
            onSave = { title, dueAtMillis ->
                onAddTask(title, dueAtMillis)
                showAddTaskDialog = false
            },
        )
    }
    editingTask?.let { task ->
        TaskEditorDialog(
            task = task,
            onDismiss = { editingTask = null },
            onSave = { title, dueAtMillis ->
                onUpdateTask(task, title, dueAtMillis)
                editingTask = null
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
    onSave: (String, Long) -> Unit,
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
    val dueAtMillis = dueDate.atTime(23, 59).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                enabled = !saving && title.isNotBlank() && (task != null || dueAtMillis > System.currentTimeMillis()),
                onClick = {
                    if (!saving) {
                        saving = true
                        onSave(title, dueAtMillis)
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
            HeroCard(
                title = if (includeTakingProjection) "预计学分" else "已修学分",
                value = tree?.let { displayedCredits.formatCredit() } ?: "--",
                unit = "学分",
                caption = tree?.let { "毕业要求 ${it.graduation.requiredCredits.formatCredit()} 学分" } ?: "暂无学分数据",
                progress = tree?.let {
                    if (it.graduation.requiredCredits <= 0.0) 0f else (displayedCredits / it.graduation.requiredCredits).toFloat().coerceIn(0f, 1f)
                } ?: 0f,
                action = {
                    Button(
                        onClick = { includeTakingProjection = !includeTakingProjection },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.primary,
                        ),
                    ) {
                        Text(if (includeTakingProjection) "恢复只看已修" else "预测本学期通过后学分")
                    }
                },
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
                modifier = Modifier.width(320.dp),
                drawerContainerColor = Color(0xFF151B24),
                drawerContentColor = Color(0xFFF2F5FA),
            ) {
                LazyColumn(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    item {
                        Text("课表设置", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color(0xFFF2F5FA))
                        Text("管理开学日期和学期课表", style = MaterialTheme.typography.bodyMedium, color = Color(0xFF98A5B7))
                    }
                    item {
                        SemesterStartCard(
                            value = semesterStartDate,
                            currentWeek = currentWeek,
                            onValueChange = onSemesterStartDateChange,
                        )
                    }
                    if (terms.isNotEmpty()) {
                        item { Text("学期课表", style = MaterialTheme.typography.titleMedium, color = Color(0xFFF2F5FA)) }
                        items(terms, key = { it }) { term ->
                            Surface(
                                modifier = Modifier.fillMaxWidth().clickable { selectedTerm = term },
                                shape = RoundedCornerShape(14.dp),
                                color = if (term == activeTerm) Color(0xFF253A58) else Color(0xFF1D2531),
                                border = BorderStroke(1.dp, if (term == activeTerm) Color(0xFF6EA8FE) else Color(0xFF303A49)),
                            ) {
                                Text(
                                    term,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (term == activeTerm) Color(0xFFD9E8FF) else Color(0xFFBAC4D2),
                                )
                            }
                        }
                    }
                }
            }
        },
    ) {
        Column(Modifier.fillMaxSize().background(Color(0xFF0D1117))) {
            ScheduleTopHeader(
                activeWeek = activeWeek,
                activeTerm = activeTerm,
                onOpenDrawer = { scope.launch { drawerState.open() } },
                onSelectWeek = { showWeekPicker = true },
            )
            if (termSessions.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("暂无课程，完成课表同步后会显示在这里", color = Color(0xFF98A5B7))
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
private fun ExamPage(state: DashboardUiState, onScheduleAlarms: () -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 116.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            HeroCard(
                title = "考试提醒",
                value = "${state.schedule.exams.size}",
                unit = "场",
                caption = "开启提醒后将为即将到来的考试创建本地通知",
                progress = if (state.schedule.exams.isEmpty()) 0f else 1f,
                action = {
                    Button(
                        onClick = onScheduleAlarms,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                            contentColor = MaterialTheme.colorScheme.primary,
                        ),
                    ) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("安排提醒")
                    }
                },
            )
        }
        item { SectionHeader("考试列表", "时间、地点与座位") }
        if (state.schedule.exams.isEmpty()) {
            item { EmptyCard("暂无考试", "同步考试安排后会显示倒计时、地点和座位。") }
        } else {
            items(state.schedule.exams.sortedBy { it.startsAtMillis }, key = { it.id }) { exam ->
                ExamCard(exam)
            }
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
            HeroCard(
                title = "AI 选课顾问",
                value = "DeepSeek",
                unit = "",
                caption = "结合培养方案缺口与真实课程数据生成可解释建议",
                progress = state.creditTree?.graduation?.progressFraction() ?: 0f,
            )
        }
        item {
            ElevatedCard(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = { apiKey = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("DeepSeek API Key") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                    )
                    Text(
                        "API Key 仅用于本次请求，不会保存在本地。系统会综合培养方案、已修与在修课程。",
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
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)),
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
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = Color(0xFF1D2531)),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("开学日期", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFFF2F5FA))
                    Text(
                        currentWeek?.let { "$value · 当前第 $it 周" } ?: value,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF98A5B7),
                    )
                }
                Button(
                    onClick = { showPicker = true },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2762B3), contentColor = Color.White),
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
    Surface(color = Color(0xFF101620)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(start = 10.dp, end = 18.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(shape = RoundedCornerShape(15.dp), color = Color(0xFF202A3A)) {
                IconButton(onClick = onOpenDrawer) {
                    Icon(Icons.Default.Menu, contentDescription = "打开课表侧栏", tint = Color(0xFFE7ECF3), modifier = Modifier.size(26.dp))
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onSelectWeek),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        activeWeek?.let { "第${it}周" } ?: "课表",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF2F5FA),
                    )
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "选择周数", tint = Color(0xFF9FB4D0))
                }
                Text(
                    activeTerm ?: "暂无学期",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF98A5B7),
                )
            }
            Surface(shape = RoundedCornerShape(14.dp), color = Color(0xFF25375B)) {
                Text(
                    "左右滑动切周",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFB7C7FF),
                )
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
                        modifier = Modifier.fillMaxWidth().clickable { onSelected(week) },
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
            .background(Color(0xFF0D1117))
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
                    .background(Color(0xFF151B24)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.width(timeWidth),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(weekDates.firstOrNull()?.monthValue?.toString().orEmpty(), style = MaterialTheme.typography.titleMedium, color = Color(0xFFE7ECF3))
                    Text("月", style = MaterialTheme.typography.bodySmall, color = Color(0xFF7F8C9E))
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
                    .background(Color(0xFF0D1117)),
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
        Text(dayName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFFBAC4D2))
        Surface(
            modifier = Modifier.size(38.dp),
            shape = RoundedCornerShape(9.dp),
            color = if (selected) Color(0xFF5F78D8) else Color.Transparent,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    dayNumber,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (selected) Color.White else Color(0xFF7F8C9E),
                )
            }
        }
    }
}

@Composable
private fun ScheduleTimeCell(section: Int, sectionHeight: androidx.compose.ui.unit.Dp) {
    Box(modifier = Modifier.width(64.dp).height(sectionHeight).padding(top = 10.dp), contentAlignment = Alignment.TopCenter) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(section.toString(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color(0xFFBAC4D2))
            scheduleSectionTime(section)?.let { times ->
                Text(times.first, style = MaterialTheme.typography.labelSmall, color = Color(0xFF687587))
                Text(times.second, style = MaterialTheme.typography.labelSmall, color = Color(0xFF687587))
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
        border = BorderStroke(0.5.dp, Color(0xFF2A3342)),
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
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF18212D),
        border = BorderStroke(1.dp, color.copy(alpha = 0.72f)),
    ) {
        Column(Modifier.padding(horizontal = 5.dp, vertical = 7.dp)) {
            Box(Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(99.dp)).background(color))
            Spacer(Modifier.height(6.dp))
            Text(
                session.courseName,
                fontSize = 10.sp,
                lineHeight = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFF0F4FA),
            )
            Spacer(Modifier.height(5.dp))
            Text(
                session.location.ifBlank { "教室待定" },
                fontSize = 9.sp,
                lineHeight = 11.sp,
                fontWeight = FontWeight.Medium,
                color = color,
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
        shape = RoundedCornerShape(28.dp),
        color = Color.Transparent,
        shadowElevation = 0.dp,
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF151D38), Color(0xFF3656B3), Color(0xFF087B69)),
                    ),
                )
                .padding(20.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = Color.White.copy(alpha = 0.82f))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(value, style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
                    if (unit.isNotBlank()) {
                        Spacer(Modifier.width(6.dp))
                        Text(unit, modifier = Modifier.padding(bottom = 4.dp), style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.74f))
                    }
                }
                LinearProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(99.dp)),
                    color = MaterialTheme.colorScheme.secondary,
                    trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.24f),
                )
                Text(caption, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.78f))
                if (action != null) action()
            }
        }
    }
}

@Composable
private fun MetricTile(label: String, value: String, icon: ImageVector, modifier: Modifier = Modifier) {
    ElevatedCard(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.56f)),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            IconBubble(icon)
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
private fun SyncEntryCard(onSync: () -> Unit) {
    ElevatedCard(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.52f)),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            IconBubble(Icons.Default.Sync)
            Column(Modifier.weight(1f)) {
                Text("同步教务数据", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("更新课表、考试和培养方案", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(onClick = onSync, shape = RoundedCornerShape(16.dp)) {
                Text("去同步")
            }
        }
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
    ElevatedCard(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f)),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onToggle, modifier = Modifier.size(44.dp)) {
                    Icon(
                        if (expanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                        contentDescription = if (expanded) "收起" else "展开",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                Spacer(Modifier.width(12.dp))
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
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
        ) {
            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { if (hasChildren) onToggle(group.id) },
                    modifier = Modifier.size(36.dp),
                    enabled = hasChildren,
                ) {
                    Icon(
                        if (expanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                        contentDescription = if (expanded) "收起" else "展开",
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
        modifier = Modifier.clickable(onClick = onClick),
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
    ElevatedCard(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f)),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
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
        shape = RoundedCornerShape(20.dp),
        color = if (task.done) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f) else MaterialTheme.colorScheme.surface,
        shadowElevation = 0.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(checked = task.done, onCheckedChange = onDoneChange)
            Column(
                modifier = Modifier.weight(1f).clickable(onClick = onEdit).padding(horizontal = 6.dp, vertical = 8.dp),
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
                Icon(Icons.Default.Delete, contentDescription = "删除任务", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun AdviceSummaryCard(advice: CourseAdvice) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            MiniInfoRow(Icons.Default.AutoAwesome, "DeepSeek 建议", "基于培养方案、已修与在修课程")
            Text(advice.summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
    }
}

@Composable
private fun RecommendationCard(recommendation: CourseRecommendation) {
    ElevatedCard(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f)),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
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
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.24f)),
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

private fun edu.jnu.smartedu.domain.model.GraduationProgress.progressFraction(): Float {
    return if (requiredCredits <= 0.0) 0f else (earnedCredits / requiredCredits).toFloat().coerceIn(0f, 1f)
}

private fun edu.jnu.smartedu.domain.model.GraduationProgress.progressPercent(): String {
    return "${(progressFraction() * 100).toInt()}%"
}

private fun LargeGroupProgress.progressFraction(): Float {
    return if (requiredCredits <= 0.0) 0f else (earnedCredits / requiredCredits).toFloat().coerceIn(0f, 1f)
}
