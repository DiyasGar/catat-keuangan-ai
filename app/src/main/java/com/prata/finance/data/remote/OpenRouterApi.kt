package com.prata.finance.data.remote

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST

// ─── Data Models (OpenAI-compatible — dipakai ulang tanpa perubahan) ──────────

// Satu pesan dalam percakapan; role = "user" atau "assistant"
data class Message(
    val role: String,
    val content: String
)

// Body request ke endpoint Chat Completions
data class ChatRequest(
    val model: String,
    val messages: List<Message>,
    val temperature: Double = 0.1  // 0.1 = sangat faktual, tidak mengarang
)

// Response dari OpenRouter (format OpenAI-compatible)
data class ChatResponse(
    val choices: List<Choice>
)

data class Choice(
    val message: Message
)

// ─── Retrofit Service Interface ───────────────────────────────────────────────

interface OpenRouterService {
    @Headers("Content-Type: application/json")
    @POST("api/v1/chat/completions")  // tanpa leading slash — baseUrl sudah diakhiri "/"
    suspend fun getChatCompletion(
        @Header("Authorization") authHeader: String,  // format: "Bearer sk-or-..."
        @Header("HTTP-Referer") referer: String,       // direkomendasikan OpenRouter
        @Header("X-Title") appTitle: String,           // nama app agar muncul di dashboard
        @Body request: ChatRequest
    ): ChatResponse
}

// ─── Retrofit Singleton ───────────────────────────────────────────────────────

object OpenRouterClient {

    // PENTING: baseUrl WAJIB diakhiri "/" untuk Retrofit
    private const val BASE_URL = "https://openrouter.ai/"

    // OkHttp client dengan logging — tiap request/response muncul di Logcat tag "OkHttp"
    private val okHttpClient: OkHttpClient by lazy {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .build()
    }

    val service: OpenRouterService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OpenRouterService::class.java)
    }
}
