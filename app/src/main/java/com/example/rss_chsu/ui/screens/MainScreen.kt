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
    // viewModel() здесь и в экранах вкладок возвращает один и тот же экземпляр (общий владелец — Activity)
    feedViewModel: FeedViewModel = viewModel()
) {
    var currentTab by remember { mutableStateOf(NavTab.TODAY) }
    var selectedArticle by remember { mutableStateOf<Article?>(null) }
    var showSettingsSheet by remember { mutableStateOf(false) }

    val articles by feedViewModel.articles.collectAsState()

    // Открытая статья — это снимок на момент клика. Берём её актуальную версию из БД,
    // чтобы закладка и «прочитано» сразу отображались в читалке.
    // Если статьи уже нет в кэше, показываем снимок.
    val openedArticle = selectedArticle?.let { selected ->
        articles.firstOrNull { it.id == selected.id } ?: selected
    }

    // Любая открытая статья (из ленты, закладок или рекомендаций) считается прочитанной
    LaunchedEffect(selectedArticle?.id) {
        selectedArticle?.id?.let { feedViewModel.onArticleRead(it) }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        if (openedArticle != null) {
            // Перехватываем системную кнопку «Назад» на смартфоне
            BackHandler {
                selectedArticle = null // Закрываем статью и возвращаемся к текущей вкладке
            }

            ArticleReaderScreen(
                article = openedArticle,
                onBackClick = { selectedArticle = null },
                onBookmarkClick = { feedViewModel.onBookmarkClick(openedArticle.id) },
                onReadToggle = { feedViewModel.onReadToggle(openedArticle.id) }
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