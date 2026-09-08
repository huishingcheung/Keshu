package com.keshu.mobile.data.source

import com.keshu.mobile.data.local.entity.AcademicProgressEntity
import com.keshu.mobile.data.local.entity.ClassSessionEntity
import com.keshu.mobile.data.local.entity.CourseEntity
import com.keshu.mobile.data.local.entity.ExamEntity
import com.keshu.mobile.data.local.entity.LargeGroupEntity
import com.keshu.mobile.data.local.entity.SmallGroupEntity

data class AcademicDataSourceDescriptor(
    val id: String,
    val displayName: String,
)

enum class AcademicDataset {
    CURRICULUM,
    SCHEDULE,
    EXAMS,
    TRANSCRIPT,
}

data class AcademicImportData(
    val academicProgress: AcademicProgressEntity? = null,
    val largeGroups: List<LargeGroupEntity> = emptyList(),
    val smallGroups: List<SmallGroupEntity> = emptyList(),
    val courses: List<CourseEntity> = emptyList(),
    val exams: List<ExamEntity> = emptyList(),
    val classSessions: List<ClassSessionEntity> = emptyList(),
)

sealed interface ExamDataResult {
    data class Available(val data: AcademicImportData) : ExamDataResult
    data class Unavailable(val visibilityRange: String?) : ExamDataResult
}

interface AcademicDataSource {
    val descriptor: AcademicDataSourceDescriptor

    suspend fun loadCurriculum(): AcademicImportData
    suspend fun loadSchedule(): AcademicImportData
    suspend fun loadExams(): ExamDataResult
}
