package com.example.rss_chsu.ui.model

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader
import java.util.Locale

internal data class RawItem(
    val title: String,
    val link: String?,
    val guid: String?,
    val summary: String?,
    val fullTexts: List<String>,
    val pubDate: String?,
    val author: String?,
    val category: String?,
    val imageUrl: String?
)

internal data class RawFeed(
    val title: String?,
    val description: String?,
    val items: List<RawItem>
)

internal object XmlFeedParser {

    internal val fullTextTags = listOf(
        "rbc_news:full-text",
        "yandex:full-text",
        "turbo:content",
        "content:encoded",
        "content"
    )

    private val dateTags = listOf("pubdate", "published", "dc:date", "updated")

    private val paragraphTags = setOf(
        "p", "div", "h1", "h2", "h3", "h4", "h5", "h6", "blockquote", "ul", "ol", "table", "tr", "figure", "pre"
    )
    private val lineTags = setOf("br", "li")

    private val imageExtRegex = Regex("""\.(?:jpe?g|png|gif|webp|avif)(?:\?.*)?$""", RegexOption.IGNORE_CASE)

    private val xmlEntities = setOf("amp", "lt", "gt", "quot", "apos")

    private val htmlEntities: Map<String, Int> = mapOf(
        "nbsp" to 160, "iexcl" to 161, "cent" to 162, "pound" to 163, "yen" to 165, "sect" to 167,
        "copy" to 169, "laquo" to 171, "reg" to 174, "shy" to 173, "deg" to 176, "plusmn" to 177,
        "para" to 182, "middot" to 183, "raquo" to 187, "frac14" to 188, "frac12" to 189, "frac34" to 190,
        "iquest" to 191, "times" to 215, "divide" to 247,
        "ensp" to 8194, "emsp" to 8195, "thinsp" to 8201,
        "ndash" to 8211, "mdash" to 8212, "lsquo" to 8216, "rsquo" to 8217, "sbquo" to 8218,
        "ldquo" to 8220, "rdquo" to 8221, "bdquo" to 8222, "bull" to 8226, "hellip" to 8230,
        "euro" to 8364, "trade" to 8482, "larr" to 8592, "uarr" to 8593, "rarr" to 8594, "darr" to 8595,
        "minus" to 8722
    )

    private val cdataRegex = Regex("""<!\[CDATA\[[\s\S]*?\]\]>""")
    private val namedEntityRegex = Regex("""&([A-Za-z][A-Za-z0-9]{1,15});""")
    private val bareAmpRegex = Regex("""&(?!(?:[A-Za-z][A-Za-z0-9]*|#[0-9]+|#[xX][0-9A-Fa-f]+);)""")
    private val invalidCharsRegex = Regex("[\\u0000-\\u0008\\u000B\\u000C\\u000E-\\u001F\\uFFFE\\uFFFF]")

    private fun fixEntities(text: String): String {
        val named = namedEntityRegex.replace(text) { match ->
            val name = match.groupValues[1]
            when {
                name in xmlEntities -> match.value
                else -> htmlEntities[name]?.let { code -> "&#$code;" } ?: "&amp;$name;"
            }
        }
        return bareAmpRegex.replace(named, "&amp;")
    }

    private fun sanitize(xml: String): String {
        val cleaned = invalidCharsRegex.replace(xml.removePrefix("\uFEFF"), "").trimStart()
        val out = StringBuilder(cleaned.length + 128)
        var last = 0
        for (match in cdataRegex.findAll(cleaned)) {
            out.append(fixEntities(cleaned.substring(last, match.range.first)))
            out.append(match.value)
            last = match.range.last + 1
        }
        out.append(fixEntities(cleaned.substring(last)))
        return out.toString()
    }

    fun parse(xml: String, skipItems: Int, maxItems: Int): RawFeed? {
        if (xml.isBlank()) return null
        return try {
            val parser = Xml.newPullParser()
            try {
                parser.setFeature("http://xmlpull.org/v1/doc/features.html#relaxed", true)
            } catch (_: Exception) {
            }
            parser.setInput(StringReader(sanitize(xml)))
            readFeed(parser, skipItems, maxItems)
        } catch (_: Exception) {
            null
        }
    }

    private fun readFeed(parser: XmlPullParser, skipItems: Int, maxItems: Int): RawFeed? {
        var feedTitle: String? = null
        var feedDescription: String? = null
        val items = ArrayList<RawItem>()
        var itemBlocks = 0
        var insideChannelOrFeed = false
        var hasFeedRoot = false

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG -> {
                    val tagName = parser.name.lowercase(Locale.ROOT)
                    if (tagName == "rss" || tagName == "feed" || tagName == "rdf:rdf") {
                        hasFeedRoot = true
                        if (tagName == "feed") insideChannelOrFeed = true
                    }
                    when (tagName) {
                        "channel" -> {
                            hasFeedRoot = true
                            insideChannelOrFeed = true
                        }
                        "image" -> {
                            // Внутри <image> бывает свой <title> логотипа — пропускаем, чтобы не затереть заголовок ленты
                            skip(parser)
                        }
                        "item", "entry" -> {
                            itemBlocks++
                            if (itemBlocks <= skipItems || items.size >= maxItems) {
                                skip(parser)
                            } else {
                                val item = readItem(parser)
                                if (item != null) items.add(item)
                            }
                        }
                        "title" -> {
                            val text = readText(parser)
                            if (insideChannelOrFeed && feedTitle == null && text.isNotBlank()) {
                                feedTitle = text
                            } else if (feedTitle == null && text.isNotBlank()) {
                                feedTitle = text
                            }
                        }
                        "description", "subtitle" -> {
                            val text = readText(parser)
                            if (insideChannelOrFeed && feedDescription == null && text.isNotBlank()) {
                                feedDescription = text
                            } else if (feedDescription == null && text.isNotBlank()) {
                                feedDescription = text
                            }
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name.lowercase(Locale.ROOT) == "channel") {
                        insideChannelOrFeed = false
                    }
                }
            }
            event = parser.next()
        }

        if (!hasFeedRoot && items.isEmpty() && feedTitle.isNullOrBlank()) return null
        return RawFeed(feedTitle, feedDescription, items)
    }

    private fun readItem(parser: XmlPullParser): RawItem? {
        var title: String? = null
        var linkText: String? = null
        var linkAlternate: String? = null
        var linkAny: String? = null
        var guid: String? = null
        var summary: String? = null
        var author: String? = null
        var category: String? = null
        var imageUrl: String? = null
        val fullTexts = HashMap<String, String>()
        val dates = HashMap<String, String>()

        var depth = 1
        while (depth > 0) {
            when (parser.next()) {
                XmlPullParser.START_TAG -> {
                    val name = parser.name.lowercase(Locale.ROOT)
                    when (name) {
                        "media:group" -> depth++

                        "title" -> {
                            val text = readText(parser)
                            if (title == null && text.isNotBlank()) title = text
                        }

                        "link" -> {
                            val href = parser.getAttributeValue(null, "href")
                            val rel = parser.getAttributeValue(null, "rel")
                            val text = readText(parser)
                            if (!href.isNullOrBlank()) {
                                if (linkAny == null) linkAny = href.trim()
                                if (linkAlternate == null && (rel == null || rel.equals("alternate", ignoreCase = true))) {
                                    linkAlternate = href.trim()
                                }
                            } else if (linkText == null && text.isNotBlank()) {
                                linkText = text
                            }
                        }

                        "guid", "id" -> {
                            val text = readText(parser)
                            if (guid == null && text.isNotBlank()) guid = text
                        }

                        "description", "summary" -> {
                            val text = readText(parser)
                            if (summary == null && text.isNotBlank()) summary = text
                        }

                        "rbc_news:full-text", "yandex:full-text", "turbo:content", "content:encoded", "content" -> {
                            val text = readText(parser)
                            if (text.isNotBlank() && !fullTexts.containsKey(name)) fullTexts[name] = text
                        }

                        "pubdate", "published", "dc:date", "updated" -> {
                            val text = readText(parser)
                            if (text.isNotBlank() && !dates.containsKey(name)) dates[name] = text
                        }

                        "dc:creator", "author" -> {
                            val text = readAuthor(parser)
                            if (author == null && text.isNotBlank()) author = text
                        }

                        "category" -> {
                            val term = parser.getAttributeValue(null, "term")
                            val text = readText(parser)
                            if (category == null) {
                                category = text.ifBlank { term?.trim().orEmpty() }.takeIf { it.isNotBlank() }
                            }
                        }

                        "enclosure" -> {
                            val url = parser.getAttributeValue(null, "url")
                            val type = parser.getAttributeValue(null, "type")
                            skip(parser)
                            if (imageUrl == null && !url.isNullOrBlank() && isImage(type, url)) imageUrl = url.trim()
                        }

                        "media:content", "media:thumbnail" -> {
                            val url = parser.getAttributeValue(null, "url")
                            val type = parser.getAttributeValue(null, "type")
                            val medium = parser.getAttributeValue(null, "medium")
                            skip(parser)
                            val looksLikeImage = name == "media:thumbnail" ||
                                    medium.equals("image", ignoreCase = true) ||
                                    (type != null && type.startsWith("image", ignoreCase = true)) ||
                                    (type == null && medium == null && url != null && imageExtRegex.containsMatchIn(url))
                            if (imageUrl == null && !url.isNullOrBlank() && looksLikeImage) imageUrl = url.trim()
                        }

                        "itunes:image" -> {
                            val href = parser.getAttributeValue(null, "href")
                            skip(parser)
                            if (imageUrl == null && !href.isNullOrBlank()) imageUrl = href.trim()
                        }

                        else -> skip(parser)
                    }
                }
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.END_DOCUMENT -> depth = 0
            }
        }

        val finalTitle = title ?: return null
        return RawItem(
            title = finalTitle,
            link = linkText ?: linkAlternate ?: linkAny,
            guid = guid,
            summary = summary,
            fullTexts = fullTextTags.mapNotNull { fullTexts[it] },
            pubDate = dateTags.firstNotNullOfOrNull { dates[it] },
            author = author,
            category = category,
            imageUrl = imageUrl
        )
    }

    private fun isImage(type: String?, url: String): Boolean =
        if (type != null) type.startsWith("image", ignoreCase = true) else imageExtRegex.containsMatchIn(url)

    private fun readText(parser: XmlPullParser): String {
        val sb = StringBuilder()
        var depth = 1
        while (depth > 0) {
            when (parser.next()) {
                XmlPullParser.TEXT -> sb.append(parser.text)
                XmlPullParser.START_TAG -> {
                    depth++
                    val tag = parser.name.lowercase(Locale.ROOT)
                    if (tag in paragraphTags) sb.append("\n\n") else if (tag in lineTags) sb.append('\n')
                }
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.END_DOCUMENT -> depth = 0
            }
        }
        return sb.toString().trim()
    }

    /** Парсер стоит на START_TAG автора; после возврата стоит на соответствующем END_TAG автора. */
    private fun readAuthor(parser: XmlPullParser): String {
        val direct = StringBuilder()
        var nested: String? = null
        var depth = 1
        while (depth > 0) {
            when (parser.next()) {
                XmlPullParser.TEXT -> if (depth == 1) direct.append(parser.text)
                XmlPullParser.START_TAG -> {
                    if (depth == 1 && nested == null && parser.name.equals("name", ignoreCase = true)) {
                        nested = readText(parser)
                        // readText уже потребил END_TAG <name>
                    } else {
                        depth++
                    }
                }
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.END_DOCUMENT -> depth = 0
            }
        }
        return (nested ?: direct.toString()).trim()
    }

    private fun skip(parser: XmlPullParser) {
        var depth = 1
        while (depth > 0) {
            when (parser.next()) {
                XmlPullParser.START_TAG -> depth++
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.END_DOCUMENT -> return
            }
        }
    }
}