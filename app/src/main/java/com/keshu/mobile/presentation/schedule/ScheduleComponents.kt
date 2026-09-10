package com.keshu.mobile.presentation.schedule

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.keshu.mobile.data.local.ClassPeriodTime
import com.keshu.mobile.data.local.ClassTimePresets
import com.keshu.mobile.data.local.ClassTimeProfile
import com.keshu.mobile.data.local.ClassTimeSettings
import com.keshu.mobile.data.local.HolidayEntry
import com.keshu.mobile.data.local.HolidayKind
import com.keshu.mobile.data.local.holidayBlocks
import com.keshu.mobile.data.local.isValidClassPeriod
import com.keshu.mobile.data.local.entity.ClassSessionEntity
import com.keshu.mobile.presentation.components.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
internal fun ClassTimeSettingsCard(
    settings: ClassTimeSettings,
    onProfileSelected: (ClassTimeProfile) -> Unit,
    onEditCustom: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f),
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Text("上课时间", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        settings.profile.displayName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(
                    listOf(
                        ClassTimeProfile.HUAQIAO_PANYU,
                        ClassTimeProfile.MAIN_ZHUHAI,
                        ClassTimeProfile.SHENZHEN,
                    ),
                    key = { it.id },
                ) { profile ->
                    FilterChip(
                        selected = settings.profile == profile,
                        onClick = { onProfileSelected(profile) },
                        label = { Text(profile.displayName, maxLines = 1) },
                    )
                }
                item {
                    FilterChip(
                        selected = settings.profile == ClassTimeProfile.CUSTOM,
                        onClick = onEditCustom,
                        label = { Text("自定义") },
                    )
                }
            }
            Text(
                "所选时间会用于首页、课表和桌面小组件。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun CustomClassTimeDialog(
    settings: ClassTimeSettings,
    onSave: (List<ClassPeriodTime>) -> Unit,
    onDismiss: () -> Unit,
) {
    var periods by remember(settings) {
        mutableStateOf(
            List(13) { index ->
                settings.periods.getOrNull(index) ?: ClassTimePresets.mainZhuhai[index]
            },
        )
    }
    val allValid = periods.all(::isValidClassPeriod)

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(enabled = allValid, onClick = { onSave(periods) }) {
                Text("保存并使用")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        },
        title = { Text("自定义上课时间") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "使用 24 小时制 HH:mm，结束时间需晚于开始时间。",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (allValid) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
                )
                LazyColumn(
                    modifier = Modifier.heightIn(max = 480.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    itemsIndexed(periods, key = { index, _ -> index }) { index, period ->
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                "第 ${index + 1} 节",
                                style = MaterialTheme.typography.labelLarge,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = period.start,
                                    onValueChange = { value ->
                                        periods = periods.toMutableList().also {
                                            it[index] = period.copy(start = value.timeInput())
                                        }
                                    },
                                    modifier = Modifier.weight(1f).semantics {
                                        contentDescription = "第 ${index + 1} 节开始时间"
                                    },
                                    label = { Text("开始") },
                                    singleLine = true,
                                    isError = !isValidTimeText(period.start),
                                )
                                OutlinedTextField(
                                    value = period.end,
                                    onValueChange = { value ->
                                        periods = periods.toMutableList().also {
                                            it[index] = period.copy(end = value.timeInput())
                                        }
                                    },
                                    modifier = Modifier.weight(1f).semantics {
                                        contentDescription = "第 ${index + 1} 节结束时间"
                                    },
                                    label = { Text("结束") },
                                    singleLine = true,
                                    isError = !isValidTimeText(period.end) || !isValidClassPeriod(period),
                                )
                            }
                        }
                    }
                }
            }
        },
    )
}

private fun isValidTimeText(value: String): Boolean {
    val match = Regex("^(\\d{2}):(\\d{2})$").matchEntire(value) ?: return false
    return match.groupValues[1].toInt() in 0..23 && match.groupValues[2].toInt() in 0..59
}

private fun String.timeInput(): String = filter { it.isDigit() || it == ':' }.take(5)

@Composable
internal fun ClassSessionCard(session: ClassSessionEntity, onClick: () -> Unit = {}) {
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
internal fun FilterChipRow(
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
internal fun WeekPickerRow(
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
internal fun ClassSessionDetailDialog(
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

private val monthDayFormatter = DateTimeFormatter.ofPattern("M月d日", Locale.CHINA)
private val monthDayWeekdayFormatter = DateTimeFormatter.ofPattern("M月d日 EEEE", Locale.CHINA)
private val allWeekdays = (1..7).map { DayOfWeek.of(it) }

/**
 * The schedule drawer's holiday section: which dates are off, and which make-up weekday each
 * make-up date uses.
 *
 * The make-up weekday comes from the school's own notice rather than the State Council arrangement,
 * so every make-up row is editable and an unverified default says so.
 */
@Composable
internal fun HolidaySettingsCard(
    entries: List<HolidayEntry>,
    onMakeupWeekdaySelected: (LocalDate, DayOfWeek) -> Unit,
    onMakeupWeekdayCleared: (LocalDate) -> Unit,
) {
    var editingDate by remember { mutableStateOf<LocalDate?>(null) }
    val blocks = remember(entries) { entries.holidayBlocks() }
    val makeupEntries = remember(entries) {
        entries.filter { it.kind == HolidayKind.MAKEUP }.sortedBy { it.date }
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.48f),
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Event, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Column {
                    Text("节假日与调休", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "日期来自国务院节假日安排",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            blocks.forEach { block ->
                val dates = if (block.start == block.end) {
                    block.start.format(monthDayFormatter)
                } else {
                    "${block.start.format(monthDayFormatter)}–${block.end.format(monthDayFormatter)}"
                }
                Column {
                    Text("${block.label} $dates", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "放假，不上课",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (makeupEntries.isEmpty()) {
                Text(
                    "本学期暂时没有调休上课日。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            makeupEntries.forEach { entry ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(role = Role.Button) { editingDate = entry.date },
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                entry.date.format(monthDayWeekdayFormatter),
                                style = MaterialTheme.typography.titleSmall,
                            )
                            Text(
                                entry.makeupDescription(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "修改${entry.date.format(monthDayFormatter)}的补课星期",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
            Text(
                "补课具体上星期几的课由各校自行通知。标“默认推测”的是应用按惯例给出的默认值，" +
                    "请以学校通知为准。",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    editingDate?.let { date ->
        val entry = makeupEntries.firstOrNull { it.date == date }
        MakeupWeekdayDialog(
            date = date,
            selected = entry?.makeupWeekday,
            canResetToDefault = entry?.makeupIsAssumed == false,
            onSelect = { weekday ->
                onMakeupWeekdaySelected(date, weekday)
                editingDate = null
            },
            onResetDefault = {
                onMakeupWeekdayCleared(date)
                editingDate = null
            },
            onDismiss = { editingDate = null },
        )
    }
}

private fun HolidayEntry.makeupDescription(): String {
    val weekday = makeupWeekday ?: return "待确认补哪天的课，点击选择"
    val name = weekday.chineseName()
    return if (makeupIsAssumed) "补$name 的课（默认推测，点击修改）" else "补$name 的课（点击修改）"
}

@Composable
private fun MakeupWeekdayDialog(
    date: LocalDate,
    selected: DayOfWeek?,
    canResetToDefault: Boolean,
    onSelect: (DayOfWeek) -> Unit,
    onResetDefault: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("取消") } },
        dismissButton = {
            if (canResetToDefault) {
                TextButton(onClick = onResetDefault) { Text("恢复默认推测") }
            }
        },
        title = { Text("${date.format(monthDayWeekdayFormatter)} 补哪天的课？") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "请按学校的调课通知选择。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                    items(allWeekdays, key = { it.value }) { weekday ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(role = Role.Button) { onSelect(weekday) },
                            shape = RoundedCornerShape(12.dp),
                            color = if (weekday == selected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                Color.Transparent
                            },
                        ) {
                            Text(
                                weekday.chineseName(),
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }
        },
    )
}
