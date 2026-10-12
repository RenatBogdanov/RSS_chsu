package com.example.rss_chsu.ui.screens

import androidx.compose.runtime.Composable

@Composable
fun FeedScreen(
    onNavClick: () -> Unit = {},
    onMenuClick: () -> Unit = {}
) {
    TodayFeedScreen(
        onArticleClick = { },
        onFilterClick = onMenuClick
    )
}
