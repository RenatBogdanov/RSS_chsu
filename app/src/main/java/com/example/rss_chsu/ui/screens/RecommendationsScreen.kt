package com.example.rss_chsu.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rss_chsu.ui.components.RssIcons
import com.example.rss_chsu.ui.components.TopAppLogoBar
import com.example.rss_chsu.ui.model.Article
import com.example.rss_chsu.ui.model.FeedViewModel
import com.example.rss_chsu.ui.theme.*

@Composable
fun RecommendationsScreen(
    onArticleClick: (Article) -> Unit,
    onFilterClick: () -> Unit = {},
    feedViewModel: FeedViewModel = viewModel()
) {
    val articlesList by feedViewModel.personalizedArticles.collectAsState()
    val listState = rememberLazyListState()

    var displayLimit by remember(articlesList.size) {
        mutableIntStateOf(25)
    }

    val visibleArticles = remember(articlesList, displayLimit) {
        articlesList.take(displayLimit)
    }

    LaunchedEffect(listState, articlesList.size) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val totalItemsNumber = layoutInfo.totalItemsCount
            val lastVisibleItemIndex = (layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0)
            totalItemsNumber > 0 && lastVisibleItemIndex >= totalItemsNumber - 4
        }.collect { isNearEnd ->
            if (isNearEnd && displayLimit < articlesList.size) {
                displayLimit = (displayLimit + 25).coerceAtMost(articlesList.size)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppLogoBar(
            rightAction = {
                IconButton(onClick = onFilterClick) {
                    Icon(
                        imageVector = RssIcons.Recommendations,
                        contentDescription = "Рекомендации",
                        tint = DarkPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        )

        if (articlesList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Рекомендации пока пусты",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Добавьте больше RSS-источников во вкладке «Поиск», чтобы сформировать персональные рекомендации.",
                        fontSize = 14.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 4.dp)
                    ) {
                        Text(
                            text = "Рекомендации",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                }

                items(visibleArticles, key = { it.id }) { article ->
                    FeedArticleRow(
                        article = article,
                        onClick = { onArticleClick(article) }
                    )
                }

                if (displayLimit < articlesList.size) {
                    item(key = "loading_more_indicator") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = DarkPrimary,
                                    strokeWidth = 2.dp
                                )
                                Text(
                                    text = "Загрузка остальных рекомендаций...",
                                    fontSize = 13.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
