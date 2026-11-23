package com.example.rss_chsu

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import com.example.rss_chsu.ui.model.KtorClient
import com.example.rss_chsu.ui.model.RssRepository
import com.example.rss_chsu.ui.screens.MainScreen
import com.example.rss_chsu.ui.theme.RSS_chsuTheme
import com.example.rss_chsu.ui.theme.ThemeManager
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeManager.init(applicationContext)

        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            ),
            navigationBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )

        super.onCreate(savedInstanceState)

        // Открываем локальную БД (Room) и подписываемся на неё. Должно быть до setContent
        RssRepository.init(applicationContext)

        // Асинхронно проверяем работу Ktor-клиента в фоновом потоке
        lifecycleScope.launch {
            try {
                val response: HttpResponse = KtorClient.httpClient.get("${KtorClient.BASE_URL}/ip")
                val responseText = response.bodyAsText()
                Log.d("KTOR_TEST", "Ура! Сеть работает. Ответ сервера: $responseText")
            } catch (e: Exception) {
                Log.e("KTOR_TEST", "Ошибка сети при проверке клиента: ${e.localizedMessage}")
            }
        }

        setContent {
            val currentTheme by ThemeManager.appTheme.collectAsState()

            RSS_chsuTheme(appTheme = currentTheme) {
                MainScreen()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) {
            ThemeManager.applyPendingIconChange(this)
        }
    }
}