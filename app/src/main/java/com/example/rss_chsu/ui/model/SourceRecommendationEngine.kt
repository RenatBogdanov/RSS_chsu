package com.example.rss_chsu.ui.model

import kotlin.math.ln
import kotlin.math.sqrt

data class RecommendedSource(
    val source: Source,
    val score: Double,
    /** true: подбор по закладкам и прочитанному; false: по подпискам или просто популярное. */
    val basedOnInterests: Boolean
)

/**
 * Рекомендации источников по тому же принципу, что и рекомендации статей: ключевые слова + IDF + косинусная мера.
 *
 * 1. Профиль интересов пользователя (слова -> вес):
 *    - явные сигналы: закладки (вес 3) и прочитанное (вес 1);
 *    - неявные: все статьи из подписок (вес 0,5 от нормированного профиля), чтобы подборка работала,
 *      пока пользователь ещё ничего не читал.
 * 2. Профиль каждого источника из каталога, на который пользователь не подписан:
 *    ключевые слова описания (вес 1,5) и слова из пробной загрузки ленты (вес 1).
 * 3. Оценка = косинус между профилем пользователя и профилем источника (редкие слова весят больше, IDF)
 *    плюс небольшая надбавка за категорию: если пользователь подписан на много «Технологий» или читает
 *    их, технологические источники поднимаются выше. Это работает и тогда, когда слов ещё мало.
 *
 * @param articles все статьи из кэша (с проставленным sourceId)
 * @param subscribed подписки пользователя
 * @param catalog каталог популярных источников (кандидаты)
 * @param previewKeywords ключевые слова источников каталога из пробной загрузки (адрес ленты -> слова)
 */
class SourceRecommendationEngine(
    private val articles: List<Article>,
    private val subscribed: List<Source>,
    private val catalog: List<Source>,
    private val previewKeywords: Map<String, List<String>> = emptyMap()
) {

    fun recommend(limit: Int = 8): List<RecommendedSource> {
        val candidates = catalog.filter { !isSubscribed(it) }
        if (candidates.isEmpty()) return emptyList()

        val candidateTerms = candidates.map { it to termsOf(it) }

        val subscribedDomains = subscribed.map { it.domain }.toSet()
        val subscribedArticles = articles.filter { it.sourceId in subscribedDomains }

        // IDF по «документам»: профили кандидатов и профили подписок
        val documents = ArrayList<Set<String>>()
        candidateTerms.forEach { documents.add(it.second.keys) }
        subscribedArticles
            .groupBy { it.sourceId }
            .values
            .forEach { documents.add(topTerms(it).toSet()) }

        val docFrequency = HashMap<String, Int>()
        documents.forEach { doc -> doc.forEach { docFrequency.merge(it, 1, Int::plus) } }
        fun idf(term: String): Double =
            ln((documents.size + 1.0) / ((docFrequency[term] ?: 0) + 1.0)) + 1.0

        // Явный профиль: закладки и прочитанное
        val explicit = HashMap<String, Double>()
        for (article in articles) {
            val weight = interestWeight(article)
            if (weight > 0.0) {
                keywordsOf(article).forEach { explicit.merge(it, weight, Double::plus) }
            }
        }

        // Интерес к категориям: подписки (вес 1) и источники закладок и прочитанного (вес 3 и 1)
        val categoryWeights = HashMap<SourceCategory, Double>()
        subscribed.forEach { source ->
            SourceCategories.of(source)?.let { categoryWeights.merge(it, 1.0, Double::plus) }
        }
        for (article in articles) {
            val weight = interestWeight(article)
            if (weight > 0.0) {
                SourceCategories.ofDomain(article.sourceId)?.let { categoryWeights.merge(it, weight, Double::plus) }
            }
        }
        val maxCategoryWeight = categoryWeights.values.maxOrNull() ?: 0.0
        fun categoryBoost(source: Source): Double {
            val category = SourceCategories.of(source) ?: return 0.0
            return if (maxCategoryWeight > 0.0) (categoryWeights[category] ?: 0.0) / maxCategoryWeight else 0.0
        }

        // Неявный профиль: всё, что приходит из подписок
        val implicit = HashMap<String, Double>()
        for (article in subscribedArticles) {
            keywordsOf(article).forEach { implicit.merge(it, 1.0, Double::plus) }
        }

        val profile = HashMap<String, Double>()
        normalized(explicit.mapValues { (term, value) -> value * idf(term) })
            .forEach { (term, value) -> profile.merge(term, value, Double::plus) }
        normalized(implicit.mapValues { (term, value) -> value * idf(term) })
            .forEach { (term, value) -> profile.merge(term, IMPLICIT_SHARE * value, Double::plus) }

        // Нет ни закладок, ни подписок: показываем каталог как есть (популярное)
        if (profile.isEmpty() && categoryWeights.isEmpty()) {
            return candidates.take(limit).map { RecommendedSource(it, 0.0, false) }
        }

        val basedOnInterests = explicit.isNotEmpty()
        return candidateTerms
            .map { (source, terms) ->
                val vector = terms.mapValues { (term, weight) -> weight * idf(term) }
                val score = cosine(profile, vector) + CATEGORY_WEIGHT * categoryBoost(source)
                RecommendedSource(source, score, basedOnInterests)
            }
            .filter { it.score > 0.0 }
            .sortedByDescending { it.score }
            .take(limit)
            .ifEmpty { candidates.take(limit).map { RecommendedSource(it, 0.0, false) } }
    }

    private fun interestWeight(article: Article): Double =
        (if (article.isBookmarked) BOOKMARK_WEIGHT else 0.0) + (if (article.isRead) READ_WEIGHT else 0.0)

    private fun termsOf(source: Source): Map<String, Double> {
        val terms = HashMap<String, Double>()
        previewKeywords[source.domain].orEmpty().forEach { terms.merge(it, PREVIEW_TERM_WEIGHT, Double::plus) }
        KeywordExtractor.extract("", source.description).forEach { terms.merge(it, DESCRIPTION_TERM_WEIGHT, Double::plus) }
        return terms
    }

    private fun keywordsOf(article: Article): List<String> =
        article.keywords.ifEmpty { KeywordExtractor.extract(article.title, article.subtitle, article.content) }

    /** Слова, встречающиеся в наибольшем числе статей источника. */
    private fun topTerms(sourceArticles: List<Article>, limit: Int = 40): List<String> {
        val docFrequency = HashMap<String, Int>()
        sourceArticles.forEach { article ->
            keywordsOf(article).distinct().forEach { docFrequency.merge(it, 1, Int::plus) }
        }
        return docFrequency.entries.sortedByDescending { it.value }.take(limit).map { it.key }
    }

    private fun isSubscribed(candidate: Source): Boolean {
        val candidateKeys = extractDomainKeywords(candidate.domain)
        return subscribed.any { sub ->
            sub.domain.equals(candidate.domain, ignoreCase = true) ||
                    (candidateKeys.isNotEmpty() && extractDomainKeywords(sub.domain) == candidateKeys)
        }
    }

    private fun normalized(vector: Map<String, Double>): Map<String, Double> {
        val norm = sqrt(vector.values.sumOf { it * it })
        return if (norm == 0.0) emptyMap() else vector.mapValues { it.value / norm }
    }

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

    private companion object {
        const val BOOKMARK_WEIGHT = 3.0
        const val READ_WEIGHT = 1.0
        const val IMPLICIT_SHARE = 0.5
        const val CATEGORY_WEIGHT = 0.2
        const val DESCRIPTION_TERM_WEIGHT = 1.5
        const val PREVIEW_TERM_WEIGHT = 1.0
    }
}