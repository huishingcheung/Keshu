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
import com.keshu.mobile.data.local.entity.TaskEntity
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DashboardWidgetReceiver : AppWidgetProvider() {
    override fun onEnabled(context: Context) {
        WidgetUpdateManager.scheduleDailyRefresh(context)
        WidgetUpdateManager.requestUpdate(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_COMPLETE_TASK) {
            val taskId = intent.getStringExtra(EXTRA_TASK_ID) ?: return
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val container = (context.applicationContext as KeshuApp).container
                    container.database.taskDao().getById(taskId)?.let { task ->
                        container.addTaskUseCase.setDone(task, true)
                    }
                } finally {
                    pendingResult.finish()
                }
            }
            return
        }
        super.onReceive(context, intent)
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = (context.applicationContext as KeshuApp).container.database
                val tasks = database.taskDao().getOpenTasks(3).distinctBy { it.id }
                val openTaskCount = database.taskDao().getOpenTaskCount()
                appWidgetIds.forEach { appWidgetId ->
                    val maxRows = dashboardMaxRowsForHeight(
                        appWidgetManager.getAppWidgetOptions(appWidgetId)
                            .getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT),
                    )
                    appWidgetManager.updateAppWidget(
                        appWidgetId,
                        buildViews(context, appWidgetId, tasks.take(maxRows), openTaskCount),
                    )
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

    private fun buildViews(context: Context, appWidgetId: Int, tasks: List<TaskEntity>, openTaskCount: Int): RemoteViews {
        val views = RemoteViews(context.packageName, R.layout.dashboard_widget_layout)
        val openApp = openAppIntent(context, appWidgetId, addTask = false)
        views.setOnClickPendingIntent(R.id.task_widget_header, openApp)
        views.setOnClickPendingIntent(R.id.task_widget_title, openApp)
        views.setTextViewText(R.id.task_widget_count, openTaskCount.toString())
        views.setViewVisibility(R.id.task_widget_empty, if (tasks.isEmpty()) View.VISIBLE else View.GONE)
        views.setOnClickPendingIntent(R.id.task_widget_add, openAppIntent(context, appWidgetId, addTask = true))

        val rowIds = intArrayOf(R.id.task_widget_row_1, R.id.task_widget_row_2, R.id.task_widget_row_3)
        val checkIds = intArrayOf(R.id.task_widget_check_1, R.id.task_widget_check_2, R.id.task_widget_check_3)
        val titleIds = intArrayOf(R.id.task_widget_item_title_1, R.id.task_widget_item_title_2, R.id.task_widget_item_title_3)
        val dateIds = intArrayOf(R.id.task_widget_item_date_1, R.id.task_widget_item_date_2, R.id.task_widget_item_date_3)
        rowIds.indices.forEach { index ->
            val task = tasks.getOrNull(index)
            views.setViewVisibility(rowIds[index], if (task == null) View.GONE else View.VISIBLE)
            if (task != null) {
                views.setTextViewText(titleIds[index], task.title)
                views.setTextViewText(dateIds[index], formatDate(task.dueAtMillis))
                views.setOnClickPendingIntent(checkIds[index], completeTaskIntent(context, appWidgetId, task))
                views.setOnClickPendingIntent(titleIds[index], openApp)
                views.setOnClickPendingIntent(dateIds[index], openApp)
            }
        }
        return views
    }

    private fun openAppIntent(context: Context, appWidgetId: Int, addTask: Boolean): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_DESTINATION, MainActivity.DESTINATION_HOME)
            .putExtra(MainActivity.EXTRA_ADD_TASK, addTask)
            .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        return PendingIntent.getActivity(
            context,
            appWidgetId * 10 + if (addTask) 1 else 0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun completeTaskIntent(context: Context, appWidgetId: Int, task: TaskEntity): PendingIntent {
        val intent = Intent(context, DashboardWidgetReceiver::class.java)
            .setAction(ACTION_COMPLETE_TASK)
            .putExtra(EXTRA_TASK_ID, task.id)
        return PendingIntent.getBroadcast(
            context,
            "$appWidgetId-${task.id}".hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun formatDate(millis: Long): String {
        return Instant.ofEpochMilli(millis)
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("MM月dd日截止"))
    }

    companion object {
        private const val ACTION_COMPLETE_TASK = "com.keshu.mobile.widget.COMPLETE_TASK"
        private const val EXTRA_TASK_ID = "task_id"
    }
}

internal fun dashboardMaxRowsForHeight(minHeightDp: Int): Int = when {
    minHeightDp < 180 -> 1
    minHeightDp < 240 -> 2
    else -> 3
}
