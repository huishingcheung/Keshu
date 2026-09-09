package com.keshu.mobile.presentation.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.keshu.mobile.data.local.ExamAvailability
import com.keshu.mobile.data.local.ClassTimeSettings
import com.keshu.mobile.data.local.entity.TaskEntity
import com.keshu.mobile.presentation.dashboard.DashboardUiState
import com.keshu.mobile.presentation.components.*
import com.keshu.mobile.presentation.exam.ExamCard
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
internal fun HomePage(
    state: DashboardUiState,
    currentWeek: Int?,
    classTimeSettings: ClassTimeSettings,
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
    val nextSession = remember(todaySessions, classTimeSettings) {
        val currentMinutes = java.time.LocalTime.now().let { it.hour * 60 + it.minute }
        todaySessions.firstOrNull { session ->
            classTimeSettings.period(session.endSection)
                ?.end
                ?.split(":")
                ?.let { parts -> parts[0].toInt() * 60 + parts[1].toInt() >= currentMinutes }
                ?: true
        }
    }
    val laterSessions = remember(todaySessions, nextSession) {
        todaySessions.filterNot { it.id == nextSession?.id }
    }
    val pendingTaskCount = remember(state.schedule.tasks) { state.schedule.tasks.count { !it.done } }
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
                        val start = classTimeSettings.period(session.startSection)?.start.orEmpty()
                        val end = classTimeSettings.period(session.endSection)?.end.orEmpty()
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
                            "$pendingTaskCount 项待完成",
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
internal fun TaskEditorDialog(
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
internal fun TaskRow(
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
