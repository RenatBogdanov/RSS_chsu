package com.example.rss_chsu.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.rss_chsu.ui.components.RssIcons
import com.example.rss_chsu.ui.components.TopAppLogoBar
import com.example.rss_chsu.ui.model.Article
import com.example.rss_chsu.ui.theme.*

@Composable
fun TodayFeedScreen(
    articlesList: List<Article>, // Передаем живые данные
    onArticleClick: (Article) -> Unit,
    onBookmarkClick: (Article) -> Unit, // Передаем событие клика по закладке
    onFilterClick: () -> Unit = {}
) {
    var selectedFilterIndex by remember { mutableIntStateOf(0) }
    val filterOptions = listOf("Для вас", "Все статьи", "Непрочитанные")
    val listState = rememberLazyListState()

    // Логика фильтрации статей на основе вкладок
    val filteredArticles = remember(selectedFilterIndex, articlesList) {
        when (selectedFilterIndex) {
            1 -> articlesList.filter { !it.isFeatured }
            2 -> articlesList.filter { !it.isFeatured && !it.isRead }
            else -> articlesList.filter { !it.isFeatured }
        }
    }

    // Изначально показываем 25 статей, далее догружаем пачками при скролле вниз
    var displayLimit by remember(selectedFilterIndex, filteredArticles.size) {
        mutableIntStateOf(25)
    }

    val visibleArticles = remember(filteredArticles, displayLimit) {
        filteredArticles.take(displayLimit)
    }

    // Автоматическая подгрузка при приближении к концу списка (Infinite Scroll)
    LaunchedEffect(listState, filteredArticles.size) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val totalItemsNumber = layoutInfo.totalItemsCount
            val lastVisibleItemIndex = (layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0)
            totalItemsNumber > 0 && lastVisibleItemIndex >= totalItemsNumber - 4
        }.collect { isNearEnd ->
            if (isNearEnd && displayLimit < filteredArticles.size) {
                displayLimit = (displayLimit + 25).coerceAtMost(filteredArticles.size)
            }
        }
    }

    // Находим главную статью в живом списке
    val featured = articlesList.find { it.isFeatured } ?: articlesList.firstOrNull()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppLogoBar(
            rightAction = {
                IconButton(onClick = onFilterClick) {
                    Icon(
                        imageVector = RssIcons.Tune,
                        contentDescription = "Фильтр",
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
                        text = "Лента пока пуста",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Перейдите во вкладку «Поиск», чтобы добавить RSS-источник.",
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
                // Header Title & Date
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 4.dp)
                    ) {
                        Text(
                            text = "Сегодня",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Новых статей: ${articlesList.size}",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                    }
                }

                // Filter Chips Row
                item {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filterOptions.size) { index ->
                            val isSelected = selectedFilterIndex == index
                            FilterChipPill(
                                text = filterOptions[index],
                                isSelected = isSelected,
                                onClick = { selectedFilterIndex = index }
                            )
                        }
                    }
                }

                // Featured Article Card
                if (featured != null) {
                    item(key = "featured_${featured.id}") {
                        FeaturedArticleCard(
                            article = featured,
                            onClick = { onArticleClick(featured) },
                            onBookmarkClick = { onBookmarkClick(featured) }
                        )
                    }
                }

                // Section Header
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, top = 24.dp, bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "В вашей ленте",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "По времени",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                    }
                }

                // Article List Items
                items(visibleArticles, key = { it.id }) { article ->
                    FeedArticleRow(
                        article = article,
                        onClick = { onArticleClick(article) }
                    )
                }

                // Индикатор динамической подгрузки статей при пролистывании вниз
                if (displayLimit < filteredArticles.size) {
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
                                    text = "Загрузка остальных статей...",
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

@Composable
fun FilterChipPill(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) DarkPrimary else ChipUnselected,
        modifier = Modifier.height(34.dp)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                color = if (isSelected) Color.White else TextPrimary
            )
        }
    }
}

@Composable
fun FeaturedArticleCard(
    article: Article,
    onClick: () -> Unit,
    onBookmarkClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(ChipUnselected),
            contentAlignment = Alignment.Center
        ) {
            if (article.imageUrl.isNotEmpty()) {
                AsyncImage(
                    model = article.imageUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF8F9E8B)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = RssIcons.Subscriptions,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(48.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.1f))
            )

            // Bookmark button top-right
            IconButton(
                onClick = onBookmarkClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.9f))
            ) {
                Icon(
                    imageVector = if (article.isBookmarked) RssIcons.BookmarkFilled else RssIcons.Bookmark,
                    contentDescription = "В закладки",
                    tint = DarkPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "${article.sourceName} · ${article.date}",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary,
            letterSpacing = 0.5.sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = article.title,
            fontSize = 20.sp,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            lineHeight = 26.sp
        )

        if (article.subtitle.isNotEmpty()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = article.subtitle,
                fontSize = 14.sp,
                color = TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 20.sp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
fun FeedArticleRow(
    article: Article,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "${article.sourceName} · ${article.date}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = article.title,
                    fontSize = 15.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 20.sp
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Thumbnail
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(ChipUnselected),
                contentAlignment = Alignment.Center
            ) {
                if (article.imageUrl.isNotEmpty()) {
                    AsyncImage(
                        model = article.imageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = RssIcons.Subscriptions,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}
