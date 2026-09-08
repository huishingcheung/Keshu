package com.keshu.mobile.presentation.sync

import android.util.Log
import com.keshu.mobile.data.local.ExamAvailabilityStore
import com.keshu.mobile.data.repository.ImportSummary
import com.keshu.mobile.data.source.AcademicDataSource
import com.keshu.mobile.data.source.AcademicDataset
import com.keshu.mobile.data.source.AcademicImportData
import com.keshu.mobile.data.source.ExamDataResult
import kotlinx.coroutines.CancellationException

internal class ManualSyncCoordinator(
    private val dataSource: AcademicDataSource,
    private val importData: suspend (AcademicImportData, AcademicDataset) -> ImportSummary,
    private val examAvailabilityStore: ExamAvailabilityStore,
) {
    suspend fun run(): String {
        val curriculum = importData(dataSource.loadCurriculum(), AcademicDataset.CURRICULUM)
        val schedule = importData(dataSource.loadSchedule(), AcademicDataset.SCHEDULE)
        val examOutcome = importExams()
        persistExamAvailability(examOutcome)

        return AutoImportSummary(
            groups = curriculum.largeGroups + curriculum.smallGroups,
            courses = curriculum.courses,
            schedule = schedule.classSessions,
            exams = examOutcome,
        ).toUserMessage()
    }

    private suspend fun importExams(): ExamImportOutcome {
        return try {
            when (val result = dataSource.loadExams()) {
                is ExamDataResult.Available -> {
                    val imported = importData(result.data, AcademicDataset.EXAMS).exams
                    ExamImportOutcome.Imported(imported)
                }
                is ExamDataResult.Unavailable -> ExamImportOutcome.Unavailable(result.visibilityRange)
            }
        } catch (throwable: CancellationException) {
            throw throwable
        } catch (throwable: Throwable) {
            Log.w(TAG, "考试安排暂未同步，其他教务数据已保留", throwable)
            ExamImportOutcome.Failed(throwable.message)
        }
    }

    private fun persistExamAvailability(outcome: ExamImportOutcome) {
        when (outcome) {
            is ExamImportOutcome.Imported -> examAvailabilityStore.clearUnavailable()
            is ExamImportOutcome.Unavailable -> examAvailabilityStore.markUnavailable(outcome.visibilityRange)
            is ExamImportOutcome.Failed -> Unit
        }
    }

    private companion object {
        const val TAG = "ManualSyncCoordinator"
    }
}
