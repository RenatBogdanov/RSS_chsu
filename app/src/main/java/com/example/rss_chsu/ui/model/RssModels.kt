package com.example.rss_chsu.ui.model

import kotlinx.serialization.Serializable

@Serializable
data class Article(
    val id: String,
    val title: String,
    val subtitle: String = "",
    val content: String = "",
    val sourceName: String,
    val category: String,
    val author: String = "",
    val date: String,
    val pubDateFormatted: String = "",
    val readTime: String,
    val imageUrl: String = "",
    val isBookmarked: Boolean = false,
    val isRead: Boolean = false,
    val isFeatured: Boolean = false,
    val publishedAt: Long = 0L,                 // время публикации, мс
    val keywords: List<String> = emptyList()    // ключевые слова для рекомендаций
)

@Serializable
data class Source(
    val id: String,
    val name: String,
    val domain: String,
    val description: String,
    val articlesPerWeek: String,
    val logoText: String,
    val isSubscribed: Boolean = false,
    val folderName: String? = null,
    val unreadCount: Int = 0
)

@Serializable
data class Folder(
    val id: String,
    val name: String,
    val sourceCount: Int,
    val totalUnread: Int,
    val isExpanded: Boolean = false,
    val sources: List<Source> = emptyList()
)

object SampleData {
    val featuredArticle = Article(
        id = "1",
        title = "Город, в котором хочется замедлиться",
        subtitle = "Как зелёные улицы меняют повседневную жизнь — и почему это важно.",
        content = """
            Утром город звучит иначе, если в нём есть место для прогулки. Вместо шума машин — разговоры у кофейни, шелест листьев и звон велосипедов.
            
            Улица как место встречи
            
            В Копенгагене удобство пешехода давно стало точкой отсчёта. Деревья дают тень, скамейки приглашают сделать паузу, а первые этажи домов возвращают улице жизнь.
        """.trimIndent(),
        sourceName = "Стрелка",
        category = "ГОРОД И ЖИЗНЬ",
        author = "Анна Смирнова",
        date = "12 октября 2026",
        readTime = "6 мин чтения",
        imageUrl = "copenhagen_street",
        isBookmarked = true,
        isFeatured = true
    )

    val recommendedFeatured = Article(
        id = "rec_1",
        title = "Как генеративный ИИ изменяет интерфейсы будущего",
        subtitle = "Обзор ключевых дизайн-систем и подходов к разработке персональных ассистанов.",
        content = "Искусственный интеллект больше не просто инструмент — он становится новым медиумом интерфейсов...",
        sourceName = "Arzamas",
        category = "ДИЗАЙН И ТЕХНОЛОГИИ",
        author = "Дмитрий Ковалев",
        date = "Сегодня",
        readTime = "7 мин чтения",
        isFeatured = true
    )

    val articlesList = listOf(
        featuredArticle,
        Article(
            id = "2",
            title = "Новый взгляд на поиск жизни за пределами Земли",
            sourceName = "N + 1",
            category = "Наука",
            date = "2 часа назад",
            readTime = "4 мин",
            imageUrl = "space_dish"
        ),
        Article(
            id = "3",
            title = "Меньше вкладок, больше внимания: цифровая гигиена",
            sourceName = "Лайфхакер",
            category = "Образ жизни",
            date = "3 часа назад",
            readTime = "5 мин",
            imageUrl = "digital_hygiene"
        ),
        Article(
            id = "4",
            title = "Что мы знаем о тёмной стороне Вселенной",
            sourceName = "N + 1",
            category = "Наука",
            date = "Вчера",
            readTime = "8 мин",
            isBookmarked = true,
            imageUrl = "universe_dish"
        ),
        Article(
            id = "5",
            title = "Как привычка читать меняет наше мышление",
            sourceName = "ПостНаука",
            category = "Культура",
            date = "10 октября",
            readTime = "7 мин",
            isBookmarked = true,
            imageUrl = "books_library"
        )
    )

    val recommendedArticles = listOf(
        recommendedFeatured,
        Article(
            id = "rec_2",
            title = "Архитектура тихого пространства: почему нам необходима тишина",
            sourceName = "Strelka Mag",
            category = "Урбанистика",
            date = "Вчера",
            readTime = "5 мин"
        ),
        Article(
            id = "rec_3",
            title = "История минимализма: от промышленного дизайна до интерфейсов",
            sourceName = "DesignPub",
            category = "Дизайн",
            date = "2 дня назад",
            readTime = "9 мин"
        ),
        Article(
            id = "rec_4",
            title = "Нейробиология фокуса: как вернуть концентрацию в эпоху уведомлений",
            sourceName = "Nautilus",
            category = "Наука",
            date = "3 дня назад",
            readTime = "6 мин"
        ),
        Article(
            id = "rec_5",
            title = "Будущее персонального транспорта: микромобильность в мегаполисе",
            sourceName = "Urban Tech",
            category = "Технологии",
            date = "4 дня назад",
            readTime = "8 мин"
        ),
        Article(
            id = "rec_6",
            title = "Как книги по философии формируют привычки критического мышления",
            sourceName = "ПостНаука",
            category = "Культура",
            date = "5 дней назад",
            readTime = "7 мин"
        ),
        Article(
            id = "rec_7",
            title = "Искусство медленного чтения в мире быстрых данных",
            sourceName = "Лайфхакер",
            category = "Образ жизни",
            date = "Неделю назад",
            readTime = "5 мин"
        )
    )

    val recommendedChannels = listOf(
        Source(
            id = "rec_ch_1",
            name = "Arzamas",
            domain = "arzamas.academy",
            description = "Курсы и подкасты по истории и культуре",
            articlesPerWeek = "5 статей в неделю",
            logoText = "AZ",
            isSubscribed = false
        ),
        Source(
            id = "rec_ch_2",
            name = "TechCrunch",
            domain = "techcrunch.com",
            description = "Главные новости стартапов и технологий",
            articlesPerWeek = "20 статей в неделю",
            logoText = "TC",
            isSubscribed = false
        ),
        Source(
            id = "rec_ch_3",
            name = "Nautilus",
            domain = "nautil.us",
            description = "Глубокие лонгриды о науке, философии и людях",
            articlesPerWeek = "4 статьи в неделю",
            logoText = "NL",
            isSubscribed = false
        )
    )

    val sampleFolders = listOf(
        Folder(
            id = "tech",
            name = "Технологии",
            sourceCount = 4,
            totalUnread = 12,
            isExpanded = true,
            sources = listOf(
                Source("1", "Хабр", "habr.com", "", "", "x", true, "Технологии", 6),
                Source("2", "vc.ru", "vc.ru", "", "", "vc", true, "Технологии", 3),
                Source("3", "Т — Ж", "t-j.ru/tech", "", "", "т-ж", true, "Технологии", 2),
                Source("4", "Код", "thecode.media", "", "", "{}", true, "Технологии", 1)
            )
        ),
        Folder(
            id = "science",
            name = "Наука",
            sourceCount = 3,
            totalUnread = 7,
            isExpanded = false,
            sources = listOf(
                Source("5", "N + 1", "nplus1.ru", "Научные открытия простым языком", "12 статей в неделю", "N+1", true, "Наука", 7)
            )
        ),
        Folder(
            id = "city",
            name = "Город и культура",
            sourceCount = 3,
            totalUnread = 5,
            isExpanded = false
        ),
        Folder(
            id = "lifestyle",
            name = "Образ жизни",
            sourceCount = 2,
            totalUnread = 0,
            isExpanded = false
        )
    )

    val sampleSearchSources = listOf(
        Source(
            id = "5",
            name = "N + 1",
            domain = "nplus1.ru",
            description = "Научные открытия простым языком\n12 статей в неделю",
            articlesPerWeek = "12 статей в неделю",
            logoText = "N+1",
            isSubscribed = true,
            folderName = "Наука"
        ),
        Source(
            id = "6",
            name = "ПостНаука",
            domain = "postnauka.ru",
            description = "Проект о современной науке и ученых",
            articlesPerWeek = "8 статей в неделю",
            logoText = "ПН",
            isSubscribed = false
        )
    )
}