package com.example.rss_chsu.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Подписка пользователя. domain — адрес RSS-ленты и идентификатор источника. */
@Entity(tableName = "sources")
data class SourceEntity(
    @PrimaryKey val domain: String,
    val name: String,
    val description: String,
    val logoText: String,
    val folderName: String?,
    val addedAt: Long
)

@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey val id: String,
    val name: String,
    val isExpanded: Boolean,
    val createdAt: Long
)

/** Ключевые слова источника из каталога, полученные пробной загрузкой ленты. */
@Entity(tableName = "source_profiles")
data class SourceProfileEntity(
    @PrimaryKey val domain: String,
    val keywords: String,
    val updatedAt: Long
)

/** Результат агрегирующего запроса: общее и непрочитанное количество статей. */
data class SourceCount(
    val sourceId: String,
    val total: Int,
    val unread: Int
)
