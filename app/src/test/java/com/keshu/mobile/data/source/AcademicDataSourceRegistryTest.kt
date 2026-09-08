package com.keshu.mobile.data.source

import android.webkit.WebView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AcademicDataSourceRegistryTest {
    @Test
    fun `resolves registered sources and exposes the configured default`() {
        val first = FakeFactory("first", "First University")
        val second = FakeFactory("second", "Second University")

        val registry = AcademicDataSourceRegistry(
            factories = listOf(first, second),
            defaultSourceId = "second",
        )

        assertEquals(second, registry.defaultFactory)
        assertEquals(listOf(first.descriptor, second.descriptor), registry.availableSources)
        assertEquals(first, registry.factory("first"))
        assertNull(registry.factory("missing"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `rejects duplicate source identifiers`() {
        AcademicDataSourceRegistry(
            factories = listOf(FakeFactory("same", "One"), FakeFactory("same", "Two")),
            defaultSourceId = "same",
        )
    }

    private class FakeFactory(id: String, name: String) : WebAcademicDataSourceFactory {
        override val descriptor = AcademicDataSourceDescriptor(id, name)

        override fun create(activeWebView: () -> WebView?): WebAcademicDataSource {
            error("Not needed by registry tests")
        }
    }
}
