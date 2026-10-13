package com.example.rss_chsu.ui.model

import io.ktor.client.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

object KtorClient {
    // Временный тестовый URL. Когда ваш бэк будет готов, поменяем на IP вашего компьютера
    const val BASE_URL = "https://httpbin.org"

    // Переключили движок на Android для стабильного обхода блокировок и таймаутов на Samsung
    val httpClient = HttpClient(Android) {
        install(ContentNegotiation) {
            json(Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true // Чтобы фронт не падал, если в JSON будут лишние поля
            })
        }

        // Устанавливаем разумные таймауты (15 секунд)
        engine {
            connectTimeout = 15_000
            socketTimeout = 15_000
        }
    }
}
