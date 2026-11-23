package com.example.rss_chsu.ui.screens

import com.example.rss_chsu.ui.model.PopularSources
import com.example.rss_chsu.ui.model.Source
import java.util.Locale

/** O(1) lookup for source labels used by visible feed rows. */
internal object SourceNameResolver {
    private fun key(value: String): String = value.trim().lowercase(Locale.ROOT)

    private val popularIndex: Map<String, String> by lazy(LazyThreadSafetyMode.PUBLICATION) {
        buildMap {
            PopularSources.topFeeds.forEach { source ->
                put(key(source.name), source.name)
                put(key(source.domain), source.name)
                put(key(source.id), source.name)
            }
        }
    }

    fun popularIndex(): Map<String, String> = popularIndex

    fun indexFor(sources: List<Source>): Map<String, String> =
        indexForEntries(sources.map { it.name to it.domain })

    /** Entries are name to domain. Subscribed source labels override catalogue labels. */
    fun indexForEntries(entries: List<Pair<String, String>>): Map<String, String> {
        if (entries.isEmpty()) return popularIndex

        return HashMap(popularIndex).apply {
            entries.forEach { (name, domain) ->
                put(key(name), name)
                put(key(domain), name)
            }
        }
    }

    fun clean(rawName: String, index: Map<String, String>): String {
        if (rawName.isBlank()) return "Источник"

        if (rawName.contains("Череповец", ignoreCase = true) || 
            rawName.contains("Череповца", ignoreCase = true) || 
            rawName.contains("cherinfo", ignoreCase = true)
        ) {
            return "Новости Череповца"
        }

        index[key(rawName)]?.let { return it }

        var clean = rawName.trim()
            .removePrefix("Официальный сайт города ")
            .removePrefix("Официальный сайт ")
            .removePrefix("Новости портала ")
            .removePrefix("Последние новости ")
            .removePrefix("Лента новостей ")
            .removePrefix("Новости ")
            .trim()

        val colonIndex = clean.indexOf(':')
        if (colonIndex > 0) clean = clean.substring(0, colonIndex).trim()
        if (clean.length > 22) clean = clean.take(22).trimEnd() + "..."
        return if (clean.isBlank()) rawName.take(22) else clean
    }
}
