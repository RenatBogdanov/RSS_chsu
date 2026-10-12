package com.example.rss_chsu.ui.screens

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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rss_chsu.ui.components.RssIcons
import com.example.rss_chsu.ui.model.Article
import com.example.rss_chsu.ui.theme.*

@Composable
fun ArticleReaderScreen(
    article: Article,
    onBackClick: () -> Unit
) {
    var isSaved by remember { mutableStateOf(article.isBookmarked) }
    var isRead by remember { mutableStateOf(article.isRead) }

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

                // Article Subtitle / Snippet
                if (article.subtitle.isNotEmpty()) {
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
                            text = "${article.date} · ${article.readTime}",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Hero Image Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF8F9E8B)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFF6E816A).copy(alpha = 0.85f))
                    ) {
                        Text(
                            text = "🌿",
                            fontSize = 54.sp,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Пешеходная улица в Копенгагене · Фото: редакция",
                    fontSize = 11.sp,
                    fontStyle = FontStyle.Italic,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Article Body Content
                Text(
                    text = "Утром город звучит иначе, если в нём есть место для прогулки. Вместо шума машин — разговоры у кофейни, шелест листьев и звон велосипедов.",
                    fontSize = 16.sp,
                    fontFamily = FontFamily.Serif,
                    color = TextPrimary,
                    lineHeight = 26.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Улица как место встречи",
                    fontSize = 20.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    lineHeight = 26.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "В Копенгагене удобство пешехода давно стало точкой отсчёта. Деревья дают тень, скамейки приглашают сделать паузу, а первые этажи домов возвращают улице жизнь.",
                    fontSize = 16.sp,
                    fontFamily = FontFamily.Serif,
                    color = TextPrimary,
                    lineHeight = 26.sp
                )

                Spacer(modifier = Modifier.height(28.dp))

                // Read Full Article Link
                Row(
                    modifier = Modifier
                        .clickable { }
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
                            .clickable { isSaved = !isSaved }
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
                        onClick = { isRead = !isRead },
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
