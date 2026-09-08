package com.keshu.mobile.presentation.exam

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.keshu.mobile.data.local.ExamAvailability
import com.keshu.mobile.data.local.entity.ExamEntity
import com.keshu.mobile.presentation.dashboard.DashboardUiState
import com.keshu.mobile.presentation.components.*

@Composable
internal fun ExamPage(
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
internal fun ExamUnavailablePanel(availability: ExamAvailability) {
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
internal fun ExamSummaryBar(count: Int, onScheduleAlarms: () -> Unit) {
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
internal fun ExamCard(exam: ExamEntity) {
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
