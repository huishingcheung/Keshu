package com.keshu.mobile.data.network

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class DeepSeekClientTest {
    @Test
    fun sendsApiKeyModelAndMessagesToDeepSeek() = runTest {
        val api = RecordingDeepSeekApi()
        val client = DeepSeekClient(api)

        val result = client.complete("test-key", "system", "user")

        assertEquals("Bearer test-key", api.authorization)
        assertEquals("deepseek-v4-flash", api.request?.model)
        assertEquals("disabled", api.request?.thinking?.type)
        assertEquals(listOf("system", "user"), api.request?.messages?.map { it.role })
        assertEquals("可读建议", result)
    }

    private class RecordingDeepSeekApi : DeepSeekApi {
        var authorization: String? = null
        var request: DeepSeekChatRequest? = null

        override suspend fun chat(authorization: String, request: DeepSeekChatRequest): DeepSeekChatResponse {
            this.authorization = authorization
            this.request = request
            return DeepSeekChatResponse(
                choices = listOf(DeepSeekChoice(DeepSeekMessage("assistant", "可读建议"))),
            )
        }
    }
}
