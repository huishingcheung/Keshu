package edu.jnu.smartedu.domain.usecase

import edu.jnu.smartedu.background.ExamAlarmScheduler
import edu.jnu.smartedu.data.local.dao.ExamDao

class ScheduleExamAlarmsUseCase(
    private val examDao: ExamDao,
    private val scheduler: ExamAlarmScheduler,
) {
    suspend operator fun invoke() {
        examDao.getAll().forEach { exam ->
            scheduler.scheduleExamCountdowns(exam)
        }
    }
}
