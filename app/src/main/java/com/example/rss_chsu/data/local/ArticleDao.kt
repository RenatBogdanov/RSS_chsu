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

    /**
     * IGNORE: уже сохранённая статья не перезаписывается,
     * поэтому флаги isRead / isBookmarked не сбрасываются при повторной загрузке ленты.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(items: List<ArticleEntity>): List<Long>

    @Query("UPDATE articles SET isBookmarked = 1 - isBookmarked WHERE id = :id")
    suspend fun toggleBookmark(id: String)

    @Query("UPDATE articles SET isRead = 1 WHERE id = :id")
    suspend fun markAsRead(id: String)

    @Query("UPDATE articles SET isRead = 1 - isRead WHERE id = :id")
    suspend fun toggleRead(id: String)

    /** Лимит по возрасту. Закладки не трогаем. */
    @Query("DELETE FROM articles WHERE isBookmarked = 0 AND fetchedAt < :minFetchedAt")
    suspend fun deleteOlderThan(minFetchedAt: Long): Int

    /** Лимит по количеству на конкретный источник: оставляем не более [max] самых свежих не-закладок. */
    @Query(
        """
        DELETE FROM articles
        WHERE isBookmarked = 0 AND sourceName = :sourceName AND id NOT IN (
            SELECT id FROM articles
            WHERE isBookmarked = 0 AND sourceName = :sourceName
            ORDER BY publishedAt DESC, fetchedAt DESC
            LIMIT :max
        )
        """
    )
    suspend fun deleteOverLimitForSource(sourceName: String, max: Int): Int

    /** Общий лимит по количеству: оставляем [max] самых свежих не-закладок. Закладки не считаются. */
    @Query(
        """
        DELETE FROM articles
        WHERE isBookmarked = 0 AND id NOT IN (
            SELECT id FROM articles
            WHERE isBookmarked = 0
            ORDER BY publishedAt DESC, fetchedAt DESC
            LIMIT :max
        )
        """
    )
    suspend fun deleteOverLimit(max: Int): Int

    @Query("DELETE FROM articles")
    suspend fun deleteAll()
}
