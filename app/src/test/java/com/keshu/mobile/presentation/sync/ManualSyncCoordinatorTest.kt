package com.keshu.mobile.presentation.sync

import com.keshu.mobile.data.local.ExamAvailabilityStore
import com.keshu.mobile.data.repository.ImportSummary
import com.keshu.mobile.data.source.AcademicDataSource
import com.keshu.mobile.data.source.AcademicDataSourceDescriptor
import com.keshu.mobile.data.source.AcademicDataset
import com.keshu.mobile.data.source.AcademicImportData
import com.keshu.mobile.data.source.ExamDataResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ManualSyncCoordinatorTest {
    @Test
    fun `imports normalized datasets without portal-specific dependencies`() = runTest {
        val importedDatasets = mutableListOf<AcademicDataset>()
        val availability = FakeExamAvailabilityStore()
        val coordinator = ManualSyncCoordinator(
            dataSource = FakeDataSource(ExamDataResult.Unavailable("2026年11月16日—12月27日")),
            importData = { _, dataset ->
                importedDatasets += dataset
                when (dataset) {
                    AcademicDataset.CURRICULUM -> summary(groups = 3, courses = 8)
                    AcademicDataset.SCHEDULE -> summary(sessions = 12)
                    else -> summary()
                }
            },
            examAvailabilityStore = availability,
        )

        val message = coordinator.run()

        assertEquals(listOf(AcademicDataset.CURRICULUM, AcademicDataset.SCHEDULE), importedDatasets)
        assertEquals("2026年11月16日—12月27日", availability.unavailableRange)
        assertFalse(availability.wasCleared)
        assertTrue(message.contains("课群3 课程8 课表12"))
        assertTrue(message.contains("考试安排当前不可查看"))
    }

    @Test
    fun `persists available exams and clears a previous unavailable state`() = runTest {
        val importedDatasets = mutableListOf<AcademicDataset>()
        val availability = FakeExamAvailabilityStore()
        val coordinator = ManualSyncCoordinator(
            dataSource = FakeDataSource(ExamDataResult.Available(AcademicImportData())),
            importData = { _, dataset ->
                importedDatasets += dataset
                if (dataset == AcademicDataset.EXAMS) summary(exams = 2) else summary()
            },
            examAvailabilityStore = availability,
        )

        val message = coordinator.run()

        assertEquals(
            listOf(AcademicDataset.CURRICULUM, AcademicDataset.SCHEDULE, AcademicDataset.EXAMS),
            importedDatasets,
        )
        assertTrue(availability.wasCleared)
        assertEquals(null, availability.unavailableRange)
        assertTrue(message.endsWith("考试2"))
    }

    private class FakeDataSource(
        private val examResult: ExamDataResult,
    ) : AcademicDataSource {
        override val descriptor = AcademicDataSourceDescriptor("synthetic", "Synthetic University")

        override suspend fun loadCurriculum() = AcademicImportData()
        override suspend fun loadSchedule() = AcademicImportData()
        override suspend fun loadExams() = examResult
    }

    private class FakeExamAvailabilityStore : ExamAvailabilityStore {
        var unavailableRange: String? = null
        var wasCleared = false

        override fun markUnavailable(visibilityRange: String?) {
            unavailableRange = visibilityRange
        }

        override fun clearUnavailable() {
            wasCleared = true
            unavailableRange = null
        }
    }

    private companion object {
        fun summary(
            groups: Int = 0,
            courses: Int = 0,
            exams: Int = 0,
            sessions: Int = 0,
        ) = ImportSummary(
            largeGroups = groups,
            smallGroups = 0,
            courses = courses,
            exams = exams,
            classSessions = sessions,
        )
    }
}
