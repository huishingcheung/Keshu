package edu.jnu.smartedu.data.repository

import edu.jnu.smartedu.data.local.dao.CourseDao
import edu.jnu.smartedu.data.local.dao.ExamDao
import edu.jnu.smartedu.data.local.dao.GroupDao
import edu.jnu.smartedu.data.local.dao.TaskDao
import edu.jnu.smartedu.data.local.dao.TranscriptCourseDao
import edu.jnu.smartedu.data.local.entity.TranscriptCourseEntity
import edu.jnu.smartedu.data.parser.AcademicHtmlParser
import edu.jnu.smartedu.data.parser.ImportPageType

class AcademicRepository(
    private val courseDao: CourseDao,
    private val classScheduleDao: edu.jnu.smartedu.data.local.dao.ClassScheduleDao,
    private val groupDao: GroupDao,
    private val examDao: ExamDao,
    @Suppress("unused") private val taskDao: TaskDao,
    private val transcriptCourseDao: TranscriptCourseDao,
    private val parser: AcademicHtmlParser,
) {
    suspend fun importCurrentPage(html: String, pageType: ImportPageType): ImportSummary {
        val parsed = parser.parse(html, pageType)
        parsed.academicProgress?.let { groupDao.upsertAcademicProgress(it) }
        if (parsed.largeGroups.isNotEmpty()) groupDao.upsertLargeGroups(parsed.largeGroups)
        if (parsed.smallGroups.isNotEmpty()) groupDao.upsertSmallGroups(parsed.smallGroups)
        if (parsed.courses.isNotEmpty()) {
            if (pageType == ImportPageType.TRANSCRIPT) {
                importTranscriptCourses(parsed.courses)
            } else {
                courseDao.upsertAll(parsed.courses)
            }
        }
        if (parsed.exams.isNotEmpty()) examDao.upsertAll(parsed.exams)
        if (parsed.classSessions.isNotEmpty()) {
            classScheduleDao.upsertAll(parsed.classSessions)
            parsed.classSessions
                .filter { it.courseCode.isNotBlank() }
                .distinctBy { it.courseCode }
                .forEach { session ->
                    courseDao.markTakingByCode(session.courseCode, session.term)
                }
        }
        return ImportSummary(
            largeGroups = parsed.largeGroups.size,
            smallGroups = parsed.smallGroups.size,
            courses = parsed.courses.size,
            exams = parsed.exams.size,
            classSessions = parsed.classSessions.size,
        )
    }

    private suspend fun importTranscriptCourses(courses: List<edu.jnu.smartedu.data.local.entity.CourseEntity>) {
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
            listOf(edu.jnu.smartedu.data.local.entity.LargeGroupEntity(fallbackLargeId, "成绩单未归类", 0.0)),
        )
        groupDao.upsertSmallGroups(
            listOf(edu.jnu.smartedu.data.local.entity.SmallGroupEntity(fallbackSmallId, fallbackLargeId, null, "成绩单未归类", 0.0)),
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
