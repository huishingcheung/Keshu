package com.keshu.mobile.presentation.schedule

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.keshu.mobile.data.local.ClassPeriodTime
import com.keshu.mobile.data.local.ClassTimeSettings
import com.keshu.mobile.data.local.DaySchedule
import com.keshu.mobile.data.local.HolidayEntry
import com.keshu.mobile.data.local.resolveDaySchedule
import com.keshu.mobile.data.local.timetableWeekday
import com.keshu.mobile.data.local.entity.ClassSessionEntity
import com.keshu.mobile.presentation.components.*
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SemesterStartCard(
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ScheduleTopHeader(
    activeWeek: Int?,
    activeTerm: String?,
    onOpenDrawer: () -> Unit,
    onSelectWeek: () -> Unit,
) {
    Column {
        BoxWithConstraints {
            val showSwipeHint = maxWidth >= 420.dp && LocalDensity.current.fontScale < 1.3f
            TopAppBar(
                windowInsets = WindowInsets(0),
                title = {
                    Column(
                        modifier = Modifier
                            .clickable(role = Role.Button, onClick = onSelectWeek)
                            .semantics {
                                contentDescription = activeWeek?.let { "选择周数，当前第 $it 周" } ?: "选择周数"
                            },
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                activeWeek?.let { "第${it}周" } ?: "课表",
                                style = MaterialTheme.typography.titleLarge,
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
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(
                            Icons.Default.Menu,
                            contentDescription = "打开课表侧栏",
                            tint = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                },
                actions = {
                    if (showSwipeHint) {
                        Surface(shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.primaryContainer) {
                            Text(
                                "左右滑动切周",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
    }
}

@Composable
internal fun ScheduleWeekPickerDialog(
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
internal fun ScheduleWeekGrid(
    modifier: Modifier = Modifier,
    sessions: List<ClassSessionEntity>,
    selectedWeek: Int?,
    semesterStartDate: String,
    classTimeSettings: ClassTimeSettings,
    holidayEntries: Map<LocalDate, HolidayEntry>,
    today: LocalDate,
    onSessionClick: (ClassSessionEntity) -> Unit,
) {
    val maxSection = remember(sessions, classTimeSettings) {
        maxOf(sessions.maxOfOrNull { it.endSection } ?: 0, classTimeSettings.periods.size, 12)
    }
    val weekDates = remember(semesterStartDate, selectedWeek) { scheduleWeekDates(semesterStartDate, selectedWeek) }
    val sessionsByDay = remember(sessions) { sessions.groupBy { it.dayOfWeek } }
    val daySchedules = remember(weekDates, holidayEntries) {
        weekDates.map { resolveDaySchedule(it, holidayEntries) }
    }
    val sectionHeight = 68.dp
    val verticalScroll = rememberScrollState()
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.72f)
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        val timeWidth = 58.dp
        val dayWidth = ((maxWidth - timeWidth) / 7).coerceAtLeast(38.dp)
        Column(Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 76.dp)
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
                val holidayTint = MaterialTheme.colorScheme.error
                val makeupTint = MaterialTheme.colorScheme.secondary
                scheduleDayNames.forEachIndexed { index, name ->
                    val date = weekDates.getOrNull(index)
                    val schedule = daySchedules.getOrNull(index)
                    ScheduleDayHeader(
                        dayName = name,
                        dayNumber = date?.dayOfMonth?.toString().orEmpty(),
                        selected = date == today,
                        badge = schedule?.headerBadge()?.let { label ->
                            DayHeaderBadge(
                                label = label,
                                tint = if (schedule is DaySchedule.Holiday) holidayTint else makeupTint,
                            )
                        },
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
                            ScheduleTimeCell(section, sectionHeight, classTimeSettings.period(section))
                        }
                    }
                    Row(
                        Modifier
                            .padding(start = timeWidth)
                            .requiredHeight(sectionHeight * maxSection)
                            .drawBehind {
                                val sectionHeightPx = sectionHeight.toPx()
                                val dayWidthPx = dayWidth.toPx()
                                val strokeWidth = 0.5.dp.toPx()
                                repeat(maxSection + 1) { section ->
                                    val y = sectionHeightPx * section
                                    drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth)
                                }
                                repeat(8) { day ->
                                    val x = (dayWidthPx * day).coerceAtMost(size.width)
                                    drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), strokeWidth)
                                }
                            },
                    ) {
                        scheduleDayNames.indices.forEach { dayIndex ->
                            Box(Modifier.width(dayWidth).requiredHeight(sectionHeight * maxSection)) {
                                daySchedules.getOrNull(dayIndex)
                                    ?.timetableWeekday()
                                    ?.let { sessionsByDay[it.value] }
                                    .orEmpty()
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

private val scheduleDayNames = listOf("一", "二", "三", "四", "五", "六", "日")

/** A short timetable marker such as `休` or `补二`, with the tint that explains it. */
internal data class DayHeaderBadge(val label: String, val tint: Color)

/**
 * The marker shown above a day column, or null on an ordinary day.
 *
 * `*` marks a make-up weekday that is only this app's convention default. `补?` marks a make-up day
 * whose substituted weekday is unknown, which is a teaching day the user still has to confirm.
 */
internal fun DaySchedule.headerBadge(): String? = when (this) {
    is DaySchedule.Holiday -> "休"
    is DaySchedule.MakeupUnconfirmed -> "补?"
    is DaySchedule.Makeup -> "补" + scheduleDayNames[weekday.value - 1] + if (assumed) "*" else ""
    is DaySchedule.Normal -> null
}

@Composable
internal fun ScheduleDayHeader(
    dayName: String,
    dayNumber: String,
    selected: Boolean,
    badge: DayHeaderBadge?,
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
        Text(
            // A blank placeholder keeps the badge line's height on ordinary days, so the week grid
            // does not shift when paging between weeks that do and do not carry a marker.
            badge?.label ?: " ",
            style = MaterialTheme.typography.labelSmall,
            color = badge?.tint ?: Color.Transparent,
            maxLines = 1,
            softWrap = false,
        )
    }
}

@Composable
internal fun ScheduleTimeCell(
    section: Int,
    sectionHeight: androidx.compose.ui.unit.Dp,
    period: ClassPeriodTime?,
) {
    Box(modifier = Modifier.width(64.dp).height(sectionHeight).padding(top = 10.dp), contentAlignment = Alignment.TopCenter) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                section.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            period?.let {
                Text(it.start, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(it.end, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
internal fun ScheduleSessionBlock(
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
