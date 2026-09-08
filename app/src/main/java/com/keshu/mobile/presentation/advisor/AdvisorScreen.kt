package com.keshu.mobile.presentation.advisor

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.keshu.mobile.domain.model.CourseAdvice
import com.keshu.mobile.domain.model.CourseRecommendation
import com.keshu.mobile.presentation.dashboard.DashboardUiState
import com.keshu.mobile.presentation.components.*

@Composable
internal fun AdvisorIntro() {
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
internal fun AdvisorPage(state: DashboardUiState, onRequestAdvice: (String) -> Unit) {
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
internal fun AdviceSummaryCard(advice: CourseAdvice) {
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
internal fun RecommendationCard(recommendation: CourseRecommendation) {
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
