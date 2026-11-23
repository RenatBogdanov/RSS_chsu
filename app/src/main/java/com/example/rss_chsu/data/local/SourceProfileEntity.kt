package com.example.rss_chsu.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "source_profiles")
data class SourceProfileEntity(
    @PrimaryKey val domain: String,
    val keywords: String,
    val updatedAt: Long
)