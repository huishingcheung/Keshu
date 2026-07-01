package edu.jnu.smartedu.widget

import androidx.compose.runtime.Composable
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.compose.ui.unit.dp
import androidx.room.Room
import edu.jnu.smartedu.data.local.JnuDatabase
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class DashboardWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: android.content.Context, id: androidx.glance.GlanceId) {
        val db = Room.databaseBuilder(context, JnuDatabase::class.java, "jnu_smart_edu.db")
            .addMigrations(JnuDatabase.MIGRATION_5_6, JnuDatabase.MIGRATION_6_7)
            .build()
        val tasks = runCatching { db.taskDao().getOpenTasks(2) }.getOrDefault(emptyList())
        val exams = runCatching { db.examDao().getUpcoming(limit = 2) }.getOrDefault(emptyList())
        db.close()
        provideContent { Content(tasks.map { it.title to it.dueAtMillis } + exams.map { it.courseName to it.startsAtMillis }) }
    }

    @Composable
    private fun Content(items: List<Pair<String, Long>>) {
        Column(GlanceModifier.fillMaxSize().padding(12.dp)) {
            Text("JNU Smart Edu", style = androidx.glance.text.TextStyle(color = GlanceTheme.colors.primary))
            if (items.isEmpty()) {
                Text("暂无待办与考试")
            } else {
                items.take(4).forEach { (title, time) ->
                    Text("${formatTime(time)}  $title")
                }
            }
        }
    }

    private fun formatTime(millis: Long): String {
        return Instant.ofEpochMilli(millis)
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("MM-dd HH:mm"))
    }
}
