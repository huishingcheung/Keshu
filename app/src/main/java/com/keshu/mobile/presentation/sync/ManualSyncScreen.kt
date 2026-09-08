package com.keshu.mobile.presentation.sync

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Message
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebView.WebViewTransport
import android.webkit.WebViewClient
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun ManualSyncScreen(
    debugReport: String,
    dataSourceName: String,
    portalUrl: String,
    desktopUserAgent: String,
    onActiveWebViewChanged: (WebView?) -> Unit,
    onPageLoaded: (WebView) -> Unit,
    isPortalReady: suspend (WebView) -> Boolean,
    onAutoImport: () -> Unit,
    onCopyDebugReport: () -> Unit,
    onClearPortalSession: () -> Unit,
) {
    var pageState by remember { mutableStateOf("正在打开教务系统") }
    var rootWebView by remember { mutableStateOf<WebView?>(null) }
    var childWebView by remember { mutableStateOf<WebView?>(null) }
    var panelExpanded by remember { mutableStateOf(true) }
    var panelOffsetX by remember { mutableFloatStateOf(14f) }
    var panelOffsetY by remember { mutableFloatStateOf(42f) }
    var autoImportStarted by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

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
                        view ?: return
                        onPageLoaded(view)
                        coroutineScope.launch {
                            if (!autoImportStarted && isPortalReady(view)) {
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
                        if (request?.isForMainFrame == true) pageState = "加载失败，可以刷新重试"
                    }
                }

                lateinit var chromeClient: WebChromeClient
                chromeClient = object : WebChromeClient() {
                    override fun onProgressChanged(view: WebView?, newProgress: Int) {
                        pageState = if (newProgress < 100) "加载中 $newProgress%" else "页面已加载"
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
                            configurePortalWebView(desktopUserAgent)
                            webViewClient = pageClient
                            webChromeClient = chromeClient
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
                    configurePortalWebView(desktopUserAgent)
                    CookieManager.getInstance().setAcceptCookie(true)
                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                    webViewClient = pageClient
                    webChromeClient = chromeClient
                    rootWebView = this
                    onActiveWebViewChanged(this)
                    loadUrl(portalUrl)
                }
            },
        )

        childWebView?.let { child ->
            AndroidView(modifier = Modifier.fillMaxSize(), factory = { child })
        }

        ImportPanel(
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
            pageState = pageState,
            dataSourceName = dataSourceName,
            expanded = panelExpanded,
            onToggleExpanded = { panelExpanded = !panelExpanded },
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
            onClearPortalSession = onClearPortalSession,
        )
    }
}

@Composable
private fun ImportPanel(
    modifier: Modifier = Modifier,
    pageState: String,
    dataSourceName: String,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onBack: () -> Unit,
    onRefresh: () -> Unit,
    onAutoImport: () -> Unit,
    debugReport: String,
    onCopyDebugReport: () -> Unit,
    onClearPortalSession: () -> Unit,
) {
    Surface(
        modifier = modifier.widthIn(min = 220.dp, max = 320.dp).padding(6.dp).statusBarsPadding(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        shadowElevation = 6.dp,
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(dataSourceName, style = MaterialTheme.typography.titleMedium)
                    Text(pageState, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = onToggleExpanded) { Text(if (expanded) "收起" else "展开") }
            }
            if (!expanded) {
                Text("拖动面板可避开网页内容", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                return@Column
            }
            Button(onClick = onAutoImport, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                Text("一键同步全部数据")
            }
            Text(
                "登录后将依次同步培养方案、详细课程、课表和考试安排。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (debugReport.isNotBlank()) {
                Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(14.dp)) {
                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "刚刚的导入未完成，可复制脱敏调试信息用于排查。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                        TextButton(onClick = onCopyDebugReport) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("复制调试信息")
                        }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(17.dp))
                    Text("返回")
                }
                OutlinedButton(onClick = onRefresh, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(17.dp))
                    Text("刷新")
                }
            }
            TextButton(onClick = onClearPortalSession, modifier = Modifier.fillMaxWidth()) {
                Text("退出并清除教务登录")
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
private fun WebView.configurePortalWebView(desktopUserAgent: String) {
    settings.javaScriptEnabled = true
    settings.domStorageEnabled = true
    settings.allowFileAccess = false
    settings.allowContentAccess = false
    settings.loadsImagesAutomatically = true
    settings.javaScriptCanOpenWindowsAutomatically = true
    settings.setSupportMultipleWindows(true)
    settings.userAgentString = desktopUserAgent
    settings.useWideViewPort = true
    settings.loadWithOverviewMode = true
    settings.builtInZoomControls = true
    settings.displayZoomControls = false
    settings.textZoom = 100
    settings.cacheMode = WebSettings.LOAD_DEFAULT
    settings.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
    settings.safeBrowsingEnabled = true
}

private fun fullSizeLayoutParams() = ViewGroup.LayoutParams(
    ViewGroup.LayoutParams.MATCH_PARENT,
    ViewGroup.LayoutParams.MATCH_PARENT,
)
