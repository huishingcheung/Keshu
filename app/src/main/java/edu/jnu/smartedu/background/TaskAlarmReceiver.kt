package edu.jnu.smartedu.background

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import edu.jnu.smartedu.R

class TaskAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "DDL 提醒", NotificationManager.IMPORTANCE_HIGH),
        )
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(context).notify(
                intent.getStringExtra(EXTRA_TITLE).orEmpty().hashCode(),
                NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_stat_exam)
                    .setContentTitle(intent.getStringExtra(EXTRA_TITLE).orEmpty())
                    .setContentText(intent.getStringExtra(EXTRA_TEXT).orEmpty())
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .build(),
            )
        }
    }

    companion object {
        const val EXTRA_TITLE = "title"
        const val EXTRA_TEXT = "text"
        private const val CHANNEL_ID = "task_alarm"
    }
}
