package com.example.rss_chsu.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rss_chsu.ui.components.NavTab
import com.example.rss_chsu.ui.components.RSSBottomBar
import com.example.rss_chsu.ui.model.Article
import com.example.rss_chsu.ui.model.FeedViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    feedViewModel: FeedViewModel = viewModel()
) {
    var currentTab by remember { mutableStateOf(NavTab.TODAY) }
    var selectedArticleId by remember { mutableStateOf<String?>(null) }

    val articles by feedViewModel.articles.collectAsState()
    val articleStatuses by feedViewModel.articleStatuses.collectAsState() // <-- ДОБАВИТЬ ЭТУ СТРОКУ


    LaunchedEffect(articles, selectedArticleId) {
        val id = selectedArticleId ?: return@LaunchedEffect
        if (articles.none { it.id == id }) selectedArticleId = null
    }

    LaunchedEffect(selectedArticleId) {
        selectedArticleId?.let { feedViewModel.onArticleRead(it) }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                bottomBar = {
                    RSSBottomBar(
                        currentTab = currentTab,
                        onTabSelected = { currentTab = it }
                    )
                }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    when (currentTab) {
                        NavTab.TODAY -> FeedScreen(
                            onNavClick = { article -> selectedArticleId = article.id }
                        )
                        NavTab.SUBSCRIPTIONS -> SubscriptionsScreen(
                            onAddSourceClick = { currentTab = NavTab.SEARCH }
                        )
                        NavTab.SEARCH -> SearchSourcesScreen()
                        NavTab.SAVED -> SavedArticlesScreen(
                            onArticleClick = { selectedArticleId = it.id }
                        )
                        NavTab.SETTINGS -> SettingsScreen()
                    }
                }
            }

            if (selectedArticleId != null) {
                BackHandler { selectedArticleId = null }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ArticleReaderScreen(
                        articles = articles,
                        initialArticleId = selectedArticleId,
                        articleStatuses = articleStatuses, // <-- ДОБАВИТЬ ЭТУ СТРОКУ
                        onClose = { selectedArticleId = null },
                        onArticleChanged = { newId -> selectedArticleId = newId },
                        onBookmarkClick = { id -> feedViewModel.toggleBookmark(id) },
                        onReadArticle = { id -> feedViewModel.onArticleRead(id) }
                    )
                }
            }
        }
    }
}