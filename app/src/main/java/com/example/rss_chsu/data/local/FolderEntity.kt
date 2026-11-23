package com.example.rss_chsu.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey val id: String,
    val name: String,
    val isExpanded: Boolean = false,
    val createdAt: Long
)