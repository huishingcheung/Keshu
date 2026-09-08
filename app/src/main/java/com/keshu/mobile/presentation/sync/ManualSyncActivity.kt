package com.keshu.mobile.presentation.sync

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebStorage
import android.webkit.WebView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.keshu.mobile.KeshuApp
import com.keshu.mobile.data.source.WebAcademicDataSource
import com.keshu.mobile.presentation.theme.KeshuTheme
import com.keshu.mobile.widget.WidgetUpdateManager
import kotlinx.coroutines.launch

private const val TAG = "ManualSyncActivity"

class ManualSyncActivity : ComponentActivity() {
    private var activeWebView: WebView? = null
    private val dataSource: WebAcademicDataSource by lazy {
        (application as KeshuApp).container.academicDataSourceRegistry.defaultFactory.create { activeWebView }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as KeshuApp).container
        val coordinator = ManualSyncCoordinator(
            dataSource = dataSource,
            importData = container.importAcademicDataUseCase::invoke,
            examAvailabilityStore = container.examAvailabilityPreferences,
        )
        setContent {
            KeshuTheme {
                var debugReport by remember { mutableStateOf("") }
                ManualSyncScreen(
                    debugReport = debugReport,
                    dataSourceName = dataSource.descriptor.displayName,
                    portalUrl = dataSource.portalUrl,
                    desktopUserAgent = dataSource.desktopUserAgent,
                    onActiveWebViewChanged = { activeWebView = it },
                    onPageLoaded = dataSource::installPageBridge,
                    isPortalReady = dataSource::isPortalReady,
                    onAutoImport = {
                        lifecycleScope.launch {
                            val result = runCatching { coordinator.run() }
                            result.exceptionOrNull()?.let { throwable ->
                                debugReport = buildFailureReport(throwable)
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
                    onCopyDebugReport = {
                        if (debugReport.isBlank()) {
                            toast("暂无调试信息")
                        } else {
                            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Keshu debug", debugReport))
                            toast("调试信息已复制")
                        }
                    },
                    onClearPortalSession = {
                        CookieManager.getInstance().removeAllCookies {
                            CookieManager.getInstance().flush()
                            WebStorage.getInstance().deleteAllData()
                            activeWebView?.clearCache(true)
                            runOnUiThread {
                                toast("教务登录信息已清除")
                                finish()
                            }
                        }
                    },
                )
            }
        }
    }

    private suspend fun buildFailureReport(throwable: Throwable): String {
        val pageInfo = runCatching { dataSource.debugPageInfo(activeWebView) }
            .getOrElse { "页面信息读取失败：${it.message}" }
        return buildString {
            appendLine("Keshu Debug Report")
            appendLine("source=${dataSource.descriptor.id}")
            appendLine("action=一键同步")
            appendLine("message=${throwable.message}")
            appendLine("exception=${throwable::class.java.name}")
            appendLine("pageInfo=$pageInfo")
            appendLine("stackTrace=${throwable.stackTraceToString()}")
        }
    }

    private fun toast(message: String, duration: Int = Toast.LENGTH_SHORT) {
        Toast.makeText(this, message, duration).show()
    }
}
