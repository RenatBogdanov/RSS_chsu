package com.example.rss_chsu

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.example.rss_chsu.ui.model.KtorClient
import com.example.rss_chsu.ui.model.RssRepository
import com.example.rss_chsu.ui.screens.MainScreen
import com.example.rss_chsu.ui.theme.RSS_chsuTheme
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

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

        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.isAppearanceLightStatusBars = true
        windowInsetsController.isAppearanceLightNavigationBars = true

        // Открываем локальную БД (Room) и подписываемся на неё. Должно быть до setContent
        RssRepository.init(applicationContext)

        // Асинхронно проверяем работу Ktor-клиента в фоновом потоке,
        // пока на экране Samsung загружается интерфейс приложения
        lifecycleScope.launch {
            try {
                val response: HttpResponse = KtorClient.httpClient.get("${KtorClient.BASE_URL}/ip")
                val responseText = response.bodyAsText()
                Log.d("KTOR_TEST", "Ура! Сеть работает. Ответ сервера: $responseText")
            } catch (e: Exception) {
                Log.e("KTOR_TEST", "Ошибка сети при проверке клиента: ${e.localizedMessage}")
            }
        }

        // Сохраняем запуск главного экрана, который написал ваш коллега
        setContent {
            RSS_chsuTheme {
                MainScreen()
            }
        }
    }
}