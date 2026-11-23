package com.example.rss_chsu.data.local

import androidx.compose.runtime.Immutable
import com.example.rss_chsu.ui.model.Article
import com.example.rss_chsu.ui.model.RssParser

/** Лёгкая проекция для карточек. Полный content и keywords сюда намеренно не входят. */
data class ArticleListItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val sourceName: String,
    val category: String,
    val author: String,
    val pubDateFormatted: String,
    val readTime: String,
    val imageUrl: String,
    val isBookmarked: Boolean,
    val isRead: Boolean,
    val publishedAt: Long,
    val sourceId: String
)

/**
 * Облегчённая проекция для индекса рекомендаций: без полного текста статьи.
 *
 * Намеренно БЕЗ isBookmarked/isRead — иначе любой UPDATE статуса (markAsRead /
 * toggleBookmark) инвалидирует курсор observeIndex() и пересобирает 500 Article.
 * Актуальный статус читается отдельным observeStatuses() (3 колонки) и накладывается
 * на UI уже в TodayFeedScreen поверх this.
 */
data class RecommendationArticleRow(
    val id: String,
    val title: String,
    val subtitle: String,
    val sourceName: String,
    val category: String,
    val author: String,
    val pubDateFormatted: String,
    val readTime: String,
    val imageUrl: String,
    val publishedAt: Long,
    val keywords: String,
    val sourceId: String
)

/** Лёгкая проекция статусов: только id + 2 флага. Используется для оверлея в UI. */
data class ArticleStatus(
    val id: String,
    val isBookmarked: Boolean,
    val isRead: Boolean
)

/** Агрегаты для подписей вкладок и счётчика закладок. */
data class ArticleCounts(
    val total: Int = 0,
    val unread: Int = 0,
    val bookmarks: Int = 0
)

/**
 * Стабильная обёртка над Map<String, String> для индекса sourceName -> domain.
 *
 * Compose считает Map нестабильным типом — параметр `sourceNameIndex: Map<String, String>`
 * в карточке не даёт Compose скипнуть рекомпозицию. `@Immutable`-обёртка перебивает
 * анализ стабильности: Compose доверяет контракту, что объект не мутирует после создания.
 */
@Immutable
data class SourceNameIndex(val map: Map<String, String> = emptyMap())

/**
 * Кэш относительных дат: форматирование выполняется ОДИН РАЗ при маппинге страницы БД
 * (вне UI-потока), а не в момент привязки карточки. Без кэша каждое обновление ленты
 * заново аллоцировало строку даты на каждую статью -> лишние GC-паузы при скролле.
 * Значение инвалидно через 60 секунд — ровно та гранулярность, что видна пользователю.
 */
private const val RELATIVE_DATE_TTL_MILLIS = 60_000L
private val relativeDateCache = android.util.LruCache<Long, String>(512)

internal fun cachedRelativeDate(millis: Long): String {
    if (millis <= 0L) return "только что"
    val bucket = millis / RELATIVE_DATE_TTL_MILLIS
    val nowBucket = System.currentTimeMillis() / RELATIVE_DATE_TTL_MILLIS
    relativeDateCache.get(bucket)?.let { return it }
    val formatted = RssParser.relativeFromMillis(millis)
    // Не держим даты старше часа: они должны пересчитываться после смены порога.
    if (nowBucket - bucket <= 60) relativeDateCache.put(bucket, formatted)
    return formatted
}

fun ArticleListItem.toArticle(isFeatured: Boolean = false): Article = Article(
    id = id,
    title = title,
    subtitle = subtitle,
    content = "", // Текст читается отдельным observeArticle(id) при открытии публикации.
    sourceName = sourceName,
    category = category,
    author = author,
    date = cachedRelativeDate(publishedAt),
    pubDateFormatted = pubDateFormatted,
    readTime = readTime,
    imageUrl = imageUrl,
    isBookmarked = isBookmarked,
    isRead = isRead,
    isFeatured = isFeatured,
    publishedAt = publishedAt,
    keywords = emptyList(),
    sourceId = sourceId
)

fun RecommendationArticleRow.toArticle(isFeatured: Boolean = false): Article = Article(
    id = id,
    title = title,
    subtitle = subtitle,
    content = "", // Не загружаем большие тексты ради рекомендаций.
    sourceName = sourceName,
    category = category,
    author = author,
    date = cachedRelativeDate(publishedAt),
    pubDateFormatted = pubDateFormatted,
    readTime = readTime,
    imageUrl = imageUrl,
    // Статусы в этой проекции не читаются: они накладываются из observeStatuses()
    // на уровне TodayFeedScreen. Дефолты false — намеренно, это индикатор «неизвестно».
    isBookmarked = false,
    isRead = false,
    isFeatured = isFeatured,
    publishedAt = publishedAt,
    keywords = keywords.split(',').filter(String::isNotBlank),
    sourceId = sourceId
)