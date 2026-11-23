package com.example.rss_chsu.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "sources",
    indices = [
        Index(value = ["folderName"]),
        Index(value = ["domain"], unique = true)
    ]
)
data class SourceEntity(
    @PrimaryKey val domain: String,
    val name: String,
    val description: String,
    val logoText: String,
    val folderName: String?,
    val addedAt: Long
)