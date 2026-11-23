
package com.example.rss_chsu.ui.model

import android.content.Context
import com.example.rss_chsu.data.local.AppDatabase
import com.example.rss_chsu.data.local.ArticleDao
import com.example.rss_chsu.data.local.ArticlePagingRepository
import com.example.rss_chsu.data.local.FolderEntity
import com.example.rss_chsu.data.local.SourceCount
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
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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
import kotlinx.coroutines.withTimeoutOrNull
import java.net.URI
import java.nio.charset.Charset
import kotlin.time.Duration.Companion.milliseconds

/** Лимиты кэша. Закладки в лимиты не входят и никогда не удаляются автоматически. */
data class CachePolicy(
    val maxArticlesPerSource: Int = 100,
    val maxArticles: Int = 5000,
    val maxAgeDays: Int = 30
) {
    val maxAgeMillis: Long get() = maxAgeDays * 24L * 60 * 60 * 1000
}

object RssRepository {

    var cachePolicy = CachePolicy()

    /** Профиль источника каталога считается свежим 7 дней; неудачную попытку повторяем через 6 часов. */
    private const val PROFILE_TTL_MILLIS = 7L * 24 * 60 * 60 * 1000
    private const val PROFILE_RETRY_MILLIS = 6L * 60 * 60 * 1000

    /** При обновлении берём только верх ленты. */
    private const val REFRESH_MAX_ITEMS = 60
    private const val REFRESH_MIN_INTERVAL_MILLIS = 5L * 60 * 1000
    private const val REFRESH_START_DELAY_MILLIS = 1_500L

    // Ограничитель количества одновременных загрузок RSS-лент.
    private val fetchSemaphore = Semaphore(15)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private lateinit var dao: ArticleDao
    private lateinit var sourceDao: SourceDao

    @Volatile
    private var initialized = false

    // Публичные типы сохранены для совместимости со старыми экранами.
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

    /** Ключевые слова источников каталога: адрес ленты -> список слов. */
    private val _sourceProfiles =
        MutableStateFlow<Map<String, List<String>>>(emptyMap())

    val sourceProfiles: StateFlow<Map<String, List<String>>> =
        _sourceProfiles.asStateFlow()

    // ---------------------------------------------------------
    // Инициализация
    // ---------------------------------------------------------

    /**
     * Вызывается один раз из MainActivity.onCreate()
     * до запуска Compose UI.
     */
    @Synchronized
    fun init(context: Context) {
        if (initialized) return

        val db = AppDatabase.get(context.applicationContext)

        // 1. Получаем DAO базы данных.
        dao = db.articleDao()

        // 2. ОБЯЗАТЕЛЬНО инициализируем Paging Repository.
        // Без этого вызова FeedViewModel может упасть
        // с IllegalStateException при создании.
        ArticlePagingRepository.init(dao)

        // 3. Получаем DAO источников.
        sourceDao = db.sourceDao()

        // 4. Только теперь разрешаем использование репозитория.
        initialized = true

        // -----------------------------------------------------
        // Основной список статей
        // -----------------------------------------------------

        scope.launch {
            dao.observeIndex()
                .map { list ->
                    list.mapIndexed { index, entity ->
                        entity.toArticle().copy(
                            isFeatured = index == 0
                        )
                    }
                }
                .flowOn(Dispatchers.Default)
                .collect { articles ->
                    _articles.value = articles
                }
        }

        // -----------------------------------------------------
        // Размер кэша
        // -----------------------------------------------------

        scope.launch {
            dao.observeCacheSizeBytes()
                .collect { size ->
                    _cacheSizeBytes.value = size
                }
        }

        // -----------------------------------------------------
        // Источники, точные счётчики и папки
        // -----------------------------------------------------

        scope.launch {

            combine(
                sourceDao.observeSources(),
                dao.observeSourceCounts(),
                sourceDao.observeFolders()
            ) { sourceRows: List<SourceEntity>,
                counts: List<SourceCount>,
                folderRows: List<FolderEntity> ->

                // Сопоставление статистики по sourceId.
                val countBySource: Map<String, SourceCount> =
                    counts.associateBy { count ->
                        count.sourceId
                    }

                val sources: List<Source> = sourceRows.map { row ->

                    val count = countBySource[row.domain]

                    val total = count?.total ?: 0
                    val unread = count?.unread ?: 0

                    Source(
                        id = row.domain,
                        name = row.name,
                        domain = row.domain,
                        description = row.description,
                        articlesPerWeek = "$total статей",
                        logoText = row.logoText,
                        isSubscribed = true,
                        folderName = row.folderName,
                        unreadCount = unread,
                        articleCount = total
                    )
                }

                val folders: List<Folder> = folderRows.map { folder ->

                    val inFolder = sources.filter { source ->
                        source.folderName == folder.name
                    }

                    Folder(
                        id = folder.id,
                        name = folder.name,
                        sourceCount = inFolder.size,
                        totalUnread = inFolder.sumOf { it.unreadCount },
                        isExpanded = folder.isExpanded,
                        sources = inFolder
                    )
                }

                // Возвращаем обе коллекции.
                sources to folders
            }
                .flowOn(Dispatchers.Default)
                .collect { (sources, folders) ->

                    _sources.value = sources
                    _folders.value = folders
                }
        }

        // -----------------------------------------------------
        // Профили источников каталога
        // -----------------------------------------------------

        scope.launch {
            sourceDao.observeSourceProfiles()
                .map { rows ->
                    rows.associate { row ->
                        row.domain to row.keywords
                            .split(",")
                            .filter { it.isNotBlank() }
                    }
                }
                .collect { profiles ->
                    _sourceProfiles.value = profiles
                }
        }

        // Проверяем ограничения кэша при запуске.
        scope.launch {
            enforceCacheLimit()
        }
    }

    // ---------------------------------------------------------
    // Обновление RSS
    // ---------------------------------------------------------

    /** Фоновое обновление публикаций для всех подписок. */
    suspend fun refreshAllSources(forceFull: Boolean = false): Boolean =
        withContext(Dispatchers.IO) {

            val currentSources = sourceDao.getSources()

            if (currentSources.isEmpty()) {
                return@withContext false
            }

            var hasNewData = false

            val fetchBlock: suspend CoroutineScope.() -> Unit = {
                currentSources.map { source ->
                    async(Dispatchers.IO) {
                        try {
                            val cleanUrl = normalizeUrl(source.domain)
                            val response = KtorClient.httpClient.get(cleanUrl)
                            val bytes = response.readBytes()
                            val xmlText = decodeXmlBytes(bytes)

                            val (_, fetchedArticles) =
                                RssParser.parseRss(
                                    xmlText,
                                    source.name,
                                    maxItemsToParse = REFRESH_MAX_ITEMS
                                )

                            if (fetchedArticles.isNotEmpty()) {
                                saveArticles(
                                    fetchedArticles.map { article ->
                                        article.copy(sourceId = source.domain)
                                    }
                                )
                                hasNewData = true
                            }
                        } catch (_: Throwable) {
                        }
                    }
                }.awaitAll()
            }

            if (forceFull) {
                coroutineScope { fetchBlock() }
            } else {
                withTimeoutOrNull(3000L) {
                    coroutineScope { fetchBlock() }
                }
            }

            if (hasNewData) {
                scope.launch { enforceCacheLimit() }
            }

            hasNewData
        }

    fun syncWithBackend() {
        val now = System.currentTimeMillis()

        if (now - lastRefreshAt < REFRESH_MIN_INTERVAL_MILLIS) {
            return
        }

        lastRefreshAt = now

        scope.launch {
            delay(REFRESH_START_DELAY_MILLIS.milliseconds)
            refreshAllSources()
        }
    }

    // ---------------------------------------------------------
    // URL
    // ---------------------------------------------------------

    private fun normalizeUrl(raw: String): String {
        val trimmed = raw.trim()

        return if (
            trimmed.startsWith("http://") ||
            trimmed.startsWith("https://")
        ) {
            trimmed
        } else {
            "https://$trimmed"
        }
    }

    // ---------------------------------------------------------
    // Сохранение статей
    // ---------------------------------------------------------

    private suspend fun saveArticles(list: List<Article>) {

        val now = System.currentTimeMillis()
        val minPublished = now - cachePolicy.maxAgeMillis

        val candidates = list
            .filter {
                it.publishedAt == 0L ||
                        it.publishedAt >= minPublished
            }
            .distinctBy { it.id }

        if (candidates.isEmpty()) return

        // Проверяем, какие статьи уже существуют.
        // Не пересоздаём сущности и не перезаписываем закладки.
        val existingIds = HashSet<String>()

        candidates
            .map { it.id }
            .chunked(400)
            .forEach { ids ->
                existingIds.addAll(dao.existingIds(ids))
            }

        val entities = withContext(Dispatchers.Default) {
            candidates
                .filter { it.id !in existingIds }
                .map { it.toEntity(fetchedAt = now) }
        }

        if (entities.isNotEmpty()) {
            dao.insertIgnore(entities)
        }

        // Привязываем старые статьи к источникам.
        candidates
            .filter { it.sourceId.isNotEmpty() }
            .groupBy { it.sourceId }
            .forEach { (sourceId, items) ->

                items
                    .map { it.id }
                    .chunked(400)
                    .forEach { ids ->
                        dao.adoptArticles(sourceId, ids)
                    }
            }
    }

    // ---------------------------------------------------------
    // Ограничения кэша
    // ---------------------------------------------------------

    /** Удаляет устаревшие и лишние статьи, кроме закладок. */
    suspend fun enforceCacheLimit() = withContext(Dispatchers.IO) {
        val policy = cachePolicy
        dao.deleteOlderThan(System.currentTimeMillis() - policy.maxAgeMillis)
        dao.deleteOverLimit(policy.maxArticles)
    }

    // ---------------------------------------------------------
    // Полная статья
    // ---------------------------------------------------------

    /** Полный текст загружается отдельно при открытии публикации. */
    fun observeArticle(id: String): Flow<Article?> =
        dao.observeById(id)
            .map { entity ->
                entity?.toArticle()
            }
            .flowOn(Dispatchers.Default)

    // ---------------------------------------------------------
    // Папки
    // ---------------------------------------------------------

    fun createFolder(name: String) {
        val trimmed = name.trim()

        if (trimmed.isBlank()) return

        if (
            _folders.value.any {
                it.name.equals(trimmed, ignoreCase = true)
            }
        ) {
            return
        }

        val now = System.currentTimeMillis()

        scope.launch {
            sourceDao.insertFolder(
                FolderEntity(
                    id = "f_$now",
                    name = trimmed,
                    isExpanded = true,
                    createdAt = now
                )
            )
        }
    }

    fun toggleFolderExpanded(folderId: String) {
        scope.launch {
            sourceDao.toggleFolderExpanded(folderId)
        }
    }

    fun deleteFolder(folderId: String) {
        val folder = _folders.value.find {
            it.id == folderId
        } ?: return

        scope.launch {
            sourceDao.clearFolderFromSources(folder.name)
            sourceDao.deleteFolder(folderId)
        }
    }

    // ---------------------------------------------------------
    // Добавление RSS
    // ---------------------------------------------------------

    suspend fun addFeed(
        inputUrl: String,
        targetFolder: String? = null
    ): Boolean = withContext(Dispatchers.IO) {

        val cleanUrl = normalizeUrl(inputUrl)

        try {
            fetchSemaphore.withPermit {

                val response =
                    KtorClient.httpClient.get(cleanUrl)

                val bytes = response.readBytes()
                val xmlText = decodeXmlBytes(bytes)

                var finalXmlText = xmlText
                var finalCleanUrl = cleanUrl

                // Первоначально загружаем только 30 публикаций.
                var (source, newArticles) =
                    RssParser.parseRss(
                        xmlText,
                        cleanUrl,
                        maxItemsToParse = 30
                    )

                // Автоматический поиск RSS внутри HTML-страницы.
                if (
                    newArticles.isEmpty() &&
                    (
                            xmlText.contains("<html", ignoreCase = true) ||
                                    xmlText.contains("<!DOCTYPE", ignoreCase = true)
                            )
                ) {
                    val rssLinkMatch =
                        Regex(
                            """<link[^>]+type=["']application/(?:rss|atom)\+xml["'][^>]+href=["']([^"']+)["']""",
                            RegexOption.IGNORE_CASE
                        ).find(xmlText)
                            ?: Regex(
                                """<a[^>]+href=["']([^"']*rss[^"']*)["']""",
                                RegexOption.IGNORE_CASE
                            ).find(xmlText)

                    if (rssLinkMatch != null) {

                        var discoveredUrl =
                            rssLinkMatch.groupValues[1]

                        if (discoveredUrl.startsWith("/")) {
                            val baseUri = URI(cleanUrl)

                            discoveredUrl =
                                "${baseUri.scheme}://${baseUri.host}$discoveredUrl"

                        } else if (
                            !discoveredUrl.startsWith("http://") &&
                            !discoveredUrl.startsWith("https://")
                        ) {
                            val baseUri = URI(cleanUrl)

                            discoveredUrl =
                                "${baseUri.scheme}://${baseUri.host}/$discoveredUrl"
                        }

                        val discoveredResponse =
                            KtorClient.httpClient.get(discoveredUrl)

                        val discoveredBytes =
                            discoveredResponse.readBytes()

                        val discoveredXml =
                            decodeXmlBytes(discoveredBytes)

                        val (discoveredSource, discoveredArticles) =
                            RssParser.parseRss(
                                discoveredXml,
                                discoveredUrl,
                                maxItemsToParse = 30
                            )

                        if (discoveredArticles.isNotEmpty()) {
                            source = discoveredSource
                            newArticles = discoveredArticles
                            finalXmlText = discoveredXml
                            finalCleanUrl = discoveredUrl
                        }
                    }
                }

                if (newArticles.isEmpty()) {
                    return@withContext false
                }

                // Ищем источник в популярном каталоге.
                val knownSource = PopularSources.topFeeds.find {
                    it.domain.equals(
                        finalCleanUrl,
                        ignoreCase = true
                    ) ||
                            (
                                    extractDomainKeywords(it.domain).isNotEmpty() &&
                                            extractDomainKeywords(it.domain) ==
                                            extractDomainKeywords(finalCleanUrl)
                                    )
                }

                val displaySource =
                    knownSource ?: source.copy(domain = finalCleanUrl)

                val existing: SourceEntity? =
                    sourceDao.getSources().firstOrNull {
                        it.name == displaySource.name ||
                                it.domain == displaySource.domain ||
                                it.domain == finalCleanUrl
                    }

                val storedDomain =
                    if (existing != null) {
                        finalCleanUrl
                    } else {
                        displaySource.domain
                    }

                // При изменении адреса переносим сохранённые статьи.
                if (
                    existing != null &&
                    existing.domain != storedDomain
                ) {
                    dao.reassignArticles(
                        existing.domain,
                        storedDomain
                    )

                    sourceDao.deleteSource(existing.domain)
                }

                // Сохраняем подписку.
                sourceDao.upsertSource(
                    SourceEntity(
                        domain = storedDomain,
                        name = displaySource.name,
                        description = displaySource.description,
                        logoText = displaySource.logoText,
                        folderName = targetFolder ?: existing?.folderName,
                        addedAt = existing?.addedAt
                            ?: System.currentTimeMillis()
                    )
                )

                // Сохраняем первые 30 статей.
                saveArticles(
                    newArticles.map { article ->
                        article.copy(sourceId = storedDomain)
                    }
                )

                // Догружаем оставшиеся статьи в фоне.
                scope.launch(Dispatchers.Default) {
                    fetchSemaphore.withPermit {

                        val (_, restArticles) =
                            RssParser.parseRss(
                                finalXmlText,
                                finalCleanUrl,
                                maxItemsToParse = 470,
                                skipItems = 30
                            )

                        if (restArticles.isNotEmpty()) {
                            saveArticles(
                                restArticles.map { article ->
                                    article.copy(sourceId = storedDomain)
                                }
                            )
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

    // ---------------------------------------------------------
    // Предварительная загрузка профилей источников
    // ---------------------------------------------------------

    /**
     * Загружает 15 статей из источника каталога,
     * извлекает ключевые слова и сохраняет профиль.
     */
    suspend fun loadSourcePreviews(
        candidates: List<Source>,
        maxFetch: Int = 12
    ) = withContext(Dispatchers.IO) {

        val cached = sourceDao.getSourceProfiles()
            .associateBy { it.domain }

        val subscribed = sourceDao.getSources()
            .map { it.domain }
            .toSet()

        val now = System.currentTimeMillis()

        val queue = candidates
            .filter { it.domain !in subscribed }
            .filter { candidate ->

                val profile = cached[candidate.domain]

                val ttl =
                    if (
                        profile == null ||
                        profile.keywords.isEmpty()
                    ) {
                        PROFILE_RETRY_MILLIS
                    } else {
                        PROFILE_TTL_MILLIS
                    }

                profile == null ||
                        now - profile.updatedAt > ttl
            }
            .take(maxFetch)

        for (candidate in queue) {

            var keywords: List<String> = emptyList()

            try {
                fetchSemaphore.withPermit {

                    val response =
                        KtorClient.httpClient.get(
                            normalizeUrl(candidate.domain)
                        )

                    val xmlText =
                        decodeXmlBytes(response.readBytes())

                    val (_, items) =
                        RssParser.parseRss(
                            xmlText,
                            candidate.domain,
                            maxItemsToParse = 15
                        )

                    if (items.size >= 3) {
                        keywords = aggregateKeywords(items)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            sourceDao.upsertSourceProfile(
                SourceProfileEntity(
                    domain = candidate.domain,
                    keywords = keywords.joinToString(","),
                    updatedAt = now
                )
            )
        }
    }

    /** Ключевые слова, встречающиеся в разных статьях источника. */
    private fun aggregateKeywords(
        items: List<Article>,
        limit: Int = 40
    ): List<String> {

        val docFrequency = LinkedHashMap<String, Int>()

        for (article in items) {
            KeywordExtractor.extract(
                article.title,
                article.subtitle,
                article.content
            )
                .distinct()
                .forEach { keyword ->
                    docFrequency.merge(
                        keyword,
                        1,
                        Int::plus
                    )
                }
        }

        return docFrequency.entries
            .sortedByDescending { it.value }
            .take(limit)
            .map { it.key }
    }

    // ---------------------------------------------------------
    // Декодирование XML
    // ---------------------------------------------------------

    private fun decodeXmlBytes(bytes: ByteArray): String {

        val asciiHeader = String(
            bytes,
            0,
            minOf(bytes.size, 500),
            Charsets.ISO_8859_1
        )

        val match = Regex(
            """encoding=["']([^"']+)["']""",
            RegexOption.IGNORE_CASE
        ).find(asciiHeader)

        val encodingName =
            match?.groupValues?.get(1) ?: "UTF-8"

        return try {
            String(
                bytes,
                Charset.forName(encodingName)
            )
        } catch (_: Exception) {
            try {
                String(
                    bytes,
                    Charset.forName("windows-1251")
                )
            } catch (_: Exception) {
                String(
                    bytes,
                    Charsets.UTF_8
                )
            }
        }
    }

    // ---------------------------------------------------------
    // Закладки и статусы прочтения
    // ---------------------------------------------------------

    fun toggleBookmark(articleId: String) {
        scope.launch {
            dao.toggleBookmark(articleId)
            dao.deleteOrphanUnbookmarkedArticles()
        }
    }

    fun markAsRead(articleId: String) {
        scope.launch {
            dao.markAsRead(articleId)
        }
    }

    fun toggleRead(articleId: String) {
        scope.launch {
            dao.toggleRead(articleId)
        }
    }

    // ---------------------------------------------------------
    // Удаление источников
    // ---------------------------------------------------------

    /** Отписка. Статьи источника удаляются, кроме закладок. */
    fun deleteSource(sourceDomain: String) {
        scope.launch {
            dao.deleteNonBookmarkedBySource(sourceDomain)
            sourceDao.deleteSource(sourceDomain)
            dao.deleteOrphanUnbookmarkedArticles()
        }
    }

    fun updateSourceFolder(
        sourceDomain: String,
        newFolderName: String?
    ) {
        scope.launch {
            sourceDao.updateSourceFolder(
                sourceDomain,
                newFolderName
            )
        }
    }

    // ---------------------------------------------------------
    // Полная очистка
    // ---------------------------------------------------------

    /** Полная очистка статей, подписок и папок. */
    fun clearAllData() {
        scope.launch {
            dao.deleteAll()
            sourceDao.deleteAllSources()
            sourceDao.deleteAllFolders()
        }
    }

    /** Добавление 50 случайных популярных источников каталога в подписки. */
    fun addAllPopularSources() {
        scope.launch {
            val popular = PopularSources.topFeeds.shuffled().take(50)
            val now = System.currentTimeMillis()
            for (src in popular) {
                sourceDao.upsertSource(
                    SourceEntity(
                        domain = src.domain,
                        name = src.name,
                        description = src.description,
                        logoText = src.logoText,
                        folderName = null,
                        addedAt = now
                    )
                )
            }
            refreshAllSources(forceFull = true)
        }
    }
}
