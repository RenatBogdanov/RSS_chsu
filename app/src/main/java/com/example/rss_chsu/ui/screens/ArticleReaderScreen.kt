package com.example.rss_chsu.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.rss_chsu.ui.components.RssIcons
import com.example.rss_chsu.ui.model.Article
import com.example.rss_chsu.ui.theme.*

@Composable
fun ArticleReaderScreen(
    article: Article,
    onBackClick: () -> Unit,
    onBookmarkClick: () -> Unit = {},
    onReadToggle: () -> Unit = {}
) {
    // Состояние берётся из актуальной статьи (БД), а не хранится локально
    val isSaved = article.isBookmarked
    val isRead = article.isRead

    val context = LocalContext.current
    val browserIntent = remember(article.id) {
        try {
            if (article.id.startsWith("http")) Intent(Intent.ACTION_VIEW, Uri.parse(article.id)) else null
        } catch (_: Exception) {
            null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Top Navigation Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back Button: "< Сегодня"
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { onBackClick() }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Назад",
                        tint = DarkPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Сегодня",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = DarkPrimary
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Typography "Aa" button
                    IconButton(onClick = { }, modifier = Modifier.size(36.dp)) {
                        Text(
                            text = "Aa",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkPrimary
                        )
                    }

                    // More button "..."
                    IconButton(onClick = { }, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Опции",
                            tint = DarkPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Article Content Scroll View
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                // Category Tag
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ChipUnselected,
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    Text(
                        text = article.category.uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        letterSpacing = 0.5.sp
                    )
                }

                // Article Title (Serif, Elegant)
                Text(
                    text = article.title,
                    fontSize = 28.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    lineHeight = 36.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Article Subtitle / Snippet (Avoid duplication if content starts with subtitle)
                val cleanSub = article.subtitle.trim()
                val cleanCont = article.content.trim()
                if (cleanSub.isNotEmpty() && cleanSub != cleanCont && !cleanCont.startsWith(cleanSub.take(30))) {
                    Text(
                        text = article.subtitle,
                        fontSize = 16.sp,
                        color = TextSecondary,
                        lineHeight = 22.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Source & Author
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "↗",
                        fontSize = 16.sp,
                        color = TextSecondary
                    )
                    Column {
                        Text(
                            text = "${article.sourceName} · ${if (article.author.isNotEmpty()) article.author else "Редакция"}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = if (article.pubDateFormatted.isNotEmpty()) article.pubDateFormatted else article.date,
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Hero Image Box
                if (article.imageUrl.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(ChipUnselected),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = article.imageUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Article Body Content (Real Content)
                Text(
                    text = if (article.content.isNotEmpty()) article.content else article.title,
                    fontSize = 16.sp,
                    fontFamily = FontFamily.Serif,
                    color = TextPrimary,
                    lineHeight = 26.sp
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Read Full Article Link
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable {
                            if (browserIntent != null) {
                                try {
                                    context.startActivity(browserIntent)
                                } catch (_: Exception) {
                                    // ignore
                                }
                            }
                        }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Читать полностью на сайте",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkPrimary
                    )
                    Text(
                        text = "↗",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkPrimary
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))
            }

            // Bottom Reading Bar
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        drawLine(
                            color = LightCardBorder,
                            start = Offset(0f, 0f),
                            end = Offset(size.width, 0f),
                            strokeWidth = 1.dp.toPx()
                        )
                    },
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Bookmark / Save action
                    Row(
                        modifier = Modifier
                            .clickable { onBookmarkClick() }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (isSaved) RssIcons.BookmarkFilled else RssIcons.Bookmark,
                            contentDescription = "Сохранено",
                            tint = DarkPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = if (isSaved) "Сохранено" else "Сохранить",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = DarkPrimary
                        )
                    }

                    // Share
                    IconButton(onClick = { }, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Поделиться",
                            tint = DarkPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Mark as read (Checkmark)
                    IconButton(
                        onClick = onReadToggle,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Прочитано",
                            tint = if (isRead) DarkPrimary else TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Next article arrow
                    IconButton(onClick = { }, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Следующая статья",
                            tint = DarkPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}