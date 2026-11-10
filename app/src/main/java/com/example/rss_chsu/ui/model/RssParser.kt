package com.example.rss_chsu.ui.model

import android.text.Html
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

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

    // Кэшированные форматтеры дат на поток (избавляет от трат CPU на создание SimpleDateFormat в цикле)
    private val dateFormatters = ThreadLocal.withInitial {
        listOf(
            SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH),
            SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH),
            SimpleDateFormat("EEE, dd MMM yyyy HH:mm Z", Locale.ENGLISH),
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.ENGLISH),
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.ENGLISH),
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.ENGLISH),
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH),
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ENGLISH),
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH),
            SimpleDateFormat("dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH),
            SimpleDateFormat("dd MMM yyyy HH:mm:ss z", Locale.ENGLISH)
        ).onEach { it.timeZone = TimeZone.getTimeZone("UTC") }
    }

    private val dayFormat = ThreadLocal.withInitial { SimpleDateFormat("EEE", Locale.ENGLISH).apply { timeZone = TimeZone.getTimeZone("UTC") } }
    private val dayNumFormat = ThreadLocal.withInitial { SimpleDateFormat("dd", Locale.ENGLISH).apply { timeZone = TimeZone.getTimeZone("UTC") } }
    private val monthFormat = ThreadLocal.withInitial { SimpleDateFormat("MMM", Locale.ENGLISH).apply { timeZone = TimeZone.getTimeZone("UTC") } }

    /** Очистка HTML-сущностей с быстрой оптимизацией для обычного текста. */
    private fun cleanHtmlEntities(input: String): String {
        if (input.isEmpty()) return ""
        var str = input.trim()
        if (!str.contains('&') && !str.contains('<')) return str

        str = str.replace("&laquo;", "«")
            .replace("&raquo;", "»")
            .replace("&quot;", "\"")
            .replace("&nbsp;", " ")
            .replace("&mdash;", "—")
            .replace("&ndash;", "–")
            .replace("&hellip;", "…")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&#39;", "'")
            .replace("&apos;", "'")

        if (str.contains('<') || str.contains('&')) {
            return try {
                Html.fromHtml(str, Html.FROM_HTML_MODE_LEGACY).toString().trim()
            } catch (_: Exception) {
                str.replace(Regex("<.*?>"), "").trim()
            }
        }
        return str.trim()
    }

    private fun formatFeedTitle(title: String): String {
        var clean = title.trim()
        // Убираем длинные SEO-хвосты, отрезая по популярным разделителям
        val separators = listOf(" - ", " — ", " | ", ": ")
        for (sep in separators) {
            if (clean.contains(sep)) {
                val parts = clean.split(sep)
                if (parts[0].isNotEmpty() && parts[0].length < 30) {
                    clean = parts[0]
                    break
                }
            }
        }
        if (clean.length > 35) {
            clean = clean.take(35) + "..."
        }
        return clean.trim()
    }

    private fun formatFeedDescription(desc: String): String {
        var clean = desc.trim().replace(Regex("\\s+"), " ")
        if (clean.length > 120) {
            clean = clean.take(120) + "..."
        }
        return clean
    }

    fun parseRss(xmlContent: String, sourceNameDefault: String = "RSS Feed", maxItemsToParse: Int = 500, skipItems: Int = 0): Pair<Source, List<Article>> {
        val articles = mutableListOf<Article>()

        // 1. Extract feed title
        val titleMatch = Regex("""<(?:channel|feed)[^>]*>[\s\S]*?<title[^>]*>([\s\S]*?)</title>""", RegexOption.IGNORE_CASE).find(xmlContent)
        val rawFeedTitle = titleMatch?.groupValues?.get(1)
            ?.replace("<![CDATA[", "")?.replace("]]>", "")
            ?.replace(Regex("<.*?>"), "")?.trim() ?: sourceNameDefault
        val feedTitle = formatFeedTitle(cleanHtmlEntities(rawFeedTitle))

        // 2. Extract feed description
        val descMatch = Regex("""<(?:channel|feed)[^>]*>[\s\S]*?<(?:description|subtitle)[^>]*>([\s\S]*?)</(?:description|subtitle)>""", RegexOption.IGNORE_CASE).find(xmlContent)
        val feedDescription = formatFeedDescription(cleanHtmlEntities(descMatch?.groupValues?.get(1)?.replace("<![CDATA[", "")?.replace("]]>", "")?.replace(Regex("<.*?>"), "")?.trim() ?: ""))

        // 3. Extract items / entries
        val itemRegex = Regex("""<(?:item|entry)[\s\S]*?>([\s\S]*?)</(?:item|entry)>""", RegexOption.IGNORE_CASE)
        val imgRegex = Regex("""<img[^>]+src=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
        val enclosureRegex = Regex("""<enclosure[^>]+url=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
        val mediaRegex = Regex("""<media:(?:content|thumbnail)[^>]+url=["']([^"']+)["']""", RegexOption.IGNORE_CASE)

        val matches = itemRegex.findAll(xmlContent).drop(skipItems).take(maxItemsToParse)
        val currentTimeMillis = System.currentTimeMillis()

        for (match in matches) {
            val block = match.groupValues[1]

            val itemTitle = Regex("""<title[^>]*>([\s\S]*?)</title>""", RegexOption.IGNORE_CASE).find(block)?.groupValues?.get(1)
                ?.replace("<![CDATA[", "")?.replace("]]>", "")?.trim()

            val itemLink = Regex("""<link[^>]*>([\s\S]*?)</link>""", RegexOption.IGNORE_CASE).find(block)?.groupValues?.get(1)
                ?.replace("<![CDATA[", "")?.replace("]]>", "")?.trim()
                ?: Regex("""<link[^>]+href=["']([^"']+)["']""", RegexOption.IGNORE_CASE).find(block)?.groupValues?.get(1)?.trim()

            val itemDesc = Regex("""<description[^>]*>([\s\S]*?)</description>""", RegexOption.IGNORE_CASE).find(block)?.groupValues?.get(1)
                ?.replace("<![CDATA[", "")?.replace("]]>", "")?.trim()

            val itemEncoded = Regex("""<content:encoded[^>]*>([\s\S]*?)</content:encoded>""", RegexOption.IGNORE_CASE).find(block)?.groupValues?.get(1)
                ?.replace("<![CDATA[", "")?.replace("]]>", "")?.trim()
                ?: Regex("""<content[^>]*>([\s\S]*?)</content>""", RegexOption.IGNORE_CASE).find(block)?.groupValues?.get(1)
                    ?.replace("<![CDATA[", "")?.replace("]]>", "")?.trim()

            val bestHtml = if ((itemEncoded?.length ?: 0) > (itemDesc?.length ?: 0)) itemEncoded!! else (itemDesc ?: itemTitle ?: "")

            val itemPubDate = Regex("""<(?:pubDate|updated|dc:date)[^>]*>(.*?)</(?:pubDate|updated|dc:date)>""", RegexOption.IGNORE_CASE).find(block)?.groupValues?.get(1)?.trim()
            val itemAuthor = Regex("""<(?:dc:creator|author)[^>]*>([\s\S]*?)</(?:dc:creator|author)>""", RegexOption.IGNORE_CASE).find(block)?.groupValues?.get(1)
                ?.replace("<![CDATA[", "")?.replace("]]>", "")?.replace(Regex("<.*?>"), "")?.trim()

            val imgUrl = enclosureRegex.find(block)?.groupValues?.get(1)
                ?: mediaRegex.find(block)?.groupValues?.get(1)
                ?: imgRegex.find(bestHtml)?.groupValues?.get(1)
                ?: imgRegex.find(itemDesc ?: "")?.groupValues?.get(1)

            if (!itemTitle.isNullOrEmpty()) {
                val cleanTitle = cleanHtmlEntities(itemTitle.replace(Regex("<.*?>"), ""))

                val cleanContent = cleanHtmlEntities(
                    bestHtml
                        .replace(Regex("(?i)<br\\s*/?>"), "\n")
                        .replace(Regex("(?i)</p>"), "\n\n")
                        .replace(Regex("<.*?>"), "")
                )

                val cleanSubtitle = cleanHtmlEntities(
                    (itemDesc ?: "").replace(Regex("<.*?>"), "")
                )

                val wordCount = cleanContent.split("\\s+".toRegex()).size
                val readMins = (wordCount / 160).coerceAtLeast(1)
                val relativeTime = parseRssDateToRelative(itemPubDate)
                val russianDate = parseRssDateToRussian(itemPubDate)
                val parsedMillis = parseDateToMillis(itemPubDate)
                val publishedAtFinal = if (parsedMillis == 0L) currentTimeMillis - articles.size else parsedMillis

                val article = Article(
                    id = itemLink ?: (currentTimeMillis.toString() + "_" + articles.size),
                    title = cleanTitle,
                    subtitle = if (cleanSubtitle.isNotEmpty()) cleanSubtitle.take(150) else cleanContent.take(150),
                    content = if (cleanContent.isNotEmpty()) cleanContent else cleanTitle,
                    sourceName = feedTitle,
                    category = "Лента",
                    author = cleanHtmlEntities(itemAuthor ?: ""),
                    date = relativeTime,
                    pubDateFormatted = russianDate,
                    publishedAt = publishedAtFinal,
                    readTime = "$readMins мин чтения",
                    imageUrl = imgUrl ?: "",
                    isFeatured = articles.isEmpty(),
                    isBookmarked = false,
                    isRead = false
                )
                articles.add(article)
            }
        }

        // Fallback-статьи больше не добавляем, чтобы не засорять базу мусорным XML кодом.
        // Репозиторий теперь сам проверяет наличие валидных статей.

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

    fun parseDateToMillis(dateStr: String?): Long {
        if (dateStr.isNullOrEmpty()) return 0L

        val formats = dateFormatters.get()!!
        for (format in formats) {
            try {
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
            days < 31 -> "${days}д"
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

        val formats = dateFormatters.get()!!
        var dateObj: java.util.Date? = null
        for (format in formats) {
            try {
                dateObj = format.parse(dateStr.trim())
                if (dateObj != null) break
            } catch (_: Exception) {}
        }

        if (dateObj == null) return dateStr

        val engDay = dayFormat.get()!!.format(dateObj)
        val dayNum = dayNumFormat.get()!!.format(dateObj)
        val engMonth = monthFormat.get()!!.format(dateObj)

        val rusDay = russianDays[engDay] ?: engDay
        val rusMonth = russianMonths[engMonth] ?: engMonth

        return "$rusDay, $dayNum $rusMonth"
    }
}
