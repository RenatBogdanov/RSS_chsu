package com.example.rss_chsu.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.rss_chsu.ui.components.NavTab
import com.example.rss_chsu.ui.components.RSSBottomBar
import com.example.rss_chsu.ui.model.Article

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    var currentTab by remember { mutableStateOf(NavTab.TODAY) }
    var selectedArticle by remember { mutableStateOf<Article?>(null) }
    var showSettingsSheet by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (selectedArticle != null) {
            // Перехватываем системную кнопку «Назад» на смартфоне
            BackHandler {
                selectedArticle = null // Закрываем статью и возвращаемся к текущей вкладке
            }

            ArticleReaderScreen(
                article = selectedArticle!!,
                onBackClick = { selectedArticle = null }
            )
        } else {
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
                        // Сюда мы аккуратно прописали передачу статьи: selectedArticle = it
                        NavTab.TODAY -> FeedScreen(
                            onMenuClick = { showSettingsSheet = true },
                            onNavClick = { article -> selectedArticle = article }
                        )
                        NavTab.SUBSCRIPTIONS -> SubscriptionsScreen(
                            onAddSourceClick = { currentTab = NavTab.SEARCH },
                            onMenuClick = { showSettingsSheet = true }
                        )
                        NavTab.RECOMMENDATIONS -> RecommendationsScreen(
                            onArticleClick = { selectedArticle = it },
                            onFilterClick = { showSettingsSheet = true }
                        )
                        NavTab.SEARCH -> SearchSourcesScreen()
                        NavTab.SAVED -> SavedArticlesScreen(
                            onArticleClick = { selectedArticle = it },
                            onMenuClick = { showSettingsSheet = true }
                        )
                    }
                }
            }
        }

        if (showSettingsSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSettingsSheet = false },
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                SettingsSheet(onDismiss = { showSettingsSheet = false })
            }
        }
    }
}
