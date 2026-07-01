package edu.jnu.smartedu.domain.usecase

import android.content.Context
import edu.jnu.smartedu.background.ExamAlarmScheduler
import edu.jnu.smartedu.data.local.dao.TaskDao
import edu.jnu.smartedu.data.local.entity.TaskEntity
import edu.jnu.smartedu.widget.WidgetUpdateManager
import java.nio.charset.StandardCharsets
import java.util.UUID

class AddTaskUseCase(
    private val taskDao: TaskDao,
    private val scheduler: ExamAlarmScheduler,
    private val context: Context,
) {
    suspend operator fun invoke(title: String, dueAtMillis: Long): TaskEntity {
        require(title.isNotBlank()) { "任务标题不能为空" }
        require(dueAtMillis > System.currentTimeMillis()) { "截止时间必须晚于当前时间" }
        val task = TaskEntity(
            id = UUID.nameUUIDFromBytes("${title.trim()}-$dueAtMillis".toByteArray(StandardCharsets.UTF_8)).toString(),
            title = title.trim(),
            dueAtMillis = dueAtMillis,
            sourceText = "手动添加",
        )
        taskDao.upsert(task)
        runCatching { scheduler.scheduleTaskCountdowns(task) }
        WidgetUpdateManager.requestUpdate(context)
        return task
    }

    suspend fun update(task: TaskEntity, title: String, dueAtMillis: Long): TaskEntity {
        require(title.isNotBlank()) { "任务标题不能为空" }
        scheduler.cancelTaskCountdowns(task)
        val updated = task.copy(title = title.trim(), dueAtMillis = dueAtMillis, sourceText = "手动添加")
        taskDao.upsert(updated)
        if (!updated.done && updated.dueAtMillis > System.currentTimeMillis()) {
            runCatching { scheduler.scheduleTaskCountdowns(updated) }
        }
        WidgetUpdateManager.requestUpdate(context)
        return updated
    }

    suspend fun setDone(task: TaskEntity, done: Boolean) {
        scheduler.cancelTaskCountdowns(task)
        taskDao.setDone(task.id, done)
        if (!done && task.dueAtMillis > System.currentTimeMillis()) {
            runCatching { scheduler.scheduleTaskCountdowns(task.copy(done = false)) }
        }
        WidgetUpdateManager.requestUpdate(context)
    }

    suspend fun delete(task: TaskEntity) {
        scheduler.cancelTaskCountdowns(task)
        taskDao.deleteById(task.id)
        WidgetUpdateManager.requestUpdate(context)
    }
}
