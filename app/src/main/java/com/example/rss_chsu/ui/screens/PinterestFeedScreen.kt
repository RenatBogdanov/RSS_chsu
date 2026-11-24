package com.example.rss_chsu.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.rss_chsu.ui.components.HoldSwipeAction
import com.example.rss_chsu.ui.components.RssIcons
import com.example.rss_chsu.ui.components.SwipeActionConfig
import com.example.rss_chsu.ui.components.TopAppLogoBar
import com.example.rss_chsu.ui.model.Article
import com.example.rss_chsu.ui.model.FeedViewModel
import com.example.rss_chsu.ui.model.RssRepository
import com.example.rss_chsu.ui.theme.*
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PinterestFeedScreen(
    feedViewModel: FeedViewModel,
    onArticleClick: (Article) -> Unit,
    onBookmarkClick: (Article) -> Unit
) {
    val personalizedArticles by feedViewModel.personalizedArticles.collectAsState()
    val articleStatuses by feedViewModel.articleStatuses.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    var isRefreshing by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppLogoBar(
            title = "ДЛЯ ВАС",
            rightAction = {
                IconButton(
                    onClick = {
                        if (!isRefreshing) coroutineScope.launch {
                            isRefreshing = true
                            RssRepository.refreshAllSources()
                            isRefreshing = false
                        }
                    },
                    enabled = !isRefreshing,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Обновить",
                        tint = DarkPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        )

        if (isRefreshing) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth().height(3.dp),
                color = DarkPrimary
            )
        }

        if (personalizedArticles.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "Пока нет рекомендаций.\nДобавьте источники во вкладке «Подписки».",
                    color = TextSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    lineHeight = 20.sp
                )
            }
        } else {
            LazyVerticalStaggeredGrid(
                columns = StaggeredGridCells.Fixed(2),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(8.dp, 8.dp, 8.dp, 120.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalItemSpacing = 8.dp
            ) {
                items(personalizedArticles, key = { it.id }) { article ->
                    val isBookmarked = articleStatuses[article.id]?.isBookmarked
                        ?: article.isBookmarked
                    PinterestCard(
                        article = article,
                        isBookmarked = isBookmarked,
                        onClick = { onArticleClick(article) },
                        onBookmarkClick = { onBookmarkClick(article) }
                    )
                }
            }
        }
    }
}

@Composable
private fun PinterestCard(
    article: Article,
    isBookmarked: Boolean,
    onClick: () -> Unit,
    onBookmarkClick: () -> Unit
) {
    val context = LocalContext.current
    val currentFont = LocalAppFont.current.toFontFamily()

    val aspectRatio = remember(article.id) {
        0.62f + (article.id.hashCode().absoluteValue % 30) / 100f
    }

    val actions = remember(isBookmarked) {
        listOf(
            SwipeActionConfig(
                id = "save",
                icon = if (isBookmarked) RssIcons.BookmarkFilled else RssIcons.Bookmark,
                label = if (isBookmarked) "Убрать из закладок" else "Сохранить"
            ),
            SwipeActionConfig(
                id = "share",
                icon = Icons.Default.Share,
                label = "Поделиться"
            ),
            SwipeActionConfig(
                id = "open",
                icon = RssIcons.Subscriptions,
                label = "Открыть оригинал"
            )
        )
    }

    val onAction: (SwipeActionConfig) -> Unit = { action ->
        when (action.id) {
            "save" -> {
                onBookmarkClick()
                Toast.makeText(
                    context,
                    if (isBookmarked) "Убрано из закладок" else "Добавлено в закладки",
                    Toast.LENGTH_SHORT
                ).show()
            }
            "share" -> {
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, article.title + "\n" + article.id)
                }
                try {
                    context.startActivity(
                        Intent.createChooser(shareIntent, "Поделиться")
                    )
                } catch (_: Exception) {
                    Toast.makeText(context, "Не удалось поделиться", Toast.LENGTH_SHORT).show()
                }
            }
            "open" -> {
                if (article.id.startsWith("http")) {
                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW, article.id.toUri()))
                    } catch (_: Exception) {
                        Toast.makeText(context, "Не удалось открыть ссылку", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    onClick()
                }
            }
        }
    }

    HoldSwipeAction(
        actions = actions,
        onAction = onAction,
        fanRotationDeg = -45f,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(aspectRatio)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF2F3542), Color(0xFF1E1E24))
                        )
                    )
                    .clickable(onClick = onClick)
            ) {
                if (article.imageUrl.isNotBlank()) {
                    val imageModel = remember(article.imageUrl) {
                        ImageRequest.Builder(context)
                            .data(article.imageUrl)
                            .crossfade(150)
                            .build()
                    }
                    AsyncImage(
                        model = imageModel,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .height(72.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.55f))
                            )
                        )
                )

                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.45f))
                        .clickable { onBookmarkClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isBookmarked) RssIcons.BookmarkFilled else RssIcons.Bookmark,
                        contentDescription = "В закладки",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Row(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.85f))
                    )
                    Text(
                        text = article.sourceName,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            Text(
                text = article.title,
                fontSize = 13.sp,
                fontFamily = currentFont,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                lineHeight = 17.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(2.dp))

            Text(
                text = article.date,
                fontSize = 11.sp,
                color = TextSecondary,
                maxLines = 1
            )

            Spacer(Modifier.height(10.dp))
        }
    }
}

private fun String.toUri() = android.net.Uri.parse(this)