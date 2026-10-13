package com.example.rss_chsu.ui.model

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object RssRepository {

    // Локальный кэш статей в оперативной памяти устройства, берем данные из заглушек коллеги
    private val _articles = MutableStateFlow<List<Article>>(SampleData.articlesList)
    val articles: StateFlow<List<Article>> = _articles.asStateFlow()

    // Функция переключения закладки
    fun toggleBookmark(articleId: String) {
        val updatedList = _articles.value.map { article ->
            if (article.id == articleId) {
                val newStatus = !article.isBookmarked
                article.copy(isBookmarked = newStatus)
            } else {
                article
            }
        }
        _articles.value = updatedList
    }

    // Функция отметки статьи как прочитанной
    fun markAsRead(articleId: String) {
        val updatedList = _articles.value.map { article ->
            if (article.id == articleId) {
                article.copy(isRead = true)
            } else {
                article
            }
        }
        _articles.value = updatedList
    }

    // Заготовка под будущую синхронизацию с вашим Ktor-бэкендом
    suspend fun syncWithBackend() {
        try {
            // Тут в будущем будет: KtorClient.httpClient.get(...)
        } catch (e: Exception) {
            // Обработка ошибок сети
        }
    }
}
