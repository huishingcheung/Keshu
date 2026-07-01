package edu.jnu.smartedu.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import edu.jnu.smartedu.JnuSmartEduApp
import edu.jnu.smartedu.MainActivity
import edu.jnu.smartedu.R
import edu.jnu.smartedu.data.local.entity.ClassSessionEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ScheduleWidgetReceiver : AppWidgetProvider() {
    override fun onEnabled(context: Context) {
        WidgetUpdateManager.scheduleDailyRefresh(context)
        WidgetUpdateManager.requestUpdate(context)
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = (context.applicationContext as JnuSmartEduApp).container.database
                val allSessions = database.classScheduleDao().getAll()
                val currentTerm = allSessions.firstOrNull()?.term
                val currentWeek = currentWeek(context)
                val today = LocalDate.now()
                val sessions = allSessions
                    .asSequence()
                    .filter { currentTerm == null || it.term == currentTerm }
                    .filter { it.dayOfWeek == today.dayOfWeek.value }
                    .filter { currentWeek == null || currentWeek in it.weekNumbers() }
                    .distinctBy { it.id }
                    .take(3)
                    .toList()
                val exam = database.examDao().getUpcoming(limit = 1).firstOrNull()
                appWidgetIds.forEach { appWidgetId ->
                    val views = RemoteViews(context.packageName, R.layout.schedule_widget_layout)
                    val openSchedule = openAppIntent(context, appWidgetId, MainActivity.DESTINATION_SCHEDULE)
                    views.setOnClickPendingIntent(R.id.schedule_widget_header, openSchedule)
                    views.setOnClickPendingIntent(R.id.schedule_widget_title, openSchedule)
                    views.setTextViewText(
                        R.id.schedule_widget_date,
                        today.format(DateTimeFormatter.ofPattern("M月d日 EEEE")),
                    )
                    bindSessions(views, sessions, openSchedule)
                    views.setViewVisibility(R.id.schedule_widget_exam, if (exam == null) View.GONE else View.VISIBLE)
                    if (exam != null) {
                        views.setTextViewText(R.id.schedule_widget_exam_title, exam.courseName)
                        views.setTextViewText(
                            R.id.schedule_widget_exam_meta,
                            "${formatExamTime(exam.startsAtMillis)} · ${exam.location}",
                        )
                        views.setOnClickPendingIntent(
                            R.id.schedule_widget_exam,
                            openAppIntent(context, appWidgetId, MainActivity.DESTINATION_EXAM),
                        )
                    }
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun bindSessions(views: RemoteViews, sessions: List<ClassSessionEntity>, openApp: PendingIntent) {
        val rowIds = intArrayOf(R.id.schedule_widget_course_1, R.id.schedule_widget_course_2, R.id.schedule_widget_course_3)
        val timeIds = intArrayOf(R.id.schedule_widget_course_time_1, R.id.schedule_widget_course_time_2, R.id.schedule_widget_course_time_3)
        val titleIds = intArrayOf(R.id.schedule_widget_course_title_1, R.id.schedule_widget_course_title_2, R.id.schedule_widget_course_title_3)
        val roomIds = intArrayOf(R.id.schedule_widget_course_room_1, R.id.schedule_widget_course_room_2, R.id.schedule_widget_course_room_3)
        views.setViewVisibility(R.id.schedule_widget_empty, if (sessions.isEmpty()) View.VISIBLE else View.GONE)
        rowIds.indices.forEach { index ->
            val session = sessions.getOrNull(index)
            views.setViewVisibility(rowIds[index], if (session == null) View.GONE else View.VISIBLE)
            if (session != null) {
                views.setTextViewText(timeIds[index], sectionTime(session.startSection))
                views.setTextViewText(titleIds[index], session.courseName)
                views.setTextViewText(roomIds[index], session.location.ifBlank { "教室待定" })
                views.setOnClickPendingIntent(rowIds[index], openApp)
            }
        }
    }

    private fun openAppIntent(context: Context, appWidgetId: Int, destination: String): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_DESTINATION, destination)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        return PendingIntent.getActivity(
            context,
            "$appWidgetId-$destination".hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun currentWeek(context: Context): Int? {
        val value = context.getSharedPreferences("academic_settings", 0)
            .getString("semester_start_date", null)
            ?: return null
        val start = runCatching { LocalDate.parse(value) }.getOrNull() ?: return null
        return (ChronoUnit.DAYS.between(start, LocalDate.now()) / 7L + 1L).toInt().coerceAtLeast(1)
    }

    private fun ClassSessionEntity.weekNumbers(): Set<Int> {
        if (weeksText.isBlank() || weeksText.contains("全周")) return (1..30).toSet()
        val values = linkedSetOf<Int>()
        val oddOnly = weeksText.contains("单")
        val evenOnly = weeksText.contains("双")
        Regex("""(\d{1,2})(?:\s*[-~－至]\s*(\d{1,2}))?""").findAll(weeksText).forEach { match ->
            val start = match.groupValues[1].toInt()
            val end = match.groupValues.getOrNull(2)?.takeIf(String::isNotBlank)?.toInt() ?: start
            values += (start.coerceAtMost(end)..start.coerceAtLeast(end))
                .filter { (!oddOnly || it % 2 == 1) && (!evenOnly || it % 2 == 0) }
        }
        return if (values.isEmpty()) (1..30).toSet() else values
    }

    private fun sectionTime(section: Int): String {
        return when (section) {
            1 -> "08:30"
            2 -> "09:25"
            3 -> "10:30"
            4 -> "11:25"
            5 -> "12:20"
            6 -> "14:00"
            7 -> "14:55"
            8 -> "15:50"
            9 -> "16:45"
            10 -> "19:00"
            11 -> "19:55"
            12 -> "20:50"
            else -> "第${section}节"
        }
    }

    private fun formatExamTime(millis: Long): String {
        return Instant.ofEpochMilli(millis)
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("MM月dd日 HH:mm"))
    }
}
