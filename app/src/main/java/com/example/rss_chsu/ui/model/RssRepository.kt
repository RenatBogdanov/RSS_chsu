package com.example.rss_chsu.ui.model

import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.URI
import java.nio.charset.Charset

object RssRepository {

    private val _articles = MutableStateFlow<List<Article>>(emptyList())
    val articles: StateFlow<List<Article>> = _articles.asStateFlow()

    private val _sources = MutableStateFlow<List<Source>>(emptyList())
    val sources: StateFlow<List<Source>> = _sources.asStateFlow()

    private val _folders = MutableStateFlow<List<Folder>>(emptyList())
    val folders: StateFlow<List<Folder>> = _folders.asStateFlow()

    fun syncWithBackend() {
        // Поддерживаем совместимость с FeedViewModel
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

            val currentArticles = _articles.value.toMutableList()
            val updatedNewArticles = newArticles.mapIndexed { idx, art ->
                if (idx == 0 && currentArticles.isEmpty()) art.copy(isFeatured = true) else art
            }
            currentArticles.addAll(0, updatedNewArticles)
            _articles.value = currentArticles

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
        val updatedList = _articles.value.map { article ->
            if (article.id == articleId) {
                article.copy(isBookmarked = !article.isBookmarked)
            } else {
                article
            }
        }
        _articles.value = updatedList
    }

    fun markAsRead(articleId: String) {
        val updatedList = _articles.value.map { article ->
            if (article.id == articleId) {
                article.copy(isRead = true)
            } else {
                article
            }
        }
        _articles.value = updatedList
    }
}
