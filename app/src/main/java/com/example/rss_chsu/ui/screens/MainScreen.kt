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
import com.example.rss_chsu.ui.model.RssRepository
import kotlinx.coroutines.flow.flowOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    feedViewModel: FeedViewModel = viewModel()
) {
    var currentTab by remember { mutableStateOf(NavTab.TODAY) }
    var selectedArticle by remember { mutableStateOf<Article?>(null) }
    var showSettingsSheet by remember { mutableStateOf(false) }

    // Полный текст читаем отдельно и только для открытой статьи: в списках он обрезан.
    // Этот же поток обновляется при смене закладки и статуса «прочитано».
    // Подписки на весь список статей здесь больше нет, поэтому MainScreen не перерисовывается при каждом его изменении.
    val articleFlow = remember(selectedArticle?.id) {
        val id = selectedArticle?.id
        if (id == null) flowOf<Article?>(null) else RssRepository.observeArticle(id)
    }
    val loadedArticle by articleFlow.collectAsState(initial = selectedArticle)

    // loadedArticle может на мгновение остаться от предыдущей статьи, поэтому сверяем id
    val openedArticle = selectedArticle?.let { selected ->
        loadedArticle?.takeIf { it.id == selected.id } ?: selected
    }

    LaunchedEffect(selectedArticle?.id) {
        selectedArticle?.id?.let { feedViewModel.onArticleRead(it) }
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
                            onMenuClick = { showSettingsSheet = true },
                            onNavClick = { article -> selectedArticle = article }
                        )
                        NavTab.SUBSCRIPTIONS -> SubscriptionsScreen(
                            onAddSourceClick = { currentTab = NavTab.SEARCH },
                            onMenuClick = { showSettingsSheet = true }
                        )
                        NavTab.SEARCH -> SearchSourcesScreen()
                        NavTab.SAVED -> SavedArticlesScreen(
                            onArticleClick = { selectedArticle = it },
                            onMenuClick = { showSettingsSheet = true }
                        )
                        NavTab.PROFILE -> Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = androidx.compose.ui.Alignment.Center
                        ) {
                            Text("Профиль (в разработке)")
                        }
                    }
                }
            }

            if (openedArticle != null) {
                BackHandler {
                    selectedArticle = null
                }

                ArticleReaderScreen(
                    article = openedArticle,
                    onBackClick = { selectedArticle = null },
                    onBookmarkClick = { feedViewModel.onBookmarkClick(openedArticle.id) },
                    onReadToggle = { feedViewModel.onReadToggle(openedArticle.id) }
                )
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