package com.keshu.mobile.data.portal

import android.webkit.WebView
import com.keshu.mobile.data.parser.JnuAcademicParser
import com.keshu.mobile.data.source.AcademicDataSourceDescriptor
import com.keshu.mobile.data.source.WebAcademicDataSource
import com.keshu.mobile.data.source.WebAcademicDataSourceFactory

class JnuDataSourceFactory(
    private val parser: JnuAcademicParser = JnuAcademicParser(),
) : WebAcademicDataSourceFactory {
    override val descriptor: AcademicDataSourceDescriptor = JnuPortalAdapter.DESCRIPTOR

    override fun create(activeWebView: () -> WebView?): WebAcademicDataSource {
        return JnuPortalAdapter(
            activeWebView = activeWebView,
            parser = parser,
        )
    }
}
