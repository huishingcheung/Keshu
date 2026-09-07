package com.keshu.mobile.domain.usecase

import com.keshu.mobile.background.ExamAlarmScheduler
import com.keshu.mobile.data.local.dao.ExamDao

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
