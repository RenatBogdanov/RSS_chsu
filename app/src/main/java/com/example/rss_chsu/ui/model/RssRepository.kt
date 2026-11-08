package com.example.rss_chsu.ui.model

import android.content.Context
import com.example.rss_chsu.data.local.AppDatabase
import com.example.rss_chsu.data.local.ArticleDao
import com.example.rss_chsu.data.local.toArticle
import com.example.rss_chsu.data.local.toEntity
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URI
import java.nio.charset.Charset

/** Лимиты кэша. Закладки в лимиты не входят и никогда не удаляются автоматически. */
data class CachePolicy(
    val maxArticles: Int = 500,
    val maxAgeDays: Int = 30
) {
    val maxAgeMillis: Long get() = maxAgeDays * 24L * 60 * 60 * 1000
}

object RssRepository {

    var cachePolicy = CachePolicy()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var dao: ArticleDao
    private var initialized = false

    // Публичный тип сохранён, экраны и FeedViewModel менять не нужно.
    // Источник данных теперь БД: любое изменение таблицы автоматически приходит сюда.
    private val _articles = MutableStateFlow<List<Article>>(emptyList())
    val articles: StateFlow<List<Article>> = _articles.asStateFlow()

    private val _sources = MutableStateFlow<List<Source>>(emptyList())
    val sources: StateFlow<List<Source>> = _sources.asStateFlow()

    private val _folders = MutableStateFlow<List<Folder>>(emptyList())
    val folders: StateFlow<List<Folder>> = _folders.asStateFlow()

    /** Вызвать один раз при старте приложения (MainActivity.onCreate или Application.onCreate). */
    @Synchronized
    fun init(context: Context) {
        if (initialized) return
        dao = AppDatabase.get(context).articleDao()
        initialized = true

        scope.launch { enforceCacheLimit() }

        scope.launch {
            dao.observeAll()
                .map { list ->
                    // «Главной» считаем самую свежую статью
                    list.mapIndexed { index, entity -> entity.toArticle().copy(isFeatured = index == 0) }
                }
                .flowOn(Dispatchers.Default)
                .collect { _articles.value = it }
        }
    }

    fun syncWithBackend() {
        // Поддерживаем совместимость с FeedViewModel
    }

    private suspend fun saveArticles(list: List<Article>) {
        val now = System.currentTimeMillis()
        val minPublished = now - cachePolicy.maxAgeMillis

        val entities = withContext(Dispatchers.Default) {
            list
                // не кладём в кэш то, что тут же будет удалено по возрасту
                .filter { it.publishedAt == 0L || it.publishedAt >= minPublished }
                .map { it.toEntity(fetchedAt = now) }
        }
        dao.insertIgnore(entities)
        enforceCacheLimit()
    }

    /** Удаляет устаревшие и лишние статьи (кроме закладок). */
    suspend fun enforceCacheLimit() {
        val policy = cachePolicy
        dao.deleteOlderThan(System.currentTimeMillis() - policy.maxAgeMillis)
        dao.deleteOverLimit(policy.maxArticles)
    }

    fun createFolder(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        val current = _folders.value.toMutableList()
        if (current.none { it.name.equals(trimmed, ignoreCase = true) }) {
            current.add(
                Folder(
                    id = "f_${System.currentTimeMillis()}",
                    name = trimmed,
                    sourceCount = 0,
                    totalUnread = 0,
                    isExpanded = true
                )
            )
            _folders.value = current
        }
    }

    fun toggleFolderExpanded(folderId: String) {
        _folders.value = _folders.value.map {
            if (it.id == folderId) it.copy(isExpanded = !it.isExpanded) else it
        }
    }

    suspend fun addFeed(inputUrl: String, targetFolder: String? = null): Boolean {
        var cleanUrl = inputUrl.trim()
        if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
            cleanUrl = "https://$cleanUrl"
        }

        return try {
            val response = KtorClient.httpClient.get(cleanUrl)
            val bytes = response.readBytes()
            val xmlText = decodeXmlBytes(bytes)

            var (source, newArticles) = RssParser.parseRss(xmlText, cleanUrl)

            // Auto-discovery if user passed an HTML page with RSS links
            if (newArticles.isEmpty() && (xmlText.contains("<html", ignoreCase = true) || xmlText.contains("<!DOCTYPE", ignoreCase = true))) {
                val rssLinkMatch = Regex("""<link[^>]+type=["']application/(?:rss|atom)\+xml["'][^>]+href=["']([^"']+)["']""", RegexOption.IGNORE_CASE).find(xmlText)
                    ?: Regex("""<a[^>]+href=["']([^"']*rss[^"']*)["']""", RegexOption.IGNORE_CASE).find(xmlText)

                if (rssLinkMatch != null) {
                    var discoveredUrl = rssLinkMatch.groupValues[1]
                    if (discoveredUrl.startsWith("/")) {
                        val baseUri = URI(cleanUrl)
                        discoveredUrl = "${baseUri.scheme}://${baseUri.host}$discoveredUrl"
                    } else if (!discoveredUrl.startsWith("http://") && !discoveredUrl.startsWith("https://")) {
                        val baseUri = URI(cleanUrl)
                        discoveredUrl = "${baseUri.scheme}://${baseUri.host}/$discoveredUrl"
                    }

                    val discoveredResponse = KtorClient.httpClient.get(discoveredUrl)
                    val discoveredBytes = discoveredResponse.readBytes()
                    val discoveredXml = decodeXmlBytes(discoveredBytes)
                    val discoveredResult = RssParser.parseRss(discoveredXml, discoveredUrl)
                    if (discoveredResult.second.isNotEmpty()) {
                        source = discoveredResult.first
                        newArticles = discoveredResult.second
                    }
                }
            }

            if (newArticles.isEmpty()) {
                return false
            }

            val sourceWithFolder = source.copy(folderName = targetFolder)

            // Было: правка списка в памяти. Стало: запись в БД, UI обновится через Flow.
            saveArticles(newArticles)

            val currentSources = _sources.value.toMutableList()
            val existingIndex = currentSources.indexOfFirst { it.name == sourceWithFolder.name || it.domain == sourceWithFolder.domain }
            if (existingIndex >= 0) {
                currentSources[existingIndex] = currentSources[existingIndex].copy(folderName = targetFolder)
            } else {
                currentSources.add(sourceWithFolder)
            }
            _sources.value = currentSources

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun decodeXmlBytes(bytes: ByteArray): String {
        val asciiHeader = String(bytes, 0, minOf(bytes.size, 500), Charsets.ISO_8859_1)
        val match = Regex("""encoding=["']([^"']+)["']""", RegexOption.IGNORE_CASE).find(asciiHeader)
        val encodingName = match?.groupValues?.get(1) ?: "UTF-8"

        return try {
            String(bytes, Charset.forName(encodingName))
        } catch (_: Exception) {
            try {
                String(bytes, Charset.forName("windows-1251"))
            } catch (_: Exception) {
                String(bytes, Charsets.UTF_8)
            }
        }
    }

    fun toggleBookmark(articleId: String) {
        scope.launch { dao.toggleBookmark(articleId) }
    }

    fun markAsRead(articleId: String) {
        scope.launch { dao.markAsRead(articleId) }
    }

    fun toggleRead(articleId: String) {
        scope.launch { dao.toggleRead(articleId) }
    }
}