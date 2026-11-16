package com.example.rss_chsu.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import com.example.rss_chsu.ui.components.TopAppLogoBar
import com.example.rss_chsu.ui.model.Article
import com.example.rss_chsu.ui.model.FeedViewModel
import com.example.rss_chsu.ui.model.RssRepository
import com.example.rss_chsu.ui.theme.*

@Composable
fun SavedArticlesScreen(
    onArticleClick: (Article) -> Unit,
    onMenuClick: () -> Unit = {},
    feedViewModel: FeedViewModel = viewModel()
) {
    val savedFlow = remember(feedViewModel) { feedViewModel.pagedBookmarks() }
    val savedArticles = savedFlow.collectAsLazyPagingItems()
    val counts by feedViewModel.articleCounts.collectAsState()
    val sourcesList by RssRepository.sources.collectAsState()
    val sourceNameEntries = remember(sourcesList) {
        sourcesList.map { it.name to it.domain }
    }
    val sourceNameIndex = remember(sourceNameEntries) {
        SourceNameResolver.indexForEntries(sourceNameEntries)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppLogoBar(
            title = "ЗАКЛАДКИ (${counts.bookmarks})",
            rightAction = {
                IconButton(onClick = onMenuClick, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Меню",
                        tint = DarkPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        )

        when {
            savedArticles.loadState.refresh is LoadState.Loading && savedArticles.itemCount == 0 -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = DarkPrimary)
                }
            }
            savedArticles.itemCount == 0 && savedArticles.loadState.refresh is LoadState.Error -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    TextButton(onClick = { savedArticles.retry() }) {
                        Text("Не удалось загрузить закладки. Повторить", color = DarkPrimary)
                    }
                }
            }
            savedArticles.itemCount == 0 && savedArticles.loadState.refresh is LoadState.NotLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Нет сохраненных статей",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Сохраняйте интересные статьи в закладки, чтобы вернуться к ним в любой момент.",
                            fontSize = 14.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 120.dp)
                ) {
                    items(
                        count = savedArticles.itemCount,
                        key = savedArticles.itemKey { it.id },
                        contentType = savedArticles.itemContentType { "saved_article" }
                    ) { index ->
                        // Элемент уже преобразован из ArticleListItem в Article
                        // на Dispatchers.Default внутри ArticlePagingRepository.
                        val article = savedArticles[index]
                        if (article != null) {
                            SwipeToDeleteBookmarkContainer(
                                onDelete = { feedViewModel.onBookmarkClick(article.id) }
                            ) {
                                FeedArticleRow(
                                    article = article,
                                    sourceNameIndex = sourceNameIndex,
                                    onClick = { onArticleClick(article) }
                                )
                            }
                        }
                    }

                    if (savedArticles.loadState.append is LoadState.Loading) {
                        item(key = "saved_loading") {
                            Box(Modifier.fillMaxWidth().padding(20.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(color = DarkPrimary)
                            }
                        }
                    }
                    if (savedArticles.loadState.append is LoadState.Error) {
                        item(key = "saved_retry") {
                            TextButton(onClick = { savedArticles.retry() }, modifier = Modifier.fillMaxWidth()) {
                                Text("Не удалось загрузить. Повторить", color = DarkPrimary)
                            }
                        }
                    }
                    val append = savedArticles.loadState.append
                    if (append is LoadState.NotLoading && append.endOfPaginationReached && savedArticles.itemCount > 0) {
                        item(key = "saved_end") {
                            Text(
                                text = "Конец списка.",
                                modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                                fontSize = 14.sp,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeToDeleteBookmarkContainer(
    onDelete: () -> Unit,
    content: @Composable () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else false
        },
        positionalThreshold = { it * 0.5f }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            val color by animateColorAsState(
                targetValue = when (dismissState.targetValue) {
                    SwipeToDismissBoxValue.EndToStart -> Color(0xFFFC4F4F)
                    else -> MaterialTheme.colorScheme.background
                },
                label = "color"
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color)
                    .padding(end = 16.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) {
                    Text("Удалить", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
        content = {
            Box(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
                content()
            }
        }
    )
}
