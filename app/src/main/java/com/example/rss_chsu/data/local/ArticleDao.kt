package com.example.rss_chsu.data.local

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** Лёгкая проекция для one-time чистки дублей по нормализованному id. */
data class ArticleRowForDedup(
    val id: String,
    val isBookmarked: Boolean
)

@Dao
interface ArticleDao {

    @Query(
        """
        SELECT id, title, subtitle, sourceName, category, author, pubDateFormatted,
               readTime, imageUrl, publishedAt, keywords, sourceId
        FROM articles
        ORDER BY publishedAt DESC, fetchedAt DESC
        LIMIT 500
        """
    )
    fun observeIndex(): Flow<List<RecommendationArticleRow>>

    @Query("SELECT id, isBookmarked, isRead FROM articles")
    fun observeStatuses(): Flow<List<ArticleStatus>>

    @Query(
        """
        SELECT
            COUNT(*) AS total,
            COALESCE(SUM(CASE WHEN isRead = 0 THEN 1 ELSE 0 END), 0) AS unread,
            COALESCE(SUM(CASE WHEN isBookmarked = 1 THEN 1 ELSE 0 END), 0) AS bookmarks
        FROM articles
        """
    )
    fun observeArticleCounts(): Flow<ArticleCounts>

    @Query(
        """
        SELECT
            COUNT(*) AS total,
            COALESCE(SUM(CASE WHEN isRead = 0 THEN 1 ELSE 0 END), 0) AS unread,
            COALESCE(SUM(CASE WHEN isBookmarked = 1 THEN 1 ELSE 0 END), 0) AS bookmarks
        FROM articles
        WHERE (
                :hasNoFilters = 1
                OR (:hasSourceFilter = 1 AND sourceId IN (:sourceDomains))
                OR (:hasFolderFilter = 1 AND sourceId IN (
                    SELECT domain FROM sources WHERE folderName IN (:folderNames)
                ))
          )
        """
    )
    fun observeFilteredArticleCounts(
        hasNoFilters: Boolean,
        hasSourceFilter: Boolean,
        sourceDomains: List<String>,
        hasFolderFilter: Boolean,
        folderNames: List<String>
    ): Flow<ArticleCounts>

    @Query(
        """
        SELECT
            sourceId AS sourceId,
            COUNT(*) AS total,
            COALESCE(SUM(CASE WHEN isRead = 0 THEN 1 ELSE 0 END), 0) AS unread
        FROM articles
        WHERE sourceId != ''
        GROUP BY sourceId
        """
    )
    fun observeSourceCounts(): Flow<List<SourceCount>>

    @Query(
        """
        SELECT id, title, subtitle, sourceName, category, author, pubDateFormatted,
               readTime, imageUrl, isBookmarked, isRead, publishedAt, sourceId
        FROM articles
        WHERE (:onlyUnread = 0 OR isRead = 0)
          AND (
                :hasNoFilters = 1
                OR (:hasSourceFilter = 1 AND sourceId IN (:sourceDomains))
                OR (:hasFolderFilter = 1 AND sourceId IN (
                    SELECT domain FROM sources WHERE folderName IN (:folderNames)
                ))
          )
        ORDER BY publishedAt DESC, fetchedAt DESC, id DESC
        """
    )
    fun pagingFeedNewest(
        onlyUnread: Boolean,
        hasNoFilters: Boolean,
        hasSourceFilter: Boolean,
        sourceDomains: List<String>,
        hasFolderFilter: Boolean,
        folderNames: List<String>
    ): PagingSource<Int, ArticleListItem>

    @Query(
        """
        SELECT id, title, subtitle, sourceName, category, author, pubDateFormatted,
               readTime, imageUrl, isBookmarked, isRead, publishedAt, sourceId
        FROM articles
        WHERE (:onlyUnread = 0 OR isRead = 0)
          AND (
                :hasNoFilters = 1
                OR (:hasSourceFilter = 1 AND sourceId IN (:sourceDomains))
                OR (:hasFolderFilter = 1 AND sourceId IN (
                    SELECT domain FROM sources WHERE folderName IN (:folderNames)
                ))
          )
        ORDER BY publishedAt ASC, fetchedAt ASC, id ASC
        """
    )
    fun pagingFeedOldest(
        onlyUnread: Boolean,
        hasNoFilters: Boolean,
        hasSourceFilter: Boolean,
        sourceDomains: List<String>,
        hasFolderFilter: Boolean,
        folderNames: List<String>
    ): PagingSource<Int, ArticleListItem>

    @Query(
        """
        SELECT id, title, subtitle, sourceName, category, author, pubDateFormatted,
               readTime, imageUrl, isBookmarked, isRead, publishedAt, sourceId
        FROM articles
        WHERE isBookmarked = 1
        ORDER BY publishedAt DESC, fetchedAt DESC, id DESC
        """
    )
    fun pagingBookmarks(): PagingSource<Int, ArticleListItem>

    @Query("SELECT id FROM articles WHERE id IN (:ids)")
    suspend fun existingIds(ids: List<String>): List<String>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(items: List<ArticleEntity>): List<Long>

    @Query("UPDATE articles SET sourceId = :sourceId WHERE id IN (:ids) AND sourceId = ''")
    suspend fun adoptArticles(sourceId: String, ids: List<String>): Int

    @Query("UPDATE articles SET sourceId = :newSourceId WHERE sourceId = :oldSourceId")
    suspend fun reassignArticles(oldSourceId: String, newSourceId: String): Int

    @Query("SELECT * FROM articles WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<ArticleEntity?>

    @Query("UPDATE articles SET isBookmarked = 1 - isBookmarked WHERE id = :id")
    suspend fun toggleBookmark(id: String)

    @Query("UPDATE articles SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: String)

    @Query("UPDATE articles SET isRead = 1 - isRead WHERE id = :id")
    suspend fun toggleRead(id: String)

    @Query("DELETE FROM articles WHERE isBookmarked = 0 AND fetchedAt < :minFetchedAt")
    suspend fun deleteOlderThan(minFetchedAt: Long): Int

    @Query(
        """
        DELETE FROM articles
        WHERE sourceId = :sourceId
          AND isBookmarked = 0
          AND id NOT IN (
              SELECT id
              FROM articles
              WHERE sourceId = :sourceId AND isBookmarked = 0
              ORDER BY publishedAt DESC, fetchedAt DESC
              LIMIT :max
          )
        """
    )
    suspend fun deleteOverLimitForSource(sourceId: String, max: Int): Int

    @Query(
        """
        DELETE FROM articles
        WHERE isBookmarked = 0
          AND id NOT IN (
              SELECT id
              FROM articles
              WHERE isBookmarked = 0
              ORDER BY publishedAt DESC, fetchedAt DESC
              LIMIT :max
          )
        """
    )
    suspend fun deleteOverLimit(max: Int): Int

    @Query("DELETE FROM articles WHERE sourceId = :sourceId AND isBookmarked = 0")
    suspend fun deleteNonBookmarkedBySource(sourceId: String): Int

    @Query("DELETE FROM articles WHERE isBookmarked = 0 AND sourceId NOT IN (SELECT domain FROM sources)")
    suspend fun deleteOrphanUnbookmarkedArticles(): Int

    @Query("DELETE FROM articles")
    suspend fun deleteAll(): Int

    // ── Чистка дублей по нестабильному id ──────────────────────────────
    @Query("SELECT id, isBookmarked FROM articles")
    suspend fun getAllForDedup(): List<ArticleRowForDedup>

    @Query("DELETE FROM articles WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<String>): Int

    @Query("UPDATE articles SET id = :newId WHERE id = :oldId")
    suspend fun updateId(oldId: String, newId: String)
}