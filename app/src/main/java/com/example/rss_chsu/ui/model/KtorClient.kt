package com.example.rss_chsu.ui.model

import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

object KtorClient {
    const val BASE_URL = "https://httpbin.org"

    val httpClient = HttpClient(Android) {
        followRedirects = true

        install(DefaultRequest) {
            header(HttpHeaders.UserAgent, "Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36")
            header(HttpHeaders.Accept, "application/rss+xml, application/xml, text/xml, application/atom+xml, text/html, */*")
            header(HttpHeaders.CacheControl, "no-cache, no-store, must-revalidate")
            header(HttpHeaders.Pragma, "no-cache")
        }

        install(HttpTimeout) {
            requestTimeoutMillis = 3_000
            connectTimeoutMillis = 3_000
            socketTimeoutMillis = 3_000
        }

        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            })
        }

        engine {
            connectTimeout = 3_000
            socketTimeout = 3_000
        }
    }
}
