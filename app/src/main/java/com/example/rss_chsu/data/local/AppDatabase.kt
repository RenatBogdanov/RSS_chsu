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
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun articleDao(): ArticleDao
    abstract fun sourceDao(): SourceDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE articles ADD COLUMN sourceId TEXT NOT NULL DEFAULT ''"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_articles_sourceId ON articles(sourceId)"
                )
                db.execSQL(
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
                db.execSQL(
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
                db.execSQL(
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

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_articles_publishedAt_fetchedAt ON articles(publishedAt, fetchedAt)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_articles_isRead ON articles(isRead)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_articles_isBookmarked ON articles(isBookmarked)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_articles_sourceId ON articles(sourceId)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_sources_folderName ON sources(folderName)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_sources_domain ON sources(domain)"
                )
            }
        }

        /**
         * Пересобираем индексы articles под реальные запросы ленты:
         *  — isRead-индекс бесполезен (WHERE (:onlyUnread = 0 OR isRead = 0) — параметр);
         *  — одиночный sourceId-индекс заменяем на (sourceId, publishedAt, fetchedAt),
         *    который покрывает pagingFeedNewest/Oldest с фильтром по источнику
         *    и deleteOverLimitForSource.
         */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DROP INDEX IF EXISTS index_articles_isRead")
                db.execSQL("DROP INDEX IF EXISTS index_articles_sourceId")
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_articles_sourceId_publishedAt_fetchedAt " +
                            "ON articles(sourceId, publishedAt, fetchedAt)"
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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}