package com.example.rss_chsu.ui.model

import java.util.Locale

/**
 * Простое извлечение ключевых слов: токенизация -> стоп-слова -> грубый стемминг -> взвешенная частота.
 * Слова из заголовка весят больше, чем из описания и текста.
 */
object KeywordExtractor {

    private val stopWords = setOf(
        // RU
        "и", "в", "во", "на", "с", "со", "но", "а", "то", "как", "что", "это", "для", "по", "от", "из",
        "или", "где", "когда", "кто", "был", "было", "были", "была", "его", "ее", "их", "они", "она",
        "оно", "мы", "вы", "все", "всё", "так", "уже", "еще", "ещё", "при", "над", "под", "без", "про",
        "чем", "если", "чтобы", "этот", "эта", "эти", "этого", "того", "тот", "тем", "там", "тут", "здесь",
        "быть", "есть", "может", "можно", "будет", "более", "только", "также", "после", "перед", "между",
        "которые", "который", "которая", "которое", "очень", "себя", "свой", "своей", "нас", "нам", "ним",
        "год", "года", "году", "лет", "читать", "далее", "подробнее", "источник",
        // EN
        "the", "and", "for", "are", "but", "not", "you", "all", "any", "can", "had", "her", "was", "one",
        "our", "out", "has", "have", "his", "how", "its", "may", "new", "now", "see", "she", "who", "why",
        "with", "this", "that", "from", "they", "will", "would", "there", "their", "what", "about", "which",
        "when", "were", "been", "more", "into", "than", "them", "then", "your", "just", "also", "read"
    )

    // Окончания отсортированы по длине (сначала длинные), чтобы срезать самое длинное совпадение
    private val ruEndings = listOf(
        "иями", "ями", "ами", "ого", "его", "ому", "ему", "ыми", "ими", "ией", "ей", "ий", "ый", "ой",
        "ая", "яя", "ое", "ее", "ые", "ие", "ия", "ов", "ев", "ам", "ям", "ах", "ях", "ом", "ем",
        "ую", "юю", "ою", "ею", "ых", "их", "а", "я", "ы", "и", "у", "ю", "е", "о", "ь"
    ).sortedByDescending { it.length }

    private val nonWord = Regex("[^а-яa-z0-9]+")

    fun tokenize(text: String): List<String> =
        text.lowercase(Locale.ROOT)
            .replace('ё', 'е')
            .split(nonWord)
            .filter { it.length >= 3 && it !in stopWords && !it.all(Char::isDigit) }

    /** Грубый стемминг: достаточно, чтобы «города», «городе», «город» считались одним словом. */
    fun stem(word: String): String {
        if (word.length <= 4 || word.any(Char::isDigit)) return word

        if (word[0] in 'a'..'z') {
            return when {
                word.endsWith("ing") && word.length > 5 -> word.dropLast(3)
                word.endsWith("ed") && word.length > 5 -> word.dropLast(2)
                word.endsWith("s") && !word.endsWith("ss") -> word.dropLast(1)
                else -> word
            }
        }

        for (ending in ruEndings) {
            if (word.endsWith(ending) && word.length - ending.length >= 4) {
                return word.dropLast(ending.length)
            }
        }
        return word
    }

    fun extract(
        title: String,
        subtitle: String = "",
        content: String = "",
        limit: Int = 12
    ): List<String> {
        val scores = LinkedHashMap<String, Double>()

        fun add(text: String, weight: Double) {
            tokenize(text).forEach { token ->
                val s = stem(token)
                scores[s] = (scores[s] ?: 0.0) + weight
            }
        }

        add(title, 3.0)
        add(subtitle, 2.0)
        add(content.take(4000), 1.0) // длинные тексты нет смысла разбирать целиком

        return scores.entries
            .sortedByDescending { it.value }
            .take(limit)
            .map { it.key }
    }
}
