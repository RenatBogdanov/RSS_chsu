package com.example.rss_chsu.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.rss_chsu.ui.model.Article
import com.example.rss_chsu.ui.model.KeywordExtractor
import com.example.rss_chsu.ui.model.RssParser

@Entity(
    tableName = "articles",
    indices = [Index("isBookmarked"), Index("publishedAt")]
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
    /** Время публикации (мс). Если в ленте даты нет — время загрузки. */
    val publishedAt: Long,
    /** Время, когда статья попала в кэш (мс). */
    val fetchedAt: Long,
    /** Ключевые слова (стемы) через запятую. */
    val keywords: String
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
        .joinToString(",")
)

fun ArticleEntity.toArticle(): Article = Article(
    id = id,
    title = RssParser.cleanGarbageText(title),
    subtitle = RssParser.cleanGarbageText(subtitle),
    content = RssParser.cleanGarbageText(content),
    sourceName = sourceName,
    category = category,
    author = RssParser.cleanGarbageText(author),
    // относительная дата пересчитывается при каждой загрузке из БД, чтобы не «протухала»
    date = RssParser.relativeFromMillis(publishedAt),
    pubDateFormatted = pubDateFormatted,
    readTime = readTime,
    imageUrl = imageUrl,
    isBookmarked = isBookmarked,
    isRead = isRead,
    publishedAt = publishedAt,
    keywords = if (keywords.isEmpty()) emptyList() else keywords.split(",")
)
