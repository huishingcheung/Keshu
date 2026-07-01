package edu.jnu.smartedu.data.network

import com.squareup.moshi.Json
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface DeepSeekApi {
    @POST("chat/completions")
    suspend fun chat(
        @Header("Authorization") authorization: String,
        @Body request: DeepSeekChatRequest,
    ): DeepSeekChatResponse
}

data class DeepSeekChatRequest(
    val model: String = "deepseek-v4-flash",
    val messages: List<DeepSeekMessage>,
    val temperature: Double = 0.2,
    val thinking: DeepSeekThinking = DeepSeekThinking(),
    @Json(name = "max_tokens") val maxTokens: Int? = null,
)

data class DeepSeekThinking(val type: String = "disabled")

data class DeepSeekMessage(
    val role: String,
    val content: String?,
)

data class DeepSeekChatResponse(
    val choices: List<DeepSeekChoice> = emptyList(),
)

data class DeepSeekChoice(
    val message: DeepSeekMessage,
)

class DeepSeekClient(private val api: DeepSeekApi) {
    suspend fun complete(
        apiKey: String,
        systemPrompt: String,
        userPrompt: String,
        maxTokens: Int = 800,
        temperature: Double = 0.2,
    ): String {
        require(apiKey.isNotBlank()) { "请输入 DeepSeek API Key" }
        val response = api.chat(
            authorization = "Bearer ${apiKey.trim()}",
            request = DeepSeekChatRequest(
                messages = listOf(
                    DeepSeekMessage(role = "system", content = systemPrompt),
                    DeepSeekMessage(role = "user", content = userPrompt),
                ),
                temperature = temperature,
                maxTokens = maxTokens,
            ),
        )
        return response.choices.firstOrNull()?.message?.content?.trim()
            .takeUnless { it.isNullOrBlank() }
            ?: error("DeepSeek 没有返回有效建议")
    }
}
