package com.example.rss_chsu.ui.model

import android.text.Html
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.ConcurrentHashMap

/**
 * Превращает RSS / Atom в статьи приложения.[cite: 1]
 *
 * 1. Основной разбор: настоящий XML-парсер ([XmlFeedParser]).[cite: 1]
 * 2. Запасной разбор регулярными выражениями: только если лента не читается как XML.[cite: 1]
 * 3. Из сырых данных ([RawItem]) собираются [Article]: чистый текст с абзацами, анонс, картинка, рубрика, дата.[cite: 1]
 */
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
    // Очистка текста
    // ---------------------------------------------------------------------------------------------

    /**
     * Html.fromHtml схлопывает переводы строк, поэтому на время декодирования сущностей
     * переводы строк заменяются этим служебным символом.[cite: 1]
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
    private val tagStripRegex = Regex("<.*?>")
    private val spacesAroundNewline = Regex("[ \\t]*\\n[ \\t]*")
    private val manyNewlines = Regex("\\n{3,}")
    private val wordSplitRegex = Regex("\\s+")
    private val multiSpaceRegex = Regex(" {2,}")

    /** Символ-заменитель объекта (остаётся после <img>), мусорные нули и управляющие коды.[cite: 1] */
    private val garbageRegex = Regex("[\\uFFFC\\uFFFD\\uFEFF\\u200B-\\u200D\\u0000-\\u0008\\u000B\\u000C\\u000E-\\u001F]")

    /** Рекламные хвосты, которые издания дописывают в конец текста.[cite: 1] */
    private val promoSuffixes = listOf(
        Regex("\\s*Оставайтесь на связи с РБК[^\\n]*$")
    )

    private fun stripGarbage(s: String): String =
        garbageRegex.replace(s, "").replace(multiSpaceRegex, " ")

    private fun stripCdata(s: String): String = s.replace("<![CDATA[", "").replace("]]>", "")

    private fun unescapeXml(s: String): String =
        s.replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&amp;", "&")

    /** Убирает CDATA и раскрывает HTML, экранированный целиком (запасной разбор и Atom type="html").[cite: 1] */
    private fun normalizeMarkup(raw: String): String {
        val s = stripCdata(raw)
        return if (escapedHtmlRegex.containsMatchIn(s)) unescapeXml(s) else s
    }

    private fun decodeEntities(s: String): String {
        if (!s.contains('&')) return s
        return try {
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
    }

    /**
     * Превращает HTML или обычный текст с сущностями в чистый текст, сохраняя абзацы:
     * абзацы разделены пустой строкой, внутри абзаца переводы строк одинарные.[cite: 1]
     */
    private fun markupToText(raw: String): String {
        if (raw.isBlank()) return ""
        var s = normalizeMarkup(raw)

        s = if (anyTagRegex.containsMatchIn(s)) {
            s.replace(scriptStyleRegex, "")
                .replace(whitespaceWithNewline, " ")      // в HTML перевод строки это просто пробел[cite: 1]
                .replace(brRegex, NL)
                .replace(blockEndRegex, "$NL$NL")
                .replace(liEndRegex, NL)
                .replace(liStartRegex, "• ")
                .replace(anyTagStripRegex, "")
        } else {
            // обычный текст (например, rbc_news:full-text): переводы строк значимы[cite: 1]
            s.replace("\r\n", "\n").replace("\r", "\n").replace("\n", NL)
        }

        s = decodeEntities(s)
            .replace("\u00A0", " ")
            .replace(NL, "\n")
            .replace(spacesAroundNewline, "\n")
            .replace(manyNewlines, "\n\n")
            .trim()

        for (promo in promoSuffixes) s = s.replace(promo, "")
        return stripGarbage(s).trim()
    }

    private fun cleanHtmlEntities(input: String): String {
        if (input.isEmpty()) return ""
        return stripGarbage(decodeEntities(input)).trim()
    }

    private fun flatten(text: String): String = text.replace(wordSplitRegex, " ").trim()

    private fun clip(text: String, max: Int): String {
        if (text.length <= max) return text
        val cut = text.take(max)
        val atWord = cut.substringBeforeLast(' ', cut)
        return atWord.trimEnd(',', ';', ':', '-', '—', ' ') + "…"
    }

    private val imgRegex = Regex("""<img[^>]+src=["']([^"']+)["']""", RegexOption.IGNORE_CASE)

    /** Первая картинка внутри HTML описания или полного текста.[cite: 1] */
    private fun firstImageIn(markups: List<String>): String? {
        for (markup in markups) {
            imgRegex.find(normalizeMarkup(markup))?.groupValues?.get(1)?.let { return it }
        }
        return null
    }

    // ---------------------------------------------------------------------------------------------
    // Запасной разбор регулярными выражениями (если лента не читается как XML)
    // ---------------------------------------------------------------------------------------------

    private val tagRegexCache = ConcurrentHashMap<String, Regex>()

    /**
     * Содержимое первого непустого тега из списка имён (в порядке приоритета).
     * Имя указывается с префиксом, например "rbc_news:full-text", поэтому "title" не совпадёт с "rbc_news:title".[cite: 1]
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
        // Atom: <link href="..." rel="alternate"/>[cite: 1]
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

    private fun pickImage(block: String): String? {
        for (m in enclosureTagRegex.findAll(block)) {
            val t = m.value
            val url = urlAttrRegex.find(t)?.groupValues?.get(1) ?: continue
            val type = typeAttrRegex.find(t)?.groupValues?.get(1)
            val isImage = type?.startsWith("image", ignoreCase = true) ?: imageExtRegex.containsMatchIn(url)
            if (isImage) return url.replace("&amp;", "&")
        }
        return mediaRegex.find(block)?.groupValues?.get(1)?.replace("&amp;", "&")
    }

    private val relatedLinksRegex = Regex("<rbc_news:related_links>[\\s\\S]*?</rbc_news:related_links>", RegexOption.IGNORE_CASE)
    private val categoryTermRegex = Regex("""<category[^>]+term=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
    private val channelBlockRegex = Regex("""<(?:channel|feed)\b[^>]*>([\s\S]*?)</(?:channel|feed)\s*>""", RegexOption.IGNORE_CASE)
    private val imageBlockRegex = Regex("""<image\b[^>]*>[\s\S]*?</image\s*>""", RegexOption.IGNORE_CASE)
    private val singleTitleRegex = Regex("""<title\b[^>]*>([\s\S]*?)</title\s*>""", RegexOption.IGNORE_CASE)
    private val singleDescRegex = Regex("""<(?:description|subtitle)\b[^>]*>([\s\S]*?)</(?:description|subtitle)\s*>""", RegexOption.IGNORE_CASE)
    private val itemRegex = Regex("""<(?:item|entry)\b[^>]*>([\s\S]*?)</(?:item|entry)\s*>""", RegexOption.IGNORE_CASE)

    private fun extractWithRegex(xmlContent: String, skipCount: Int, itemLimit: Int): RawFeed {
        val headerArea = channelBlockRegex.find(xmlContent)?.groupValues?.get(1)
            ?.replace(itemRegex, "")
            ?.replace(imageBlockRegex, "")
            ?: xmlContent.replace(itemRegex, "")

        val feedTitle = singleTitleRegex.find(headerArea)?.groupValues?.get(1)
            ?.let { stripCdata(it).replace(tagStripRegex, "").trim() }
        val feedDescription = singleDescRegex.find(headerArea)?.groupValues?.get(1)
            ?.let { stripCdata(it).replace(tagStripRegex, "").trim() }

        val items = ArrayList<RawItem>()
        var itemIndex = 0

        for (match in itemRegex.findAll(xmlContent)) {
            if (itemIndex < skipCount) {
                itemIndex++
                continue
            }
            if (items.size >= itemLimit) break
            itemIndex++

            val block = match.groupValues[1].replace(relatedLinksRegex, "")

            val title = tag(block, "title") ?: continue
            val fullTexts = XmlFeedParser.fullTextTags.mapNotNull { tag(block, it) }
            val summary = tag(block, "description", "summary")

            items.add(
                RawItem(
                    title = title,
                    link = pickLink(block),
                    guid = tag(block, "guid", "id"),
                    summary = summary,
                    fullTexts = fullTexts,
                    pubDate = tag(block, "pubDate", "published", "dc:date", "updated"),
                    author = tag(block, "dc:creator", "author")?.replace(tagStripRegex, "")?.trim(),
                    category = tag(block, "category")?.replace(tagStripRegex, "")?.trim()
                        ?: categoryTermRegex.find(block)?.groupValues?.get(1),
                    imageUrl = pickImage(block)
                )
            )
        }
        return RawFeed(feedTitle, feedDescription, items)
    }

    // ---------------------------------------------------------------------------------------------
    // Разбор ленты
    // ---------------------------------------------------------------------------------------------

    /**
     * @param maxItemsToParse сколько статей разобрать[cite: 1]
     * @param skipItems сколько первых блоков item/entry пропустить (для догрузки остатка ленты)[cite: 1]
     */
    fun parseRss(
        xmlContent: String,
        sourceNameDefault: String = "RSS Feed",
        maxItemsToParse: Int = Int.MAX_VALUE,
        skipItems: Int = 0
    ): Pair<Source, List<Article>> {
        val skipCount = skipItems.coerceAtLeast(0)
        val itemLimit = maxItemsToParse.coerceAtLeast(0)

        val xmlRaw = XmlFeedParser.parse(xmlContent, skipCount, itemLimit)
        val raw = if (xmlRaw != null && (xmlRaw.items.isNotEmpty() || !xmlRaw.title.isNullOrBlank())) {
            xmlRaw
        } else {
            val regexRaw = extractWithRegex(xmlContent, skipCount, itemLimit)
            if (regexRaw.items.isNotEmpty() || !regexRaw.title.isNullOrBlank()) regexRaw else (xmlRaw ?: regexRaw)
        }

        val rawFeedTitle = raw.title.orEmpty().replace(tagStripRegex, "").trim()
        val feedTitle = cleanHtmlEntities(rawFeedTitle).ifBlank { sourceNameDefault }
        val feedDescription = cleanHtmlEntities(raw.description.orEmpty().replace(tagStripRegex, ""))

        val articles = ArrayList<Article>()
        for (item in raw.items) {
            val article = buildArticle(item, feedTitle, isFeatured = articles.isEmpty())
            if (article != null) articles.add(article)
        }

        val sourceKey = if (rawFeedTitle.isNotBlank()) {
            feedTitle
        } else {
            "${sourceNameDefault}_${xmlContent.take(256).hashCode()}"
        }

        val source = Source(
            id = sourceKey.hashCode().toString(),
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

    private fun buildArticle(item: RawItem, feedTitle: String, isFeatured: Boolean): Article? {
        val cleanTitle = cleanHtmlEntities(item.title.replace(tagStripRegex, ""))
        if (cleanTitle.isEmpty()) return null

        val rawFull = item.fullTexts.firstOrNull()
        val fullText = rawFull?.let { markupToText(it) }.orEmpty()
        val summaryText = item.summary?.let { markupToText(it) }.orEmpty()

        val cleanContent = if (fullText.length >= summaryText.length) fullText else summaryText

        val subtitleSource = flatten(summaryText).ifEmpty { flatten(cleanContent) }
        val wordCount = (if (cleanContent.isNotEmpty()) cleanContent else subtitleSource)
            .split(wordSplitRegex).count { it.isNotEmpty() }
        val readMins = (wordCount / 160).coerceAtLeast(1)

        val category = item.category
            ?.let { cleanHtmlEntities(it.replace(tagStripRegex, "")) }
            ?.takeIf { it.isNotBlank() }
            ?: "Лента"

        val imageUrl = item.imageUrl ?: firstImageIn(listOfNotNull(rawFull, item.summary))

        val id = item.link?.takeIf { it.isNotBlank() }
            ?: item.guid?.takeIf { it.isNotBlank() }
            ?: "${feedTitle.hashCode()}_${cleanTitle.hashCode()}_${item.pubDate.orEmpty().hashCode()}"

        return Article(
            id = id,
            title = cleanTitle,
            subtitle = clip(subtitleSource, 300),
            content = cleanContent,
            sourceName = feedTitle,
            category = category,
            author = cleanHtmlEntities(item.author.orEmpty().replace(tagStripRegex, "")),
            date = parseRssDateToRelative(item.pubDate),
            pubDateFormatted = parseRssDateToRussian(item.pubDate),
            publishedAt = parseDateToMillis(item.pubDate),
            readTime = "$readMins мин чтения",
            imageUrl = imageUrl.orEmpty(),
            isFeatured = isFeatured,
            isBookmarked = false,
            isRead = false
        )
    }

    // ---------------------------------------------------------------------------------------------
    // Даты
    // ---------------------------------------------------------------------------------------------

    /** Форматтеры создаются один раз на поток: SimpleDateFormat не потокобезопасен и дорог в создании.[cite: 1] */
    private val dateFormatters = ThreadLocal.withInitial {
        listOf(
            "EEE, dd MMM yyyy HH:mm:ss Z",
            "EEE, dd MMM yyyy HH:mm:ss z",
            "EEE, dd MMM yyyy HH:mm Z",
            "EEE, dd MMM yy HH:mm:ss Z",
            "yyyy-MM-dd'T'HH:mm:ss.SSSXXX",
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "yyyy-MM-dd'T'HH:mm:ssXXX",
            "yyyy-MM-dd'T'HH:mm:ss'Z'",
            "yyyy-MM-dd'T'HH:mm:ssZ",
            // форматы без часового пояса идут последними: они разбирают и строки с поясом, отбрасывая его[cite: 1]
            "yyyy-MM-dd'T'HH:mm:ss",
            "yyyy-MM-dd HH:mm:ss",
            "dd MMM yyyy HH:mm:ss Z",
            "dd MMM yyyy HH:mm:ss z"
        ).map { pattern ->
            SimpleDateFormat(pattern, Locale.ENGLISH).apply { timeZone = TimeZone.getTimeZone("UTC") }
        }
    }

    private val dayFormat = ThreadLocal.withInitial { SimpleDateFormat("EEE", Locale.ENGLISH) }
    private val dayNumFormat = ThreadLocal.withInitial { SimpleDateFormat("dd", Locale.ENGLISH) }
    private val monthFormat = ThreadLocal.withInitial { SimpleDateFormat("MMM", Locale.ENGLISH) }

    fun parseDateToMillis(dateStr: String?): Long {
        if (dateStr.isNullOrEmpty()) return 0L

        for (format in dateFormatters.get()!!) {
            try {
                val date = format.parse(dateStr.trim())
                if (date != null) return date.time
            } catch (_: Exception) {
            }
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
            months < 12 -> "${months.coerceAtLeast(1)}мес"
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

        val millis = parseDateToMillis(dateStr)
        if (millis == 0L) return dateStr

        val date = java.util.Date(millis)
        val engDay = dayFormat.get()!!.format(date)
        val dayNum = dayNumFormat.get()!!.format(date)
        val engMonth = monthFormat.get()!!.format(date)

        val rusDay = russianDays[engDay] ?: engDay
        val rusMonth = russianMonths[engMonth] ?: engMonth

        return "$rusDay, $dayNum $rusMonth"
    }
}