package com.example.rss_chsu.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.rss_chsu.ui.model.Article

@Entity(
    tableName = "articles",
    indices = [
        Index(value = ["publishedAt", "fetchedAt"]),
        Index(value = ["isRead"]),
        Index(value = ["isBookmarked"]),
        Index(value = ["sourceId"])
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

// Конвертер из UI-модели в БД (из-за отсутствия которого была ошибка)
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