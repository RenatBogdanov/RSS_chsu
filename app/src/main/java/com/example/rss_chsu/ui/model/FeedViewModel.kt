package com.example.rss_chsu.ui.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.example.rss_chsu.data.local.ArticleCounts
import com.example.rss_chsu.data.local.ArticlePagingRepository
import com.example.rss_chsu.data.local.ArticleStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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

/**
 * distinctUntilChangedBy отсутствует в kotlinx.coroutines.flow. Объявляем локально,
 * чтобы не тащить отдельную зависимость.
 */
private fun <T, K> Flow<T>.distinctUntilChangedBy(
    keySelector: (T) -> K
): Flow<T> = distinctUntilChanged { old, new ->
    keySelector(old) == keySelector(new)
}

class FeedViewModel : ViewModel() {

    /** Лёгкий индекс статей без полного текста и без статусов; для рекомендаций. */
    val articles: StateFlow<List<Article>> = RssRepository.articles

    /** Статусы (id -> isBookmarked/isRead) отдельным потоком, без пересборки корпуса. */
    val articleStatuses: StateFlow<Map<String, ArticleStatus>> = RssRepository.articleStatuses

    val articleCounts: StateFlow<ArticleCounts> = ArticlePagingRepository.observeArticleCounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ArticleCounts())

    /**
     * Корпус для рекомендаций: только id+keywords. Статусы в него не входят,
     * поэтому TF-IDF пересобирается ТОЛЬКО при реальном изменении корпуса
     * (новые/удалённые статьи), а не на каждый тап «прочитано».
     */
    private val recommendationCorpus = articles
        .distinctUntilChangedBy { list -> list.map { it.id to it.keywords } }

    private val recommendationEngine: StateFlow<RecommendationEngine> = recommendationCorpus
        .map(::RecommendationEngine)
        .flowOn(Dispatchers.Default)
        .stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            RecommendationEngine(emptyList())
        )

    // ── «Похожие статьи» для экрана читалки ─────────────────────────────
    private val _recommendedArticles = MutableStateFlow<List<Article>>(emptyList())
    val recommendedArticles: StateFlow<List<Article>> = _recommendedArticles.asStateFlow()

    /** id статьи, для которой сейчас посчитаны рекомендации в _recommendedArticles. */
    private val _recommendedFor = MutableStateFlow<String?>(null)
    val recommendedFor: StateFlow<String?> = _recommendedFor.asStateFlow()
    // ────────────────────────────────────────────────────────────────────

    /**
     * top-100 пересчитывается только при изменении recommendationEngine (корпуса),
     * а не при каждом обновлении статусов. `getPersonalized` получает corpus,
     * не полный articles — это ключевое отличие от прежней реализации.
     */
    val personalizedArticles: StateFlow<List<Article>> = combine(
        recommendationEngine,
        recommendationCorpus
    ) { engine, corpus ->
        engine.getPersonalized(corpus.dedupForDisplay(), limit = 100)
    }
        .distinctUntilChangedBy { list -> list.map(Article::id) }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            RssRepository.syncWithBackend()
        }
    }

    /**
     * cachedIn(viewModelScope) критичен: при уничтожении FeedPage внутри HorizontalPager
     * (beyondViewportPageCount) без cachedIn кэш PagingData терялся бы, и свайп на вкладку
     * и обратно запускал бы полную перезагрузку с page 1.
     */
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

    /**
     * Вычисляет похожие статьи при открытии конкретной публикации.
     *
     * Защита от гонки: если пользователь успел свайпнуть на другую статью до того,
     * как отработал расчёт, результат для предыдущей статьи не публикуем.
     * `_recommendedFor` — это «ключ», по которому UI понимает, для какой статьи
     * сейчас валиден список рекомендаций.
     */
    fun loadRecommendations(targetArticle: Article) {
        viewModelScope.launch {
            _recommendedFor.value = targetArticle.id
            _recommendedArticles.value = emptyList()

            val candidates = articles.value
            val recommendations = withContext(Dispatchers.Default) {
                recommendationEngine.value.getRecommendations(
                    targetArticle = targetArticle,
                    candidates = candidates,
                    limit = 5
                )
            }
            // Публикуем, только если пользователь всё ещё смотрит ту же статью.
            if (_recommendedFor.value == targetArticle.id) {
                _recommendedArticles.value = recommendations
            }
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

    private fun List<Article>.dedupForDisplay(): List<Article> {
        val seen = HashSet<String>()
        return buildList(size) {
            for (article in this@dedupForDisplay) {
                val key = buildString {
                    append(article.sourceId.lowercase())
                    append('|')
                    append(article.title.trim().lowercase().replace(Regex("\\s+"), " "))
                }
                if (seen.add(key)) add(article)
            }
        }
    }
}