package com.keshu.mobile.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.RemoteViews
import com.keshu.mobile.KeshuApp
import com.keshu.mobile.MainActivity
import com.keshu.mobile.R
import com.keshu.mobile.data.local.ClassTimePreferences
import com.keshu.mobile.data.local.ClassTimeSettings
import com.keshu.mobile.data.local.DaySchedule
import com.keshu.mobile.data.local.HolidayCalendar
import com.keshu.mobile.data.local.HolidayEntry
import com.keshu.mobile.data.local.HolidayPreferences
import com.keshu.mobile.data.local.byDate
import com.keshu.mobile.data.local.resolveDaySchedule
import com.keshu.mobile.data.local.timetableWeekday
import com.keshu.mobile.data.local.entity.ClassSessionEntity
import com.keshu.mobile.domain.weekNumbers
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
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
                val container = (context.applicationContext as KeshuApp).container
                val database = container.database
                val examAvailability = container.examAvailabilityPreferences.getUnavailable()
                val allSessions = database.classScheduleDao().getAll()
                val currentTerm = allSessions.firstOrNull()?.term
                val currentWeek = currentWeek(context)
                val academicSettings = context.getSharedPreferences("academic_settings", Context.MODE_PRIVATE)
                val classTimeSettings = ClassTimePreferences(academicSettings).load()
                val holidayEntries = HolidayCalendar.entries(
                    HolidayPreferences(academicSettings).loadConfirmedMakeupWeekdays(),
                ).byDate()
                val today = LocalDate.now()
                val todaySchedule = resolveDaySchedule(today, holidayEntries)
                val todayWeekday = todaySchedule.timetableWeekday()
                val sessions = todayWeekday
                    ?.let { weekday ->
                        allSessions
                            .asSequence()
                            .filter { currentTerm == null || it.term == currentTerm }
                            .filter { it.dayOfWeek == weekday.value }
                            .filter { currentWeek == null || currentWeek in it.weekNumbers() }
                            .distinctBy { it.id }
                            .take(3)
                            .toList()
                    }
                    .orEmpty()
                val exam = database.examDao().getUpcoming(limit = 1).firstOrNull()
                appWidgetIds.forEach { appWidgetId ->
                    val minHeightDp = appWidgetManager.getAppWidgetOptions(appWidgetId)
                        .getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
                    val hasExamInfo = exam != null || examAvailability != null
                    val compact = minHeightDp < 190
                    val maxCourseRows = scheduleMaxCourseRowsForHeight(minHeightDp, hasExamInfo)
                    val views = RemoteViews(context.packageName, R.layout.schedule_widget_layout)
                    val openSchedule = openAppIntent(context, appWidgetId, MainActivity.DESTINATION_SCHEDULE)
                    views.setOnClickPendingIntent(R.id.schedule_widget_header, openSchedule)
                    views.setOnClickPendingIntent(R.id.schedule_widget_title, openSchedule)
                    views.setTextViewText(
                        R.id.schedule_widget_date,
                        buildString {
                            append(today.format(DateTimeFormatter.ofPattern("M月d日 EEEE", Locale.CHINA)))
                            todaySchedule.dateSuffix(holidayEntries[today])?.let {
                                append(" · ")
                                append(it)
                            }
                        },
                    )
                    bindSessions(
                        views = views,
                        sessions = sessions.take(maxCourseRows),
                        openApp = openSchedule,
                        showEmptyState = sessions.isEmpty() && !(compact && hasExamInfo),
                        classTimeSettings = classTimeSettings,
                    )
                    views.setViewVisibility(
                        R.id.schedule_widget_exam,
                        if (exam == null && examAvailability == null) View.GONE else View.VISIBLE,
                    )
                    if (exam != null) {
                        views.setTextViewText(R.id.schedule_widget_exam_title, exam.courseName)
                        views.setTextViewText(
                            R.id.schedule_widget_exam_meta,
                            "${formatExamTime(exam.startsAtMillis)} · ${exam.location}",
                        )
                    } else if (examAvailability != null) {
                        views.setTextViewText(R.id.schedule_widget_exam_title, "考试安排当前不可查看")
                        views.setTextViewText(
                            R.id.schedule_widget_exam_meta,
                            examAvailability.visibilityRange
                                ?.takeIf { it.isNotBlank() }
                                ?.let { "可查看时间 $it" }
                                ?: "学校当前未开放考试安排",
                        )
                    }
                    views.setOnClickPendingIntent(
                        R.id.schedule_widget_exam,
                        openAppIntent(context, appWidgetId, MainActivity.DESTINATION_EXAM),
                    )
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) {
        onUpdate(context, appWidgetManager, intArrayOf(appWidgetId))
    }

    private fun bindSessions(
        views: RemoteViews,
        sessions: List<ClassSessionEntity>,
        openApp: PendingIntent,
        showEmptyState: Boolean,
        classTimeSettings: ClassTimeSettings,
    ) {
        val rowIds = intArrayOf(R.id.schedule_widget_course_1, R.id.schedule_widget_course_2, R.id.schedule_widget_course_3)
        val timeIds = intArrayOf(R.id.schedule_widget_course_time_1, R.id.schedule_widget_course_time_2, R.id.schedule_widget_course_time_3)
        val titleIds = intArrayOf(R.id.schedule_widget_course_title_1, R.id.schedule_widget_course_title_2, R.id.schedule_widget_course_title_3)
        val roomIds = intArrayOf(R.id.schedule_widget_course_room_1, R.id.schedule_widget_course_room_2, R.id.schedule_widget_course_room_3)
        views.setViewVisibility(R.id.schedule_widget_empty, if (showEmptyState) View.VISIBLE else View.GONE)
        rowIds.indices.forEach { index ->
            val session = sessions.getOrNull(index)
            views.setViewVisibility(rowIds[index], if (session == null) View.GONE else View.VISIBLE)
            if (session != null) {
                views.setTextViewText(
                    timeIds[index],
                    classTimeSettings.period(session.startSection)?.start ?: "第${session.startSection}节",
                )
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
        val firstWeekStart = start.minusDays((start.dayOfWeek.value - 1).toLong())
        return (ChronoUnit.DAYS.between(firstWeekStart, LocalDate.now()) / 7L + 1L)
            .toInt()
            .coerceAtLeast(1)
    }

    private fun formatExamTime(millis: Long): String {
        return Instant.ofEpochMilli(millis)
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("MM月dd日 HH:mm"))
    }
}

internal fun scheduleMaxCourseRowsForHeight(minHeightDp: Int, hasExamInfo: Boolean): Int = when {
    minHeightDp < 190 && hasExamInfo -> 0
    minHeightDp < 240 -> 1
    minHeightDp < 285 -> 2
    else -> 3
}

private val widgetWeekdayNames = listOf("一", "二", "三", "四", "五", "六", "日")

/**
 * Extra context for the widget's date line: the holiday name, or the make-up weekday.
 *
 * A convention default is labelled as a guess, matching the timetable, because the make-up weekday
 * is the school's decision rather than part of the State Council arrangement.
 */
internal fun DaySchedule.dateSuffix(entry: HolidayEntry?): String? = when (this) {
    is DaySchedule.Holiday -> entry?.label ?: "放假"
    is DaySchedule.MakeupUnconfirmed -> "调休上课，补课星期待确认"
    is DaySchedule.Makeup ->
        "补周" + widgetWeekdayNames[weekday.value - 1] + if (assumed) "（推测）" else ""
    is DaySchedule.Normal -> null
}
