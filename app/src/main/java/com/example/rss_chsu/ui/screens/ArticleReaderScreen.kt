package com.example.rss_chsu.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
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
    val isSaved = article.isBookmarked
    val isRead = article.isRead
    var showDetails by remember(article.id) { mutableStateOf(false) }

    val context = LocalContext.current
    val articleUrl = remember(article.id) {
        if (article.id.startsWith("http://") || article.id.startsWith("https://")) article.id else null
    }
    val browserIntent = remember(articleUrl) {
        articleUrl?.let {
            try {
                Intent(Intent.ACTION_VIEW, Uri.parse(it))
            } catch (_: Exception) {
                null
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Компактная верхняя навигационная панель
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onBackClick)
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
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                IconButton(
                    onClick = { showDetails = !showDetails },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Сведения о статье",
                        tint = if (showDetails) DarkPrimary else TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(onClick = { }, modifier = Modifier.size(36.dp)) {
                    Text(
                        text = "Aa",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkPrimary
                    )
                }

                IconButton(onClick = { }, modifier = Modifier.size(36.dp)) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Опции",
                        tint = DarkPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Контент статьи — текст начинается максимально высоко
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 6.dp)
        ) {
            // Рубрика + одна компактная строка метаданных
            val dateLabel = if (article.pubDateFormatted.isNotBlank()) article.pubDateFormatted else article.date
            val metaParts = listOfNotNull(
                article.sourceName.takeIf { it.isNotBlank() },
                dateLabel.takeIf { it.isNotBlank() },
                article.readTime.takeIf { it.isNotBlank() }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = ChipUnselected
                ) {
                    Text(
                        text = article.category.uppercase(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }

                Text(
                    text = metaParts.joinToString(" · "),
                    fontSize = 12.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Аккуратный заголовок
            Text(
                text = article.title,
                fontSize = 22.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                lineHeight = 28.sp
            )

            // Скрытый по умолчанию блок с автором, источником и ссылкой на оригинал
            if (showDetails) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Источник: ${article.sourceName}",
                            fontSize = 12.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = "Автор: ${article.author.ifBlank { "Редакция" }}",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                        if (browserIntent != null) {
                            Text(
                                text = "Открыть оригинал на сайте",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = DarkPrimary,
                                modifier = Modifier
                                    .clickable {
                                        try {
                                            context.startActivity(browserIntent)
                                        } catch (_: Exception) {
                                        }
                                    }
                                    .padding(top = 2.dp)
                            )
                        }
                    }
                }
            }

            // Короткий анонс показываем только если он не дублирует начало текста и не длинный
            val cleanSub = article.subtitle.trim()
            val cleanCont = article.content.trim()
            if (
                cleanSub.isNotEmpty() &&
                cleanSub.length <= 160 &&
                cleanSub != cleanCont &&
                !cleanCont.startsWith(cleanSub.take(30))
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = cleanSub,
                    fontSize = 14.sp,
                    color = TextSecondary,
                    lineHeight = 20.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Иллюстрация компактной высоты
            if (article.imageUrl.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(176.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    SubcomposeAsyncImage(
                        model = article.imageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        error = {
                            Icon(
                                imageVector = RssIcons.Subscriptions,
                                contentDescription = null,
                                tint = TextSecondary.copy(alpha = 0.6f),
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Основной текст статьи по абзацам
            if (cleanCont.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    cleanCont.split("\n\n").filter { it.isNotBlank() }.forEach { paragraph ->
                        Text(
                            text = paragraph.trim(),
                            fontSize = 17.sp,
                            fontFamily = FontFamily.Serif,
                            color = TextPrimary,
                            lineHeight = 26.sp
                        )
                    }
                }
            }

            if (cleanCont.isEmpty() || cleanCont.length <= cleanSub.length + 20) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Этот источник не передаёт в ленте полный текст статьи. Прочитать её целиком можно на сайте.",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    lineHeight = 19.sp
                )
                if (browserIntent != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Читать на сайте",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DarkPrimary,
                        modifier = Modifier.clickable {
                            try {
                                context.startActivity(browserIntent)
                            } catch (_: Exception) {
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }

        // Нижняя панель действий читалки
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
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(onClick = onBookmarkClick)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (isSaved) RssIcons.BookmarkFilled else RssIcons.Bookmark,
                        contentDescription = "Сохранено",
                        tint = DarkPrimary,
                        modifier = Modifier.size(19.dp)
                    )
                    Text(
                        text = if (isSaved) "Сохранено" else "Сохранить",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = DarkPrimary
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(
                        onClick = {
                            val shareText = buildString {
                                append(article.title)
                                if (articleUrl != null) {
                                    append("\n")
                                    append(articleUrl)
                                }
                            }
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, shareText)
                            }
                            try {
                                context.startActivity(Intent.createChooser(shareIntent, "Поделиться статьёй"))
                            } catch (_: Exception) {
                            }
                        },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Поделиться",
                            tint = DarkPrimary,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    IconButton(
                        onClick = onReadToggle,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Прочитано",
                            tint = if (isRead) DarkPrimary else TextSecondary,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    IconButton(
                        onClick = { },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Следующая статья",
                            tint = DarkPrimary,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }
        }
    }
}