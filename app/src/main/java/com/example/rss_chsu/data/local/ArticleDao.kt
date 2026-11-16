package com.example.rss_chsu.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ArticleDao {

    @Query("SELECT * FROM articles ORDER BY publishedAt DESC, fetchedAt DESC")
    fun observeAll(): Flow<List<ArticleEntity>>

    // Используется репозиторием для основного списка статей.
    @Query("SELECT * FROM articles ORDER BY publishedAt DESC, fetchedAt DESC")
    fun observeIndex(): Flow<List<ArticleEntity>>

    // Статистика только по статьям, привязанным к источнику.
    @Query(
        """
        SELECT
            sourceId AS sourceId,
            COUNT(*) AS total,
            SUM(CASE WHEN isRead = 0 THEN 1 ELSE 0 END) AS unread
        FROM articles
        WHERE sourceId != ''
        GROUP BY sourceId
        """
    )
    fun observeSourceCounts(): Flow<List<SourceCount>>

    @Query("SELECT id FROM articles WHERE id IN (:ids)")
    suspend fun existingIds(ids: List<String>): List<String>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(items: List<ArticleEntity>): List<Long>

    // Привязка старых записей, у которых до миграции не было sourceId.
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

    /** Удаляет устаревшие статьи, но сохраняет закладки. */
    @Query("DELETE FROM articles WHERE isBookmarked = 0 AND fetchedAt < :minFetchedAt")
    suspend fun deleteOlderThan(minFetchedAt: Long): Int

    /** Оставляет max самых свежих незакладок в каждом источнике. */
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

    /** Оставляет max самых свежих незакладок суммарно по приложению. */
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

    @Query("DELETE FROM articles")
    suspend fun deleteAll(): Int
}
