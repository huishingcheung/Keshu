package com.keshu.mobile.presentation.credit

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.keshu.mobile.data.local.entity.CourseEntity
import com.keshu.mobile.domain.model.LargeGroupProgress
import com.keshu.mobile.domain.model.SmallGroupProgress
import com.keshu.mobile.presentation.dashboard.DashboardUiState
import com.keshu.mobile.presentation.components.*

@Composable
internal fun CreditPage(state: DashboardUiState) {
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
internal fun CreditOverview(
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
internal fun CreditMetric(label: String, value: String) {
    Column(horizontalAlignment = Alignment.Start) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun CreditMetricRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
internal fun CourseStatusRow(course: CourseEntity, modifier: Modifier = Modifier) {
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

@Composable
internal fun LargeGroupCard(
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
internal fun SmallGroupTree(
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
            val sortedCourses = remember(group.courses) {
                group.courses.sortedWith(compareBy<CourseEntity> { it.status.sortOrder() }.thenBy { it.name })
            }
            group.childGroups.forEach { child ->
                SmallGroupTree(
                    group = child,
                    expandedSmallGroups = expandedSmallGroups,
                    onToggle = onToggle,
                )
            }
            sortedCourses.forEach { course ->
                    CourseStatusRow(
                        course = course,
                        modifier = Modifier.padding(start = 18.dp),
                    )
                }
        }
    }
}
