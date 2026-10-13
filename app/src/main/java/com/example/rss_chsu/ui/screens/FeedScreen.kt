package com.example.rss_chsu.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rss_chsu.ui.model.Article
import com.example.rss_chsu.ui.model.FeedViewModel

@Composable
fun FeedScreen(
    onNavClick: (Article) -> Unit = {}, // Меняем тип параметра, чтобы передавать статью наружу
    onMenuClick: () -> Unit = {},
    feedViewModel: FeedViewModel = viewModel()
) {
    val articlesList by feedViewModel.articles.collectAsState()

    TodayFeedScreen(
        articlesList = articlesList,
        onArticleClick = { article ->
            feedViewModel.onArticleRead(article.id)
            onNavClick(article) // Передаем кликнутую статью в MainScreen
        },
        onBookmarkClick = { article ->
            feedViewModel.onBookmarkClick(article.id)
        },
        onFilterClick = onMenuClick
    )
}
