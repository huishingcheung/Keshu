package com.keshu.mobile.background

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.keshu.mobile.data.local.entity.ExamEntity
import com.keshu.mobile.data.local.entity.TaskEntity

class ExamAlarmScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun scheduleExamCountdowns(exam: ExamEntity) {
        listOf(
            7L * 24 * 60 * 60 * 1000 to "一周",
            24L * 60 * 60 * 1000 to "24小时",
            2L * 60 * 60 * 1000 to "2小时",
        ).forEach { (offset, label) ->
            val triggerAt = exam.startsAtMillis - offset
            if (triggerAt > System.currentTimeMillis()) {
                schedule(triggerAt, pendingIntent(exam, label))
            }
        }
    }

    fun scheduleTaskCountdowns(task: TaskEntity) {
        listOf(
            24L * 60 * 60 * 1000 to "24小时",
            2L * 60 * 60 * 1000 to "2小时",
        ).forEach { (offset, label) ->
            val triggerAt = task.dueAtMillis - offset
            if (triggerAt > System.currentTimeMillis()) {
                schedule(triggerAt, taskPendingIntent(task, label))
            }
        }
    }

    fun cancelTaskCountdowns(task: TaskEntity) {
        listOf("24小时", "2小时").forEach { label ->
            alarmManager.cancel(taskPendingIntent(task, label))
        }
    }

    private fun schedule(triggerAt: Long, operation: PendingIntent) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarmManager.canScheduleExactAlarms()) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, operation)
        } else {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, operation)
        }
    }

    private fun pendingIntent(exam: ExamEntity, label: String): PendingIntent {
        val intent = Intent(context, ExamAlarmReceiver::class.java)
            .putExtra(ExamAlarmReceiver.EXTRA_TITLE, "${exam.courseName} 考前$label")
            .putExtra(ExamAlarmReceiver.EXTRA_TEXT, "${exam.location} ${exam.seatNo.orEmpty()}")
        return PendingIntent.getBroadcast(
            context,
            "${exam.id}-$label".hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun taskPendingIntent(task: TaskEntity, label: String): PendingIntent {
        val intent = Intent(context, TaskAlarmReceiver::class.java)
            .putExtra(TaskAlarmReceiver.EXTRA_TITLE, "${task.title} 截止前$label")
            .putExtra(TaskAlarmReceiver.EXTRA_TEXT, task.sourceText)
        return PendingIntent.getBroadcast(
            context,
            "${task.id}-$label".hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
