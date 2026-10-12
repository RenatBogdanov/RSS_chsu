package com.example.rss_chsu.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rss_chsu.ui.components.RssIcons
import com.example.rss_chsu.ui.model.Article
import com.example.rss_chsu.ui.model.SampleData
import com.example.rss_chsu.ui.model.Source
import com.example.rss_chsu.ui.theme.*

@Composable
fun RecommendationsScreen(
    onArticleClick: (Article) -> Unit,
    onFilterClick: () -> Unit = {}
) {
    var selectedFilterIndex by remember { mutableIntStateOf(0) }
    val topics = listOf("Все", "Интервью", "Обзоры", "Лонгриды", "Дизайн", "Наука")

    val featured = SampleData.recommendedFeatured
    val articles = SampleData.recommendedArticles.filter { !it.isFeatured }
    val channels = remember { mutableStateListOf(*SampleData.recommendedChannels.toTypedArray()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            // Header Title & Subtitle with Action Icon
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Рекомендации",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        IconButton(onClick = onFilterClick) {
                            Icon(
                                imageVector = RssIcons.Recommendations,
                                contentDescription = "Рекомендации",
                                tint = DarkPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Персональный выбор редакций и лучшие каналы",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }
            }

            // Topic Chips Row
            item {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(topics.size) { index ->
                        val isSelected = selectedFilterIndex == index
                        FilterChipPill(
                            text = topics[index],
                            isSelected = isSelected,
                            onClick = { selectedFilterIndex = index }
                        )
                    }
                }
            }

            // Featured Recommendation Hero Card
            item {
                RecommendedHeroCard(
                    article = featured,
                    onClick = { onArticleClick(featured) }
                )
            }

            // Section: Recommended Channels Carousel
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Популярные издания",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Смотреть все",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                    }

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(channels) { channel ->
                            val index = channels.indexOf(channel)
                            RecommendedChannelCard(
                                source = channel,
                                onToggleSubscribe = {
                                    channels[index] = channel.copy(isSubscribed = !channel.isSubscribed)
                                }
                            )
                        }
                    }
                }
            }

            // Section Header: Выбор недели
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 28.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Выбор недели",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Сначала новые",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }
            }

            // Recommended Articles List
            items(articles) { article ->
                FeedArticleRow(
                    article = article,
                    onClick = { onArticleClick(article) }
                )
            }
        }
    }
}

@Composable
fun RecommendedHeroCard(
    article: Article,
    onClick: () -> Unit
) {
    var isBookmarked by remember { mutableStateOf(article.isBookmarked) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        // Hero Graphic Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF3F4E4F)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF2C3639).copy(alpha = 0.9f))
            ) {
                // Editorial Badge inside image
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.2f),
                    modifier = Modifier
                        .padding(12.dp)
                        .align(Alignment.TopStart)
                ) {
                    Text(
                        text = "✨ РЕДАКЦИЯ РЕКОМЕНДУЕТ",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }

                Text(
                    text = "🤖",
                    fontSize = 48.sp,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Category Tag + Bookmark Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${article.sourceName} · ${article.category}",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 0.5.sp
            )

            IconButton(
                onClick = { isBookmarked = !isBookmarked },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = if (isBookmarked) RssIcons.BookmarkFilled else RssIcons.Bookmark,
                    contentDescription = "Сохранить",
                    tint = TextPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Title
        Text(
            text = article.title,
            fontSize = 21.sp,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            lineHeight = 27.sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Subtitle
        Text(
            text = article.subtitle,
            fontSize = 14.sp,
            color = TextSecondary,
            lineHeight = 19.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "${article.date} · ${article.readTime}",
            fontSize = 12.sp,
            color = TextSecondary
        )
    }
}

@Composable
fun RecommendedChannelCard(
    source: Source,
    onToggleSubscribe: () -> Unit
) {
    Surface(
        modifier = Modifier
            .width(200.dp)
            .clip(RoundedCornerShape(14.dp)),
        color = LightSurfaceContainer
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(ChipUnselected),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = source.logoText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                if (source.isSubscribed) {
                    Surface(
                        shape = CircleShape,
                        color = ChipUnselected,
                        modifier = Modifier.clickable { onToggleSubscribe() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = DarkPrimary,
                            modifier = Modifier
                                .padding(6.dp)
                                .size(14.dp)
                        )
                    }
                } else {
                    IconButton(
                        onClick = onToggleSubscribe,
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(DarkPrimary)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Подписаться",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = source.name,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Text(
                text = source.domain,
                fontSize = 11.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = source.description,
                fontSize = 12.sp,
                color = TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 16.sp
            )
        }
    }
}
