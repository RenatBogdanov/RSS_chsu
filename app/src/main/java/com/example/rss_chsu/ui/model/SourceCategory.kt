@file:Suppress("SpellCheckingInspection")

package com.example.rss_chsu.ui.model

/** Тематика источника. Порядок элементов задаёт порядок разделов на экране поиска. */
enum class SourceCategory(val title: String) {
    NEWS("Новости"),
    BUSINESS("Бизнес и экономика"),
    TECH("Технологии"),
    DEV("Разработка"),
    SCIENCE("Наука"),
    GAMES("Игры"),
    CULTURE("Кино, музыка, культура"),
    SPORT("Спорт"),
    AUTO("Авто"),
    DESIGN("Дизайн"),
    LIFESTYLE("Стиль жизни"),
    REGIONAL("Города и регионы")
}

/**
 * Категории источников каталога [PopularSources].
 * Хранятся отдельно от каталога: у каждой ленты своя запись по её id, правится в одном месте.
 * Источники, которых нет в каталоге (добавлены по ссылке), остаются без категории.
 */
object SourceCategories {

    private fun ids(category: SourceCategory, vararg ids: String): Map<String, SourceCategory> =
        ids.associateWith { category }

    /** id ленты в каталоге -> категория */
    private val byCatalogId: Map<String, SourceCategory> =
        ids(
            SourceCategory.NEWS,
            "aif", "baza", "gazeta", "interfax", "iz", "kommersant", "komsomolskaya", "lenta", "life",
            "mediazona", "meduza", "meduza_cards", "mk", "ng", "ntv", "ostorozhno", "profile", "rbc",
            "regnum", "ren", "republic", "rg", "ria", "rt", "ruposters", "snob", "sobesednik",
            "svoboda", "tass", "trud", "tvrain", "vesti", "vzglyad", "yandex", "zavtra", "tjournal",
            "abcnews", "aljazeera", "bbc_en", "cnn", "guardian", "independent", "latimes", "newyorker",
            "npr", "nyt", "reddit_worldnews", "reuters", "theatlantic", "vice", "vox", "wapo", "time",
            "usatoday", "nypost", "telegraph", "foxnews", "sky", "politico", "hill", "huffpost",
            "vice_news", "aljazeera_us"
        ) +
                ids(
                    SourceCategory.BUSINESS,
                    "adindex", "banki", "bfm", "expert", "forbes_ru", "inc", "rubase", "secretmag", "thebell",
                    "vedomosti", "vc", "bloomberg", "bnext_en", "cnbc", "economist", "forbes", "ft", "wsj"
                ) +
                ids(
                    SourceCategory.TECH,
                    "3dnews", "applemix", "cnews", "comnews", "droider", "ferra", "ixbt", "macdigger",
                    "mobile_review", "xakep", "9to5google", "9to5mac", "androidauthority", "androidpolice",
                    "appleinsider", "arstechnica", "cnet", "engadget", "gizmodo", "macrumors", "mashable",
                    "mashable_tech", "medium", "producthunt", "reddit_android", "reddit_technology",
                    "techcrunch", "techradar", "theverge", "wired", "zdnet", "digitaltrends", "tomshardware",
                    "anandtech", "slashdot"
                ) +
                ids(
                    SourceCategory.DEV,
                    "habr", "opennet", "tproger", "androiddev", "css_tricks", "devto", "hackernews", "kotlin",
                    "reddit_kotlin", "smashing"
                ) +
                ids(
                    SourceCategory.SCIENCE,
                    "elementy", "nakedscience", "nplus1", "popmech", "postnauka", "vokrugsveta", "nasa",
                    "nationalgeo", "sciencedaily", "nature", "sciencemag", "newscientist", "physorg",
                    "livescience", "space"
                ) +
                ids(
                    SourceCategory.GAMES,
                    "cybersport", "dtf", "goha", "igromania", "shazoo", "stopgame", "igromania_news",
                    "playground", "kanobu", "destructoid", "escapist", "eurogamer", "gamespot", "gamesradar",
                    "ign", "ign_articles", "kotaku", "nintendolife", "pcgamer", "polygon", "polygon_features",
                    "purexbox", "pushsquare", "rockpapershotgun", "gamasutra"
                ) +
                ids(
                    SourceCategory.CULTURE,
                    "afisha", "diletant", "kino-teatr", "kinonews", "kinopoisk", "mirf", "pitchfork",
                    "rollingstone"
                ) +
                ids(
                    SourceCategory.SPORT,
                    "championat", "matchtv", "sport_express", "sportsru", "espn"
                ) +
                ids(
                    SourceCategory.AUTO,
                    "autonews", "kolesa"
                ) +
                ids(
                    SourceCategory.DESIGN,
                    "dribbble", "behance", "awwwards"
                ) +
                ids(
                    SourceCategory.LIFESTYLE,
                    "krasota", "maximov", "mel", "pikabu", "playboy", "wonderzine", "znamenitki", "buzzfeed",
                    "lifehacker", "reddit"
                ) +
                ids(
                    SourceCategory.REGIONAL,
                    "dp", "fontanka", "paper", "thevillage", "cherinfo", "vologdanews", "vologdazso"
                )

    /**
     * Приводит адрес ленты к виду для сравнения: без схемы, без «www.» и без хвостового «/».
     * Так «http://www.lenta.ru/rss/news/» и «https://lenta.ru/rss/news» считаются одним адресом.
     */
    private fun normalize(domain: String): String =
        domain.trim().lowercase()
            .removePrefix("https://")
            .removePrefix("http://")
            .removePrefix("www.")
            .trimEnd('/')

    /** Нормализованный адрес ленты -> категория. */
    private val byDomain: Map<String, SourceCategory> by lazy<Map<String, SourceCategory>> {
        val result = HashMap<String, SourceCategory>()
        for (source in PopularSources.topFeeds) {
            val category = byCatalogId[source.id] ?: continue
            result[normalize(source.domain)] = category
        }
        result
    }

    /** Категория по адресу ленты. Для источников, добавленных по своей ссылке, возвращает null. */
    fun ofDomain(domain: String): SourceCategory? {
        if (domain.isBlank()) return null
        return byDomain[normalize(domain)]
    }

    fun of(source: Source): SourceCategory? = ofDomain(source.domain)

    /** Категории в порядке показа. */
    val inOrder: List<SourceCategory> = SourceCategory.entries
}