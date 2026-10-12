package com.example.rss_chsu.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rss_chsu.ui.components.RssIcons
import com.example.rss_chsu.ui.model.Article
import com.example.rss_chsu.ui.model.SampleData
import com.example.rss_chsu.ui.theme.*

@Composable
fun SavedArticlesScreen(
    onArticleClick: (Article) -> Unit,
    onMenuClick: () -> Unit = {}
) {
    var selectedFilterIndex by remember { mutableIntStateOf(0) }
    val filterOptions = listOf("Все · 4", "Непрочитанные")

    val savedArticles = SampleData.articlesList.filter { it.isBookmarked || it.id == "1" || it.id == "3" || it.id == "4" || it.id == "5" }

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
                            text = "Сохранённое",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        IconButton(onClick = onMenuClick) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Меню",
                                tint = DarkPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "4 статьи · хорошие тексты не теряются",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }
            }

            // Filter Chips
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

            // Section Header: Читать позже | Сначала новые
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Читать позже",
                        fontSize = 15.sp,
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

            // Saved Articles
            items(savedArticles.take(4)) { article ->
                SavedArticleRow(
                    article = article,
                    onClick = { onArticleClick(article) }
                )
            }

            // Bottom Info Note Box
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 20.dp)
                        .clip(RoundedCornerShape(14.dp)),
                    color = LightSurfaceContainer
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = RssIcons.BookmarkFilled,
                            contentDescription = null,
                            tint = DarkPrimary,
                            modifier = Modifier.size(20.dp)
                        )

                        Text(
                            text = "Сохраняйте статьи из любой ленты, чтобы вернуться к ним в удобный момент.",
                            fontSize = 13.sp,
                            color = TextSecondary,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SavedArticleRow(
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
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = article.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "${article.date} · ${article.readTime}",
                    fontSize = 12.sp,
                    color = TextSecondary
                )

                Icon(
                    imageVector = RssIcons.BookmarkFilled,
                    contentDescription = null,
                    tint = TextPrimary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        // Thumbnail placeholder
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(ChipUnselected),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = when {
                    article.category.contains("Наука") -> "🛰️"
                    article.category.contains("Образ") -> "☕"
                    article.category.contains("Культура") -> "📚"
                    else -> "🏙️"
                },
                fontSize = 24.sp
            )
        }
    }
}
