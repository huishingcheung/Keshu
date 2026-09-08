package com.keshu.mobile.data.portal

import android.net.Uri
import android.webkit.WebView
import com.keshu.mobile.data.parser.JnuAcademicParser
import com.keshu.mobile.data.parser.JnuPageType
import com.keshu.mobile.data.parser.ParsedAcademicPage
import com.keshu.mobile.data.source.AcademicDataSourceDescriptor
import com.keshu.mobile.data.source.AcademicImportData
import com.keshu.mobile.data.source.ExamDataResult
import com.keshu.mobile.data.source.WebAcademicDataSource
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import org.json.JSONTokener
import kotlin.coroutines.resume

class JnuPortalAdapter(
    private val activeWebView: () -> WebView?,
    private val parser: JnuAcademicParser,
) : WebAcademicDataSource {
    private enum class Service(
        val title: String,
        val pathMarker: String,
    ) {
        CURRICULUM("学业完成查询", "/sys/xywccx/"),
        SCHEDULE("我的课表", "/sys/wdkb/"),
        EXAMS("我的考试安排", "/studentWdksapApp/"),
    }

    private data class ExamPage(
        val webView: WebView,
        val hasRows: Boolean,
        val visibilityRange: String?,
    )

    override val descriptor = DESCRIPTOR
    override val portalUrl: String = PORTAL_URL
    override val desktopUserAgent: String = DESKTOP_USER_AGENT

    private val serviceEntryUrls = mutableMapOf<Service, String>()
    private val serviceTargetUrls = mutableMapOf<Service, String>()

    override suspend fun loadCurriculum(): AcademicImportData {
        preparePortal()
        openService(Service.CURRICULUM)
        val page = ensureCurriculumDetailPage()
        val groups = parser.parse(pageHtml(page), JnuPageType.CURRICULUM_GROUPS)
        val courses = parser.parse(readAllCurriculumCoursesPayload(page), JnuPageType.ALL_CURRICULUM_COURSES)
        if (groups.largeGroups.isEmpty() || courses.courses.isEmpty()) {
            error("培养方案导入结果为空：课群${groups.largeGroups.size + groups.smallGroups.size} 课程${courses.courses.size}")
        }
        return AcademicImportData(
            academicProgress = groups.academicProgress,
            largeGroups = groups.largeGroups,
            smallGroups = groups.smallGroups,
            courses = courses.courses,
        )
    }

    override suspend fun loadSchedule(): AcademicImportData {
        openService(Service.SCHEDULE)
        val page = waitForSchedulePage()
        return parser.parse(readSchedulePayload(page), JnuPageType.SCHEDULE).toImportData()
    }

    override suspend fun loadExams(): ExamDataResult {
        openService(Service.EXAMS)
        val page = waitForExamPage()
        if (!page.hasRows) return ExamDataResult.Unavailable(page.visibilityRange)
        val data = parser.parse(pageHtml(page.webView), JnuPageType.EXAMS).toImportData()
        if (data.exams.isEmpty()) error("考试页面包含安排，但没有识别到考试字段")
        return ExamDataResult.Available(data)
    }

    private suspend fun preparePortal(timeoutMillis: Long = 30_000): WebView {
        val portal = waitForPortalServiceList(timeoutMillis)
        cacheServiceEntries(portal)
        return portal
    }

    private suspend fun openService(service: Service): WebView {
        activeWebView()?.takeIf { it.url.orEmpty().contains(service.pathMarker) }?.let { return it }

        val cachedUrl = serviceTargetUrls[service] ?: serviceEntryUrls[service]
        if (cachedUrl != null) {
            activeWebViewOrThrow().loadUrl(cachedUrl)
        } else {
            val portal = if (isPortalReady(activeWebView())) {
                activeWebViewOrThrow().also { cacheServiceEntries(it) }
            } else {
                activeWebViewOrThrow().loadUrl(PORTAL_URL)
                preparePortal()
            }
            val discoveredUrl = serviceTargetUrls[service] ?: serviceEntryUrls[service]
            if (discoveredUrl != null) portal.loadUrl(discoveredUrl) else clickPortalEntry(portal, service)
        }

        val detailOrPage = waitForServiceDetailOrPage(service)
        if (detailOrPage.url.orEmpty().contains(service.pathMarker)) return detailOrPage

        val targetUrl = resolveDetailTarget(detailOrPage, service)
        if (targetUrl != null) {
            serviceTargetUrls[service] = targetUrl
            detailOrPage.loadUrl(targetUrl)
        }
        return waitForOpenedServicePage(service)
    }

    override fun installPageBridge(webView: WebView) {
        exporterScriptFor(webView.url.orEmpty())?.let { script ->
            webView.evaluateJavascript(script, null)
        }
    }

    override suspend fun isPortalReady(webView: WebView?): Boolean {
        return webView?.evalJs(JnuPortalScripts.portalReady) == "ready"
    }

    private suspend fun ensureCurriculumDetailPage(): WebView {
        activeWebView()?.takeIf { it.hasLoadedCurriculumTree() }?.let { return it }
        val curriculum = waitForCurriculumEntryPage(timeoutMillis = 36_000)
        var lastClickResult = ""
        repeat(10) {
            val clickResult = curriculum.evalJs(JnuPortalScripts.curriculumDetailEntry)
            lastClickResult = clickResult
            when (clickResult) {
                "already-detail", "clicked" -> return waitForCurriculumTreeLoaded(timeoutMillis = 90_000)
                "page-busy", "not-ready", "missing" -> delay(750)
                else -> delay(500)
            }
        }
        runCatching { return waitForCurriculumTreeLoaded(timeoutMillis = 90_000) }
        error("没有成功进入培养方案详情页，查看详情点击结果：$lastClickResult")
    }

    private suspend fun readAllCurriculumCoursesPayload(webView: WebView): String {
        waitForCurriculumTreeLoaded(webView, timeoutMillis = 90_000)
        ensurePageBridge(webView)
        webView.evalJs("window.__keshuJnuStartCourseExport ? window.__keshuJnuStartCourseExport() : 'missing';")
            .takeUnless { it == "missing" }
            ?: error("详细课程导出器尚未准备好")
        val payload = awaitExportJob(webView, "courses", timeoutMillis = 120_000)
        if (payload.contains("\"groups\"") && payload.totalExportedCourseRows() > 0) return payload
        error("没有读到全部详细课程，可能仍在加载或教务接口返回空数据：${payload.take(1_000).ifBlank { "空返回" }}")
    }

    private suspend fun readSchedulePayload(webView: WebView): String {
        ensurePageBridge(webView)
        webView.evalJs("window.__keshuJnuStartScheduleExport ? window.__keshuJnuStartScheduleExport() : 'missing';")
            .takeUnless { it == "missing" }
            ?: error("课表导出器尚未准备好")
        val payload = awaitExportJob(webView, "schedule", timeoutMillis = 45_000)
        if (payload.isNotBlank() && payload.scheduleRowCount() > 0) return payload
        if (payload.contains("\"rows\"")) {
            error("没有从课表接口读到课程行，请确认当前学期有课表数据：${payload.take(1_000)}")
        }
        return pageHtml(webView).also { if (it.isBlank()) error("读取当前页面失败") }
    }

    private suspend fun waitForSchedulePage(timeoutMillis: Long = 24_000): WebView {
        return waitForPage(
            timeoutMillis = timeoutMillis,
            failureMessage = "等待课表页面超时",
        ) { webView ->
            val url = webView.url.orEmpty()
            if (!url.contains(Service.SCHEDULE.pathMarker)) return@waitForPage false
            webView.evalJs(
                "(function(){var text=document.body?(document.body.innerText||''):'';return text.indexOf('课表')>=0?'ready':'';})();",
            ) == "ready"
        }
    }

    private suspend fun waitForExamPage(timeoutMillis: Long = 18_000): ExamPage {
        val start = System.currentTimeMillis()
        var visibilityRange: String? = null
        var emptyStateObservedAt: Long? = null
        while (System.currentTimeMillis() - start < timeoutMillis) {
            val webView = activeWebView()
            if (webView != null) {
                val payload = webView.evalJs(EXAM_PAGE_STATE_SCRIPT)
                val pageState = runCatching { JSONObject(payload) }.getOrNull()
                pageState?.optString("visibilityRange")?.takeIf(String::isNotBlank)?.let { visibilityRange = it }
                if (pageState?.optString("state") == "ready") {
                    if (pageState.optBoolean("hasRows")) return ExamPage(webView, hasRows = true, visibilityRange)
                    if (visibilityRange != null) return ExamPage(webView, hasRows = false, visibilityRange)
                    val observedAt = emptyStateObservedAt ?: System.currentTimeMillis().also { emptyStateObservedAt = it }
                    if (System.currentTimeMillis() - observedAt >= EXAM_EMPTY_STATE_GRACE_MILLIS) {
                        return ExamPage(webView, hasRows = false, visibilityRange = null)
                    }
                }
            }
            delay(POLL_INTERVAL_MILLIS)
        }
        error("考试安排页面尚未开放或加载超时")
    }

    private suspend fun pageHtml(webView: WebView): String = webView.evalJs(
        "(function(){return document.documentElement.outerHTML;})();",
        timeoutMillis = 8_000,
    )

    override suspend fun debugPageInfo(webView: WebView?): String {
        return webView?.evalJs(DEBUG_PAGE_INFO_SCRIPT).orEmpty()
    }

    private suspend fun cacheServiceEntries(webView: WebView) {
        val payload = webView.evalJs(JnuPortalScripts.discoverServiceEntries)
        val entries = runCatching { JSONObject(payload) }.getOrNull() ?: return
        Service.entries.forEach { service ->
            entries.optString(service.title)
                .takeIf(::isAllowedPortalUrl)
                ?.let { serviceEntryUrls[service] = it }
        }
    }

    private suspend fun clickPortalEntry(webView: WebView, service: Service) {
        val title = JSONObject.quote(service.title)
        val result = webView.evalJs(
            """
            (function() {
              var title = $title;
              var target = Array.prototype.slice.call(document.querySelectorAll('#ampPersonalAsideLeftAllCanUseApps .canUseAppFlag[amp-title], #ampPersonalAsideLeftAllCanUseApps [amp-title]'))
                .find(function(node) { return (node.getAttribute('amp-title') || node.getAttribute('title') || '').trim() === title; });
              if (!target) return 'missing';
              target = target.closest && target.closest('.appFlag[amp-title], [amp-url][amp-title]') || target;
              target.scrollIntoView({block: 'center'});
              target.click();
              return 'clicked';
            })();
            """.trimIndent(),
        )
        if (result != "clicked") error("没有找到入口：${service.title}")
    }

    private suspend fun resolveDetailTarget(webView: WebView, service: Service): String? {
        val title = JSONObject.quote(service.title)
        val result = webView.evalJs(
            """
            (function() {
              var enter = document.querySelector('#ampDetailEnter.amp-active, #ampDetailEnter');
              if (!enter) return '';
              var expectedTitle = $title;
              var actualTitle = (enter.getAttribute('amp-title') || '').trim();
              if (actualTitle && actualTitle !== expectedTitle) return 'wrong-detail:' + actualTitle;
              var raw = enter.getAttribute('amp-url') || '';
              if (!raw) return '';
              if (/^https?:\/\//i.test(raw) || raw.indexOf('//') === 0) {
                try { return new URL(raw, location.href).href; } catch (ignored) { return ''; }
              }
              var root = (window.AMPConstant && window.AMPConstant.requestPath) || '/';
              if (root.charAt(root.length - 1) !== '/') root += '/';
              if (raw.charAt(0) === '/') raw = raw.substring(1);
              try { return new URL(root + raw, location.origin).href; } catch (ignored) { return ''; }
            })();
            """.trimIndent(),
        )
        if (result.startsWith("wrong-detail:")) error("打开了错误的服务详情：$result")
        if (result.isBlank()) return null
        if (!isAllowedPortalUrl(result)) error("服务地址异常")
        return result
    }

    private suspend fun waitForPortalServiceList(timeoutMillis: Long): WebView {
        return waitForPage(timeoutMillis, "没有进入可用应用列表，请确认已经登录并进入教务系统主页") { webView ->
            webView.evalJs(JnuPortalScripts.portalReady) == "ready"
        }
    }

    private suspend fun waitForServiceDetailOrPage(service: Service, timeoutMillis: Long = 18_000): WebView {
        val title = JSONObject.quote(service.title)
        return waitForPage(timeoutMillis, "服务详情没有打开：${service.title}") { webView ->
            if (webView.url.orEmpty().contains(service.pathMarker)) return@waitForPage true
            webView.evalJs(
                """
                (function() {
                  var enter = document.querySelector('#ampDetailEnter.amp-active, #ampDetailEnter');
                  if (!enter) return '';
                  var actual = (enter.getAttribute('amp-title') || '').trim();
                  return !actual || actual === $title ? 'detail' : '';
                })();
                """.trimIndent(),
            ) == "detail"
        }
    }

    private suspend fun waitForOpenedServicePage(service: Service, timeoutMillis: Long = 30_000): WebView {
        return waitForPage(timeoutMillis, "点击进入服务后页面没有打开：${service.title}") { webView ->
            webView.url.orEmpty().contains(service.pathMarker)
        }
    }

    private suspend fun waitForCurriculumEntryPage(timeoutMillis: Long): WebView {
        var reloadedBlankShell = false
        val startedAt = System.currentTimeMillis()
        return waitForPage(timeoutMillis, "等待培养方案首页超时，没有出现“查看详情”按钮") { webView ->
            if (webView.hasLoadedCurriculumTree()) return@waitForPage true
            val state = webView.evalJs(CURRICULUM_ENTRY_STATE_SCRIPT)
            if (state == "ready" || state == "detail-loading") return@waitForPage true
            if (state == "blank-shell" && !reloadedBlankShell && System.currentTimeMillis() - startedAt > 8_000) {
                reloadedBlankShell = true
                webView.reload()
            }
            false
        }
    }

    private suspend fun waitForCurriculumTreeLoaded(preferred: WebView? = null, timeoutMillis: Long): WebView {
        return waitForPage(timeoutMillis, "等待培养方案课群树加载超时", preferred) { it.hasLoadedCurriculumTree() }
    }

    private suspend fun WebView.hasLoadedCurriculumTree(): Boolean {
        return evalJs(CURRICULUM_TREE_STATE_SCRIPT) == "true"
    }

    private suspend fun awaitExportJob(webView: WebView, name: String, timeoutMillis: Long): String {
        val quotedName = JSONObject.quote(name)
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < timeoutMillis) {
            val statePayload = webView.evalJs(
                "window.__keshuJnuReadJob ? window.__keshuJnuReadJob($quotedName) : '{\"status\":\"missing\"}';",
            )
            val state = runCatching { JSONObject(statePayload) }.getOrNull()
            when (state?.optString("status")) {
                "done" -> return state.optString("payload")
                "error" -> error("教务数据请求失败：${state.optString("message").ifBlank { "未知错误" }}")
            }
            delay(POLL_INTERVAL_MILLIS)
        }
        error("等待教务数据请求超时")
    }

    private suspend fun ensurePageBridge(webView: WebView) {
        val script = exporterScriptFor(webView.url.orEmpty())
            ?: error("当前页面不支持教务数据导出")
        val result = webView.evalJs(script, timeoutMillis = 8_000)
        if (result != "installed") error("教务页面适配器注入失败")
    }

    private fun exporterScriptFor(url: String): String? = when {
        url.contains(Service.CURRICULUM.pathMarker) -> JnuCurriculumScripts.exporter
        url.contains(Service.SCHEDULE.pathMarker) -> JnuScheduleScripts.exporter
        else -> null
    }

    private fun ParsedAcademicPage.toImportData() = AcademicImportData(
        academicProgress = academicProgress,
        largeGroups = largeGroups,
        smallGroups = smallGroups,
        courses = courses,
        exams = exams,
        classSessions = classSessions,
    )

    private suspend fun waitForPage(
        timeoutMillis: Long,
        failureMessage: String,
        preferred: WebView? = null,
        condition: suspend (WebView) -> Boolean,
    ): WebView {
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < timeoutMillis) {
            val webView = preferred ?: activeWebView()
            if (webView != null && condition(webView)) return webView
            delay(POLL_INTERVAL_MILLIS)
        }
        error(failureMessage)
    }

    private fun activeWebViewOrThrow(): WebView = activeWebView() ?: error("页面还没准备好")

    private fun isAllowedPortalUrl(url: String): Boolean {
        val parsed = runCatching { Uri.parse(url) }.getOrNull() ?: return false
        return parsed.scheme in setOf("https", "http") && parsed.host == PORTAL_HOST
    }

    private suspend fun WebView.evalJs(script: String, timeoutMillis: Long = 2_500): String {
        return withTimeoutOrNull(timeoutMillis) {
            suspendCancellableCoroutine { continuation ->
                evaluateJavascript(script) { encoded ->
                    if (continuation.isActive) continuation.resume(decodeJsString(encoded))
                }
            }
        }.orEmpty()
    }

    private fun decodeJsString(encoded: String?): String {
        return runCatching { JSONTokener(encoded ?: "\"\"").nextValue() as String }.getOrDefault("")
    }

    private fun String.totalExportedCourseRows(): Int = runCatching {
        val groups = JSONObject(this).optJSONArray("groups") ?: return@runCatching 0
        (0 until groups.length()).sumOf { groups.optJSONObject(it)?.optJSONArray("rows")?.length() ?: 0 }
    }.getOrDefault(0)

    private fun String.scheduleRowCount(): Int = runCatching {
        JSONObject(this).optJSONArray("rows")?.length() ?: 0
    }.getOrDefault(0)

    companion object {
        val DESCRIPTOR = AcademicDataSourceDescriptor(
            id = "jnu-undergraduate",
            displayName = "暨南大学本科生院",
        )
        const val PORTAL_URL = "https://jw.jnu.edu.cn/new/index.html"
        const val DESKTOP_USER_AGENT =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

        private const val PORTAL_HOST = "jw.jnu.edu.cn"
        private const val POLL_INTERVAL_MILLIS = 350L
        private const val EXAM_EMPTY_STATE_GRACE_MILLIS = 2_500L

        private val CURRICULUM_ENTRY_STATE_SCRIPT =
            """
            (function() {
              var text = document.body ? (document.body.innerText || '') : '';
              var href = location.href || '';
              var htmlLength = document.documentElement ? document.documentElement.outerHTML.length : 0;
              var hasDetail = text.indexOf('查看详情') >= 0 || !!Array.prototype.slice.call(document.querySelectorAll('a,button')).find(function(el) {
                return ((el.getAttribute('data-action') || '') + ' ' + (el.textContent || '')).indexOf('查看详情') >= 0;
              });
              if (hasDetail) return 'ready';
              if (href.indexOf('/sys/xywccx/') >= 0 && (text.indexOf('基础教育课程') >= 0 || text.indexOf('专业教育课程') >= 0)) return 'detail-loading';
              if (href.indexOf('/sys/xywccx/') >= 0 && htmlLength > 20000 && text.trim().length < 80) return 'blank-shell';
              return href.indexOf('/sys/xywccx/') >= 0 ? 'loading-shell' : 'other';
            })();
            """.trimIndent()

        private val CURRICULUM_TREE_STATE_SCRIPT =
            """
            (function() {
              var htmlLength = document.documentElement ? document.documentElement.outerHTML.length : 0;
              var realNodes = Array.prototype.slice.call(document.querySelectorAll('jmnode[nodeid]')).filter(function(node) {
                if (node.classList.contains('root')) return false;
                return ((node.getAttribute('title') || node.textContent || '').trim().length > 0);
              });
              if (htmlLength > 50000 && realNodes.length >= 3) return 'true';
              var text = (document.title || '') + '\n' + (document.body ? document.body.innerText : '');
              return text.indexOf('基础教育课程') >= 0 && text.indexOf('专业教育课程') >= 0 ? 'true' : 'false';
            })();
            """.trimIndent()

        private val EXAM_PAGE_STATE_SCRIPT =
            """
            (function() {
              var url = location.href || '';
              var title = document.title || '';
              var text = document.body ? (document.body.innerText || '') : '';
              var isExamApp = url.indexOf('/studentWdksapApp/') >= 0 || title.indexOf('我的考试安排') >= 0;
              if (!isExamApp) return 'other';
              var hasTerm = !!document.querySelector('#dqxnxq-wdksap, .jxrw-label-term');
              var candidates = Array.prototype.slice.call(document.querySelectorAll('.scenes-cbrt-card-item, .scenes-card, .ksap-table-container tbody tr, .ksap-table-container [data-row-key]'));
              var hasRows = candidates.some(function(row) {
                var rowText = row.innerText || row.textContent || '';
                return /\d{4}-\d{2}-\d{2}\s+\d{2}:\d{2}/.test(rowText) && (rowText.indexOf('考试') >= 0 || rowText.indexOf('座位') >= 0 || rowText.indexOf('地点') >= 0);
              });
              var hasEmpty = !!document.querySelector('.demo-no-data, .bh-no-data') || text.indexOf('暂无考试') >= 0 || text.indexOf('无考试安排') >= 0;
              var hasLegacy = text.indexOf('未完成考试') >= 0 || text.indexOf('已完成考试') >= 0;
              var range = text.match(/学生可查看时间范围[：:]?\s*([0-9-]+\s+[0-9:]+)\s*(?:-|–|—|至)\s*([0-9-]+\s+[0-9:]+)/);
              return JSON.stringify({state: hasTerm && (hasRows || hasEmpty || hasLegacy) ? 'ready' : 'loading', hasRows: hasRows, visibilityRange: range ? range[1] + '–' + range[2] : ''});
            })();
            """.trimIndent()

        private val DEBUG_PAGE_INFO_SCRIPT =
            """
            (function() {
              var text = document.body ? (document.body.innerText || '') : '';
              return JSON.stringify({
                href: location.origin + location.pathname,
                title: document.title || '',
                appCount: document.querySelectorAll('#ampPersonalAsideLeftAllCanUseApps .canUseAppFlag[amp-title], #ampPersonalAsideLeftAllCanUseApps [amp-title]').length,
                hasDetailEnter: !!document.querySelector('#ampDetailEnter'),
                hasJsmind: !!document.querySelector('#jsmind_container, jmnode, jmnodes'),
                jmnodeCount: document.querySelectorAll('jmnode[nodeid]').length,
                hasScheduleExporter: !!window.__keshuJnuStartScheduleExport,
                hasCourseExporter: !!window.__keshuJnuStartCourseExport,
                htmlLength: document.documentElement ? document.documentElement.outerHTML.length : 0,
                textHead: text.slice(0, 600)
              });
            })();
            """.trimIndent()
    }
}
