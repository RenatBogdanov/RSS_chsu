package com.example.rss_chsu.ui.model

import kotlin.math.ln
import kotlin.math.sqrt

/**
 * Рекомендации на основе ключевых слов.
 *
 * - [getRecommendations]: статьи, похожие на одну конкретную (экран чтения).
 * - [getPersonalized]: персональная лента по профилю интересов (закладки и прочитанное).
 *
 * Статья представлена множеством ключевых слов, где вес слова = IDF
 * (редкие слова информативнее частых). Близость считается косинусной мерой.
 */
class RecommendationEngine(private val allArticles: List<Article>) {

    private val docFrequency: Map<String, Int> = HashMap<String, Int>().also { map ->
        allArticles.forEach { article ->
            keywordsOf(article).forEach { map.merge(it, 1, Int::plus) }
        }
    }

    private fun keywordsOf(article: Article): List<String> =
        article.keywords.ifEmpty {
            KeywordExtractor.extract(article.title, article.subtitle, article.content)
        }

    private fun idf(term: String): Double =
        ln((allArticles.size + 1.0) / ((docFrequency[term] ?: 0) + 1.0)) + 1.0

    private fun vectorOf(article: Article): Map<String, Double> =
        keywordsOf(article).associateWith { idf(it) }

    private fun cosine(a: Map<String, Double>, b: Map<String, Double>): Double {
        if (a.isEmpty() || b.isEmpty()) return 0.0
        val (small, large) = if (a.size <= b.size) a to b else b to a
        var dot = 0.0
        for ((term, weight) in small) {
            val other = large[term] ?: continue
            dot += weight * other
        }
        if (dot == 0.0) return 0.0
        val normA = sqrt(a.values.sumOf { it * it })
        val normB = sqrt(b.values.sumOf { it * it })
        return dot / (normA * normB)
    }

    /** Статьи, похожие на [targetArticle]. */
    fun getRecommendations(targetArticle: Article, limit: Int = 5): List<Article> {
        if (allArticles.size <= 1) return emptyList()
        val targetVector = vectorOf(targetArticle)

        return allArticles
            .asSequence()
            .filter { it.id != targetArticle.id }
            .map { it to cosine(targetVector, vectorOf(it)) }
            .filter { it.second > 0.0 }
            .sortedByDescending { it.second } // сортировка стабильна: при равенстве первыми идут более свежие
            .take(limit)
            .map { it.first }
            .toList()
    }

    /**
     * Персональные рекомендации: профиль собирается из закладок (вес 3) и прочитанного (вес 1).
     * Показываем только непрочитанные и не добавленные в закладки. Пока профиль пуст — просто свежие статьи.
     */
    fun getPersonalized(limit: Int = 20): List<Article> {
        val candidates = allArticles.filter { !it.isRead && !it.isBookmarked }

        val profile = HashMap<String, Double>()
        allArticles.forEach { article ->
            val weight = (if (article.isBookmarked) BOOKMARK_WEIGHT else 0.0) +
                    (if (article.isRead) READ_WEIGHT else 0.0)
            if (weight > 0.0) {
                keywordsOf(article).forEach { term ->
                    profile.merge(term, idf(term) * weight, Double::plus)
                }
            }
        }
        if (profile.isEmpty()) return candidates.take(limit)

        return candidates
            .map { it to cosine(profile, vectorOf(it)) }
            .filter { it.second > 0.0 }
            .sortedByDescending { it.second }
            .take(limit)
            .map { it.first }
            .ifEmpty { candidates.take(limit) }
    }

    private companion object {
        const val BOOKMARK_WEIGHT = 3.0
        const val READ_WEIGHT = 1.0
    }
}