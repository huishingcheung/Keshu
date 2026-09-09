package com.keshu.mobile.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.keshu.mobile.data.local.entity.ClassSessionEntity
import com.keshu.mobile.data.local.entity.CourseEntity
import com.keshu.mobile.data.local.entity.CourseStatus
import com.keshu.mobile.domain.model.CreditTree
import com.keshu.mobile.domain.model.LargeGroupProgress
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.Locale

@Composable
internal fun HeroCard(
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
internal fun SectionHeader(title: String, subtitle: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(subtitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun TodayCourseRow(session: ClassSessionEntity) {
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
internal fun EmptyCard(title: String, subtitle: String) {
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
internal fun MiniInfoRow(icon: ImageVector, title: String, subtitle: String, modifier: Modifier = Modifier) {
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
internal fun IconBubble(icon: ImageVector, small: Boolean = false) {
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
internal fun TimePill(day: String, sections: String) {
    Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(day, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSecondaryContainer)
            Text(sections, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSecondaryContainer, fontWeight = FontWeight.Bold)
        }
    }
}

internal fun Double.formatCredit(): String {
    return if (this % 1.0 == 0.0) toInt().toString() else String.format(Locale.CHINA, "%.1f", this)
}

internal fun Long.formatDateTime(): String {
    return SimpleDateFormat("MM月dd日 HH:mm", Locale.CHINA).format(Date(this))
}

internal fun Long.formatDate(): String {
    return SimpleDateFormat("MM月dd日", Locale.CHINA).format(Date(this))
}

internal fun defaultSemesterStartDate(): String {
    val today = LocalDate.now()
    val year = today.year
    return if (today.monthValue >= 8) "$year-09-01" else "$year-03-01"
}

internal fun weekFromSemesterStart(value: String): Int? {
    val start = runCatching { LocalDate.parse(value.trim(), DateTimeFormatter.ISO_LOCAL_DATE) }.getOrNull() ?: return null
    val days = ChronoUnit.DAYS.between(start, LocalDate.now())
    return if (days < 0) 1 else (days / 7 + 1).toInt().coerceAtLeast(1)
}

internal fun scheduleWeekDates(semesterStartDate: String, selectedWeek: Int?): List<LocalDate> {
    val start = runCatching { LocalDate.parse(semesterStartDate.trim(), DateTimeFormatter.ISO_LOCAL_DATE) }
        .getOrNull()
        ?: LocalDate.now()
    val weekStart = start
        .plusWeeks(((selectedWeek ?: 1) - 1).toLong())
        .minusDays((start.dayOfWeek.value - 1).toLong())
    return (0..6).map { weekStart.plusDays(it.toLong()) }
}

internal fun ClassSessionEntity.occursInWeek(week: Int?): Boolean {
    return week == null || week in weekNumbers()
}

internal fun ClassSessionEntity.weekNumbers(): List<Int> {
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

internal fun CreditTree.allCourses(): List<CourseEntity> {
    return largeGroups
        .flatMap { it.allGroups }
        .flatMap { it.courses }
        .distinctBy { it.id }
}

internal fun CourseStatus.statusLabel(): String {
    return when (this) {
        CourseStatus.PASSED -> "已通过"
        CourseStatus.TAKING -> "在修"
        CourseStatus.FAILED -> "未通过"
        CourseStatus.NOT_TAKEN -> "未修"
    }
}

internal fun CourseStatus.sortOrder(): Int {
    return when (this) {
        CourseStatus.TAKING -> 0
        CourseStatus.FAILED -> 1
        CourseStatus.NOT_TAKEN -> 2
        CourseStatus.PASSED -> 3
    }
}

internal fun CourseStatus.statusColor(): Color {
    return when (this) {
        CourseStatus.PASSED -> Color(0xFF168A4A)
        CourseStatus.TAKING -> Color(0xFF0F6FBD)
        CourseStatus.FAILED -> Color(0xFFB3261E)
        CourseStatus.NOT_TAKEN -> Color(0xFF6B7280)
    }
}

internal fun String.courseColor(): Color {
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

internal fun String.scheduleCourseColor(): Color {
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

internal fun Set<String>.toggle(value: String): Set<String> {
    return if (value in this) this - value else this + value
}

internal fun com.keshu.mobile.domain.model.GraduationProgress.progressFraction(): Float {
    return if (requiredCredits <= 0.0) 0f else (earnedCredits / requiredCredits).toFloat().coerceIn(0f, 1f)
}

internal fun com.keshu.mobile.domain.model.GraduationProgress.progressPercent(): String {
    return "${(progressFraction() * 100).toInt()}%"
}

internal fun LargeGroupProgress.progressFraction(): Float {
    return if (requiredCredits <= 0.0) 0f else (earnedCredits / requiredCredits).toFloat().coerceIn(0f, 1f)
}
