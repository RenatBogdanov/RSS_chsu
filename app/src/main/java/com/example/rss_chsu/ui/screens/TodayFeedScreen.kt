package com.example.rss_chsu.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.rss_chsu.ui.components.TopAppLogoBar
import com.example.rss_chsu.ui.model.Article
import com.example.rss_chsu.ui.model.SampleData
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

    // Логика фильтрации статей на основе вкладок
    val filteredArticles = when (selectedFilterIndex) {
        1 -> articlesList.filter { !it.isFeatured }
        2 -> articlesList.filter { !it.isFeatured && !it.isRead }
        else -> articlesList.filter { !it.isFeatured }
    }

    // Находим главную статью в живом списке
    val featured = articlesList.find { it.isFeatured } ?: SampleData.featuredArticle

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

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            // Header Title & Date
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Сегодня",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Понедельник, 12 октября · ${filteredArticles.size} новых статей",
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
            item {
                FeaturedArticleCard(
                    article = featured,
                    onClick = { onArticleClick(featured) },
                    onBookmarkClick = { onBookmarkClick(featured) }
                )
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
            items(filteredArticles) { article ->
                FeedArticleRow(
                    article = article,
                    onClick = { onArticleClick(article) }
                )
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
                .background(Color(0xFF8F9E8B)),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF6E816A).copy(alpha = 0.85f))
            ) {
                Text(
                    text = "🏙️",
                    fontSize = 48.sp,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

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
                onClick = onBookmarkClick, // Логика идет сразу в репозиторий
                modifier = Modifier.size(28.dp)
            ) {
                Icon(
                    imageVector = if (article.isBookmarked) RssIcons.BookmarkFilled else RssIcons.Bookmark,
                    contentDescription = "Сохранить",
                    tint = TextPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = article.title,
            fontSize = 22.sp,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            lineHeight = 28.sp
        )

        Spacer(modifier = Modifier.height(6.dp))

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
            text = "1 час назад · ${article.readTime}",
            fontSize = 12.sp,
            color = TextSecondary
        )
    }
}

@Composable
fun FeedArticleRow(
    article: Article,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "${article.sourceName} · ${article.category}",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (article.isRead) TextSecondary.copy(alpha = 0.5f) else TextSecondary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = article.title,
                fontSize = 15.sp,
                fontWeight = if (article.isRead) FontWeight.Normal else FontWeight.Bold, // Текст тускнеет, если прочитано
                color = if (article.isRead) TextPrimary.copy(alpha = 0.6f) else TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 20.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "${article.date} · ${article.readTime}",
                fontSize = 12.sp,
                color = TextSecondary
            )
        }
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFD6DAD8)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = when {
                    article.category.contains("Наука") -> "🛰️"
                    article.category.contains("Образ") -> "☕"
                    article.category.contains("Культура") -> "📚"
                    article.category.contains("Дизайн") -> "🎨"
                    article.category.contains("Бизнес") -> "📈"
                    else -> "📰"
                },
                fontSize = 24.sp
            )
        }
    }
}