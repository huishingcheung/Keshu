package edu.jnu.smartedu.domain.usecase

import edu.jnu.smartedu.data.local.dao.ExamDao
import edu.jnu.smartedu.data.local.dao.ClassScheduleDao
import edu.jnu.smartedu.data.local.dao.TaskDao
import edu.jnu.smartedu.data.local.entity.ClassSessionEntity
import edu.jnu.smartedu.data.local.entity.ExamEntity
import edu.jnu.smartedu.data.local.entity.TaskEntity
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
            ScheduleSnapshot(exams = exams, tasks = tasks, classSessions = classSessions)
        }
    }
}
