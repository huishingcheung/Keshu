package edu.jnu.smartedu.data.parser

import edu.jnu.smartedu.data.local.entity.CourseStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AcademicHtmlParserTest {
    private val parser = AcademicHtmlParser()

    @Test
    fun parsesCurriculumTreeFromJmindNodes() {
        val result = parser.parse(fixture("curriculum_groups.html"), ImportPageType.CURRICULUM_GROUPS)

        assertTrue(result.largeGroups.size >= 3)
        assertEquals(160.0, result.academicProgress?.requiredCredits ?: 0.0, 0.0)
        assertEquals(122.5, result.academicProgress?.earnedCredits ?: 0.0, 0.0)
        assertTrue(result.largeGroups.any { it.requiredCredits == 54.0 && it.earnedCredits == 53.0 })
        assertTrue(result.smallGroups.any { it.requiredCredits == 10.0 })
        assertTrue(result.smallGroups.any { it.requiredCredits == 38.5 })
        assertTrue(result.smallGroups.size >= 25)
        val constitution = result.smallGroups.first { it.name.contains("宪法") || it.name.contains("法律") }
        assertTrue(constitution.parentSmallGroupId != null)
        assertEquals(3, constitution.depth)
    }

    @Test
    fun parsesSelectedSmallGroupCourses() {
        val result = parser.parse(fixture("small_group_detail_1.html"), ImportPageType.CURRICULUM_COURSES)

        assertTrue(result.courses.isNotEmpty())
        assertTrue(result.courses.any { it.code == "08060067" })
        assertTrue(result.courses.any { it.status == CourseStatus.NOT_TAKEN })
        assertEquals(1, result.courses.map { it.smallGroupId }.distinct().size)
    }

    @Test
    fun parsesManualDetailStatusesFromSavedHtml() {
        val result = parser.parse(fixture("small_group_detail_7.html"), ImportPageType.CURRICULUM_COURSES)

        assertTrue(result.courses.any { it.status == CourseStatus.PASSED })
        assertTrue(result.courses.any { it.status == CourseStatus.TAKING })
        assertTrue(result.courses.any { it.status == CourseStatus.NOT_TAKEN })
    }

    @Test
    fun parsesBulkCurriculumCourseJson() {
        val result = parser.parse(
            """
            {
              "type": "JNU_ALL_COURSES",
              "groups": [
                {
                  "id": "leaf-a",
                  "title": "信息应用知识群",
                  "rows": [
                    {"KCH": "08060067", "KCM": "数据结构", "KCXF": "3", "SFTG": "1", "CJ": "89", "XNXQDM_DISPLAY": "2025-2026-1", "KCXZDM_DISPLAY": "必修"}
                  ]
                },
                {
                  "id": "leaf-b",
                  "title": "算法课程群",
                  "rows": [
                    {"KCH": "08060068", "KCM": "算法设计", "KCXF": 2.5, "SFTG_DISPLAY": "未修读"}
                  ]
                },
                {
                  "id": "leaf-c",
                  "title": "taking-course-group",
                  "rows": [
                    {"KCH": "08060069", "KCM": "Taking Course", "KCXF": "2.0", "SFTG": "4", "XNXQDM_DISPLAY": "2025-2026-2"}
                  ]
                },
                {
                  "id": "leaf-d",
                  "title": "object-status-group",
                  "rows": [
                    {"KCH": "08060070", "KCM": "Object Status Course", "KCXF": "2.0", "SFTG": {"id": "4", "name": "已选课"}, "SFTG_DISPLAY": "-"}
                  ]
                },
                {
                  "id": "leaf-e",
                  "title": "pending-selected-group",
                  "rows": [
                    {"KCH": "08060202", "KCM": "软件工程", "KCXF": "3.0", "SFTG_DISPLAY": "未修读(待选)", "XNXQDM_DISPLAY": "2025-2026学年 第2学期"}
                  ]
                },
                {
                  "id": "leaf-f",
                  "title": "sfjg-taking-group",
                  "rows": [
                    {"KCH": "08060203", "KCM": "移动开发", "KCXF": "2.0", "SFJG": "在读", "XNXQDM_DISPLAY": "2025-2026学年 第2学期"}
                  ]
                }
              ]
            }
            """.trimIndent(),
            ImportPageType.ALL_CURRICULUM_COURSES,
        )

        assertEquals(6, result.courses.size)
        assertTrue(result.courses.any { it.code == "08060067" && it.smallGroupId == "small-leaf-a" && it.status == CourseStatus.PASSED })
        assertTrue(result.courses.any { it.code == "08060068" && it.smallGroupId == "small-leaf-b" && it.status == CourseStatus.NOT_TAKEN })
        assertTrue(result.courses.any { it.code == "08060069" && it.smallGroupId == "small-leaf-c" && it.status == CourseStatus.TAKING })
        assertTrue(result.courses.any { it.code == "08060070" && it.smallGroupId == "small-leaf-d" && it.status == CourseStatus.TAKING })
        assertTrue(result.courses.any { it.code == "08060202" && it.status == CourseStatus.TAKING })
        assertTrue(result.courses.any { it.code == "08060203" && it.status == CourseStatus.TAKING })
    }

    @Test
    fun parsesTranscriptRows() {
        val result = parser.parse(fixture("transcript.html"), ImportPageType.TRANSCRIPT)

        assertTrue(result.courses.any { it.code == "01010017" && it.score == 88.0 && it.status == CourseStatus.PASSED })
        assertTrue(result.courses.size > 40)
    }

    @Test
    fun parsesExamCards() {
        val result = parser.parse(fixture("exams.html"), ImportPageType.EXAMS)

        assertTrue(result.exams.any { it.location.contains("414") && it.seatNo == "86" })
    }

    @Test
    fun parsesScheduleRows() {
        val result = parser.parse(fixture("我的课表.html"), ImportPageType.SCHEDULE)

        assertTrue(result.classSessions.isNotEmpty())
        assertTrue(result.classSessions.any { it.dayOfWeek == 5 && it.startSection == 1 })
        assertTrue(result.classSessions.any { it.location.contains("414") })
    }

    @Test
    fun parsesScheduleJsonRows() {
        val result = parser.parse(
            """
            {
              "type": "JNU_SCHEDULE",
              "termName": "2025-2026学年 第2学期",
              "rows": [
                {"KCH": "08060067", "KCM": "数据结构", "JSXM": "张老师", "KCXF": "3", "ZCMC": "1-16周", "SKXQ": "5", "KSJC": "1", "JSJC": "2", "JASMC": "教学楼414"},
                {"KCH": "08060068", "KCM": "算法设计", "JSXM": "李老师", "SKXQ_DISPLAY": "星期一", "KSJC": 3, "JSJC": 4, "JASMC": "实验室"},
                {"KCH": "08060202", "KCM": "软件工程", "JSXM": "许瑞", "KCXF": "3.0", "SKSJ": "11周 星期日 第10-12节", "JASMC": "番禺教学大楼114室"}
              ]
            }
            """.trimIndent(),
            ImportPageType.SCHEDULE,
        )

        assertEquals(3, result.classSessions.size)
        assertTrue(result.classSessions.any { it.courseName == "数据结构" && it.dayOfWeek == 5 && it.startSection == 1 })
        assertTrue(result.classSessions.any { it.courseName == "算法设计" && it.dayOfWeek == 1 && it.endSection == 4 })
        assertTrue(result.classSessions.any { it.courseCode == "08060202" && it.dayOfWeek == 7 && it.startSection == 10 && it.endSection == 12 })
    }

    @Test
    fun parsesRawScheduleApiRows() {
        val result = parser.parse(
            """
            {
              "datas": {
                "xskcb": {
                  "rows": [
                    {"KCH": "08060204", "KCM": "计算机网络", "JSXM": "王老师", "KCXF": "3.0", "ZCMC": "1-16周", "SKXQ": 2, "KSJC": 5, "JSJC": 6, "JASMC": "教学楼301", "JXBMC": "软工1班"}
                  ]
                }
              }
            }
            """.trimIndent(),
            ImportPageType.SCHEDULE,
        )

        assertEquals(1, result.classSessions.size)
        assertTrue(result.classSessions.any { it.courseName == "计算机网络" && it.dayOfWeek == 2 && it.startSection == 5 })
    }

    @Test
    fun prefersSpecialMeetingTimeFromScheduleNote() {
        val result = parser.parse(
            """
            {
              "rows": [
                {
                  "KCH": "08060205",
                  "KCM": "在线课程",
                  "KCSM": "见面课时间为第1、6、10、14周周末的星期天 第9-10节",
                  "SKXQ": 1,
                  "KSJC": 1,
                  "JSJC": 2,
                  "ZCMC": "1-16周"
                }
              ]
            }
            """.trimIndent(),
            ImportPageType.SCHEDULE,
        )

        assertEquals(1, result.classSessions.size)
        assertTrue(result.classSessions.any {
            it.courseCode == "08060205" &&
                it.dayOfWeek == 7 &&
                it.startSection == 9 &&
                it.endSection == 10 &&
                it.weeksText.contains("1、6、10、14周")
        })
    }

    private fun fixture(name: String): String {
        var dir: File? = File(System.getProperty("user.dir")).absoluteFile
        while (dir != null) {
            val candidate = File(dir, name)
            if (candidate.exists()) return candidate.readText()
            dir = dir.parentFile
        }
        error("Missing fixture $name from ${System.getProperty("user.dir")}")
    }
}
