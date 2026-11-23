package com.example.rss_chsu.ui.model

import kotlin.math.ln
import kotlin.math.sqrt

/**
 * Индекс рекомендаций. IDF, векторы и их нормы вычисляются один раз при построении индекса.
 * Статусы прочтения/закладки передаются отдельно, поэтому их изменение не перестраивает индекс.
 */
class RecommendationEngine(private val corpus: List<Article>) {
    private data class Vector(val values: Map<String, Double>, val norm: Double)

    private val docFrequency: Map<String, Int> = buildMap {
        corpus.forEach { article ->
            keywordsOf(article).distinct().forEach { term ->
                put(term, (get(term) ?: 0) + 1)
            }
        }
    }

    private val vectors: Map<String, Vector> = corpus.associate { article ->
        article.id to buildVector(article)
    }

    private fun keywordsOf(article: Article): List<String> = article.keywords.ifEmpty {
        KeywordExtractor.extract(article.title, article.subtitle, article.content)
    }

    private fun idf(term: String): Double =
        ln((corpus.size + 1.0) / ((docFrequency[term] ?: 0) + 1.0)) + 1.0

    private fun buildVector(article: Article): Vector {
        val values = keywordsOf(article).distinct().associateWith(::idf)
        return Vector(values, sqrt(values.values.sumOf { it * it }))
    }

    private fun vectorOf(article: Article): Vector =
        vectors[article.id] ?: buildVector(article)

    private fun cosine(a: Vector, b: Vector): Double {
        if (a.values.isEmpty() || b.values.isEmpty() || a.norm == 0.0 || b.norm == 0.0) return 0.0
        val (small, large) = if (a.values.size <= b.values.size) a.values to b.values else b.values to a.values
        var dot = 0.0
        for ((term, weight) in small) dot += weight * (large[term] ?: 0.0)
        return if (dot == 0.0) 0.0 else dot / (a.norm * b.norm)
    }

    /** Похожие статьи. Кандидаты могут содержать актуальные статусы, но векторы берутся из кэша. */
    fun getRecommendations(targetArticle: Article, limit: Int = 5): List<Article> =
        getRecommendations(targetArticle, corpus, limit)

    fun getRecommendations(
        targetArticle: Article,
        candidates: List<Article>,
        limit: Int = 5
    ): List<Article> {
        if (candidates.size <= 1) return emptyList()
        val targetVector = vectorOf(targetArticle)
        return candidates.asSequence()
            .filter { it.id != targetArticle.id }
            .map { it to cosine(targetVector, vectorOf(it)) }
            .filter { it.second > 0.0 }
            .sortedByDescending { it.second }
            .take(limit)
            .map { it.first }
            .toList()
    }

    /**
     * Персональные рекомендации по актуальному списку статусов.
     * Профиль: закладки с весом 3, прочитанное с весом 1.
     */
    fun getPersonalized(currentArticles: List<Article>, limit: Int = 20): List<Article> {
        val candidates = currentArticles.filter { !it.isRead && !it.isBookmarked }
        if (candidates.isEmpty()) return emptyList()

        val profile = HashMap<String, Double>()
        currentArticles.forEach { article ->
            val weight = (if (article.isBookmarked) BOOKMARK_WEIGHT else 0.0) +
                    (if (article.isRead) READ_WEIGHT else 0.0)
            if (weight > 0.0) {
                keywordsOf(article).distinct().forEach { term ->
                    profile.merge(term, idf(term) * weight, Double::plus)
                }
            }
        }

        if (profile.isEmpty()) return candidates.take(limit)
        val profileVector = Vector(profile, sqrt(profile.values.sumOf { it * it }))

        return candidates.asSequence()
            .map { it to cosine(profileVector, vectorOf(it)) }
            .filter { it.second > 0.0 }
            .sortedByDescending { it.second }
            .take(limit)
            .map { it.first }
            .toList()
            .ifEmpty { candidates.take(limit) }
    }

    /** Совместимость со старым вызовом. */
    fun getPersonalized(limit: Int = 20): List<Article> = getPersonalized(corpus, limit)

    private companion object {
        const val BOOKMARK_WEIGHT = 3.0
        const val READ_WEIGHT = 1.0
    }
}
