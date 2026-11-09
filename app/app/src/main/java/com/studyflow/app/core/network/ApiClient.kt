package com.studyflow.app.core.network

import com.studyflow.app.BuildConfig
import com.studyflow.app.core.session.SessionManager
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.TimeZone
import java.util.concurrent.TimeUnit

/**
 * Cliente HTTP único. Injeta o Bearer token, manda `X-User-ID` nas chamadas de `/tasks`
 * (o microserviço de tarefas exige o header) e derruba a sessão em 401.
 */
class ApiClient(
    private val sessionManager: SessionManager,
    baseUrl: String = BuildConfig.API_BASE_URL,
) {
    val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        explicitNulls = false
        encodeDefaults = false
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor(sessionManager))
        .addInterceptor(
            HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC
                else HttpLoggingInterceptor.Level.NONE
            },
        )
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(normalizeBaseUrl(baseUrl))
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    private class AuthInterceptor(private val sessionManager: SessionManager) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val original = chain.request()
            val builder = original.newBuilder()
            sessionManager.token?.let { builder.header("Authorization", "Bearer $it") }
            // O back resolve os dias de calendário no fuso do cliente; sem isso a diária
            // criada à noite começa no dia seguinte (UTC) e a conclusão é recusada.
            // Vale também para /study/dashboard, que consulta o serviço de tarefas.
            builder.header("X-Time-Zone", TimeZone.getDefault().id)
            if (original.url.encodedPath.startsWith("/tasks")) {
                sessionManager.userId?.let { builder.header("X-User-ID", it) }
            }
            val response = chain.proceed(builder.build())
            if (response.code == 401) sessionManager.expire()
            return response
        }
    }

    internal companion object {
        fun normalizeBaseUrl(value: String): String {
            val trimmed = value.trim().ifBlank { "http://10.0.2.2/" }
            return if (trimmed.endsWith("/")) trimmed else "$trimmed/"
        }
    }
}
