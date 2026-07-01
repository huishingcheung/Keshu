package edu.jnu.smartedu.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import edu.jnu.smartedu.JnuSmartEduApp
import edu.jnu.smartedu.MainActivity
import edu.jnu.smartedu.R
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DashboardWidgetReceiver : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = (context.applicationContext as JnuSmartEduApp).container.database
                val items = database.taskDao().getOpenTasks(2).map { it.title to it.dueAtMillis } +
                    database.examDao().getUpcoming(limit = 2).map { it.courseName to it.startsAtMillis }
                val body = items
                    .sortedBy { it.second }
                    .take(4)
                    .joinToString("\n") { (title, time) -> "${formatTime(time)}  $title" }
                    .ifBlank { "暂无待办与考试" }
                appWidgetIds.forEach { appWidgetId ->
                    val pendingIntent = PendingIntent.getActivity(
                        context,
                        appWidgetId,
                        Intent(context, MainActivity::class.java),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                    )
                    val views = RemoteViews(context.packageName, R.layout.dashboard_widget_layout).apply {
                        setTextViewText(R.id.dashboard_widget_body, body)
                        setOnClickPendingIntent(R.id.dashboard_widget_root, pendingIntent)
                    }
                    appWidgetManager.updateAppWidget(appWidgetId, views)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun formatTime(millis: Long): String {
        return Instant.ofEpochMilli(millis)
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("MM-dd HH:mm"))
    }
}
