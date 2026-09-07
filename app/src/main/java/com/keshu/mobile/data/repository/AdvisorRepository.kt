package com.keshu.mobile.data.repository

import com.keshu.mobile.data.local.dao.CourseDao
import com.keshu.mobile.data.local.dao.CourseFeedbackStatsDao
import com.keshu.mobile.data.local.dao.GroupDao
import com.keshu.mobile.data.local.entity.CourseEntity
import com.keshu.mobile.data.local.entity.CourseFeedbackStatsEntity
import com.keshu.mobile.data.local.entity.SmallGroupEntity
import com.keshu.mobile.data.network.DeepSeekClient
import com.keshu.mobile.domain.model.CourseAdvice
import com.keshu.mobile.domain.model.CourseRecommendation
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.time.LocalDate

class AdvisorRepository(
    private val courseDao: CourseDao,
    private val groupDao: GroupDao,
    private val courseFeedbackStatsDao: CourseFeedbackStatsDao,
    private val deepSeekClient: DeepSeekClient,
) {
    suspend fun requestAdvice(apiKey: String): CourseAdvice {
        val targetTerm = nextAcademicTermKey(LocalDate.now())
        val displayTerm = targetTerm.toDisplayTerm()
        val groups = groupDao.observeSmallGroups().first()
        val takingByGroup = projectTakingCreditsByGroup(
            groups = groups,
            takingCourses = courseDao.getByStatus(com.keshu.mobile.data.local.entity.CourseStatus.TAKING),
        )
        val unmetGroups = groups
            .map { group -> AdvisingGroup(group, takingByGroup[group.id] ?: 0.0) }
            .filter { it.projectedEarnedCredits < it.group.requiredCredits }
        val gapById = unmetGroups.associateBy { it.group.id }
        val candidates = if (unmetGroups.isEmpty()) {
            emptyList()
        } else {
            courseDao.getAdvisorCandidates(unmetGroups.map { it.group.id })
        }
        val feedbackByCode = courseFeedbackStatsDao.getByCourseCodes(candidates.map { it.code }.distinct())
            .associateBy { it.courseCode }
        val ranked = candidates
            .filter { it.term.isNullOrBlank() || it.term.orEmpty().matchesAcademicTerm(targetTerm) }
            .mapNotNull { course ->
                val group = gapById[course.smallGroupId] ?: return@mapNotNull null
                val feedback = feedbackByCode[course.code]
                RankedCourse(
                    course = course,
                    group = group,
                    feedback = feedback,
                    score = scoreCourse(course, group, targetTerm, feedback),
                )
            }
            .sortedWith(compareByDescending<RankedCourse> { it.score }.thenByDescending { it.course.credit }.thenBy { it.course.name })
            .take(8)

        val recommendations = ranked.mapIndexed { index, item -> item.toRecommendation(index) }
        val aiSummary = deepSeekClient.complete(
            apiKey = apiKey,
            systemPrompt = SYSTEM_PROMPT,
            userPrompt = buildPrompt(displayTerm, unmetGroups, recommendations),
            maxTokens = 500,
            temperature = 0.2,
        ).toReadableAiSummary()

        return CourseAdvice(
            targetTerm = displayTerm,
            summary = aiSummary,
            recommendations = recommendations,
        )
    }

    private fun scoreCourse(
        course: CourseEntity,
        group: AdvisingGroup,
        targetTerm: String,
        feedback: CourseFeedbackStatsEntity?,
    ): Int {
        var score = 40
        if (course.isRequired(group.group.name)) score += 30
        if (course.term.orEmpty().matchesAcademicTerm(targetTerm)) score += 18
        score += (course.credit * 4).toInt().coerceAtMost(16)
        feedback?.let {
            score += (it.easiness - 3) * 3
            score += (3 - it.workload) * 3
            score += it.recommendCount.coerceAtMost(8)
        }
        return score.coerceIn(0, 100)
    }

    private fun RankedCourse.toRecommendation(index: Int): CourseRecommendation {
        val gap = (group.group.requiredCredits - group.projectedEarnedCredits).coerceAtLeast(0.0)
        val required = course.isRequired(group.group.name)
        val reason = buildList {
            add(if (required) "优先补齐必修要求" else "可补充课群学分")
            add("${group.group.name}预计还差 ${gap.round1()} 学分")
            add("本课程 ${course.credit.round1()} 学分")
        }.joinToString("，")
        val feedbackText = feedback?.let {
            val average = it.avgScore?.let { score -> "历史均分 ${score.round1()}" }
            listOfNotNull(average, "工作量 ${it.workload}/5", "推荐 ${it.recommendCount} 人").joinToString(" · ")
        } ?: "暂无历史评价"
        return CourseRecommendation(
            courseCode = course.code,
            courseName = course.name,
            groupName = group.group.name,
            credit = course.credit,
            priority = when {
                index < 3 -> "优先推荐"
                index < 6 -> "建议考虑"
                else -> "备选"
            },
            reason = reason,
            feedback = feedbackText,
        )
    }

    private fun CourseEntity.isRequired(groupName: String): Boolean {
        return "$groupName $tags $name".contains("必修")
    }

    private fun buildPrompt(
        targetTerm: String,
        unmetGroups: List<AdvisingGroup>,
        recommendations: List<CourseRecommendation>,
    ): String {
        return JSONObject()
            .put("目标学期", targetTerm)
            .put(
                "学分缺口",
                JSONArray(unmetGroups.take(12).map {
                    "${it.group.name}：要求 ${it.group.requiredCredits.round1()}，已修 ${it.group.earnedCredits.round1()}，在修 ${it.takingCredits.round1()}，预计还差 ${(it.group.requiredCredits - it.projectedEarnedCredits).coerceAtLeast(0.0).round1()}"
                }),
            )
            .put(
                "本地筛选结果",
                JSONArray(recommendations.map {
                    "${it.courseName}（${it.credit.round1()}学分，${it.groupName}）：${it.reason}"
                }),
            )
            .toString()
    }

    private fun Double.round1(): String = String.format(Locale.CHINA, "%.1f", this)

    private data class RankedCourse(
        val course: CourseEntity,
        val group: AdvisingGroup,
        val feedback: CourseFeedbackStatsEntity?,
        val score: Int,
    )

    private data class AdvisingGroup(
        val group: SmallGroupEntity,
        val takingCredits: Double,
    ) {
        val projectedEarnedCredits: Double = group.earnedCredits + takingCredits
    }

    private companion object {
        const val SYSTEM_PROMPT =
            "你是大学选课顾问。只能依据用户提供的数据，用自然、简洁的中文给出3到5句总体建议。" +
                "不要输出JSON、Markdown代码块、字段名、内部ID或虚构课程；不要逐条重复课程清单。"
    }
}

fun nextAcademicTermKey(date: LocalDate): String {
    return if (date.monthValue in 2..7) {
        "${date.year}-${date.year + 1}-1"
    } else {
        val startYear = if (date.monthValue >= 8) date.year else date.year - 1
        "$startYear-${startYear + 1}-2"
    }
}

fun projectTakingCreditsByGroup(
    groups: List<SmallGroupEntity>,
    takingCourses: List<CourseEntity>,
): Map<String, Double> {
    val groupsById = groups.associateBy { it.id }
    val result = mutableMapOf<String, Double>()
    takingCourses
        .distinctBy { it.code.ifBlank { it.name } }
        .forEach { course ->
            var groupId: String? = course.smallGroupId
            val visited = mutableSetOf<String>()
            while (groupId != null && visited.add(groupId)) {
                result[groupId] = result.getOrDefault(groupId, 0.0) + course.credit
                groupId = groupsById[groupId]?.parentSmallGroupId
            }
        }
    return result
}

fun String.matchesAcademicTerm(targetTerm: String): Boolean {
    val sourceKey = academicTermKey() ?: return true
    val targetKey = targetTerm.academicTermKey() ?: return contains(targetTerm, ignoreCase = true)
    return sourceKey == targetKey
}

fun String.toDisplayTerm(): String {
    val key = academicTermKey() ?: return this
    val parts = key.split('-')
    return "${parts[0]}-${parts[1]}学年 第${parts[2]}学期"
}

private fun String.academicTermKey(): String? {
    val years = Regex("(20\\d{2})\\D+(20\\d{2})").find(this) ?: return null
    val semester = Regex("(?:第\\s*)?([12])\\s*学期|[-_]([12])$").find(this) ?: return null
    val semesterValue = semester.groupValues.drop(1).firstOrNull { it.isNotBlank() } ?: return null
    return "${years.groupValues[1]}-${years.groupValues[2]}-$semesterValue"
}

fun String.toReadableAiSummary(): String {
    val cleaned = trim()
        .removePrefix("```json")
        .removePrefix("```")
        .removeSuffix("```")
        .trim()
    if (!cleaned.startsWith("{")) return cleaned.take(800)
    val parsed = runCatching { JSONObject(cleaned) }.getOrNull()
    val readable = listOf("summary", "advice", "note", "conclusion", "建议", "总结")
        .firstNotNullOfOrNull { key -> parsed?.optString(key)?.takeIf { it.isNotBlank() } }
    return readable ?: "DeepSeek 已完成分析，请结合下方课程卡片安排选课。"
}
