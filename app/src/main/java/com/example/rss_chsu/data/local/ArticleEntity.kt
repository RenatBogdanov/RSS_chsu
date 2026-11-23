package com.example.rss_chsu.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.rss_chsu.ui.model.Article
import com.example.rss_chsu.ui.model.KeywordExtractor
import com.example.rss_chsu.ui.model.RssParser

@Entity(
    tableName = "articles",
    indices = [
        Index("isBookmarked"),
        Index("publishedAt"),
        Index("sourceId")
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
    val fetchedAt: Long,
    val keywords: String,
    @ColumnInfo(defaultValue = "''")
    val sourceId: String = ""
)

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
    publishedAt = if (publishedAt > 0L) publishedAt else fetchedAt,
    fetchedAt = fetchedAt,
    keywords = keywords
        .ifEmpty { KeywordExtractor.extract(title, subtitle, content) }
        .joinToString(","),
    sourceId = sourceId
)

fun ArticleEntity.toArticle(): Article = Article(
    id = id,
    title = title,
    subtitle = subtitle,
    content = content,
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
    keywords = if (keywords.isEmpty()) emptyList() else keywords.split(","),
    sourceId = sourceId
)
