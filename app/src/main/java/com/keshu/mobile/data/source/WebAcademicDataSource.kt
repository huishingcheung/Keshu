package com.keshu.mobile.data.source

import android.webkit.WebView

interface WebAcademicDataSource : AcademicDataSource {
    val portalUrl: String
    val desktopUserAgent: String

    fun installPageBridge(webView: WebView)
    suspend fun isPortalReady(webView: WebView?): Boolean
    suspend fun debugPageInfo(webView: WebView?): String
}

interface WebAcademicDataSourceFactory {
    val descriptor: AcademicDataSourceDescriptor

    fun create(activeWebView: () -> WebView?): WebAcademicDataSource
}
