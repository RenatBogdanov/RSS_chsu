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
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.net.URI
import java.nio.charset.Charset

/** Лимиты кэша. Закладки в лимиты не входят и никогда не удаляются автоматически. */
data class CachePolicy(
    val maxArticlesPerSource: Int = 100,
    val maxArticles: Int = 1000,
    val maxAgeDays: Int = 30
) {
    val maxAgeMillis: Long get() = maxAgeDays * 24L * 60 * 60 * 1000
}

object RssRepository {

    var cachePolicy = CachePolicy()
    
    // Ограничитель одновременных загрузок RSS-лент
    // Защищает приложение от вылета (OOM), если попытаться добавить 200 ссылок разом
    private val fetchSemaphore = Semaphore(3)

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

        scope.launch {
            dao.observeAll()
                .map { list ->
                    // «Главной» считаем самую свежую статью
                    list.mapIndexed { index, entity -> entity.toArticle().copy(isFeatured = index == 0) }
                }
                .flowOn(Dispatchers.Default)
                .collect { articles ->
                    _articles.value = articles

                    // Динамически обновляем количество статей и точный счетчик непрочитанных для источников
                    val currentSources = _sources.value
                    if (currentSources.isNotEmpty()) {
                        _sources.value = currentSources.map { src ->
                            val totalForSource = articles.count { src.matchesArticle(it) }
                            val unreadForSource = articles.count { src.matchesArticle(it) && !it.isRead }
                            src.copy(
                                unreadCount = unreadForSource,
                                articlesPerWeek = "$totalForSource статей"
                            )
                        }
                    }
                }
        }

        scope.launch { enforceCacheLimit() }
    }

    /** Фоновое обновление свежих публикаций для всех подписок из сети */
    suspend fun refreshAllSources(): Boolean = withContext(Dispatchers.IO) {
        val currentSources = _sources.value
        if (currentSources.isEmpty()) return@withContext false

        var hasNewData = false
        for (source in currentSources) {
            try {
                var cleanUrl = source.domain.trim()
                if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
                    cleanUrl = "https://$cleanUrl"
                }
                // Ограничиваем одновременный парсинг для предотвращения зависаний
                fetchSemaphore.withPermit {
                    val response = KtorClient.httpClient.get(cleanUrl)
                    val bytes = response.readBytes()
                    val xmlText = decodeXmlBytes(bytes)

                    // Парсим статьи последовательно в фоне
                    val (_, fetchedArticles) = RssParser.parseRss(xmlText, source.name, maxItemsToParse = 500)
                    if (fetchedArticles.isNotEmpty()) {
                        saveArticles(fetchedArticles)
                        hasNewData = true
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        hasNewData
    }

    fun syncWithBackend() {
        scope.launch {
            refreshAllSources()
        }
    }

    private suspend fun saveArticles(list: List<Article>) {
        val now = System.currentTimeMillis()
        val minPublished = now - cachePolicy.maxAgeMillis

        val entities = withContext(Dispatchers.Default) {
            list
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

        val currentSources = _sources.value
        for (source in currentSources) {
            dao.deleteOverLimitForSource(source.name, policy.maxArticlesPerSource)
            dao.deleteOverLimitForSource(source.domain, policy.maxArticlesPerSource)
        }

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

    suspend fun addFeed(inputUrl: String, targetFolder: String? = null): Boolean = withContext(Dispatchers.IO) {
        var cleanUrl = inputUrl.trim()
        if (!cleanUrl.startsWith("http://") && !cleanUrl.startsWith("https://")) {
            cleanUrl = "https://$cleanUrl"
        }

        try {
            fetchSemaphore.withPermit {
                val response = KtorClient.httpClient.get(cleanUrl)
                val bytes = response.readBytes()
                val xmlText = decodeXmlBytes(bytes)
    
                var finalXmlText = xmlText
                var finalCleanUrl = cleanUrl
    
                // Парсим только первые 30 статей, чтобы моментально добавить источник
                var (source, newArticles) = RssParser.parseRss(xmlText, cleanUrl, maxItemsToParse = 30)
    
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
                        val discoveredResult = RssParser.parseRss(discoveredXml, discoveredUrl, maxItemsToParse = 30)
                        if (discoveredResult.second.isNotEmpty()) {
                            source = discoveredResult.first
                            newArticles = discoveredResult.second
                            finalXmlText = discoveredXml
                            finalCleanUrl = discoveredUrl
                        }
                    }
                }
    
                if (newArticles.isEmpty()) {
                    return@withContext false
                }
    
                // Проверяем, есть ли уже этот источник в нашем каталоге популярных
                val knownSource = PopularSources.topFeeds.find { 
                    it.domain.equals(finalCleanUrl, ignoreCase = true) || 
                    (extractDomainKeywords(it.domain).isNotEmpty() && extractDomainKeywords(it.domain) == extractDomainKeywords(finalCleanUrl))
                }
                
                val sourceWithFolder = if (knownSource != null) {
                    // Если это известный источник из поиска, сохраняем его аккуратное название и описание
                    knownSource.copy(
                        folderName = targetFolder,
                        unreadCount = newArticles.size,
                        articlesPerWeek = "${newArticles.size} статей",
                        isSubscribed = true
                    )
                } else {
                    // Если это полностью кастомный источник, берем то, что распарсили
                    source.copy(
                        domain = finalCleanUrl,
                        folderName = targetFolder,
                        unreadCount = newArticles.size,
                        articlesPerWeek = "${newArticles.size} статей"
                    )
                }
    
                // Моментально записываем первые 30 статей в БД
                saveArticles(newArticles)
    
                val currentSources = _sources.value.toMutableList()
                val existingIndex = currentSources.indexOfFirst { it.name == sourceWithFolder.name || it.domain == sourceWithFolder.domain }
                if (existingIndex >= 0) {
                    currentSources[existingIndex] = currentSources[existingIndex].copy(
                        domain = finalCleanUrl,
                        folderName = targetFolder,
                        unreadCount = newArticles.size,
                        articlesPerWeek = "${newArticles.size} статей",
                        name = sourceWithFolder.name, // восстанавливаем красивое имя
                        description = sourceWithFolder.description, // восстанавливаем красивое описание
                        logoText = sourceWithFolder.logoText // восстанавливаем логотип
                    )
                } else {
                    currentSources.add(sourceWithFolder)
                }
                _sources.value = currentSources
                
                // В фоне продолжаем допарсивать и догружать остаток (до 500)
                scope.launch(Dispatchers.Default) {
                    fetchSemaphore.withPermit {
                        val (_, restArticles) = RssParser.parseRss(finalXmlText, finalCleanUrl, maxItemsToParse = 470, skipItems = 30)
                        if (restArticles.isNotEmpty()) {
                            saveArticles(restArticles)
                        }
                    }
                }
    
                true
            }
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

    fun deleteFolder(folderId: String) {
        val folder = _folders.value.find { it.id == folderId }
        if (folder != null) {
            _sources.value = _sources.value.map {
                if (it.folderName == folder.name) it.copy(folderName = null) else it
            }
            _folders.value = _folders.value.filter { it.id != folderId }
        }
    }

    fun deleteSource(sourceDomain: String) {
        val source = _sources.value.find { it.domain == sourceDomain }
        if (source != null) {
            _sources.value = _sources.value.filter { it.domain != sourceDomain }
            scope.launch {
                // Удаляем статьи и по домену, и по имени источника (на случай, если id сгенерирован по имени)
                dao.deleteArticlesBySource(sourceDomain, source.name)
            }
        }
    }

    fun updateSourceFolder(sourceDomain: String, newFolderName: String?) {
        _sources.value = _sources.value.map {
            if (it.domain == sourceDomain) it.copy(folderName = newFolderName) else it
        }
    }

    /** Полная очистка всех данных приложения для тестирования (статьи, подписки, папки) */
    fun clearAllData() {
        scope.launch {
            dao.deleteAll()
            _sources.value = emptyList()
            _folders.value = emptyList()
            // _articles очистятся сами через Flow от БД
        }
    }
}
