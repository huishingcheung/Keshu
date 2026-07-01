package edu.jnu.smartedu.data.parser

import edu.jnu.smartedu.data.local.entity.ClassSessionEntity
import edu.jnu.smartedu.data.local.entity.AcademicProgressEntity
import edu.jnu.smartedu.data.local.entity.CourseEntity
import edu.jnu.smartedu.data.local.entity.CourseStatus
import edu.jnu.smartedu.data.local.entity.ExamEntity
import edu.jnu.smartedu.data.local.entity.LargeGroupEntity
import edu.jnu.smartedu.data.local.entity.SmallGroupEntity
import org.json.JSONArray
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

enum class ImportPageType { CURRICULUM_GROUPS, CURRICULUM_COURSES, ALL_CURRICULUM_COURSES, TRANSCRIPT, EXAMS, SCHEDULE }

data class ParsedAcademicPage(
    val academicProgress: AcademicProgressEntity? = null,
    val largeGroups: List<LargeGroupEntity> = emptyList(),
    val smallGroups: List<SmallGroupEntity> = emptyList(),
    val courses: List<CourseEntity> = emptyList(),
    val exams: List<ExamEntity> = emptyList(),
    val classSessions: List<ClassSessionEntity> = emptyList(),
)

class AcademicHtmlParser {
    fun parse(html: String, pageType: ImportPageType): ParsedAcademicPage {
        return when (pageType) {
            ImportPageType.CURRICULUM_GROUPS -> parseCurriculumGroups(html)
            ImportPageType.CURRICULUM_COURSES -> parseCurriculumCourses(html)
            ImportPageType.ALL_CURRICULUM_COURSES -> parseAllCurriculumCourses(html)
            ImportPageType.TRANSCRIPT -> parseTranscript(html)
            ImportPageType.EXAMS -> parseExams(html)
            ImportPageType.SCHEDULE -> parseSchedule(html)
        }
    }

    private fun parseCurriculumGroups(html: String): ParsedAcademicPage {
        val doc = Jsoup.parse(html)
        val nodes = doc.select("jmnode").mapNotNull { it.toPlanNode() }
        val root = nodes.firstOrNull { it.isRoot } ?: nodes.minByOrNull { it.left } ?: return ParsedAcademicPage()
        val parentByNodeId = buildParentMap(nodes)
        val largeNodes = nodes.filter { parentByNodeId[it.id] == root.id }
        val largeGroups = largeNodes.mapIndexed { index, node ->
            LargeGroupEntity(
                id = largeId(node),
                name = node.title,
                requiredCredits = node.requiredCredits,
                sortOrder = index,
                earnedCredits = node.earnedCredits,
            )
        }
        val childCountByParent = parentByNodeId.values.groupingBy { it }.eachCount()
        val smallGroups = mutableListOf<SmallGroupEntity>()
        largeNodes.forEachIndexed { largeIndex, large ->
            nodes
                .filter { it.id != root.id && it.id != large.id }
                .filter { it.requiredCredits > 0.0 || it.courseNature != null || childCountByParent.containsKey(it.id) }
                .filter { candidate -> candidate.belongsToLargeGroup(large, parentByNodeId) }
                .forEachIndexed { smallIndex, small ->
                    val parentId = parentByNodeId[small.id]
                    smallGroups += SmallGroupEntity(
                        id = smallId(small),
                        largeGroupId = largeId(large),
                        parentSmallGroupId = parentId
                            ?.takeUnless { it == large.id }
                            ?.let { "small-$it" },
                        name = small.title,
                        requiredCredits = small.requiredCredits,
                        sortOrder = largeIndex * 100 + smallIndex,
                        depth = small.depthUnder(large, parentByNodeId),
                        earnedCredits = small.earnedCredits,
                    )
                }
        }
        return ParsedAcademicPage(
            academicProgress = AcademicProgressEntity(
                requiredCredits = root.requiredCredits,
                earnedCredits = root.earnedCredits,
            ),
            largeGroups = largeGroups.distinctBy { it.id },
            smallGroups = smallGroups.distinctBy { it.id },
        )
    }

    private fun buildParentMap(nodes: List<PlanNode>): Map<String, String> {
        val stack = mutableListOf<PlanNode>()
        val parentByNodeId = mutableMapOf<String, String>()
        nodes.forEach { node ->
            while (stack.isNotEmpty() && stack.last().left >= node.left) {
                stack.removeAt(stack.lastIndex)
            }
            stack.lastOrNull()?.let { parentByNodeId[node.id] = it.id }
            stack += node
        }
        return parentByNodeId
    }

    private fun PlanNode.belongsToLargeGroup(
        large: PlanNode,
        parentByNodeId: Map<String, String>,
    ): Boolean {
        var parentId = parentByNodeId[id]
        while (parentId != null) {
            if (parentId == large.id) return true
            parentId = parentByNodeId[parentId]
        }
        return false
    }

    private fun PlanNode.depthUnder(
        large: PlanNode,
        parentByNodeId: Map<String, String>,
    ): Int {
        var depth = 1
        var parentId = parentByNodeId[id]
        while (parentId != null && parentId != large.id) {
            depth += 1
            parentId = parentByNodeId[parentId]
        }
        return depth
    }

    private fun parseCurriculumCourses(html: String): ParsedAcademicPage {
        val doc = Jsoup.parse(html)
        val planNodes = doc.select("jmnode").mapNotNull { it.toPlanNode() }
        val selected = doc.select("jmnode.selected, jmnode[class*=selected]").firstOrNull()?.toPlanNode()
        val detailTitle = doc.select("[bh-property-dialog-role=header] .h3, .bh-property-dialog .h3")
            .lastOrNull()
            ?.text()
            ?.trim()
            ?.ifBlank { null }
        val detailNode = detailTitle
            ?.let { title -> planNodes.lastOrNull { it.title == title } }
            ?: selected
        val smallGroupId = detailNode?.let { smallId(it) } ?: "small-unmatched-detail"
        val detailRoot = doc.select(".bh-property-dialog")
            .lastOrNull { it.select("table tr[id*=dykkrws-index-table], table tr[data-key]").isNotEmpty() }
            ?: doc
        val courses = detailRoot.select("table tr[id*=dykkrws-index-table], table tr[data-key]").mapNotNull { row ->
            val cells = gridCells(row)
            if (cells.size < 7) return@mapNotNull null
            val code = cells.getOrNull(0).orEmpty()
            val name = cells.getOrNull(1).orEmpty()
            val credit = cells.getOrNull(2)?.toDoubleOrNull() ?: return@mapNotNull null
            if (!code.matches(Regex("[A-Za-z0-9]{6,}")) || name.isBlank()) return@mapNotNull null
            val statusText = cells.getOrNull(5).orEmpty()
            val score = cells.getOrNull(6)?.toDoubleOrNull()
            CourseEntity(
                id = courseId(code, name),
                code = code,
                name = name,
                credit = credit,
                smallGroupId = smallGroupId,
                status = statusFromPlan(statusText, score),
                score = score,
                term = cells.getOrNull(7)?.ifBlank { null },
                tags = cells.getOrNull(4).orEmpty(),
            )
        }
        return ParsedAcademicPage(courses = courses.distinctBy { it.id })
    }

    private fun parseAllCurriculumCourses(json: String): ParsedAcademicPage {
        val root = JSONObject(json)
        val groups = root.optJSONArray("groups") ?: JSONArray()
        val courses = buildList {
            for (index in 0 until groups.length()) {
                val group = groups.optJSONObject(index) ?: continue
                val nodeId = group.optString("id").trim()
                if (nodeId.isBlank()) continue
                val rows = group.optJSONArray("rows") ?: JSONArray()
                for (rowIndex in 0 until rows.length()) {
                    val row = rows.optJSONObject(rowIndex) ?: continue
                    val code = row.optFirstString("KCH", "KCDM", "KCH_DISPLAY", "KCBH")
                    val name = row.optFirstString("KCM", "KCMC", "KCM_DISPLAY", "KCM1")
                    val credit = row.optFirstDouble("KCXF", "XF", "XF_DISPLAY", "XDF")
                    if (!code.matches(Regex("[A-Za-z0-9]{6,}")) || name.isBlank() || credit == null) continue
                    val statusText = row.optFirstString(
                        "SFTGDM",
                        "SFTG",
                        "SFTG_DISPLAY",
                        "SFTGDM_DISPLAY",
                        "SFJG_DISPLAY",
                        "SFJG",
                        "ZT_DISPLAY",
                        "ZT",
                        "XKZT_DISPLAY",
                        "XKZT",
                    )
                    val score = row.optFirstDouble("CJ", "ZCJ", "KSPSCJ")
                    add(
                        CourseEntity(
                            id = courseId(code, name),
                            code = code,
                            name = name,
                            credit = credit,
                            smallGroupId = "small-$nodeId",
                            status = statusFromPlan(statusText, score),
                            score = score,
                            term = row.optFirstString("XDXQDM_DISPLAY", "XNXQDM_DISPLAY", "XNXQ", "XDXQ")
                                .ifBlank { null },
                            tags = row.optFirstString("KCXZDM_DISPLAY", "KCLBDM_DISPLAY", "KCXZ", "KCLB"),
                        ),
                    )
                }
            }
        }
        return ParsedAcademicPage(courses = courses.mergeDuplicateCourses())
    }

    private fun parseTranscript(html: String): ParsedAcademicPage {
        val doc = Jsoup.parse(html)
        val courses = doc.select("tr").mapNotNull { row ->
            val byColumn = row.children().associateBy { it.id().substringBefore("-").take(1) }
            val code = byColumn["B"]?.text()?.trim().orEmpty()
            val name = byColumn["C"]?.text()?.trim().orEmpty()
            val category = byColumn["F"]?.text()?.trim().orEmpty()
            val scoreText = byColumn["G"]?.text()?.trim().orEmpty()
            val credit = byColumn["H"]?.text()?.trim()?.toDoubleOrNull()
            if (!code.matches(Regex("[A-Za-z0-9]{6,}")) || name.isBlank() || credit == null) return@mapNotNull null
            val score = scoreText.toDoubleOrNull()
            CourseEntity(
                id = courseId(code, name),
                code = code,
                name = name,
                credit = credit,
                smallGroupId = "small-unclassified-transcript",
                status = if (scoreText.contains("不") || (score != null && score < 60.0)) CourseStatus.FAILED else CourseStatus.PASSED,
                score = score,
                tags = category,
            )
        }
        return ParsedAcademicPage(courses = courses.mergeDuplicateCourses())
    }

    private fun parseExams(html: String): ParsedAcademicPage {
        val doc = Jsoup.parse(html)
        val timeRegex = Regex("""(\d{4}-\d{2}-\d{2})\s+(\d{2}:\d{2})""")
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        val cards = doc.select(".scenes-cbrt-card-item, .scenes-card, li, tr").ifEmpty { doc.select("body") }
        val exams = cards.mapNotNull { card ->
            val text = card.text()
            val time = timeRegex.find(text)?.value ?: return@mapNotNull null
            val title = card.select("h3[title]").firstOrNull()?.attr("title")?.trim()
                ?: card.select("[title]").map { it.attr("title").trim() }
                    .firstOrNull { it.isNotBlank() && !it.contains("学年") && !it.contains("星期") && !it.contains("-") }
                ?: text.substringBefore("考试时间").trim().take(40)
            val location = card.select("[title]").map { it.attr("title").trim() }
                .firstOrNull { it.contains("室") || it.contains("楼") || it.contains("校区") }
                ?: text.substringAfter("考试地点：", "").substringBefore("座位号").trim()
            val seat = card.select("[title]").map { it.attr("title").trim() }
                .firstOrNull { it.matches(Regex("""\d{1,3}""")) }
                ?: text.substringAfter("座位号：", "").substringBefore("考试方式").trim().ifBlank { null }
            val startsAt = LocalDateTime.parse(time, formatter)
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
            ExamEntity(
                id = stableId("exam", "$title-$startsAt-$location"),
                courseName = title,
                startsAtMillis = startsAt,
                location = location,
                seatNo = seat,
            )
        }
        return ParsedAcademicPage(exams = exams.distinctBy { it.id })
    }

    private fun parseSchedule(html: String): ParsedAcademicPage {
        if (html.trimStart().startsWith("{")) return parseScheduleJson(html)
        val doc = Jsoup.parse(html)
        val term = doc.selectFirst("#dqxnxq2")?.text()?.trim()
            ?: doc.selectFirst(".jxrw-label-term")?.text()?.trim()
            ?: "当前学期"
        val sessions = doc.select("tr[role=row][id*=jqxWidget]").mapNotNull { row ->
            val cells = gridCells(row)
            if (cells.size < 11) return@mapNotNull null
            val className = cells.getOrNull(3).orEmpty()
            val teacher = cells.getOrNull(4).orEmpty()
            val courseName = cells.getOrNull(5).orEmpty()
            val code = cells.getOrNull(6).orEmpty()
            val credit = cells.getOrNull(7)?.toDoubleOrNull()
            val timeText = cells.getOrNull(9).orEmpty()
            val location = cells.getOrNull(10).orEmpty()
            if (courseName.isBlank() || !code.matches(Regex("[A-Za-z0-9]{6,}"))) return@mapNotNull null
            val parsedTime = parseClassTime(className) ?: parseClassTime(timeText) ?: return@mapNotNull null
            ClassSessionEntity(
                id = stableId("class", "$term-$code-$courseName-${parsedTime.weeksText}-${parsedTime.dayOfWeek}-${parsedTime.startSection}-${parsedTime.endSection}-$location"),
                term = term,
                courseCode = code,
                courseName = courseName,
                teacher = teacher,
                credit = credit,
                weeksText = parsedTime.weeksText,
                dayOfWeek = parsedTime.dayOfWeek,
                startSection = parsedTime.startSection,
                endSection = parsedTime.endSection,
                location = location,
                className = className,
            )
        }
        return ParsedAcademicPage(classSessions = sessions.distinctBy { it.id })
    }

    private fun parseScheduleJson(json: String): ParsedAcademicPage {
        val root = JSONObject(json)
        val term = root.optString("termName").ifBlank { root.optString("term") }.ifBlank { "当前学期" }
        val rows = root.optJSONArray("rows")
            ?: root.optNestedRows("xskcb")
            ?: root.optNestedRows("jshkcb")
            ?: JSONArray()
        val sessions = buildList {
            for (index in 0 until rows.length()) {
                val row = rows.optJSONObject(index) ?: continue
                val courseName = row.optFirstString("KCM", "KCMC", "KCM_DISPLAY", "KCMC_DISPLAY")
                val code = row.optFirstString("KCH", "KCDM", "KCH_DISPLAY", "KCBH")
                if (courseName.isBlank()) continue
                val timeText = row.optFirstString("SKSJ", "PKSJ", "SJD", "SKSJ_DISPLAY", "PKSJ_DISPLAY")
                val noteText = row.optFirstString("SKSM", "BZ", "KCSM")
                val parsedNote = parseClassTime(noteText)
                val parsedTime = parsedNote ?: parseClassTime(timeText)
                val day = parsedNote?.dayOfWeek
                    ?: row.optFirstInt("SKXQ", "XQJ", "ZC_XQ", "WEEKDAY")
                    ?: row.optFirstString("SKXQ_DISPLAY", "XQJ_DISPLAY", "WEEKDAY_DISPLAY").dayOfWeekFromText()
                    ?: parsedTime?.dayOfWeek
                    ?: continue
                val startSection = parsedNote?.startSection
                    ?: row.optFirstInt("KSJC", "QSSKJC", "JC", "BEGINUNIT")
                    ?: parsedTime?.startSection
                    ?: continue
                val endSection = parsedNote?.endSection
                    ?: row.optFirstInt("JSJC", "JSSKJC", "ENDUNIT")
                    ?: parsedTime?.endSection
                    ?: startSection
                val location = row.optFirstString("JASMC", "CDMC", "JASMC_DISPLAY", "SKDD", "SKDD_DISPLAY")
                add(
                    ClassSessionEntity(
                        id = stableId("class", "$term-$code-$courseName-$day-$startSection-$endSection-$location"),
                        term = term,
                        courseCode = code.ifBlank { stableId("course-code", courseName) },
                        courseName = courseName,
                        teacher = row.optFirstString("JSXM", "RKJS", "JSXM_DISPLAY", "SKJS"),
                        credit = row.optFirstDouble("KCXF", "XF"),
                        weeksText = parsedNote?.weeksText ?: noteText.ifBlank {
                            row.optFirstString(
                            "SKSJ",
                            "PKSJ",
                            "SJD",
                            "SKZCMC",
                            "ZCMC",
                            "ZC",
                            "SKZC",
                            "QSZC",
                            "ZCMC_DISPLAY",
                            )
                        }.ifBlank { parsedTime?.weeksText ?: "全周" },
                        dayOfWeek = day,
                        startSection = startSection,
                        endSection = endSection,
                        location = location,
                        className = row.optFirstString("BJMC", "JXBMC", "JXBMC_DISPLAY", "KXH"),
                    ),
                )
            }
        }
        return ParsedAcademicPage(classSessions = sessions.distinctBy { it.id })
    }

    private fun Element.toPlanNode(): PlanNode? {
        val title = selectFirst(".jsmind_node_title_span")?.attr("title")?.trim()
            ?: selectFirst(".jsmind_node_title_span")?.text()?.trim()
            ?: return null
        val id = attr("nodeid").ifBlank {
            selectFirst(".jsmind_node_title_span")?.attr("data-node-val").orEmpty()
        }.ifBlank { stableId("node", title) }
        val style = attr("style")
        val creditText = text()
        return PlanNode(
            id = id,
            title = title,
            requiredCredits = Regex("""要求\s*([0-9]+(?:\.[0-9]+)?)""")
                .find(creditText)
                ?.groupValues
                ?.get(1)
                ?.toDoubleOrNull()
                ?: 0.0,
            earnedCredits = Regex("""已修\s*([0-9]+(?:\.[0-9]+)?)""")
                .find(creditText)
                ?.groupValues
                ?.get(1)
                ?.toDoubleOrNull()
                ?: 0.0,
            courseNature = Regex("""课程性质\s*(必修|选修)""")
                .find(creditText)
                ?.groupValues
                ?.get(1),
            left = styleNumber(style, "left") ?: 0.0,
            top = styleNumber(style, "top") ?: 0.0,
            isRoot = hasClass("root"),
        )
    }

    private fun gridCells(row: Element): List<String> {
        return row.select("td[role=gridcell]").map { cell ->
            cell.selectFirst("span[title]")?.attr("title")?.trim()
                ?.ifBlank { cell.text().trim() }
                ?: cell.text().trim()
        }
    }

    private fun styleNumber(style: String, name: String): Double? {
        return Regex("""$name\s*:\s*([0-9.]+)px""").find(style)?.groupValues?.get(1)?.toDoubleOrNull()
    }

    private fun statusFromPlan(statusText: String, score: Double?): CourseStatus {
        return when {
            statusText == "0" || statusText.contains("未通过") || (score != null && score < 60.0) -> CourseStatus.FAILED
            statusText == "1" || statusText.contains("通过") || (score != null && score >= 60.0) -> CourseStatus.PASSED
            statusText == "4" ||
                statusText.contains("待选") ||
                statusText.contains("已选课") ||
                statusText.contains("在读") ||
                statusText.contains("在修") ||
                statusText.contains("修读中") -> CourseStatus.TAKING
            statusText == "2" || statusText == "3" || statusText.contains("未修读") -> CourseStatus.NOT_TAKEN
            else -> CourseStatus.NOT_TAKEN
        }
    }

    private fun List<CourseEntity>.mergeDuplicateCourses(): List<CourseEntity> {
        return groupBy { it.id }.values.map { duplicates ->
            duplicates.maxWith(
                compareBy<CourseEntity> { it.status.importPriority() }
                    .thenBy { it.score ?: -1.0 }
                    .thenBy { it.term.orEmpty() },
            )
        }
    }

    private fun CourseStatus.importPriority(): Int {
        return when (this) {
            CourseStatus.PASSED -> 4
            CourseStatus.TAKING -> 3
            CourseStatus.FAILED -> 2
            CourseStatus.NOT_TAKEN -> 1
        }
    }

    private fun parseClassTime(raw: String): ClassTime? {
        val text = raw.trim()
        val day = mapOf("一" to 1, "二" to 2, "三" to 3, "四" to 4, "五" to 5, "六" to 6, "日" to 7, "天" to 7)
            .entries
            .firstOrNull { text.contains("星期${it.key}") || text.contains("周${it.key}") }
            ?.value
            ?: return null
        val sectionMatch = Regex("""第\s*(\d+)(?:-(\d+))?\s*节""").find(text) ?: return null
        val start = sectionMatch.groupValues[1].toInt()
        val end = sectionMatch.groupValues.getOrNull(2)?.takeIf { it.isNotBlank() }?.toInt() ?: start
        val weeks = text.substringBefore("星期").ifBlank { text.substringBefore("周") + "周" }
        return ClassTime(weeksText = weeks, dayOfWeek = day, startSection = start, endSection = end)
    }

    private fun largeId(node: PlanNode): String = "large-${node.id}"

    private fun smallId(node: PlanNode): String = "small-${node.id}"

    private fun courseId(code: String, name: String): String = stableId("course", "$code-$name")

    private fun stableId(prefix: String, value: String): String {
        return "$prefix-${UUID.nameUUIDFromBytes(value.trim().lowercase().toByteArray())}"
    }
}

private fun JSONObject.optFirstString(vararg names: String): String {
    for (name in names) {
        val value = opt(name) ?: continue
        val text = when (value) {
            is JSONObject -> value.optFirstString("id", "value", "name", "text", "label", "display", "DISPLAY")
            else -> value.toString().trim()
        }
        if (text.isNotBlank() && text != "null" && text != "-") return text
    }
    return ""
}

private fun JSONObject.optFirstDouble(vararg names: String): Double? {
    for (name in names) {
        val value = opt(name) ?: continue
        when (value) {
            is Number -> return value.toDouble()
            is String -> value.trim().toDoubleOrNull()?.let { return it }
        }
    }
    return null
}

private fun JSONObject.optFirstInt(vararg names: String): Int? {
    for (name in names) {
        val value = opt(name) ?: continue
        when (value) {
            is Number -> return value.toInt()
            is String -> value.trim().toIntOrNull()?.let { return it }
        }
    }
    return null
}

private fun JSONObject.optNestedRows(action: String): JSONArray? {
    return optJSONObject("datas")
        ?.optJSONObject(action)
        ?.optJSONArray("rows")
}

private fun String.dayOfWeekFromText(): Int? {
    val text = trim()
    return when {
        text.contains("一") -> 1
        text.contains("二") -> 2
        text.contains("三") -> 3
        text.contains("四") -> 4
        text.contains("五") -> 5
        text.contains("六") -> 6
        text.contains("日") || text.contains("天") -> 7
        else -> toIntOrNull()
    }
}

private data class PlanNode(
    val id: String,
    val title: String,
    val requiredCredits: Double,
    val earnedCredits: Double,
    val courseNature: String?,
    val left: Double,
    val top: Double,
    val isRoot: Boolean,
)

private data class ClassTime(
    val weeksText: String,
    val dayOfWeek: Int,
    val startSection: Int,
    val endSection: Int,
)
