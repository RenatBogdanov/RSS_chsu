package com.example.rss_chsu.ui.model

import android.content.Context
import com.example.rss_chsu.data.local.AppDatabase
import com.example.rss_chsu.data.local.ArticleDao
import com.example.rss_chsu.data.local.FolderEntity
import com.example.rss_chsu.data.local.SourceDao
import com.example.rss_chsu.data.local.SourceEntity
import com.example.rss_chsu.data.local.SourceProfileEntity
import com.example.rss_chsu.data.local.toArticle
import com.example.rss_chsu.data.local.toEntity
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
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

    /** Профиль источника каталога считается свежим 7 дней; неудачную попытку повторяем через 6 часов. */
    private const val PROFILE_TTL_MILLIS = 7L * 24 * 60 * 60 * 1000
    private const val PROFILE_RETRY_MILLIS = 6L * 60 * 60 * 1000

    /** При обновлении берём только верх ленты: новое всегда в начале, а 500 статей на каждый запуск разбирать незачем. */
    private const val REFRESH_MAX_ITEMS = 60
    private const val REFRESH_MIN_INTERVAL_MILLIS = 5L * 60 * 1000
    private const val REFRESH_START_DELAY_MILLIS = 1_500L

    // Ограничитель одновременных загрузок RSS-лент
    // Защищает приложение от вылета (OOM), если попытаться добавить 200 ссылок разом
    private val fetchSemaphore = Semaphore(3)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var dao: ArticleDao
    private lateinit var sourceDao: SourceDao
    private var initialized = false

    // Публичные типы сохранены, экраны менять не нужно.
    // Источник данных теперь БД: любое изменение таблиц автоматически приходит сюда.
    private val _articles = MutableStateFlow<List<Article>>(emptyList())
    val articles: StateFlow<List<Article>> = _articles.asStateFlow()

    /** Объём данных статей, сохранённых в локальном кэше (в байтах). */
    private val _cacheSizeBytes = MutableStateFlow(0L)
    val cacheSizeBytes: StateFlow<Long> = _cacheSizeBytes.asStateFlow()

    private val _sources = MutableStateFlow<List<Source>>(emptyList())
    val sources: StateFlow<List<Source>> = _sources.asStateFlow()

    private val _folders = MutableStateFlow<List<Folder>>(emptyList())
    val folders: StateFlow<List<Folder>> = _folders.asStateFlow()

    @Volatile
    private var lastRefreshAt = 0L

    /** Ключевые слова источников каталога (адрес ленты -> слова). Нужны для рекомендаций источников. */
    private val _sourceProfiles = MutableStateFlow<Map<String, List<String>>>(emptyMap())
    val sourceProfiles: StateFlow<Map<String, List<String>>> = _sourceProfiles.asStateFlow()

    /** Вызвать один раз при старте приложения (MainActivity.onCreate или Application.onCreate). */
    @Synchronized
    fun init(context: Context) {
        if (initialized) return
        val db = AppDatabase.get(context)
        dao = db.articleDao()
        sourceDao = db.sourceDao()
        initialized = true

        // Все статьи из БД преобразуются в модель для списка, закладок и подсчётов.
        scope.launch {
            dao.observeIndex()
                .map { list ->
                    // «Главной» считаем самую свежую статью
                    list.mapIndexed { index, entity -> entity.toArticle().copy(isFeatured = index == 0) }
                }
                .flowOn(Dispatchers.Default)
                .collect { _articles.value = it }
        }

        // Размер кэша пересчитывается Room автоматически при изменении таблицы articles.
        scope.launch {
            dao.observeCacheSizeBytes()
                .collect { _cacheSizeBytes.value = it }
        }

        // Источники и папки со счётчиками. Счётчики приходят из БД (GROUP BY sourceId),
        // поэтому всегда совпадают с реальным содержимым кэша.
        scope.launch {
            combine(
                sourceDao.observeSources(),
                dao.observeSourceCounts(),
                sourceDao.observeFolders()
            ) { sourceRows, counts, folderRows ->
                val countBySource = counts.associateBy { it.sourceId }

                val sources = sourceRows.map { row ->
                    val count = countBySource[row.domain]
                    val total = count?.total ?: 0
                    Source(
                        id = row.domain,
                        name = row.name,
                        domain = row.domain,
                        description = row.description,
                        articlesPerWeek = "$total статей",
                        logoText = row.logoText,
                        isSubscribed = true,
                        folderName = row.folderName,
                        unreadCount = count?.unread ?: 0,
                        articleCount = total
                    )
                }

                val folders = folderRows.map { folder ->
                    val inFolder = sources.filter { it.folderName == folder.name }
                    Folder(
                        id = folder.id,
                        name = folder.name,
                        sourceCount = inFolder.size,
                        totalUnread = inFolder.sumOf { it.unreadCount },
                        isExpanded = folder.isExpanded,
                        sources = inFolder
                    )
                }

                Pair(sources, folders)
            }
                .flowOn(Dispatchers.Default)
                .collect { result ->
                    _sources.value = result.first
                    _folders.value = result.second
                }
        }

        // Профили источников каталога
        scope.launch {
            sourceDao.observeSourceProfiles()
                .map { rows ->
                    rows.associate { row ->
                        row.domain to row.keywords.split(",").filter { it.isNotBlank() }
                    }
                }
                .collect { _sourceProfiles.value = it }
        }

        scope.launch { enforceCacheLimit() }
    }

    /** Фоновое обновление свежих публикаций для всех подписок из сети */
    suspend fun refreshAllSources(): Boolean = withContext(Dispatchers.IO) {
        // Читаем подписки напрямую из БД: на старте приложения StateFlow ещё может быть пустым
        val currentSources = sourceDao.getSources()
        if (currentSources.isEmpty()) return@withContext false

        var hasNewData = false
        for (source in currentSources) {
            try {
                val cleanUrl = normalizeUrl(source.domain)
                // Ограничиваем одновременный парсинг для предотвращения зависаний
                fetchSemaphore.withPermit {
                    val response = KtorClient.httpClient.get(cleanUrl)
                    val bytes = response.readBytes()
                    val xmlText = decodeXmlBytes(bytes)

                    // Парсим статьи последовательно в фоне
                    val (_, fetchedArticles) = RssParser.parseRss(xmlText, source.name, maxItemsToParse = REFRESH_MAX_ITEMS)
                    if (fetchedArticles.isNotEmpty()) {
                        saveArticles(fetchedArticles.map { it.copy(sourceId = source.domain) })
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
        val now = System.currentTimeMillis()
        if (now - lastRefreshAt < REFRESH_MIN_INTERVAL_MILLIS) return
        lastRefreshAt = now

        scope.launch {
            // Даём интерфейсу отрисоваться: сетевое обновление и разбор лент не должны мешать запуску
            delay(REFRESH_START_DELAY_MILLIS)
            refreshAllSources()
        }
    }

    private fun normalizeUrl(raw: String): String {
        val trimmed = raw.trim()
        return if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) trimmed else "https://$trimmed"
    }

    private suspend fun saveArticles(list: List<Article>) {
        val now = System.currentTimeMillis()
        val minPublished = now - cachePolicy.maxAgeMillis

        val candidates = list
            .filter { it.publishedAt == 0L || it.publishedAt >= minPublished }
            .distinctBy { it.id }
        if (candidates.isEmpty()) return

        // Статьи, которые уже есть в БД, заново не обрабатываем: при каждом обновлении почти вся лента
        // состоит из уже сохранённого, а извлечение ключевых слов из текста — самая дорогая часть
        val existingIds = HashSet<String>()
        candidates.map { it.id }.chunked(400).forEach { existingIds.addAll(dao.existingIds(it)) }

        val entities = withContext(Dispatchers.Default) {
            candidates
                .filter { it.id !in existingIds }
                .map { it.toEntity(fetchedAt = now) }
        }
        if (entities.isNotEmpty()) dao.insertIgnore(entities)

        // Старые статьи без источника (сохранены до миграции БД) привязываем к источнику, как только он их снова отдаёт
        candidates
            .filter { it.sourceId.isNotEmpty() }
            .groupBy { it.sourceId }
            .forEach { (sourceId, items) ->
                items.map { it.id }.chunked(400).forEach { ids -> dao.adoptArticles(sourceId, ids) }
            }

        if (entities.isNotEmpty()) enforceCacheLimit()
    }

    /** Удаляет устаревшие и лишние статьи (кроме закладок). */
    suspend fun enforceCacheLimit() {
        val policy = cachePolicy
        dao.deleteOlderThan(System.currentTimeMillis() - policy.maxAgeMillis)

        for (source in sourceDao.getSources()) {
            dao.deleteOverLimitForSource(source.domain, policy.maxArticlesPerSource)
        }

        dao.deleteOverLimit(policy.maxArticles)
    }

    /** Одна статья с полным текстом. В списках текст обрезан, полный нужен только в читалке. */
    fun observeArticle(id: String): Flow<Article?> =
        dao.observeById(id)
            .map { it?.toArticle() }
            .flowOn(Dispatchers.Default)

    fun createFolder(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        if (_folders.value.any { it.name.equals(trimmed, ignoreCase = true) }) return

        val now = System.currentTimeMillis()
        scope.launch {
            sourceDao.insertFolder(
                FolderEntity(id = "f_$now", name = trimmed, isExpanded = true, createdAt = now)
            )
        }
    }

    fun toggleFolderExpanded(folderId: String) {
        scope.launch { sourceDao.toggleFolderExpanded(folderId) }
    }

    suspend fun addFeed(inputUrl: String, targetFolder: String? = null): Boolean = withContext(Dispatchers.IO) {
        val cleanUrl = normalizeUrl(inputUrl)

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

                // Если это известный источник из поиска, сохраняем его аккуратное название и описание,
                // если полностью кастомный — берём то, что распарсили
                val displaySource = knownSource ?: source.copy(domain = finalCleanUrl)

                val existing = sourceDao.getSources().firstOrNull {
                    it.name == displaySource.name || it.domain == displaySource.domain || it.domain == finalCleanUrl
                }

                // Идентификатор источника: у уже существующей подписки адрес ленты обновляем на найденный
                val storedDomain = if (existing != null) finalCleanUrl else displaySource.domain

                if (existing != null && existing.domain != storedDomain) {
                    dao.reassignArticles(existing.domain, storedDomain)
                    sourceDao.deleteSource(existing.domain)
                }

                sourceDao.upsertSource(
                    SourceEntity(
                        domain = storedDomain,
                        name = displaySource.name,
                        description = displaySource.description,
                        logoText = displaySource.logoText,
                        folderName = targetFolder ?: existing?.folderName,
                        addedAt = existing?.addedAt ?: System.currentTimeMillis()
                    )
                )

                // Моментально записываем первые 30 статей в БД
                saveArticles(newArticles.map { it.copy(sourceId = storedDomain) })

                // В фоне продолжаем допарсивать и догружать остаток (до 500)
                scope.launch(Dispatchers.Default) {
                    fetchSemaphore.withPermit {
                        val (_, restArticles) = RssParser.parseRss(finalXmlText, finalCleanUrl, maxItemsToParse = 470, skipItems = 30)
                        if (restArticles.isNotEmpty()) {
                            saveArticles(restArticles.map { it.copy(sourceId = storedDomain) })
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

    /**
     * Пробная загрузка лент из каталога: берём 15 свежих статей и запоминаем их ключевые слова.
     * По ним потом оценивается, насколько источник подходит пользователю.
     * За один вызов обрабатывается не больше [maxFetch] лент, уже посчитанные пропускаются.
     */
    suspend fun loadSourcePreviews(candidates: List<Source>, maxFetch: Int = 12) = withContext(Dispatchers.IO) {
        val cached = sourceDao.getSourceProfiles().associateBy { it.domain }
        val subscribed = sourceDao.getSources().map { it.domain }.toSet()
        val now = System.currentTimeMillis()

        val queue = candidates
            .filter { it.domain !in subscribed }
            .filter { candidate ->
                val profile = cached[candidate.domain]
                val ttl = if (profile == null || profile.keywords.isEmpty()) PROFILE_RETRY_MILLIS else PROFILE_TTL_MILLIS
                profile == null || now - profile.updatedAt > ttl
            }
            .take(maxFetch)

        for (candidate in queue) {
            var keywords: List<String> = emptyList()
            try {
                fetchSemaphore.withPermit {
                    val response = KtorClient.httpClient.get(normalizeUrl(candidate.domain))
                    val xmlText = decodeXmlBytes(response.readBytes())
                    val (_, items) = RssParser.parseRss(xmlText, candidate.domain, maxItemsToParse = 15)
                    // меньше трёх статей — скорее всего, это не лента (страница-заглушка, ошибка)
                    if (items.size >= 3) keywords = aggregateKeywords(items)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            sourceDao.upsertSourceProfile(
                SourceProfileEntity(candidate.domain, keywords.joinToString(","), now)
            )
        }
    }

    /** Слова, которые чаще всего встречаются в разных статьях ленты (по числу статей, а не упоминаний). */
    private fun aggregateKeywords(items: List<Article>, limit: Int = 40): List<String> {
        val docFrequency = LinkedHashMap<String, Int>()
        for (article in items) {
            KeywordExtractor.extract(article.title, article.subtitle, article.content)
                .distinct()
                .forEach { docFrequency.merge(it, 1, Int::plus) }
        }
        return docFrequency.entries
            .sortedByDescending { it.value }
            .take(limit)
            .map { it.key }
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
        val folder = _folders.value.find { it.id == folderId } ?: return
        scope.launch {
            sourceDao.clearFolderFromSources(folder.name)
            sourceDao.deleteFolder(folderId)
        }
    }

    /** Отписка. Статьи источника удаляются, кроме сохранённых в закладки. */
    fun deleteSource(sourceDomain: String) {
        scope.launch {
            dao.deleteNonBookmarkedBySource(sourceDomain)
            sourceDao.deleteSource(sourceDomain)
        }
    }

    fun updateSourceFolder(sourceDomain: String, newFolderName: String?) {
        scope.launch { sourceDao.updateSourceFolder(sourceDomain, newFolderName) }
    }

    /** Полная очистка всех данных приложения для тестирования (статьи, подписки, папки) */
    fun clearAllData() {
        scope.launch {
            dao.deleteAll()
            sourceDao.deleteAllSources()
            sourceDao.deleteAllFolders()
            // _articles, _sources и _folders очистятся сами через Flow от БД
        }
    }
}
