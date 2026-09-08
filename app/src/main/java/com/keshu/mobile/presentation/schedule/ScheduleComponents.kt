package com.keshu.mobile.presentation.schedule

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.keshu.mobile.data.local.entity.ClassSessionEntity
import com.keshu.mobile.presentation.components.*

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
