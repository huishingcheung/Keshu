package com.keshu.mobile.domain.usecase

import com.keshu.mobile.data.local.dao.ExamDao
import com.keshu.mobile.data.local.dao.ClassScheduleDao
import com.keshu.mobile.data.local.dao.TaskDao
import com.keshu.mobile.data.local.entity.ClassSessionEntity
import com.keshu.mobile.data.local.entity.ExamEntity
import com.keshu.mobile.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

data class ScheduleSnapshot(
    val exams: List<ExamEntity>,
    val tasks: List<TaskEntity>,
    val classSessions: List<ClassSessionEntity>,
)

class ObserveScheduleUseCase(
    private val examDao: ExamDao,
    private val taskDao: TaskDao,
    private val classScheduleDao: ClassScheduleDao,
) {
    operator fun invoke(): Flow<ScheduleSnapshot> {
        return combine(
            examDao.observeUpcoming(),
            taskDao.observeAllTasks(),
            classScheduleDao.observeAll(),
        ) { exams, tasks, classSessions ->
            ScheduleSnapshot(
                exams = exams.distinctBy { it.id },
                tasks = tasks.distinctBy { it.id },
                classSessions = classSessions.distinctBy { it.id },
            )
        }
    }
}
