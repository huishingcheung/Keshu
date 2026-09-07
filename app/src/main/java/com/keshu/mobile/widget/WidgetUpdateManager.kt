package com.keshu.mobile.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import java.time.Duration
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

object WidgetUpdateManager {
    private const val DAILY_REFRESH_WORK = "daily-widget-refresh"

    fun requestUpdate(context: Context) {
        requestReceiverUpdate(context, DashboardWidgetReceiver::class.java)
        requestReceiverUpdate(context, ScheduleWidgetReceiver::class.java)
    }

    fun scheduleDailyRefresh(context: Context) {
        val now = ZonedDateTime.now()
        val nextRefresh = now.toLocalDate().plusDays(1).atStartOfDay(now.zone).plusMinutes(1)
        val request = PeriodicWorkRequestBuilder<WidgetRefreshWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(Duration.between(now, nextRefresh))
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            DAILY_REFRESH_WORK,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    private fun requestReceiverUpdate(context: Context, receiverClass: Class<*>) {
        val manager = AppWidgetManager.getInstance(context)
        val component = ComponentName(context, receiverClass)
        val ids = manager.getAppWidgetIds(component)
        if (ids.isEmpty()) return
        context.sendBroadcast(
            Intent(context, receiverClass)
                .setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids),
        )
    }
}

class WidgetRefreshWorker(
    context: Context,
    params: WorkerParameters,
) : Worker(context, params) {
    override fun doWork(): Result {
        WidgetUpdateManager.requestUpdate(applicationContext)
        return Result.success()
    }
}
