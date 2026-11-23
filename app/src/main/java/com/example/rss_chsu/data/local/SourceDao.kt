package com.example.rss_chsu.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SourceDao {

    @Query("SELECT * FROM sources ORDER BY addedAt ASC")
    fun observeSources(): Flow<List<SourceEntity>>

    @Query("SELECT * FROM sources")
    suspend fun getSources(): List<SourceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSource(source: SourceEntity)

    @Query("DELETE FROM sources WHERE domain = :domain")
    suspend fun deleteSource(domain: String)

    @Query("UPDATE sources SET folderName = :folderName WHERE domain = :domain")
    suspend fun updateSourceFolder(domain: String, folderName: String?)

    @Query("UPDATE sources SET folderName = NULL WHERE folderName = :folderName")
    suspend fun clearFolderFromSources(folderName: String)

    @Query("DELETE FROM sources")
    suspend fun deleteAllSources()

    @Query("SELECT * FROM folders ORDER BY createdAt ASC")
    fun observeFolders(): Flow<List<FolderEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertFolder(folder: FolderEntity)

    @Query("UPDATE folders SET isExpanded = 1 - isExpanded WHERE id = :id")
    suspend fun toggleFolderExpanded(id: String)

    @Query("DELETE FROM folders WHERE id = :id")
    suspend fun deleteFolder(id: String)

    @Query("DELETE FROM folders")
    suspend fun deleteAllFolders()

    @Query("SELECT * FROM source_profiles")
    fun observeSourceProfiles(): Flow<List<SourceProfileEntity>>

    @Query("SELECT * FROM source_profiles")
    suspend fun getSourceProfiles(): List<SourceProfileEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSourceProfile(profile: SourceProfileEntity)

    /** Batch-upsert: одна инвалидация observeSourceProfiles вместо N подряд. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSourceProfiles(profiles: List<SourceProfileEntity>)
}