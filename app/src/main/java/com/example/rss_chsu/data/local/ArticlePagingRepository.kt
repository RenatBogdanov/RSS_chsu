package com.example.rss_chsu.data.local

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import com.example.rss_chsu.ui.model.Article
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/** Постраничные выборки Room. Преобразование карточек выполняется вне UI-потока. */
object ArticlePagingRepository {
    private const val PAGE_SIZE = 30

    @Volatile
    private var articleDao: ArticleDao? = null

    @Synchronized
    fun init(dao: ArticleDao) {
        articleDao = dao
    }

    private fun dao(): ArticleDao =
        checkNotNull(articleDao) {
            "ArticlePagingRepository must be initialized from RssRepository.init()"
        }

    fun observeArticleCounts(): Flow<ArticleCounts> = dao().observeArticleCounts()

    fun observeFilteredArticleCounts(
        sourceDomains: List<String> = emptyList(),
        folderNames: List<String> = emptyList()
    ): Flow<ArticleCounts> {
        val hasSourceFilter = sourceDomains.isNotEmpty()
        val hasFolderFilter = folderNames.isNotEmpty()
        val noFilters = !hasSourceFilter && !hasFolderFilter
        val safeDomains = sourceDomains.ifEmpty { listOf("__no_source_filter__") }
        val safeFolders = folderNames.ifEmpty { listOf("__no_folder_filter__") }

        return dao().observeFilteredArticleCounts(
            hasNoFilters = noFilters,
            hasSourceFilter = hasSourceFilter,
            sourceDomains = safeDomains,
            hasFolderFilter = hasFolderFilter,
            folderNames = safeFolders
        )
    }

    /**
     * Маппинг всей страницы целиком на Dispatchers.Default.
     * Один контекст-свитч на страницу вместо N переключений на каждый элемент.
     */
    private fun Flow<PagingData<ArticleListItem>>.mapToArticlesOnDefault(): Flow<PagingData<Article>> =
        map { page ->
            withContext(Dispatchers.Default) {
                page.map { it.toArticle(isFeatured = false) }
            }
        }

    fun pagedFeed(
        onlyUnread: Boolean = false,
        oldestFirst: Boolean = false,
        sourceDomains: List<String> = emptyList(),
        folderNames: List<String> = emptyList()
    ): Flow<PagingData<Article>> {
        val hasSourceFilter = sourceDomains.isNotEmpty()
        val hasFolderFilter = folderNames.isNotEmpty()
        val noFilters = !hasSourceFilter && !hasFolderFilter
        // Room/SQLite должен получить непустые IN-списки даже при выключенном фильтре.
        val safeDomains = sourceDomains.ifEmpty { listOf("__no_source_filter__") }
        val safeFolders = folderNames.ifEmpty { listOf("__no_folder_filter__") }

        val source = Pager(
            config = PagingConfig(
                pageSize = PAGE_SIZE,
                initialLoadSize = PAGE_SIZE * 2,   // 60 элементов сразу
                prefetchDistance = PAGE_SIZE,      // 30 — грузим заранее
                maxSize = PAGE_SIZE * 5,
                enablePlaceholders = false
            ),
            pagingSourceFactory = {
                if (oldestFirst) {
                    dao().pagingFeedOldest(
                        onlyUnread = onlyUnread,
                        hasNoFilters = noFilters,
                        hasSourceFilter = hasSourceFilter,
                        sourceDomains = safeDomains,
                        hasFolderFilter = hasFolderFilter,
                        folderNames = safeFolders
                    )
                } else {
                    dao().pagingFeedNewest(
                        onlyUnread = onlyUnread,
                        hasNoFilters = noFilters,
                        hasSourceFilter = hasSourceFilter,
                        sourceDomains = safeDomains,
                        hasFolderFilter = hasFolderFilter,
                        folderNames = safeFolders
                    )
                }
            }
        ).flow

        return source.mapToArticlesOnDefault()
    }

    fun pagedBookmarks(): Flow<PagingData<Article>> = Pager(
        config = PagingConfig(
            pageSize = PAGE_SIZE,
            initialLoadSize = PAGE_SIZE * 2,
            prefetchDistance = PAGE_SIZE,
            enablePlaceholders = false
        ),
        pagingSourceFactory = { dao().pagingBookmarks() }
    ).flow.mapToArticlesOnDefault()
}