package com.example.rss_chsu.ui.model

import android.text.Html
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.ConcurrentHashMap

object RssParser {

    private val russianDays = mapOf(
        "Mon" to "Понедельник", "Tue" to "Вторник", "Wed" to "Среда",
        "Thu" to "Четверг", "Fri" to "Пятница", "Sat" to "Суббота", "Sun" to "Воскресенье",
        "Monday" to "Понедельник", "Tuesday" to "Вторник", "Wednesday" to "Среда",
        "Thursday" to "Четверг", "Friday" to "Пятница", "Saturday" to "Суббота", "Sunday" to "Воскресенье"
    )

    private val russianMonths = mapOf(
        "Jan" to "января", "Feb" to "февраля", "Mar" to "марта", "Apr" to "апреля",
        "May" to "мая", "Jun" to "июня", "Jul" to "июля", "Aug" to "августа",
        "Sep" to "сентября", "Oct" to "октября", "Nov" to "ноября", "Dec" to "декабря",
        "January" to "января", "February" to "февраля", "March" to "марта", "April" to "апреля",
        "June" to "июня", "July" to "июля", "August" to "августа",
        "September" to "сентября", "October" to "октября", "November" to "ноября", "December" to "декабря"
    )

    // ---------------------------------------------------------------------------------------------
    // Разбор текста
    // ---------------------------------------------------------------------------------------------

    /**
     * Html.fromHtml схлопывает переводы строк, поэтому на время декодирования сущностей
     * переводы строк заменяются этим служебным символом.
     */
    private const val NL = "\uE000"

    private val escapedHtmlRegex = Regex(
        "&lt;/?(?:p|div|br|a|img|span|h[1-6]|ul|ol|li|blockquote|strong|b|em|i|figure|pre|code|table)\\b",
        RegexOption.IGNORE_CASE
    )
    private val anyTagRegex = Regex("<[a-zA-Z/!][^>]*>")
    private val scriptStyleRegex = Regex("(?is)<(?:script|style)[\\s\\S]*?</(?:script|style)>")
    private val whitespaceWithNewline = Regex("\\s*\\n\\s*")
    private val brRegex = Regex("(?i)<br\\s*/?>")
    private val blockEndRegex = Regex("(?i)</(?:p|div|h[1-6]|blockquote|tr|ul|ol|figure|pre|table)>")
    private val liEndRegex = Regex("(?i)</li>")
    private val liStartRegex = Regex("(?i)<li[^>]*>")
    private val anyTagStripRegex = Regex("<[^>]+>")
    private val spacesAroundNewline = Regex("[ \\t]*\\n[ \\t]*")
    private val manyNewlines = Regex("\\n{3,}")

    /** Рекламные хвосты, которые издания дописывают в конец текста. */
    private val promoSuffixes = listOf(
        Regex("\\s*Оставайтесь на связи с РБК[^\\n]*$")
    )

    private fun stripCdata(s: String): String = s.replace("<![CDATA[", "").replace("]]>", "")

    private fun unescapeXml(s: String): String =
        s.replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&amp;", "&")

    /** Убирает CDATA и раскрывает HTML, экранированный целиком (Atom type="html"). */
    private fun normalizeMarkup(raw: String): String {
        val s = stripCdata(raw)
        return if (escapedHtmlRegex.containsMatchIn(s)) unescapeXml(s) else s
    }

    private fun decodeEntities(s: String): String =
        try {
            Html.fromHtml(s, Html.FROM_HTML_MODE_LEGACY).toString()
        } catch (_: Exception) {
            s.replace("&laquo;", "«")
                .replace("&raquo;", "»")
                .replace("&quot;", "\"")
                .replace("&nbsp;", " ")
                .replace("&mdash;", "—")
                .replace("&ndash;", "–")
                .replace("&hellip;", "…")
                .replace("&amp;", "&")
        }

    /**
     * Превращает HTML или обычный текст с сущностями в чистый текст, сохраняя абзацы:
     * абзацы разделены пустой строкой, внутри абзаца переводы строк одинарные.
     */
    private fun markupToText(raw: String): String {
        if (raw.isBlank()) return ""
        var s = normalizeMarkup(raw)

        s = if (anyTagRegex.containsMatchIn(s)) {
            s.replace(scriptStyleRegex, "")
                .replace(whitespaceWithNewline, " ")      // в HTML перевод строки — это просто пробел
                .replace(brRegex, NL)
                .replace(blockEndRegex, "$NL$NL")
                .replace(liEndRegex, NL)
                .replace(liStartRegex, "• ")
                .replace(anyTagStripRegex, "")
        } else {
            // обычный текст (например, rbc_news:full-text): переводы строк значимы
            s.replace("\r\n", "\n").replace("\r", "\n").replace("\n", NL)
        }

        s = decodeEntities(s)
            .replace("\u00A0", " ")
            .replace(NL, "\n")
            .replace(spacesAroundNewline, "\n")
            .replace(manyNewlines, "\n\n")
            .trim()

        for (promo in promoSuffixes) s = s.replace(promo, "")
        return s.trim()
    }

    private fun flatten(text: String): String = text.replace(Regex("\\s+"), " ").trim()

    private fun clip(text: String, max: Int): String {
        if (text.length <= max) return text
        val cut = text.take(max)
        val atWord = cut.substringBeforeLast(' ', cut)
        return atWord.trimEnd(',', ';', ':', '-', '—', ' ') + "…"
    }

    private fun cleanHtmlEntities(input: String): String {
        if (input.isEmpty()) return ""
        return decodeEntities(input).trim()
    }

    // ---------------------------------------------------------------------------------------------
    // Поиск тегов
    // ---------------------------------------------------------------------------------------------

    private val tagRegexCache = ConcurrentHashMap<String, Regex>()

    /**
     * Содержимое первого непустого тега из списка имён (в порядке приоритета).
     * Имя указывается с префиксом, например "rbc_news:full-text", поэтому "title" не совпадёт с "rbc_news:title".
     */
    private fun tag(block: String, vararg names: String): String? {
        for (name in names) {
            val regex = tagRegexCache.getOrPut(name) {
                val n = Regex.escape(name)
                Regex("<$n(?:\\s[^>]*)?>([\\s\\S]*?)</$n\\s*>", RegexOption.IGNORE_CASE)
            }
            val value = regex.find(block)?.groupValues?.get(1)?.let { stripCdata(it).trim() }
            if (!value.isNullOrEmpty()) return value
        }
        return null
    }

    private val linkTagRegex = Regex("<link\\b[^>]*>", RegexOption.IGNORE_CASE)
    private val hrefRegex = Regex("""href=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
    private val relRegex = Regex("""rel=["']([^"']+)["']""", RegexOption.IGNORE_CASE)

    private fun pickLink(block: String): String? {
        tag(block, "link")?.let { return it }
        // Atom: <link href="..." rel="alternate"/>
        val links = linkTagRegex.findAll(block).map { it.value }.toList()
        val preferred = links.firstOrNull {
            val rel = relRegex.find(it)?.groupValues?.get(1)
            rel == null || rel.equals("alternate", ignoreCase = true)
        } ?: links.firstOrNull()
        return preferred?.let { hrefRegex.find(it)?.groupValues?.get(1)?.trim() }
    }

    private val enclosureTagRegex = Regex("<enclosure\\b[^>]*>", RegexOption.IGNORE_CASE)
    private val urlAttrRegex = Regex("""url=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
    private val typeAttrRegex = Regex("""type=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
    private val imageExtRegex = Regex("""\.(?:jpe?g|png|gif|webp|avif)(?:\?.*)?$""", RegexOption.IGNORE_CASE)
    private val mediaRegex = Regex("""<media:(?:content|thumbnail)[^>]+url=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
    private val imgRegex = Regex("""<img[^>]+src=["']([^"']+)["']""", RegexOption.IGNORE_CASE)

    /** Картинка статьи: сначала enclosure типа image (видео пропускаем), затем media:*, затем <img> в тексте. */
    private fun pickImage(block: String, markups: List<String>): String? {
        for (m in enclosureTagRegex.findAll(block)) {
            val t = m.value
            val url = urlAttrRegex.find(t)?.groupValues?.get(1) ?: continue
            val type = typeAttrRegex.find(t)?.groupValues?.get(1)
            val isImage = type?.startsWith("image", ignoreCase = true) ?: imageExtRegex.containsMatchIn(url)
            if (isImage) return url.replace("&amp;", "&")
        }
        mediaRegex.find(block)?.groupValues?.get(1)?.let { return it.replace("&amp;", "&") }
        for (markup in markups) {
            imgRegex.find(normalizeMarkup(markup))?.groupValues?.get(1)?.let { return it }
        }
        return null
    }

    private val relatedLinksRegex = Regex("<rbc_news:related_links>[\\s\\S]*?</rbc_news:related_links>", RegexOption.IGNORE_CASE)
    private val categoryTermRegex = Regex("""<category[^>]+term=["']([^"']+)["']""", RegexOption.IGNORE_CASE)

    // ---------------------------------------------------------------------------------------------
    // Разбор ленты
    // ---------------------------------------------------------------------------------------------

    fun parseRss(
        xmlContent: String,
        sourceNameDefault: String = "RSS Feed",
        maxItemsToParse: Int = Int.MAX_VALUE,
        skipItems: Int = 0
    ): Pair<Source, List<Article>> {
        val articles = mutableListOf<Article>()

        // 1. Extract feed title
        val titleMatch = Regex("""<(?:channel|feed)[^>]*>[\s\S]*?<title[^>]*>([\s\S]*?)</title>""", RegexOption.IGNORE_CASE).find(xmlContent)
        val rawFeedTitle = titleMatch?.groupValues?.get(1)
            ?.replace("<![CDATA[", "")?.replace("]]>", "")
            ?.replace(Regex("<.*?>"), "")?.trim() ?: sourceNameDefault
        val feedTitle = cleanHtmlEntities(rawFeedTitle)

        // 2. Extract feed description
        val descMatch = Regex("""<(?:channel|feed)[^>]*>[\s\S]*?<(?:description|subtitle)[^>]*>([\s\S]*?)</(?:description|subtitle)>""", RegexOption.IGNORE_CASE).find(xmlContent)
        val feedDescription = cleanHtmlEntities(descMatch?.groupValues?.get(1)?.replace("<![CDATA[", "")?.replace("]]>", "")?.replace(Regex("<.*?>"), "")?.trim() ?: "")

        // 3. Extract items / entries
        val itemRegex = Regex("""<(?:item|entry)[\s\S]*?>([\s\S]*?)</(?:item|entry)>""", RegexOption.IGNORE_CASE)

        var itemIndex = 0
        val skipCount = skipItems.coerceAtLeast(0)
        val itemLimit = maxItemsToParse.coerceAtLeast(0)

        for (match in itemRegex.findAll(xmlContent)) {
            // skipItems считает исходные блоки item/entry, а maxItemsToParse — успешно разобранные статьи.
            if (itemIndex < skipCount) {
                itemIndex++
                continue
            }
            if (articles.size >= itemLimit) break
            itemIndex++

            // «Связанные материалы» РБК содержат вложенные title/link/description — к самой статье они не относятся
            val block = match.groupValues[1].replace(relatedLinksRegex, "")

            val itemTitle = tag(block, "title") ?: continue
            val itemLink = pickLink(block)

            // Полный текст бывает в разных тегах: rbc_news:full-text, yandex:full-text, turbo:content,
            // content:encoded, Atom <content>. Короткий анонс — в description или Atom <summary>.
            val rawFull = tag(block, "rbc_news:full-text", "yandex:full-text", "turbo:content", "content:encoded", "content")
            val rawSummary = tag(block, "description", "summary")

            val fullText = rawFull?.let { markupToText(it) }.orEmpty()
            val summaryText = rawSummary?.let { markupToText(it) }.orEmpty()
            // Берём самый полный из вариантов. Если текста нет вообще, остаётся пустая строка
            // (раньше сюда подставлялся заголовок, и читалка показывала его дважды).
            val cleanContent = if (fullText.length >= summaryText.length) fullText else summaryText

            val itemPubDate = tag(block, "pubDate", "published", "dc:date", "updated")
            val itemAuthor = tag(block, "dc:creator", "author")?.replace(Regex("<.*?>"), "")?.trim()

            val category = tag(block, "category")?.let { cleanHtmlEntities(it.replace(Regex("<.*?>"), "")) }?.takeIf { it.isNotBlank() }
                ?: categoryTermRegex.find(block)?.groupValues?.get(1)?.let { cleanHtmlEntities(it) }
                ?: "Лента"

            val imgUrl = pickImage(block, listOfNotNull(rawFull, rawSummary))

            val cleanTitle = cleanHtmlEntities(itemTitle.replace(Regex("<.*?>"), ""))
            if (cleanTitle.isEmpty()) continue

            val subtitleSource = flatten(summaryText).ifEmpty { flatten(cleanContent) }
            val wordCount = (if (cleanContent.isNotEmpty()) cleanContent else subtitleSource)
                .split(Regex("\\s+")).count { it.isNotEmpty() }
            val readMins = (wordCount / 160).coerceAtLeast(1)

            val relativeTime = parseRssDateToRelative(itemPubDate)
            val russianDate = parseRssDateToRussian(itemPubDate)

            articles.add(
                Article(
                    id = itemLink ?: tag(block, "guid", "id") ?: (System.currentTimeMillis().toString() + "_" + articles.size),
                    title = cleanTitle,
                    subtitle = clip(subtitleSource, 300),
                    content = cleanContent,
                    sourceName = feedTitle,
                    category = category,
                    author = cleanHtmlEntities(itemAuthor ?: ""),
                    date = relativeTime,
                    pubDateFormatted = russianDate,
                    publishedAt = parseDateToMillis(itemPubDate),
                    readTime = "$readMins мин чтения",
                    imageUrl = imgUrl ?: "",
                    isFeatured = articles.isEmpty(),
                    isBookmarked = false,
                    isRead = false
                )
            )
        }

        // Не создаём служебную fallback-статью при фоновой догрузке второй части ленты.
        if (articles.isEmpty() && xmlContent.isNotEmpty() && skipCount == 0 && itemLimit > 0) {
            articles.add(
                Article(
                    id = "fallback_${System.currentTimeMillis()}",
                    title = "Лента: $feedTitle",
                    subtitle = "Материалы получены из RSS потока.",
                    content = xmlContent.take(1000),
                    sourceName = feedTitle,
                    category = "RSS",
                    date = "только что",
                    pubDateFormatted = "Сегодня",
                    readTime = "1 мин чтения",
                    isFeatured = true
                )
            )
        }

        val source = Source(
            id = feedTitle.hashCode().toString(),
            name = feedTitle,
            domain = feedTitle,
            description = feedDescription,
            articlesPerWeek = "${articles.size} статей",
            logoText = feedTitle.take(2).uppercase(),
            isSubscribed = true,
            unreadCount = articles.size
        )

        return Pair(source, articles)
    }

    // ---------------------------------------------------------------------------------------------
    // Даты
    // ---------------------------------------------------------------------------------------------

    fun parseDateToMillis(dateStr: String?): Long {
        if (dateStr.isNullOrEmpty()) return 0L

        val formats = listOf(
            SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH),
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.ENGLISH),
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH),
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH),
            SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH)
        )

        for (format in formats) {
            try {
                format.timeZone = TimeZone.getTimeZone("UTC")
                val date = format.parse(dateStr.trim())
                if (date != null) return date.time
            } catch (_: Exception) {}
        }
        return 0L
    }

    fun relativeFromMillis(millis: Long): String {
        if (millis == 0L) return "недавно"

        val diffMillis = System.currentTimeMillis() - millis
        val minutes = diffMillis / (1000 * 60)
        val hours = minutes / 60
        val days = hours / 24
        val months = days / 30
        val years = days / 365

        return when {
            minutes < 1 -> "только что"
            minutes < 60 -> "${minutes}м"
            hours < 24 -> "${hours}ч"
            days < 7 -> "${days}д"
            months < 12 -> "${months}мес"
            else -> "${years}г"
        }
    }

    private fun parseRssDateToRelative(dateStr: String?): String {
        if (dateStr.isNullOrEmpty()) return "недавно"
        val millis = parseDateToMillis(dateStr)
        return if (millis == 0L) dateStr.take(16) else relativeFromMillis(millis)
    }

    private fun parseRssDateToRussian(dateStr: String?): String {
        if (dateStr.isNullOrEmpty()) return "Сегодня"

        val formats = listOf(
            SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH),
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.ENGLISH),
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH),
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH),
            SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH)
        )

        var dateObj: java.util.Date? = null
        for (format in formats) {
            try {
                format.timeZone = TimeZone.getTimeZone("UTC")
                dateObj = format.parse(dateStr.trim())
                if (dateObj != null) break
            } catch (_: Exception) {}
        }

        if (dateObj == null) return dateStr

        val dayFormat = SimpleDateFormat("EEE", Locale.ENGLISH)
        val dayNumFormat = SimpleDateFormat("dd", Locale.ENGLISH)
        val monthFormat = SimpleDateFormat("MMM", Locale.ENGLISH)

        val engDay = dayFormat.format(dateObj)
        val dayNum = dayNumFormat.format(dateObj)
        val engMonth = monthFormat.format(dateObj)

        val rusDay = russianDays[engDay] ?: engDay
        val rusMonth = russianMonths[engMonth] ?: engMonth

        return "$rusDay, $dayNum $rusMonth"
    }
}
