package com.example.rss_chsu.ui.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.rss_chsu.data.local.ArticleCounts
import com.example.rss_chsu.data.local.ArticlePagingRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class FeedViewModel : ViewModel() {

    /** Лёгкий индекс статей без полного текста; оставлен для рекомендаций и совместимости экранов. */
    val articles: StateFlow<List<Article>> = RssRepository.articles

    val articleCounts: StateFlow<ArticleCounts> = ArticlePagingRepository.observeArticleCounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ArticleCounts())

    // Статусы не входят в корпус: изменение прочтения/закладки не заставляет перестраивать IDF и векторы.
    private val recommendationCorpus = articles
        .map { list -> list.map { it.copy(isRead = false, isBookmarked = false, isFeatured = false) } }
        .distinctUntilChanged()

    private val recommendationEngine: StateFlow<RecommendationEngine> = recommendationCorpus
        .map(::RecommendationEngine)
        .flowOn(Dispatchers.Default)
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            RecommendationEngine(emptyList())
        )

    private val _recommendedArticles = kotlinx.coroutines.flow.MutableStateFlow<List<Article>>(emptyList())
    val recommendedArticles: StateFlow<List<Article>> = _recommendedArticles.asStateFlow()

    val personalizedArticles: StateFlow<List<Article>> = combine(
        recommendationEngine,
        articles
    ) { engine, currentArticles ->
        engine.getPersonalized(currentArticles, limit = 100)
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            RssRepository.syncWithBackend()
        }
    }

    fun pagedArticles(
        onlyUnread: Boolean,
        oldestFirst: Boolean,
        sourceDomains: List<String>,
        folderNames: List<String>
    ): Flow<PagingData<Article>> = ArticlePagingRepository.pagedFeed(
        onlyUnread = onlyUnread,
        oldestFirst = oldestFirst,
        sourceDomains = sourceDomains,
        folderNames = folderNames
    ).cachedIn(viewModelScope)

    fun pagedBookmarks(): Flow<PagingData<Article>> =
        ArticlePagingRepository.pagedBookmarks().cachedIn(viewModelScope)

    /** Вычисляет похожие статьи при открытии конкретной публикации. */
    fun loadRecommendations(targetArticle: Article) {
        viewModelScope.launch {
            val candidates = articles.value
            val recommendations = withContext(Dispatchers.Default) {
                recommendationEngine.value.getRecommendations(targetArticle, candidates, limit = 5)
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

    fun onReadToggle(articleId: String) {
        RssRepository.toggleRead(articleId)
    }

    /**
     * Возвращает предыдущую статью перед [currentId] по порядку в общем списке [articles].
     * Возвращает null, если текущая первая или не найдена.
     */
    fun getPreviousArticle(currentId: String): Article? {
        val list = articles.value
        val index = list.indexOfFirst { it.id == currentId }
        return if (index > 0) list[index - 1] else null
    }

    /**
     * Возвращает следующую статью после [currentId] по порядку в общем списке [articles].
     * Возвращает null, если текущая последняя или не найдена.
     */
    fun getNextArticle(currentId: String): Article? {
        val list = articles.value
        val index = list.indexOfFirst { it.id == currentId }
        return if (index >= 0 && index + 1 < list.size) list[index + 1] else null
    }

    /** Переключает закладку у статьи. */
    fun toggleBookmark(articleId: String) {
        RssRepository.toggleBookmark(articleId)
    }
}