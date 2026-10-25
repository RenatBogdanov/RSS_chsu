package com.example.rss_chsu.ui.model

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader
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

    fun parseRss(xmlContent: String, sourceNameDefault: String = "RSS Feed"): Pair<Source, List<Article>> {
        val articles = mutableListOf<Article>()
        var feedTitle = sourceNameDefault
        var feedDescription = ""

        try {
            val parser = Xml.newPullParser()
            parser.setInput(StringReader(xmlContent))

            var eventType = parser.eventType
            var currentTitle: String? = null
            var currentLink: String? = null
            var currentDescription: String? = null
            var currentContent: String? = null
            var currentPubDate: String? = null
            var currentAuthor: String? = null
            var currentImageUrl: String? = null
            var inItem = false
            var activeTag = ""

            val imgRegex = Regex("""<img[^>]+src=["']([^"']+)["']""", RegexOption.IGNORE_CASE)

            while (eventType != XmlPullParser.END_DOCUMENT) {
                val rawName = parser.name ?: ""
                val name = rawName.substringAfter(":").lowercase()

                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        activeTag = name
                        if (name == "item" || name == "entry") {
                            inItem = true
                            currentTitle = null
                            currentLink = null
                            currentDescription = null
                            currentContent = null
                            currentPubDate = null
                            currentAuthor = null
                            currentImageUrl = null
                        }
                        if (name == "enclosure" || name == "content" || name == "thumbnail" || name == "image") {
                            val url = parser.getAttributeValue(null, "url")
                            if (!url.isNullOrEmpty() && currentImageUrl == null) {
                                currentImageUrl = url
                            }
                        }
                    }
                    XmlPullParser.TEXT, XmlPullParser.CDSECT -> {
                        val text = parser.text ?: ""
                        if (text.isNotEmpty()) {
                            if (inItem) {
                                when (activeTag) {
                                    "title" -> currentTitle = (currentTitle ?: "") + text
                                    "link" -> if (currentLink.isNullOrEmpty()) currentLink = text
                                    "description", "summary" -> currentDescription = (currentDescription ?: "") + text
                                    "encoded", "content" -> currentContent = (currentContent ?: "") + text
                                    "pubdate", "updated", "date" -> currentPubDate = (currentPubDate ?: "") + text
                                    "creator", "author" -> currentAuthor = (currentAuthor ?: "") + text
                                }
                            } else {
                                when (activeTag) {
                                    "title" -> if (feedTitle == sourceNameDefault) feedTitle = text
                                    "description", "subtitle" -> if (feedDescription.isEmpty()) feedDescription = text
                                }
                            }
                        }
                    }
                    XmlPullParser.END_TAG -> {
                        if (name == "item" || name == "entry") {
                            inItem = false
                            val titleToUse = currentTitle?.trim() ?: ""
                            if (titleToUse.isNotEmpty()) {
                                val rawContent = currentContent?.trim() ?: ""
                                val rawDesc = currentDescription?.trim() ?: ""
                                val bestHtml = if (rawContent.length > rawDesc.length) rawContent else rawDesc

                                if (currentImageUrl == null) {
                                    val match = imgRegex.find(bestHtml)
                                    if (match != null) {
                                        currentImageUrl = match.groupValues[1]
                                    }
                                }

                                val cleanContent = bestHtml
                                    .replace(Regex("(?i)<br\\s*/?>"), "\n")
                                    .replace(Regex("(?i)</p>"), "\n\n")
                                    .replace(Regex("<.*?>"), "")
                                    .replace("&nbsp;", " ")
                                    .replace("&mdash;", "—")
                                    .replace("&ndash;", "–")
                                    .replace("&quot;", "\"")
                                    .replace("&amp;", "&")
                                    .trim()

                                val cleanSubtitle = rawDesc
                                    .replace(Regex("<.*?>"), "")
                                    .replace("&nbsp;", " ")
                                    .trim()

                                val wordCount = cleanContent.split("\\s+".toRegex()).size
                                val readMins = (wordCount / 160).coerceAtLeast(1)

                                val relativeTime = parseRssDateToRelative(currentPubDate)
                                val russianDate = parseRssDateToRussian(currentPubDate)

                                val article = Article(
                                    id = currentLink?.trim() ?: (System.currentTimeMillis().toString() + "_" + articles.size),
                                    title = titleToUse,
                                    subtitle = if (cleanSubtitle.isNotEmpty()) cleanSubtitle.take(150) else cleanContent.take(150),
                                    content = if (cleanContent.isNotEmpty()) cleanContent else titleToUse,
                                    sourceName = feedTitle,
                                    category = "Лента",
                                    author = currentAuthor?.trim() ?: "",
                                    date = relativeTime,
                                    pubDateFormatted = russianDate,
                                    readTime = "$readMins мин чтения",
                                    imageUrl = currentImageUrl ?: "",
                                    isFeatured = articles.isEmpty(),
                                    isBookmarked = false,
                                    isRead = false
                                )
                                articles.add(article)
                            }
                        }
                        activeTag = ""
                    }
                }
                eventType = parser.next()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (articles.isEmpty() && xmlContent.isNotEmpty()) {
            articles.add(
                Article(
                    id = "fallback_${System.currentTimeMillis()}",
                    title = "Лента: $sourceNameDefault",
                    subtitle = "Материалы получены из RSS потока.",
                    content = xmlContent.take(1000),
                    sourceName = sourceNameDefault,
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

    private fun parseRssDateToRelative(dateStr: String?): String {
        if (dateStr.isNullOrEmpty()) return "недавно"

        val formats = listOf(
            SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH),
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.ENGLISH),
            SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ENGLISH),
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH),
            SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH)
        )

        var millis = 0L
        for (format in formats) {
            try {
                format.timeZone = TimeZone.getTimeZone("UTC")
                val date = format.parse(dateStr.trim())
                if (date != null) {
                    millis = date.time
                    break
                }
            } catch (_: Exception) {}
        }

        if (millis == 0L) {
            return dateStr.take(16)
        }

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
