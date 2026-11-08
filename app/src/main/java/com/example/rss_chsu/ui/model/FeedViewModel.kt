package com.example.rss_chsu.ui.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FeedViewModel : ViewModel() {

    // Экраны реактивно наблюдают за этим потоком (источник данных теперь БД)
    val articles: StateFlow<List<Article>> = RssRepository.articles

    // Похожие статьи для конкретной открытой статьи
    private val _recommendedArticles = MutableStateFlow<List<Article>>(emptyList())
    val recommendedArticles: StateFlow<List<Article>> = _recommendedArticles.asStateFlow()

    // Персональные рекомендации: пересчитываются при любом изменении БД
    // (новая статья, закладка, прочтение)
    val personalizedArticles: StateFlow<List<Article>> = articles
        .map { RecommendationEngine(it).getPersonalized(limit = 20) }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            RssRepository.syncWithBackend()
        }
    }

    /** Вычисляет похожие статьи. Вызывается при открытии статьи. */
    fun loadRecommendations(targetArticle: Article) {
        viewModelScope.launch {
            val recommendations = withContext(Dispatchers.Default) {
                RecommendationEngine(articles.value).getRecommendations(targetArticle, limit = 5)
            }
            _recommendedArticles.value = recommendations
        }
    }

    fun onBookmarkClick(articleId: String) {
        RssRepository.toggleBookmark(articleId)
    }

    fun onArticleRead(articleId: String) {
        RssRepository.markAsRead(articleId)
    }

    /** Переключатель «прочитано / не прочитано» (галочка в читалке). */
    fun onReadToggle(articleId: String) {
        RssRepository.toggleRead(articleId)
    }
}