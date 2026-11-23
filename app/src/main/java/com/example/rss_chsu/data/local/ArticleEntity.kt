package com.example.rss_chsu.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.rss_chsu.ui.model.Article

@Entity(
    tableName = "articles",
    indices = [
        // Сортировка ленты без фильтров: ORDER BY publishedAt DESC, fetchedAt DESC
        Index(value = ["publishedAt", "fetchedAt"]),
        // Счётчик закладок + pagingBookmarks
        Index(value = ["isBookmarked"]),
        // Лента с фильтром по источнику: WHERE sourceId IN (...) ORDER BY publishedAt DESC, fetchedAt DESC
        // Также покрывает deleteOverLimitForSource и observeSourceCounts (префикс sourceId).
        Index(value = ["sourceId", "publishedAt", "fetchedAt"])
        // isRead-индекс удалён: WHERE (:onlyUnread = 0 OR isRead = 0) не даёт SQLite
        // выбрать индекс по параметру :onlyUnread — индекс всё равно не используется.
    ]
)
data class ArticleEntity(
    @PrimaryKey val id: String,
    val title: String,
    val subtitle: String,
    val content: String,
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
    val sourceId: String,
    val fetchedAt: Long
)

// Конвертер из БД в UI-модель
fun ArticleEntity.toArticle(): Article = Article(
    id = id,
    title = title,
    subtitle = subtitle,
    content = content,
    sourceName = sourceName,
    category = category,
    author = author,
    date = pubDateFormatted,
    pubDateFormatted = pubDateFormatted,
    readTime = readTime,
    imageUrl = imageUrl,
    isBookmarked = isBookmarked,
    isRead = isRead,
    publishedAt = publishedAt,
    keywords = keywords.split(",").filter { it.isNotBlank() },
    sourceId = sourceId
)

// Конвертер из UI-модели в БД
fun Article.toEntity(fetchedAt: Long): ArticleEntity = ArticleEntity(
    id = id,
    title = title,
    subtitle = subtitle,
    content = content,
    sourceName = sourceName,
    category = category,
    author = author,
    pubDateFormatted = pubDateFormatted,
    readTime = readTime,
    imageUrl = imageUrl,
    isBookmarked = isBookmarked,
    isRead = isRead,
    publishedAt = publishedAt,
    keywords = keywords.joinToString(","),
    sourceId = sourceId,
    fetchedAt = fetchedAt
)