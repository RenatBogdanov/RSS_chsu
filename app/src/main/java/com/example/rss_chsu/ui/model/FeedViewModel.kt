package com.example.rss_chsu.ui.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class FeedViewModel : ViewModel() {

    // Экраны будут реактивно наблюдать за этим потоком данных
    val articles: StateFlow<List<Article>> = RssRepository.articles

    init {
        // При создании ViewModel автоматически запускаем фоновую синхронизацию
        viewModelScope.launch {
            RssRepository.syncWithBackend()
        }
    }

    // Обработка клика по закладке
    fun onBookmarkClick(articleId: String) {
        RssRepository.toggleBookmark(articleId)
    }

    // Обработка прочтения статьи
    fun onArticleRead(articleId: String) {
        RssRepository.markAsRead(articleId)
    }
}
