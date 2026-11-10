package com.example.rss_chsu.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rss_chsu.ui.components.TopAppLogoBar
import com.example.rss_chsu.ui.model.Article
import com.example.rss_chsu.ui.model.FeedViewModel
import com.example.rss_chsu.ui.theme.*

@Composable
fun SavedArticlesScreen(
    onArticleClick: (Article) -> Unit,
    onMenuClick: () -> Unit = {},
    feedViewModel: FeedViewModel = viewModel()
) {
    val allArticles by feedViewModel.articles.collectAsState()
    val savedArticles = allArticles.filter { it.isBookmarked }
    val listState = rememberLazyListState()

    // Compose LazyColumn автоматически отрисовывает элементы только когда они появляются на экране

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppLogoBar(
            rightAction = {
                IconButton(onClick = onMenuClick) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Меню",
                        tint = DarkPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        )

        if (savedArticles.isEmpty()) {
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
                            .padding(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 4.dp)
                    ) {
                        Text(
                            text = "Закладки",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Сохранено статей: ${savedArticles.size}",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                    }
                }

                items(savedArticles, key = { it.id }) { article ->
                    FeedArticleRow(
                        article = article,
                        onClick = { onArticleClick(article) }
                    )
                }

                item(key = "end_of_list_indicator") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp, horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Конец списка.",
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
