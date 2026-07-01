package edu.jnu.smartedu.presentation.sync

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.os.Message
import android.util.Log
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebView.WebViewTransport
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.lifecycleScope
import edu.jnu.smartedu.JnuSmartEduApp
import edu.jnu.smartedu.data.parser.ImportPageType
import edu.jnu.smartedu.data.repository.ImportSummary
import edu.jnu.smartedu.presentation.theme.JnuSmartEduTheme
import edu.jnu.smartedu.widget.WidgetUpdateManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import org.json.JSONTokener
import kotlin.math.roundToInt
import kotlin.coroutines.resume

private const val JNU_PORTAL_URL = "https://jw.jnu.edu.cn/new/index.html"
private const val TAG = "ManualSyncActivity"
private const val DESKTOP_USER_AGENT =
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

class ManualSyncActivity : ComponentActivity() {
    private var activeWebView: WebView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val importUseCase = (application as JnuSmartEduApp).container.importCurrentPageUseCase
        setContent {
            JnuSmartEduTheme {
                var debugReport by remember { mutableStateOf("") }
                ManualSyncScreen(
                    debugReport = debugReport,
                    onActiveWebViewChanged = { activeWebView = it },
                    onAutoImport = {
                        lifecycleScope.launch {
                            val result = runCatching { runAutoImport(importUseCase) }
                            result.exceptionOrNull()?.let { throwable ->
                                debugReport = buildFailureReport(activeWebView, null, "一键同步", throwable)
                                Log.e(TAG, debugReport, throwable)
                            }
                            result.fold(
                                onSuccess = {
                                    toast(it, Toast.LENGTH_LONG)
                                    WidgetUpdateManager.requestUpdate(this@ManualSyncActivity)
                                    finish()
                                },
                                onFailure = { toast("一键同步失败：${it.message}", Toast.LENGTH_LONG) },
                            )
                        }
                    },
                    onImport = { type ->
                        val webView = activeWebView
                        if (webView == null) {
                            toast("页面还没准备好")
                            return@ManualSyncScreen
                        }
                        lifecycleScope.launch {
                            val result = runCatching { importVisiblePageLikeManual(webView, type, importUseCase) }
                            result.exceptionOrNull()?.let { throwable ->
                                debugReport = buildFailureReport(webView, type, "手动导入", throwable)
                                Log.e(TAG, debugReport, throwable)
                            }
                            toast(
                                result.fold(
                                    onSuccess = { "导入完成：${it.toToastText()}" },
                                    onFailure = { "导入失败：${it.message}" },
                                ),
                                Toast.LENGTH_LONG,
                            )
                        }
                    },
                    onCopyDebugReport = {
                        if (debugReport.isBlank()) {
                            toast("暂无调试信息")
                        } else {
                            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("JNU Smart Edu debug", debugReport))
                            toast("调试信息已复制")
                        }
                    },
                    onCopyCurrentPageDebug = {
                        val webView = activeWebView
                        val minimal = buildMinimalPageReport(webView)
                        debugReport = minimal
                        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("JNU Smart Edu current page debug", minimal))
                        toast("已复制基础调试信息，正在补充网页信息")
                        lifecycleScope.launch {
                            val report = buildCurrentPageReport(webView, minimal)
                            debugReport = report
                            clipboard.setPrimaryClip(ClipData.newPlainText("JNU Smart Edu current page debug", report))
                            toast("完整调试信息已复制")
                        }
                    },
                )
            }
        }
    }

    private suspend fun runAutoImport(importUseCase: edu.jnu.smartedu.domain.usecase.ImportCurrentPageUseCase): String {
        val first = activeWebView ?: error("页面还没准备好")
        first.installCourseDetailClickPatch()
        var importedGroups = 0
        var importedCourses = 0
        var importedSchedule = 0
        var importedExams = 0

        waitForPortalServiceList()
        openPortalService(first, "学业完成查询")
        importCurriculumLikeManual(importUseCase).also { (groups, courses) ->
            importedGroups = groups
            importedCourses = courses
        }
        returnToPortal()

        val portalForSchedule = waitForPortalServiceList(timeoutMillis = 30_000)
        openPortalService(portalForSchedule, "我的课表")
        val schedulePage = waitForPageText("我的课表", "课表")
        importVisiblePageLikeManual(schedulePage, ImportPageType.SCHEDULE, importUseCase).also {
            importedSchedule = it.classSessions
        }
        returnToPortal()

        val portalForExams = waitForPortalServiceList(timeoutMillis = 30_000)
        openPortalService(portalForExams, "我的考试安排")
        val examPage = waitForPageText("我的考试安排", "未完成考试")
        importVisiblePageLikeManual(examPage, ImportPageType.EXAMS, importUseCase).also {
            importedExams = it.exams
        }

        return "一键同步完成：课群$importedGroups 课程$importedCourses 课表$importedSchedule 考试$importedExams"
    }

    private suspend fun importCurriculumLikeManual(
        importUseCase: edu.jnu.smartedu.domain.usecase.ImportCurrentPageUseCase,
    ): Pair<Int, Int> {
        val detailPage = ensureCurriculumDetailPage()
        val groups = importVisiblePageLikeManual(detailPage, ImportPageType.CURRICULUM_GROUPS, importUseCase).let {
            it.largeGroups + it.smallGroups
        }
        val courses = importVisiblePageLikeManual(detailPage, ImportPageType.ALL_CURRICULUM_COURSES, importUseCase).courses
        if (groups == 0 || courses == 0) {
            error("培养方案导入结果为空：课群$groups 课程$courses")
        }
        return groups to courses
    }

    private suspend fun ensureCurriculumDetailPage(): WebView {
        activeWebView?.takeIf { it.hasLoadedCurriculumTree() }?.let { return it }
        val curriculum = waitForCurriculumEntryPage(timeoutMillis = 36_000)
        delay(1_500)
        var lastClickResult = ""
        repeat(8) {
            val clickResult = curriculum.evalJs(AUTO_CURRICULUM_DETAIL_SCRIPT)
            lastClickResult = clickResult
            when (clickResult) {
                "already-detail", "clicked" -> return waitForCurriculumTreeLoaded(timeoutMillis = 90_000)
                "page-busy", "not-ready", "missing" -> delay(1_000)
                else -> delay(700)
            }
        }
        runCatching {
            return waitForCurriculumTreeLoaded(timeoutMillis = 90_000)
        }
        error("没有成功进入培养方案详情页，查看详情点击结果：$lastClickResult")
    }

    private suspend fun importVisiblePageLikeManual(
        webView: WebView,
        type: ImportPageType,
        importUseCase: edu.jnu.smartedu.domain.usecase.ImportCurrentPageUseCase,
    ): ImportSummary {
        val payload = readCurrentPagePayloadLikeManual(webView, type)
        return importUseCase(payload, type)
    }

    private suspend fun readCurrentPagePayloadLikeManual(webView: WebView, type: ImportPageType): String {
        return when (type) {
            ImportPageType.ALL_CURRICULUM_COURSES -> readAllCurriculumCoursesPayload(webView)
            ImportPageType.SCHEDULE -> readSchedulePayload(webView)
            else -> webView.pageHtml().also {
                if (it.isBlank()) error("读取当前页面失败")
            }
        }
    }

    private suspend fun readAllCurriculumCoursesPayload(webView: WebView): String {
        waitForCurriculumTreeLoaded(webView, timeoutMillis = 90_000)
        var lastPayload = ""
        repeat(3) {
            webView.installCourseDetailClickPatch()
            val payload = webView.evalJs(
                "window.__jnuSmartEduExportAllCourseDetails ? window.__jnuSmartEduExportAllCourseDetails() : '';",
                timeoutMillis = 120_000,
            )
            lastPayload = payload
            if (payload.contains("\"groups\"") && payload.totalExportedCourseRows() > 0) return payload
            delay(1_500)
        }
        val debug = lastPayload.take(1_000).ifBlank { "空返回" }
        error("没有读到全部详细课程，可能仍在加载或教务接口返回空数据：$debug")
    }

    private suspend fun readSchedulePayload(webView: WebView): String {
        var lastPayload = ""
        repeat(6) {
            webView.installCourseDetailClickPatch()
            val payload = webView.evalJs(
                "window.__jnuSmartEduExportSchedule ? window.__jnuSmartEduExportSchedule() : '';",
                timeoutMillis = 30_000,
            )
            lastPayload = payload
            if (payload.isNotBlank() && payload.scheduleRowCount() > 0) return payload
            delay(1_000)
        }
        if (lastPayload.contains("\"rows\"")) {
            error("没有从课表接口读到课程行，请确认当前学期有课表数据：${lastPayload.take(1_000)}")
        }
        return webView.pageHtml().also {
            if (it.isBlank()) error("读取当前页面失败")
        }
    }

    private suspend fun returnToPortal(): WebView {
        val webView = activeWebView ?: error("页面还没准备好")
        webView.loadUrl(JNU_PORTAL_URL)
        return waitForPortalServiceList(timeoutMillis = 30_000)
    }

    private suspend fun buildFailureReport(
        webView: WebView?,
        type: ImportPageType?,
        action: String,
        throwable: Throwable,
    ): String {
        val pageInfo = runCatching {
            webView?.evalJs(DEBUG_PAGE_INFO_SCRIPT).orEmpty()
        }.getOrElse { "页面信息读取失败：${it.message}" }
        return buildString {
            appendLine("JNU Smart Edu Debug Report")
            appendLine("action=$action")
            appendLine("importType=${type?.name ?: "AUTO"}")
            appendLine("message=${throwable.message}")
            appendLine("exception=${throwable::class.java.name}")
            appendLine("pageInfo=$pageInfo")
            appendLine("stackTrace=${throwable.stackTraceToString()}")
        }
    }

    private fun buildMinimalPageReport(webView: WebView?): String {
        return buildString {
            appendLine("JNU Smart Edu Current Page Debug")
            appendLine("action=手动复制当前页面")
            appendLine("stage=minimal")
            appendLine("webViewNull=${webView == null}")
            appendLine("url=${webView?.url.orEmpty()}")
            appendLine("title=${webView?.title.orEmpty()}")
            appendLine("progress=${webView?.progress ?: -1}")
            appendLine("canGoBack=${webView?.canGoBack() ?: false}")
        }
    }

    private suspend fun buildCurrentPageReport(webView: WebView?, minimal: String): String {
        val pageInfo = withTimeoutOrNull(1_800) {
            runCatching { webView?.evalJs(DEBUG_PAGE_INFO_SCRIPT).orEmpty() }
                .getOrElse { "页面信息读取失败：${it.message}" }
        } ?: "页面信息读取超时：evaluateJavascript 没有回调"
        val htmlHead = withTimeoutOrNull(1_800) {
            runCatching {
                webView?.evalJs("(function(){return document.documentElement ? document.documentElement.outerHTML.slice(0, 5000) : '';})();").orEmpty()
            }.getOrElse { "HTML读取失败：${it.message}" }
        } ?: "HTML读取超时：evaluateJavascript 没有回调"
        return buildString {
            append(minimal)
            appendLine("stage=full")
            appendLine("pageInfo=$pageInfo")
            appendLine("htmlHead=$htmlHead")
        }
    }

    private suspend fun openPortalApp(webView: WebView, title: String) {
        val escapedTitle = JSONObject.quote(title)
        val result = webView.evalJs(
            """
            (function() {
              var title = $escapedTitle;
              var availableTabs = Array.prototype.slice.call(document.querySelectorAll('[amp-id="allCanUseApps"], [data-i18n="availableApps"]'));
              availableTabs.forEach(function(tab) {
                if ((tab.textContent || '').indexOf('可用应用') >= 0 || tab.getAttribute('amp-id') === 'allCanUseApps') {
                  tab.click();
                }
              });
              var target = Array.prototype.slice.call(document.querySelectorAll('#ampPersonalAsideLeftAllCanUseApps .canUseAppFlag[amp-title], #ampPersonalAsideLeftAllCanUseApps [amp-title]'))
                .find(function(el) { return (el.getAttribute('amp-title') || el.getAttribute('title') || '').trim() === title; });
              if (!target) {
                target = Array.prototype.slice.call(document.querySelectorAll('.appFlag, .appTitleFlag, [title]'))
                  .find(function(el) { return (el.textContent || el.getAttribute('title') || '').indexOf(title) >= 0; });
              }
              if (!target) return 'missing:' + title;
              target = target.closest && target.closest('.appFlag[amp-title], [amp-url][amp-title]') || target;
              target.scrollIntoView({block:'center'});
              ['pointerdown', 'mousedown', 'pointerup', 'mouseup', 'click'].forEach(function(eventName) {
                target.dispatchEvent(new MouseEvent(eventName, {
                  bubbles: true,
                  cancelable: true,
                  view: window
                }));
              });
              if (window.jQuery) window.jQuery(target).trigger('click');
              target.click();
              return JSON.stringify({
                status: 'clicked',
                title: title,
                appTitle: target.getAttribute('amp-title') || target.getAttribute('title') || '',
                url: target.getAttribute('amp-url') || '',
                appId: target.getAttribute('amp-appid') || ''
              });
            })();
            """.trimIndent(),
        )
        if (!result.contains("\"status\":\"clicked\"")) error("没有找到入口：$title，结果：$result")
        delay(1200)
    }

    private suspend fun openPortalService(webView: WebView, title: String) {
        openPortalApp(webView, title)
        val detailOrPage = waitForServiceDetailOrPage(title)
        val escapedTitle = JSONObject.quote(title)
        val result = detailOrPage.evalJs(
            """
            (function() {
              var title = $escapedTitle;
              var enter = document.querySelector('#ampDetailEnter.amp-active, #ampDetailEnter');
              function serviceUrlFrom(node) {
                if (!node) return '';
                var ampUrl = node.getAttribute('amp-url') || '';
                if (!ampUrl) return '';
                if (/^https?:\/\//i.test(ampUrl) || ampUrl.indexOf('//') === 0) return ampUrl;
                var root = (window.AMPConstant && window.AMPConstant.requestPath) || '/';
                if (root.charAt(root.length - 1) !== '/') root += '/';
                if (ampUrl.charAt(0) === '/') ampUrl = ampUrl.substring(1);
                return location.origin + root + ampUrl;
              }
              if (!enter) {
                var app = Array.prototype.slice.call(document.querySelectorAll('.appFlag[amp-title], [amp-url][amp-title]'))
                  .find(function(el) { return (el.getAttribute('amp-title') || el.getAttribute('title') || '').trim() === title; });
                if (app) {
                  return JSON.stringify({ status: 'direct-url', title: title, fallbackUrl: serviceUrlFrom(app) });
                }
                return location.href.indexOf('appShow') >= 0 ? 'already-open' : 'missing-enter';
              }
              var enterTitle = (enter.getAttribute('amp-title') || '').trim();
              if (enterTitle && enterTitle !== title) return 'wrong-detail:' + enterTitle;
              var fullUrl = serviceUrlFrom(enter);
              if (!fullUrl) return 'missing-enter-url';
              return JSON.stringify({ status: 'direct-url', title: title, fallbackUrl: fullUrl });
            })();
            """.trimIndent(),
        )
        if (result.startsWith("wrong-detail")) error("打开了错误的服务详情：$result")
        if (result == "missing-enter") error("没有看到“进入服务”按钮：$title")
        if (result == "missing-enter-url") error("“进入服务”按钮没有可打开的地址：$title")
        if (result == "already-open") return
        if (result.contains("\"status\":\"direct-url\"")) {
            val parsed = runCatching { JSONTokener(result).nextValue() as? JSONObject }.getOrNull()
            val fallbackUrl = parsed?.optString("fallbackUrl").orEmpty()
            if (!fallbackUrl.contains("appShow")) error("服务地址异常：$fallbackUrl")
            detailOrPage.loadUrl(fallbackUrl)
            waitForOpenedServicePage(title)
        }
    }

    private suspend fun waitForPortalServiceList(timeoutMillis: Long = 30_000): WebView {
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < timeoutMillis) {
            val webView = activeWebView
            if (webView != null) {
                val result = webView.evalJs(PORTAL_READY_SCRIPT)
                when (result) {
                    "ready" -> return webView
                    "login" -> Unit
                }
            }
            delay(600)
        }
        error("没有进入可用应用列表，请确认已经登录并进入教务系统主页")
    }

    private suspend fun waitForServiceDetailOrPage(title: String, timeoutMillis: Long = 18_000): WebView {
        val escapedTitle = JSONObject.quote(title)
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < timeoutMillis) {
            val webView = activeWebView
            if (webView != null) {
                val result = webView.evalJs(
                    """
                    (function() {
                      var title = $escapedTitle;
                      var enter = document.querySelector('#ampDetailEnter.amp-active, #ampDetailEnter');
                      if (enter) {
                        var enterTitle = (enter.getAttribute('amp-title') || '').trim();
                        if (!enterTitle || enterTitle === title) return 'detail';
                      }
                      var url = location.href || '';
                      if (url.indexOf('appShow') >= 0) return 'page';
                      return '';
                    })();
                    """.trimIndent(),
                )
                if (result == "detail" || result == "page") return webView
            }
            delay(500)
        }
        error("服务详情没有打开：$title")
    }

    private suspend fun waitForOpenedServicePage(title: String, timeoutMillis: Long = 24_000): WebView {
        val escapedTitle = JSONObject.quote(title)
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < timeoutMillis) {
            val webView = activeWebView
            if (webView != null) {
                val currentUrl = webView.url.orEmpty()
                val currentTitle = webView.title.orEmpty()
                if (isOpenedServiceWebViewState(currentUrl, currentTitle, title)) return webView
                val state = webView.evalJs(
                    """
                    (function() {
                      var title = $escapedTitle;
                      if (document.querySelector('#ampDetailEnter')) return 'detail';
                      var url = location.href || '';
                      var text = document.body ? (document.body.innerText || '') : '';
                      var htmlLength = document.documentElement ? document.documentElement.outerHTML.length : 0;
                      if (url === 'about:blank' || htmlLength < 120) return 'blank';
                      if (url.indexOf('appShow') >= 0) return 'loading-app-shell';
                      if (url.indexOf('/jwapp/sys/') >= 0 || text.indexOf(title) >= 0) return 'page';
                      var hasPortalApps = document.querySelectorAll('#ampPersonalAsideLeftAllCanUseApps .canUseAppFlag[amp-title]').length > 0;
                      return hasPortalApps ? 'portal' : 'loading';
                    })();
                    """.trimIndent(),
                )
                if (state == "page") return webView
            }
            delay(500)
        }
        error("点击进入服务后页面没有打开：$title")
    }

    private fun isOpenedServiceWebViewState(url: String, pageTitle: String, serviceTitle: String): Boolean {
        if (url.contains("/jwapp/sys/") && !url.contains("/new/") && !url.contains("appShow")) return true
        if (url.contains("/jwapp/sys/") && pageTitle.contains(serviceTitle)) return true
        return false
    }

    private suspend fun waitForPageText(vararg markers: String, timeoutMillis: Long = 18_000): WebView {
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < timeoutMillis) {
            val webView = activeWebView
            if (webView != null) {
                val text = webView.evalJs("(function(){return (document.title || '') + '\\n' + (document.body ? document.body.innerText : '');})();")
                if (markers.all { text.contains(it) }) return webView
            }
            delay(500)
        }
        error("等待页面超时：${markers.joinToString("、")}")
    }

    private suspend fun waitForCurriculumEntryPage(timeoutMillis: Long = 36_000): WebView {
        val start = System.currentTimeMillis()
        var reloadedBlankShell = false
        while (System.currentTimeMillis() - start < timeoutMillis) {
            val webView = activeWebView
            if (webView != null) {
                if (webView.hasLoadedCurriculumTree()) return webView
                val state = webView.evalJs(
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
                      if (href.indexOf('/sys/xywccx/') >= 0) return 'loading-shell';
                      return 'other';
                    })();
                    """.trimIndent(),
                )
                if (state == "ready" || state == "detail-loading") return webView
                if (state == "blank-shell" && !reloadedBlankShell && System.currentTimeMillis() - start > 8_000) {
                    reloadedBlankShell = true
                    webView.reload()
                    delay(2_000)
                }
            }
            delay(600)
        }
        error("等待培养方案首页超时，没有出现“查看详情”按钮")
    }

    private suspend fun waitForCurriculumTreeLoaded(
        preferredWebView: WebView? = null,
        timeoutMillis: Long = 45_000,
    ): WebView {
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < timeoutMillis) {
            val webView = preferredWebView ?: activeWebView
            if (webView != null && webView.hasLoadedCurriculumTree()) return webView
            delay(500)
        }
        error("等待培养方案课群树加载超时")
    }

    private suspend fun WebView.hasLoadedCurriculumTree(): Boolean {
        return evalJs(
            """
            (function() {
              var htmlLength = document.documentElement ? document.documentElement.outerHTML.length : 0;
              var allNodes = Array.prototype.slice.call(document.querySelectorAll('jmnode[nodeid]'));
              var realNodes = allNodes.filter(function(node) {
                if (node.classList.contains('root')) return false;
                var title = node.getAttribute('title') || node.textContent || '';
                return title.trim().length > 0;
              });
              if (htmlLength > 50000 && realNodes.length >= 3) return 'true';
              var text = (document.title || '') + '\n' + (document.body ? document.body.innerText : '');
              return text.indexOf('基础教育课程') >= 0 && text.indexOf('专业教育课程') >= 0 ? 'true' : 'false';
            })();
            """.trimIndent(),
        ) == "true"
    }

    private suspend fun WebView.pageHtml(): String = evalJs(
        "(function(){return document.documentElement.outerHTML;})();",
        timeoutMillis = 8_000,
    )

    private suspend fun WebView.evalJs(script: String, timeoutMillis: Long = 2_500): String {
        return withTimeoutOrNull(timeoutMillis) {
            suspendCancellableCoroutine { continuation ->
                evaluateJavascript(script) { encoded ->
                    if (continuation.isActive) continuation.resume(decodeJsString(encoded))
                }
            }
        }.orEmpty()
    }

    private fun ImportSummary.toToastText(): String {
        return "大课群$largeGroups 子课群$smallGroups 课程$courses 考试$exams 课表$classSessions"
    }

    private fun toast(message: String, duration: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(this, message, duration).show()
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun ManualSyncScreen(
    debugReport: String,
    onActiveWebViewChanged: (WebView?) -> Unit,
    onAutoImport: () -> Unit,
    onImport: (ImportPageType) -> Unit,
    onCopyDebugReport: () -> Unit,
    onCopyCurrentPageDebug: () -> Unit,
) {
    var type by remember { mutableStateOf(ImportPageType.CURRICULUM_GROUPS) }
    var menuOpen by remember { mutableStateOf(false) }
    var pageState by remember { mutableStateOf("正在打开教务系统") }
    var rootWebView by remember { mutableStateOf<WebView?>(null) }
    var childWebView by remember { mutableStateOf<WebView?>(null) }
    var panelExpanded by remember { mutableStateOf(true) }
    var panelOffsetX by remember { mutableStateOf(14f) }
    var panelOffsetY by remember { mutableStateOf(42f) }
    var autoImportStarted by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose {
            childWebView?.destroy()
            rootWebView?.destroy()
            onActiveWebViewChanged(null)
        }
    }

    Box(Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                val pageClient = object : WebViewClient() {
                    override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                        pageState = "正在加载"
                    }

                    override fun onPageFinished(view: WebView?, url: String?) {
                        pageState = "电脑网页模式"
                        view?.installCourseDetailClickPatch()
                        view?.evaluateJavascript(PORTAL_READY_SCRIPT) { encoded ->
                            if (!autoImportStarted && decodeJsString(encoded) == "ready") {
                                autoImportStarted = true
                                pageState = "已登录，开始自动同步"
                                onAutoImport()
                            }
                        }
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: WebResourceError?,
                    ) {
                        if (request?.isForMainFrame == true) {
                            pageState = "加载失败，可以刷新重试"
                        }
                    }
                }

                lateinit var chromeClient: WebChromeClient
                chromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        pageState = if (newProgress < 100) "加载中 $newProgress%" else "页面已加载"
                        view?.installCourseDetailClickPatch()
                    }

                    override fun onCreateWindow(
                        view: WebView?,
                        isDialog: Boolean,
                        isUserGesture: Boolean,
                        resultMsg: Message?,
                    ): Boolean {
                        val transport = resultMsg?.obj as? WebViewTransport ?: return false
                        childWebView?.destroy()
                        val child = WebView(context).apply {
                            layoutParams = fullSizeLayoutParams()
                            configureJnuDesktopMode()
                            webViewClient = pageClient
                            webChromeClient = chromeClient
                            addJavascriptInterface(NoopBridge, "JnuSmartEdu")
                        }
                        childWebView = child
                        onActiveWebViewChanged(child)
                        pageState = "已打开新页面"
                        transport.webView = child
                        resultMsg.sendToTarget()
                        return true
                    }

                    override fun onCloseWindow(window: WebView?) {
                        window?.destroy()
                        childWebView = null
                        rootWebView?.let(onActiveWebViewChanged)
                    }
                }

                WebView(context).apply {
                    layoutParams = fullSizeLayoutParams()
                    configureJnuDesktopMode()
                    CookieManager.getInstance().setAcceptCookie(true)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                    }
                    webViewClient = pageClient
                    webChromeClient = chromeClient
                    addJavascriptInterface(NoopBridge, "JnuSmartEdu")
                    rootWebView = this
                    onActiveWebViewChanged(this)
                    loadUrl(JNU_PORTAL_URL)
                }
            },
        )

        childWebView?.let { child ->
            AndroidView(modifier = Modifier.fillMaxSize(), factory = { child })
        }

        ImportPanel2(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset { IntOffset(panelOffsetX.roundToInt(), panelOffsetY.roundToInt()) }
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        panelOffsetX = (panelOffsetX + dragAmount.x).coerceAtLeast(0f)
                        panelOffsetY = (panelOffsetY + dragAmount.y).coerceAtLeast(0f)
                    }
                },
            type = type,
            menuOpen = menuOpen,
            pageState = pageState,
            expanded = panelExpanded,
            onMenuOpenChange = { menuOpen = it },
            onTypeChange = { type = it },
            onToggleExpanded = { panelExpanded = !panelExpanded },
            onImport = { onImport(type) },
            onBack = {
                val active = childWebView ?: rootWebView
                when {
                    active?.canGoBack() == true -> active.goBack()
                    childWebView != null -> {
                        childWebView?.destroy()
                        childWebView = null
                        rootWebView?.let(onActiveWebViewChanged)
                    }
                }
            },
            onRefresh = { (childWebView ?: rootWebView)?.reload() },
            onAutoImport = onAutoImport,
            debugReport = debugReport,
            onCopyDebugReport = onCopyDebugReport,
            onCopyCurrentPageDebug = onCopyCurrentPageDebug,
        )
    }
}

@Composable
private fun ImportPanel2(
    modifier: Modifier = Modifier,
    type: ImportPageType,
    menuOpen: Boolean,
    pageState: String,
    expanded: Boolean,
    onMenuOpenChange: (Boolean) -> Unit,
    onTypeChange: (ImportPageType) -> Unit,
    onToggleExpanded: () -> Unit,
    onImport: () -> Unit,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onAutoImport: () -> Unit,
    debugReport: String,
    onCopyDebugReport: () -> Unit,
    onCopyCurrentPageDebug: () -> Unit,
) {
    Card(
        modifier = modifier
            .widthIn(min = 148.dp, max = 260.dp)
            .padding(6.dp)
            .statusBarsPadding(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
    ) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (expanded) "教务页面导入 · $pageState" else "导入",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onToggleExpanded) {
                    Text(if (expanded) "收起" else "展开")
                }
            }
            if (!expanded) {
                Text("可拖动浮窗，避开网页按钮", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                return@Column
            }
            Button(onClick = onAutoImport, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Download, contentDescription = null)
                Text("登录后一键同步")
            }
            if (debugReport.isNotBlank()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "刚刚导入失败，点下面按钮复制调试信息发给我。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                        TextButton(onClick = onCopyDebugReport) {
                            Text("复制调试信息")
                        }
                    }
                }
            }
            OutlinedButton(onClick = onRefresh, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Text("刷新页面")
            }
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.ArrowBack, contentDescription = null)
                Text("返回")
            }
        }
    }
}

@Composable
private fun ImportPanel(
    modifier: Modifier = Modifier,
    type: ImportPageType,
    menuOpen: Boolean,
    pageState: String,
    expanded: Boolean,
    onMenuOpenChange: (Boolean) -> Unit,
    onTypeChange: (ImportPageType) -> Unit,
    onToggleExpanded: () -> Unit,
    onImport: () -> Unit,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
) {
    Card(
        modifier = modifier
            .widthIn(min = 152.dp, max = 360.dp)
            .padding(6.dp)
            .statusBarsPadding(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (expanded) "教务页面导入 · $pageState" else "导入",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onToggleExpanded) {
                    Text(if (expanded) "收起" else "展开")
                }
            }
            if (!expanded) {
                Text("可拖动浮窗，避开网页按钮", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                return@Column
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.weight(1f)) {
                    OutlinedButton(
                        onClick = { onMenuOpenChange(true) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(type.label, modifier = Modifier.weight(1f))
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                    }
                    DropdownMenu(
                        expanded = menuOpen,
                        onDismissRequest = { onMenuOpenChange(false) },
                    ) {
                        visibleImportTypes.forEach { item ->
                            DropdownMenuItem(
                                text = { Text(item.label) },
                                onClick = {
                                    onTypeChange(item)
                                    onMenuOpenChange(false)
                                },
                            )
                        }
                    }
                }
                Button(onClick = onImport) {
                    Icon(Icons.Default.Download, contentDescription = null)
                    Text("导入")
                }
            }
            OutlinedButton(onClick = onRefresh, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Refresh, contentDescription = null)
                Text("刷新页面")
            }
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.ArrowBack, contentDescription = null)
                Text("返回")
            }
            Text(
                text = if (type == ImportPageType.ALL_CURRICULUM_COURSES) {
                    "进入培养方案页后选“全部详细课程”并导入，系统会按所有叶子课群直接读取详细课程，不需要逐个点开。"
                } else if (type == ImportPageType.SCHEDULE) {
                    "进入我的课表页后直接导入。若页面接口不可读，再在网页右下角把每页改成 100 后重试。"
                } else {
                    "导入前请确认当前网页就是对应的教务页面。"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
private fun WebView.configureJnuDesktopMode() {
    settings.javaScriptEnabled = true
    settings.domStorageEnabled = true
    settings.databaseEnabled = true
    settings.loadsImagesAutomatically = true
    settings.javaScriptCanOpenWindowsAutomatically = true
    settings.setSupportMultipleWindows(true)
    settings.userAgentString = DESKTOP_USER_AGENT
    settings.useWideViewPort = true
    settings.loadWithOverviewMode = true
    settings.builtInZoomControls = true
    settings.displayZoomControls = false
    settings.textZoom = 100
    settings.cacheMode = WebSettings.LOAD_DEFAULT
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
    }
}

private fun fullSizeLayoutParams() = ViewGroup.LayoutParams(
    ViewGroup.LayoutParams.MATCH_PARENT,
    ViewGroup.LayoutParams.MATCH_PARENT,
)

private fun WebView.installCourseDetailClickPatch() {
    evaluateJavascript(COURSE_DETAIL_PATCH_SCRIPT, null)
}

private fun WebView.openSelectedCourseDetail() {
    evaluateJavascript(
        "window.__jnuSmartEduOpenSelectedCourse && window.__jnuSmartEduOpenSelectedCourse();",
        null,
    )
}

private fun decodeJsString(encoded: String?): String {
    return runCatching { JSONTokener(encoded ?: "\"\"").nextValue() as String }.getOrDefault("")
}

private fun String.totalExportedCourseRows(): Int {
    return runCatching {
        val groups = JSONObject(this).optJSONArray("groups") ?: return@runCatching 0
        var total = 0
        for (index in 0 until groups.length()) {
            total += groups.optJSONObject(index)?.optJSONArray("rows")?.length() ?: 0
        }
        total
    }.getOrDefault(0)
}

private fun String.scheduleRowCount(): Int {
    return runCatching {
        JSONObject(this).optJSONArray("rows")?.length() ?: 0
    }.getOrDefault(0)
}

private val visibleImportTypes = listOf(
    ImportPageType.CURRICULUM_GROUPS,
    ImportPageType.ALL_CURRICULUM_COURSES,
    ImportPageType.SCHEDULE,
    ImportPageType.EXAMS,
)

private val COURSE_DETAIL_PATCH_SCRIPT =
    """
    (function() {
      function titleFromNode(node, domNode) {
        if (domNode) {
          var titleEl = domNode.querySelector(".jsmind_node_title_span");
          if (titleEl) return titleEl.getAttribute("title") || titleEl.textContent || "";
        }
        if (!node) return "";
        if (typeof node.topic === "string") return node.topic.replace(/<[^>]+>/g, "");
        try { return node.topic.textContent || node.topic.innerText || ""; } catch (ignored) { return ""; }
      }

      function findJmNode(target) {
        if (!target) return null;
        if (target.closest) {
          var closest = target.closest("jmnode");
          if (closest) return closest;
        }
        var current = target;
        while (current && current !== document) {
          if (current.tagName && current.tagName.toLowerCase() === "jmnode") return current;
          current = current.parentNode;
        }
        return null;
      }

      function isLeaf(mindNode) {
        return !!mindNode && !mindNode.isroot && (!mindNode.children || mindNode.children.length === 0);
      }

      function ensureMenu(callback) {
        if (window.jsMindformMenu && window.jsMindformMenu.checkCourse) {
          callback();
          return;
        }
        try {
          if (window.require) {
            window.require(["public/commonpage_PYFAGL/jsmindFrame/jsmindFormMenu/jsmindFormMenu"], function() {
              if (window.jsMindformMenu && window.jsMindformMenu.checkCourse) callback();
            });
          }
        } catch (ignored) {}
      }

      function openCourse(nodeId, title, domNode, force) {
        try {
          if (!window._jm || !window._jm.get_node) return false;
          var mindNode = window._jm.get_node(nodeId);
          if (!force && !isLeaf(mindNode)) return false;
          var now = Date.now();
          if (window.__jnuSmartEduLastCourseNode === nodeId && now - (window.__jnuSmartEduLastCourseOpenAt || 0) < 900) {
            return true;
          }
          ensureMenu(function() {
            window.__jnuSmartEduLastCourseNode = nodeId;
            window.__jnuSmartEduLastCourseOpenAt = now;
            window.jsMindformMenu.checkCourse(nodeId, title || titleFromNode(mindNode, domNode));
          });
          return true;
        } catch (ignored) {
          return false;
        }
      }

      function requestCourses(nodeId) {
        var params = {
          SCLBDM: "04",
          BYNJDM: window.CURRENT_PYFACX_BYNJDM || "",
          XH: window.CURRENT_PYFACX_USERID || "",
          "*order": "+SFTG",
          pageSize: 1000,
          pageNumber: 1
        };
        if (window.CURRENT_PYFACX_PYFADM && window.CURRENT_PYFACX_PYFADM === nodeId) {
          params.PYFADM = nodeId;
        } else {
          params.KZH = nodeId;
          params.PYFADM = window.CURRENT_PYFACX_PYFADM || "";
        }
        try {
          if (window.BH_UTILS && window.BH_UTILS.doSyncAjax && window.WIS_EMAP_SERV) {
            return window.BH_UTILS.doSyncAjax(
              window.WIS_EMAP_SERV.getAbsPath("/modules/xywccx/cxscfakzkchxkqkx.do"),
              params
            );
          }
        } catch (ignored) {}
        try {
          var body = Object.keys(params).map(function(key) {
            return encodeURIComponent(key) + "=" + encodeURIComponent(params[key]);
          }).join("&");
          var url = window.WIS_EMAP_SERV
            ? window.WIS_EMAP_SERV.getAbsPath("/modules/xywccx/cxscfakzkchxkqkx.do")
            : "/jwapp/sys/xywccx/modules/xywccx/cxscfakzkchxkqkx.do";
          var xhr = new XMLHttpRequest();
          xhr.open("POST", url, false);
          xhr.setRequestHeader("Content-Type", "application/x-www-form-urlencoded");
          xhr.send(body);
          if (xhr.status >= 200 && xhr.status < 300) return JSON.parse(xhr.responseText);
        } catch (ignored) {}
        return null;
      }

      function rowsFromResponse(response) {
        try {
          return (((response || {}).datas || {}).cxscfakzkchxkqkx || {}).rows || [];
        } catch (ignored) {
          return [];
        }
      }

      function textFromCell(cells, index) {
        var cell = cells[index];
        if (!cell) return "";
        var span = cell.querySelector("span[title]");
        return ((span && span.getAttribute("title")) || cell.textContent || "").trim();
      }

      function getNestedRows(response, action) {
        try {
          return (((response || {}).datas || {})[action] || {}).rows || [];
        } catch (ignored) {
          return [];
        }
      }

      function extractTermFromResponse(response) {
        var rows = getNestedRows(response, "dqxnxq");
        if (rows.length > 0) {
          return {
            term: rows[0].DM || rows[0].XNXQDM || rows[0].DM_DISPLAY || "",
            termName: rows[0].MC || rows[0].XNXQMC || rows[0].DM_DISPLAY || ""
          };
        }
        return { term: "", termName: "" };
      }

      function currentScheduleTerm() {
        var candidates = [];
        try { if (window.pub_param) candidates.push({ term: window.pub_param.xnxqdm, termName: window.pub_param.xnxqmc }); } catch (ignored) {}
        try { if (window.parentParam) candidates.push({ term: window.parentParam.xnxqdm, termName: window.parentParam.xnxqmc }); } catch (ignored) {}
        try {
          var termEl = document.querySelector("#dqxnxq2, [name='XNXQDM'], [data-name='XNXQDM']");
          if (termEl) {
            candidates.push({
              term: termEl.getAttribute("value") || termEl.value || termEl.textContent,
              termName: termEl.textContent
            });
          }
        } catch (ignored) {}
        for (var i = 0; i < candidates.length; i++) {
          var term = (candidates[i].term || "").trim();
          if (term) return { term: term, termName: (candidates[i].termName || term).trim() };
        }
        try {
          if (window.BH_UTILS && window.BH_UTILS.doSyncAjax && window.WIS_EMAP_SERV) {
            return extractTermFromResponse(window.BH_UTILS.doSyncAjax(window.WIS_EMAP_SERV.getAbsPath("/modules/jshkcb/dqxnxq.do"), {}));
          }
        } catch (ignored) {}
        try {
          var url = window.WIS_EMAP_SERV
            ? window.WIS_EMAP_SERV.getAbsPath("/modules/jshkcb/dqxnxq.do")
            : "/jwapp/sys/wdkb/modules/jshkcb/dqxnxq.do";
          var xhr = new XMLHttpRequest();
          xhr.open("POST", url, false);
          xhr.setRequestHeader("Content-Type", "application/x-www-form-urlencoded");
          xhr.send("");
          if (xhr.status >= 200 && xhr.status < 300) return extractTermFromResponse(JSON.parse(xhr.responseText));
        } catch (ignored) {}
        return { term: "", termName: "" };
      }

      function rowsFromVisibleScheduleTable() {
        var rows = [];
        document.querySelectorAll(".kblb-index-table tr[role=row], tr[id*='jqxWidget'][role=row]").forEach(function(row) {
          var cells = row.querySelectorAll("td[role=gridcell]");
          if (cells.length < 11) return;
          var courseName = textFromCell(cells, 5);
          var code = textFromCell(cells, 6);
          if (!courseName) return;
          rows.push({
            BJMC: textFromCell(cells, 3),
            JSXM: textFromCell(cells, 4),
            KCM: courseName,
            KCH: code,
            KCXF: textFromCell(cells, 7),
            SKSJ: textFromCell(cells, 9),
            JASMC: textFromCell(cells, 10)
          });
        });
        return rows;
      }

      function requestScheduleRows() {
        var currentTerm = currentScheduleTerm();
        var term = currentTerm.term || "";
        var termName = currentTerm.termName || term;
        var visibleRows = rowsFromVisibleScheduleTable();
        if (visibleRows.length > 0) {
          return JSON.stringify({
            type: "JNU_SCHEDULE",
            term: term,
            termName: termName,
            rows: visibleRows
          });
        }
        var params = {
          XNXQDM: term,
          pageSize: 1000,
          pageNumber: 1,
          "*order": "+XH,+KCH,+KXH,+SKXQ,+KSJC"
        };
        try {
          if (window.BH_UTILS && window.BH_UTILS.doSyncAjax && window.WIS_EMAP_SERV) {
            var response = window.BH_UTILS.doSyncAjax(
              window.WIS_EMAP_SERV.getAbsPath("/modules/xskcb/xskcb.do"),
              params
            );
            return JSON.stringify({
              type: "JNU_SCHEDULE",
              term: term,
              termName: termName,
              rows: getNestedRows(response, "xskcb")
            });
          }
        } catch (ignored) {}
        try {
          var body = Object.keys(params).map(function(key) {
            return encodeURIComponent(key) + "=" + encodeURIComponent(params[key]);
          }).join("&");
          var url = window.WIS_EMAP_SERV
            ? window.WIS_EMAP_SERV.getAbsPath("/modules/xskcb/xskcb.do")
            : "/jwapp/sys/wdkb/modules/xskcb/xskcb.do";
          var xhr = new XMLHttpRequest();
          xhr.open("POST", url, false);
          xhr.setRequestHeader("Content-Type", "application/x-www-form-urlencoded");
          xhr.send(body);
          if (xhr.status >= 200 && xhr.status < 300) {
            var parsed = JSON.parse(xhr.responseText);
            return JSON.stringify({
              type: "JNU_SCHEDULE",
              term: term,
              termName: termName,
              rows: getNestedRows(parsed, "xskcb")
            });
          }
        } catch (ignored) {}
        return "";
      }

      window.__jnuSmartEduExportAllCourseDetails = function() {
        var startedAt = Date.now();
        var rawNodes = [];
        if (window._jm && window._jm.mind && window._jm.mind.nodes) {
          Object.keys(window._jm.mind.nodes).forEach(function(key) { rawNodes.push(window._jm.mind.nodes[key]); });
        } else {
          document.querySelectorAll("jmnode[nodeid]").forEach(function(domNode) {
            rawNodes.push({ id: domNode.getAttribute("nodeid"), topic: titleFromNode(null, domNode), children: [], _data: { view: { element: domNode } } });
          });
        }
        var groups = [];
        var totalRows = 0;
        var failedGroups = [];
        rawNodes.forEach(function(node) {
          if (!isLeaf(node) || !node.id) return;
          var domNode = node._data && node._data.view && node._data.view.element;
          var response = requestCourses(node.id);
          var rows = rowsFromResponse(response);
          totalRows += rows.length;
          if (!response) failedGroups.push(node.id);
          groups.push({
            id: node.id,
            title: titleFromNode(node, domNode),
            rows: rows
          });
        });
        return JSON.stringify({
          type: "JNU_ALL_COURSES",
          groupCount: groups.length,
          totalRows: totalRows,
          failedGroups: failedGroups,
          durationMs: Date.now() - startedAt,
          groups: groups
        });
      };

      window.__jnuSmartEduExportSchedule = requestScheduleRows;

      window.__jnuSmartEduOpenSelectedCourse = function() {
        var selected = window._jm && window._jm.get_selected_node ? window._jm.get_selected_node() : null;
        if (selected && selected.id) {
          return openCourse(selected.id, titleFromNode(selected, selected._data && selected._data.view && selected._data.view.element), selected._data && selected._data.view && selected._data.view.element, false);
        }
        var selectedDom = document.querySelector("jmnode.selected, jmnode[class*=selected]");
        if (selectedDom) {
          return openCourse(selectedDom.getAttribute("nodeid"), titleFromNode(null, selectedDom), selectedDom, false);
        }
        return false;
      };

      if (!window.__jnuSmartEduCourseDetailEventsInstalled) {
        window.__jnuSmartEduCourseDetailEventsInstalled = true;
        ["pointerup", "touchend", "mouseup", "click", "dblclick"].forEach(function(eventName) {
          document.addEventListener(eventName, function(event) {
            var domNode = findJmNode(event.target);
            if (!domNode || domNode.classList.contains("root")) return;
            var nodeId = domNode.getAttribute("nodeid");
            window.setTimeout(function() {
              openCourse(nodeId, titleFromNode(null, domNode), domNode, false);
            }, 120);
          }, true);
        });
      }

      function patchSelectNode() {
        if (!window._jm || !window._jm.select_node || window._jm.__jnuSmartEduSelectNodePatched) return false;
        var originalSelectNode = window._jm.select_node;
        window._jm.select_node = function(node) {
          var result = originalSelectNode.apply(this, arguments);
          var selected = typeof node === "string" && this.get_node ? this.get_node(node) : node;
          if (selected && selected.id) {
            window.setTimeout(function() {
              openCourse(selected.id, titleFromNode(selected, selected._data && selected._data.view && selected._data.view.element), selected._data && selected._data.view && selected._data.view.element, false);
            }, 120);
          }
          return result;
        };
        window._jm.__jnuSmartEduSelectNodePatched = true;
        return true;
      }

      if (!window.__jnuSmartEduCourseDetailPatchTimer) {
        window.__jnuSmartEduCourseDetailPatchTimer = window.setInterval(patchSelectNode, 500);
      }
      patchSelectNode();
      return "installed";
    })();
    """.trimIndent()

private val AUTO_CURRICULUM_DETAIL_SCRIPT =
    """
    (function() {
      var allNodes = Array.prototype.slice.call(document.querySelectorAll('jmnode[nodeid]'));
      if (allNodes.length >= 3) return "already-detail";
      var bodyText = document.body ? (document.body.innerText || '') : '';
      if (bodyText.indexOf('基础教育课程') >= 0 || bodyText.indexOf('专业教育课程') >= 0) return "already-detail";
      var button = Array.prototype.slice.call(document.querySelectorAll("a,button"))
        .find(function(el) {
          return (el.getAttribute("data-action") || el.textContent || "").indexOf("查看详情") >= 0;
        });
      if (!button) return "missing";
      if (button.__jnuSmartEduClicked) return "clicked";
      var loading = document.querySelector(".app-loading-show, .jqx-datatable-load[style*='display: block'], .jqx-loader[style*='display: block']");
      if (loading) return "page-busy";
      if (!button.getAttribute("pyfadm")) return "not-ready";
      button.__jnuSmartEduClicked = true;
      button.scrollIntoView({block:"center"});
      button.click();
      return "clicked";
    })();
    """.trimIndent()

private val PORTAL_READY_SCRIPT =
    """
    (function() {
      var tabs = Array.prototype.slice.call(document.querySelectorAll('[amp-id="allCanUseApps"], [data-i18n="availableApps"], .amp-aside-box-mini-item, .amp-left-tab-item'));
      tabs.forEach(function(tab) {
        var text = (tab.textContent || '').trim();
        if (tab.getAttribute('amp-id') === 'allCanUseApps' || text.indexOf('可用应用') >= 0) {
          tab.click();
        }
      });
      var realApps = document.querySelectorAll('#ampPersonalAsideLeftAllCanUseApps .canUseAppFlag[amp-title], #ampPersonalAsideLeftAllCanUseApps [amp-title]').length;
      if (realApps > 0) return 'ready';
      var loginButton = document.querySelector('#ampHasNoLogin:not(.amp-hide), #ampLoginBtn');
      if (loginButton && (loginButton.offsetParent !== null || loginButton.id === 'ampLoginBtn')) return 'login';
      return '';
    })();
    """.trimIndent()

private val DEBUG_PAGE_INFO_SCRIPT =
    """
    (function() {
      var apps = document.querySelectorAll('#ampPersonalAsideLeftAllCanUseApps .canUseAppFlag[amp-title], #ampPersonalAsideLeftAllCanUseApps [amp-title]').length;
      var text = document.body ? (document.body.innerText || '') : '';
      var info = {
        href: location.href,
        title: document.title || '',
        ready: (function(){ try { return eval(${JSONObject.quote(PORTAL_READY_SCRIPT)}); } catch(e) { return 'error:' + e.message; } })(),
        appCount: apps,
        hasDetailEnter: !!document.querySelector('#ampDetailEnter'),
        detailEnterTitle: (document.querySelector('#ampDetailEnter') && document.querySelector('#ampDetailEnter').getAttribute('amp-title')) || '',
        detailEnterUrl: (document.querySelector('#ampDetailEnter') && document.querySelector('#ampDetailEnter').getAttribute('amp-url')) || '',
        hasJsmind: !!document.querySelector('#jsmind_container, jmnode, jmnodes'),
        jmnodeCount: document.querySelectorAll('jmnode[nodeid]').length,
        realJmnodeCount: Array.prototype.slice.call(document.querySelectorAll('jmnode[nodeid]')).filter(function(node) {
          return !node.classList.contains('root') && ((node.getAttribute('title') || node.textContent || '').trim().length > 0);
        }).length,
        hasScheduleExporter: !!window.__jnuSmartEduExportSchedule,
        hasCourseExporter: !!window.__jnuSmartEduExportAllCourseDetails,
        htmlLength: document.documentElement ? document.documentElement.outerHTML.length : 0,
        textHead: text.slice(0, 600)
      };
      return JSON.stringify(info);
    })();
    """.trimIndent()

private object NoopBridge {
    @JavascriptInterface
    fun noop() = Unit
}

private val ImportPageType.displayLabel: String
    get() = when (this) {
        ImportPageType.CURRICULUM_GROUPS -> "培养方案课群"
        ImportPageType.CURRICULUM_COURSES -> "详细课程"
        ImportPageType.ALL_CURRICULUM_COURSES -> "全部详细课程"
        ImportPageType.TRANSCRIPT -> "成绩单"
        ImportPageType.EXAMS -> "考试安排"
        ImportPageType.SCHEDULE -> "我的课表"
    }

private val ImportPageType.label: String
    get() = when (this) {
        ImportPageType.CURRICULUM_GROUPS -> "培养方案课群"
        ImportPageType.CURRICULUM_COURSES -> "详细课程"
        ImportPageType.ALL_CURRICULUM_COURSES -> "全部详细课程"
        ImportPageType.TRANSCRIPT -> "成绩单"
        ImportPageType.EXAMS -> "考试安排"
        ImportPageType.SCHEDULE -> "我的课表"
    }
