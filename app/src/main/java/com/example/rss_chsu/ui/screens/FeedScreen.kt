package com.example.rss_chsu.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rss_chsu.ui.model.Article
import com.example.rss_chsu.ui.model.FeedViewModel
import com.example.rss_chsu.ui.model.RssRepository

@Composable
fun FeedScreen(
    onNavClick: (Article) -> Unit = {},
    onMenuClick: () -> Unit = {},
    feedViewModel: FeedViewModel = viewModel()
) {
    val personalizedArticles by feedViewModel.personalizedArticles.collectAsState()
    val articleCounts by feedViewModel.articleCounts.collectAsState()
    val sourcesList by RssRepository.sources.collectAsState()
    val foldersList by RssRepository.folders.collectAsState()

    TodayFeedScreen(
        personalizedArticles = personalizedArticles,
        totalArticleCount = articleCounts.total,
        unreadArticleCount = articleCounts.unread,
        sourcesList = sourcesList,
        foldersList = foldersList,
        feedViewModel = feedViewModel,
        onArticleClick = { article -> onNavClick(article) },
        onBookmarkClick = { article -> feedViewModel.onBookmarkClick(article.id) },
        onFilterClick = onMenuClick
    )
}
