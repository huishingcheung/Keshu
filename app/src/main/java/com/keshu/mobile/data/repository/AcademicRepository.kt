package com.keshu.mobile.data.repository

import com.keshu.mobile.data.local.dao.CourseDao
import com.keshu.mobile.data.local.dao.ExamDao
import com.keshu.mobile.data.local.dao.GroupDao
import com.keshu.mobile.data.local.dao.TaskDao
import com.keshu.mobile.data.local.dao.TranscriptCourseDao
import com.keshu.mobile.data.local.entity.TranscriptCourseEntity
import com.keshu.mobile.data.source.AcademicDataset
import com.keshu.mobile.data.source.AcademicImportData

class AcademicRepository(
    private val courseDao: CourseDao,
    private val classScheduleDao: com.keshu.mobile.data.local.dao.ClassScheduleDao,
    private val groupDao: GroupDao,
    private val examDao: ExamDao,
    @Suppress("unused") private val taskDao: TaskDao,
    private val transcriptCourseDao: TranscriptCourseDao,
) {
    suspend fun importData(data: AcademicImportData, dataset: AcademicDataset): ImportSummary {
        data.academicProgress?.let { groupDao.upsertAcademicProgress(it) }
        if (data.largeGroups.isNotEmpty()) groupDao.upsertLargeGroups(data.largeGroups)
        if (data.smallGroups.isNotEmpty()) groupDao.upsertSmallGroups(data.smallGroups)
        if (data.courses.isNotEmpty()) {
            if (dataset == AcademicDataset.TRANSCRIPT) {
                importTranscriptCourses(data.courses)
            } else {
                courseDao.upsertAll(data.courses)
            }
        }
        if (data.exams.isNotEmpty()) examDao.upsertAll(data.exams)
        if (data.classSessions.isNotEmpty()) {
            classScheduleDao.upsertAll(data.classSessions)
            data.classSessions
                .filter { it.courseCode.isNotBlank() }
                .distinctBy { it.courseCode }
                .forEach { session ->
                    courseDao.markTakingByCode(session.courseCode, session.term)
                }
        }
        return ImportSummary(
            largeGroups = data.largeGroups.size,
            smallGroups = data.smallGroups.size,
            courses = data.courses.size,
            exams = data.exams.size,
            classSessions = data.classSessions.size,
        )
    }

    private suspend fun importTranscriptCourses(courses: List<com.keshu.mobile.data.local.entity.CourseEntity>) {
        transcriptCourseDao.upsertAll(
            courses.map { course ->
                TranscriptCourseEntity(
                    id = course.id,
                    code = course.code,
                    name = course.name,
                    credit = course.credit,
                    status = course.status,
                )
            },
        )
        val fallbackLargeId = "large-unclassified-transcript"
        val fallbackSmallId = "small-unclassified-transcript"
        groupDao.upsertLargeGroups(
            listOf(com.keshu.mobile.data.local.entity.LargeGroupEntity(fallbackLargeId, "成绩单未归类", 0.0)),
        )
        groupDao.upsertSmallGroups(
            listOf(com.keshu.mobile.data.local.entity.SmallGroupEntity(fallbackSmallId, fallbackLargeId, null, "成绩单未归类", 0.0)),
        )
        courses.forEach { course ->
            if (courseDao.getById(course.id) == null) {
                courseDao.upsertAll(listOf(course.copy(smallGroupId = fallbackSmallId)))
            } else {
                courseDao.updateTranscriptFields(course.id, course.status, course.score, course.term)
            }
        }
    }

}

data class ImportSummary(
    val largeGroups: Int,
    val smallGroups: Int,
    val courses: Int,
    val exams: Int,
    val classSessions: Int,
)
