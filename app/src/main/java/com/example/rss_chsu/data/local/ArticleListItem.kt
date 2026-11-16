package com.example.rss_chsu.data.local

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

/** Облегчённая проекция для индекса рекомендаций: без полного текста статьи. */
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
    val isBookmarked: Boolean,
    val isRead: Boolean,
    val publishedAt: Long,
    val keywords: String,
    val sourceId: String
)

/** Агрегаты для подписей вкладок и счётчика закладок. */
data class ArticleCounts(
    val total: Int = 0,
    val unread: Int = 0,
    val bookmarks: Int = 0
)

fun ArticleListItem.toArticle(isFeatured: Boolean = false): Article = Article(
    id = id,
    title = title,
    subtitle = subtitle,
    content = "", // Текст читается отдельным observeArticle(id) при открытии публикации.
    sourceName = sourceName,
    category = category,
    author = author,
    date = RssParser.relativeFromMillis(publishedAt),
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

fun RecommendationArticleRow.toArticle(): Article = Article(
    id = id,
    title = title,
    subtitle = subtitle,
    content = "", // Не загружаем большие тексты ради рекомендаций.
    sourceName = sourceName,
    category = category,
    author = author,
    date = RssParser.relativeFromMillis(publishedAt),
    pubDateFormatted = pubDateFormatted,
    readTime = readTime,
    imageUrl = imageUrl,
    isBookmarked = isBookmarked,
    isRead = isRead,
    publishedAt = publishedAt,
    keywords = keywords.split(',').filter(String::isNotBlank),
    sourceId = sourceId
)
