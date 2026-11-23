package com.example.rss_chsu.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        ArticleEntity::class,
        SourceEntity::class,
        FolderEntity::class,
        SourceProfileEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun articleDao(): ArticleDao
    abstract fun sourceDao(): SourceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Версия 1 содержала только articles. Миграция сохраняет существующие статьи,
        // закладки и отметки о прочтении, добавляя таблицы источников и папок.
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE articles ADD COLUMN sourceId TEXT NOT NULL DEFAULT ''"
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_articles_sourceId ON articles(sourceId)"
                )

                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS sources (
                        domain TEXT NOT NULL,
                        name TEXT NOT NULL,
                        description TEXT NOT NULL,
                        logoText TEXT NOT NULL,
                        folderName TEXT,
                        addedAt INTEGER NOT NULL,
                        PRIMARY KEY(domain)
                    )
                    """.trimIndent()
                )

                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS folders (
                        id TEXT NOT NULL,
                        name TEXT NOT NULL,
                        isExpanded INTEGER NOT NULL,
                        createdAt INTEGER NOT NULL,
                        PRIMARY KEY(id)
                    )
                    """.trimIndent()
                )

                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS source_profiles (
                        domain TEXT NOT NULL,
                        keywords TEXT NOT NULL,
                        updatedAt INTEGER NOT NULL,
                        PRIMARY KEY(domain)
                    )
                    """.trimIndent()
                )
            }
        }

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "rss.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
