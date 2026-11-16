package com.example.rss_chsu.ui.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Рекомендации источников. Отдельная ViewModel, чтобы не трогать FeedViewModel. */
class SourceRecommendationsViewModel : ViewModel() {

    /** Пересчитывается при любом изменении статей, подписок или профилей источников. */
    val recommendedSources: StateFlow<List<RecommendedSource>> = combine(
        RssRepository.articles,
        RssRepository.sources,
        RssRepository.sourceProfiles
    ) { articles, sources, profiles ->
        SourceRecommendationEngine(articles, sources, PopularSources.topFeeds, profiles).recommend(limit = 6)
    }
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _subscribing = MutableStateFlow<Set<String>>(emptySet())

    /** Адреса лент, на которые идёт подписка прямо сейчас (для индикатора на кнопке). */
    val subscribing: StateFlow<Set<String>> = _subscribing.asStateFlow()

    /**
     * Пробная загрузка лент каталога, чтобы оценить, какие из них подходят пользователю.
     * Без сигналов (нет ни подписок, ни закладок, ни прочитанного) не качаем ничего.
     */
    fun loadPreviews() {
        val hasSignals = RssRepository.sources.value.isNotEmpty() ||
                RssRepository.articles.value.any { it.isBookmarked || it.isRead }
        if (!hasSignals) return

        viewModelScope.launch {
            RssRepository.loadSourcePreviews(PopularSources.topFeeds)
        }
    }

    fun subscribe(source: Source) {
        if (source.domain in _subscribing.value) return
        _subscribing.update { it + source.domain }
        viewModelScope.launch {
            try {
                RssRepository.addFeed(source.domain, null)
            } finally {
                _subscribing.update { it - source.domain }
            }
        }
    }
}