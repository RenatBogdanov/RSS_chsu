package com.example.rss_chsu.ui.screens

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import coil.ImageLoader
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.rss_chsu.data.local.ArticleStatus
import com.example.rss_chsu.ui.components.RssIcons
import com.example.rss_chsu.ui.model.Article
import com.example.rss_chsu.ui.model.RssRepository
import com.example.rss_chsu.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.roundToInt

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ArticleReaderScreen(
    articles: List<Article>,
    initialArticleId: String?,
    articleStatuses: Map<String, ArticleStatus> = emptyMap(),
    recommendedArticles: List<Article> = emptyList(),
    recommendedFor: String? = null,
    onLoadRecommendations: (Article) -> Unit = {},
    onClose: () -> Unit,
    onArticleChanged: (String) -> Unit = {},
    onBookmarkClick: (String) -> Unit = {},
    onReadArticle: (String) -> Unit = {}
) {
    if (articles.isEmpty()) {
        LaunchedEffect(Unit) { onClose() }
        return
    }

    val navigationStack = remember { mutableStateListOf<String>() }
    var isProgrammaticNavigation by remember { mutableStateOf(false) }

    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    val animatedDragOffset by animateFloatAsState(
        targetValue = dragOffsetY,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 400f),
        label = "dragOffset"
    )

    val dismissThreshold = 150.dp
    val density = LocalDensity.current.density
    val dismissThresholdPx = dismissThreshold.value * density

    val initialPage = remember(articles, initialArticleId) {
        val idx = articles.indexOfFirst { it.id == initialArticleId }
        if (idx >= 0) idx else 0
    }

    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { articles.size }
    )

    val currentArticle = articles.getOrNull(pagerState.currentPage) ?: articles.first()
    val isCurrentBookmarked = articleStatuses[currentArticle.id]?.isBookmarked ?: currentArticle.isBookmarked

    val handleBack: () -> Unit = {
        if (navigationStack.isNotEmpty()) {
            val prevId = navigationStack.removeAt(navigationStack.lastIndex)
            val prevIndex = articles.indexOfFirst { it.id == prevId }
            if (prevIndex >= 0) {
                onArticleChanged(prevId)
                isProgrammaticNavigation = true
                kotlinx.coroutines.MainScope().launch {
                    pagerState.scrollToPage(prevIndex)
                    isProgrammaticNavigation = false
                }
            } else {
                onClose()
            }
        } else {
            onClose()
        }
    }

    BackHandler(enabled = true) { handleBack() }

    LaunchedEffect(pagerState.currentPage) {
        val id = articles.getOrNull(pagerState.currentPage)?.id ?: return@LaunchedEffect
        if (!isProgrammaticNavigation) {
            navigationStack.clear()
        }
        onArticleChanged(id)
    }

    LaunchedEffect(initialArticleId, articles.size) {
        val target = articles.indexOfFirst { it.id == initialArticleId }
        if (target >= 0 && target != pagerState.currentPage) {
            pagerState.animateScrollToPage(target)
        }
    }

    LaunchedEffect(currentArticle.id) {
        onReadArticle(currentArticle.id)
    }

    LaunchedEffect(currentArticle.id) {
        onLoadRecommendations(currentArticle)
    }

    var showDetails by remember(currentArticle.id) { mutableStateOf(false) }

    val canGoPrevious = pagerState.currentPage > 0
    val canGoNext = pagerState.currentPage < articles.lastIndex

    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    val recommendationsForCurrent =
        if (recommendedFor == currentArticle.id) recommendedArticles else emptyList()

    val jumpToArticle: (String) -> Unit = { targetId ->
        val targetIndex = articles.indexOfFirst { it.id == targetId }
        if (targetIndex >= 0 && targetIndex != pagerState.currentPage) {
            navigationStack.add(currentArticle.id)
            isProgrammaticNavigation = true
            coroutineScope.launch {
                if (abs(targetIndex - pagerState.currentPage) > 2) {
                    pagerState.scrollToPage(targetIndex)
                } else {
                    pagerState.animateScrollToPage(targetIndex)
                }
                isProgrammaticNavigation = false
            }
        }
    }

    val swipeDownModifier = Modifier.pointerInput(Unit) {
        detectVerticalDragGestures(
            onDragEnd = {
                if (dragOffsetY > dismissThresholdPx) {
                    onClose()
                } else {
                    dragOffsetY = 0f
                }
            },
            onDragCancel = { dragOffsetY = 0f },
            onVerticalDrag = { change, dragAmount ->
                change.consume()
                if (dragAmount > 0) {
                    dragOffsetY += dragAmount
                } else if (dragOffsetY > 0) {
                    dragOffsetY = (dragOffsetY + dragAmount).coerceAtLeast(0f)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .offset { IntOffset(0, animatedDragOffset.roundToInt()) }
            .graphicsLayer {
                val progress = (animatedDragOffset / dismissThresholdPx).coerceIn(0f, 1f)
                scaleX = 1f - (progress * 0.05f)
                scaleY = 1f - (progress * 0.05f)
                alpha = 1f - (progress * 0.5f)
            }
            .background(MaterialTheme.colorScheme.background)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, bottom = 4.dp)
                .then(swipeDownModifier),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(TextSecondary.copy(alpha = 0.3f))
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .then(swipeDownModifier)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = handleBack)
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
        }

        HorizontalPager(
            state = pagerState,
            beyondViewportPageCount = 1,
            key = { page -> articles[page].id },
            modifier = Modifier.weight(1f)
        ) { page ->
            val article = articles[page]
            val isCurrent = article.id == currentArticle.id
            ArticlePage(
                article = article,
                showDetails = isCurrent && showDetails,
                recommendations = if (isCurrent) recommendationsForCurrent else emptyList(),
                onRecommendationClick = jumpToArticle
            )
        }

        val borderColor = LightCardBorder
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    drawLine(
                        color = borderColor,
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
                        .clickable { onBookmarkClick(currentArticle.id) }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (isCurrentBookmarked) RssIcons.BookmarkFilled else RssIcons.Bookmark,
                        contentDescription = "Сохранено",
                        tint = DarkPrimary,
                        modifier = Modifier.size(19.dp)
                    )
                    Text(
                        text = if (isCurrentBookmarked) "Сохранено" else "Сохранить",
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
                            if (canGoPrevious) coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                            }
                        },
                        enabled = canGoPrevious,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Предыдущая статья",
                            tint = if (canGoPrevious) DarkPrimary else TextSecondary,
                            modifier = Modifier.size(19.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            val shareText = buildString {
                                append(currentArticle.title)
                                val url = currentArticle.id
                                if (url.startsWith("http://") || url.startsWith("https://")) {
                                    append("\n")
                                    append(url)
                                }
                            }
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, shareText)
                            }
                            try {
                                context.startActivity(
                                    Intent.createChooser(shareIntent, "Поделиться статьёй")
                                )
                            } catch (_: Exception) {}
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
                        onClick = {
                            if (canGoNext) coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        },
                        enabled = canGoNext,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Следующая статья",
                            tint = if (canGoNext) DarkPrimary else TextSecondary,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────
// Одна «страница» пейджера
// ──────────────────────────────────────────────────────────────────────────

@Composable
private fun ArticlePage(
    article: Article,
    showDetails: Boolean,
    recommendations: List<Article> = emptyList(),
    onRecommendationClick: (String) -> Unit = {}
) {
    val articleFlow = remember(article.id) { RssRepository.observeArticle(article.id) }
    val fullArticle by articleFlow.collectAsState(initial = article)

    ArticleContent(
        article = fullArticle ?: article,
        showDetails = showDetails,
        recommendations = recommendations,
        onRecommendationClick = onRecommendationClick
    )
}

@Composable
private fun ShimmerBlock(widthFraction: Float, height: Dp) {
    val base = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val highlight = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.16f)

    var flag by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(700)
            flag = !flag
        }
    }
    val alpha by animateFloatAsState(
        targetValue = if (flag) 1f else 0.5f,
        animationSpec = tween(700),
        label = "shimmer"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth(widthFraction)
            .height(height)
            .clip(RoundedCornerShape(4.dp))
            .background(if (alpha > 0.75f) highlight else base)
    )
}

// ──────────────────────────────────────────────────────────────────────────
// Тело статьи + блок «Похожие статьи» в конце
// ──────────────────────────────────────────────────────────────────────────

@Composable
private fun ArticleContent(
    article: Article,
    showDetails: Boolean,
    recommendations: List<Article> = emptyList(),
    onRecommendationClick: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val currentFont = LocalAppFont.current.toFontFamily()
    var showFullScreenImage by remember(article.id) { mutableStateOf(false) }

    val articleUrl = remember(article.id) {
        if (article.id.startsWith("http://") || article.id.startsWith("https://")) article.id else null
    }
    val browserIntent = remember(articleUrl) {
        articleUrl?.let {
            try { Intent(Intent.ACTION_VIEW, it.toUri()) } catch (_: Exception) { null }
        }
    }

    if (showFullScreenImage && article.imageUrl.isNotBlank()) {
        FullScreenImageViewer(
            imageUrl = article.imageUrl,
            onDismiss = { showFullScreenImage = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp, vertical = 6.dp)
    ) {
        val dateLabel = article.pubDateFormatted.ifBlank { article.date }
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
            Surface(shape = RoundedCornerShape(6.dp), color = ChipUnselected) {
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

        Text(
            text = article.title,
            fontSize = 22.sp,
            fontFamily = currentFont,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            lineHeight = 28.sp
        )

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
                    Text("Источник: ${article.sourceName}", fontSize = 12.sp, color = TextPrimary)
                    Text(
                        "Автор: ${article.author.ifBlank { "Редакция" }}",
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
                                    try { context.startActivity(browserIntent) } catch (_: Exception) {}
                                }
                                .padding(top = 2.dp)
                        )
                    }
                }
            }
        }

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

        var imageLoadFailed by remember(article.imageUrl) { mutableStateOf(false) }
        if (article.imageUrl.isNotBlank() && !imageLoadFailed) {
            Spacer(modifier = Modifier.height(12.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(176.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { showFullScreenImage = true },
                contentAlignment = Alignment.Center
            ) {
                SubcomposeAsyncImage(
                    model = article.imageUrl,
                    contentDescription = "Открыть изображение",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    onError = { imageLoadFailed = true }
                )
            }
        }
        Spacer(modifier = Modifier.height(14.dp))

        if (cleanCont.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                cleanCont.split("\n\n").filter { it.isNotBlank() }.forEach { paragraph ->
                    Text(
                        text = paragraph.trim(),
                        fontSize = 17.sp,
                        fontFamily = currentFont,
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
                        try { context.startActivity(browserIntent) } catch (_: Exception) {}
                    }
                )
            }
        }

        if (recommendations.isNotEmpty()) {
            SimilarArticlesBlock(
                recommendations = recommendations,
                onArticleClick = onRecommendationClick
            )
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

// ──────────────────────────────────────────────────────────────────────────
// Блок «Похожие статьи»
// ──────────────────────────────────────────────────────────────────────────

@Composable
private fun SimilarArticlesBlock(
    recommendations: List<Article>,
    onArticleClick: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Spacer(modifier = Modifier.height(16.dp))

        HorizontalDivider(
            thickness = 0.5.dp,
            color = LightCardBorder.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Похожие статьи",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(12.dp))

        recommendations.forEach { rec ->
            SimilarArticleRow(
                article = rec,
                onClick = { onArticleClick(rec.id) }
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SimilarArticleRow(
    article: Article,
    onClick: () -> Unit
) {
    val currentFont = LocalAppFont.current.toFontFamily()

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, LightCardBorder.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = ChipUnselected
                ) {
                    Text(
                        text = article.category.uppercase(),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 0.4.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                    )
                }
                Text(
                    text = article.sourceName,
                    fontSize = 11.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (article.readTime.isNotBlank()) {
                    Text(
                        text = article.readTime,
                        fontSize = 11.sp,
                        color = TextSecondary,
                        maxLines = 1
                    )
                }
            }
            Text(
                text = article.title,
                fontSize = 14.sp,
                fontFamily = currentFont,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                lineHeight = 19.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────
// Полноэкранный просмотрщик изображения
// ──────────────────────────────────────────────────────────────────────────

@Composable
private fun FullScreenImageViewer(
    imageUrl: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isSaving by remember { mutableStateOf(false) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    val startSaving: () -> Unit = {
        if (!isSaving) {
            isSaving = true
            coroutineScope.launch {
                val success = saveImageToGallery(context, imageUrl)
                isSaving = false
                Toast.makeText(
                    context,
                    if (success) "Изображение сохранено в галерею" else "Не удалось сохранить изображение",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) startSaving()
        else Toast.makeText(context, "Нет разрешения на сохранение в память", Toast.LENGTH_SHORT).show()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = true)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .pointerInput(Unit) {
                    detectTapGestures(onDoubleTap = {
                        if (scale > 1f) { scale = 1f; offset = Offset.Zero } else scale = 2.5f
                    })
                }
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        val newScale = (scale * zoom).coerceIn(1f, 5f)
                        scale = newScale
                        offset = if (newScale > 1f) offset + pan else Offset.Zero
                    }
                }
        ) {
            SubcomposeAsyncImage(
                model = imageUrl,
                contentDescription = "Полноэкранное изображение",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale, scaleY = scale,
                        translationX = offset.x, translationY = offset.y
                    ),
                loading = {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color.White)
                    }
                }
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(42.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, "Закрыть", tint = Color.White)
                }
                IconButton(
                    onClick = {
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
                            ContextCompat.checkSelfPermission(
                                context, Manifest.permission.WRITE_EXTERNAL_STORAGE
                            ) != PackageManager.PERMISSION_GRANTED
                        ) permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        else startSaving()
                    },
                    enabled = !isSaving,
                    modifier = Modifier
                        .size(42.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    if (isSaving) CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    else Icon(DownloadIcon, "Сохранить в галерею", tint = Color.White)
                }
            }
        }
    }
}

private val DownloadIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "Download", defaultWidth = 24.dp, defaultHeight = 24.dp,
        viewportWidth = 24f, viewportHeight = 24f
    ).apply {
        path(
            fill = SolidColor(Color.White),
            pathFillType = PathFillType.NonZero,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter
        ) {
            moveTo(19f, 9f); horizontalLineToRelative(-4f); verticalLineTo(3f)
            horizontalLineTo(9f); verticalLineToRelative(6f); horizontalLineTo(5f)
            lineToRelative(7f, 7f); lineToRelative(7f, -7f); close()
            moveTo(5f, 18f); verticalLineToRelative(2f); horizontalLineToRelative(14f)
            verticalLineToRelative(-2f); horizontalLineTo(5f); close()
        }
    }.build()
}

private suspend fun saveImageToGallery(context: Context, imageUrl: String): Boolean =
    withContext(Dispatchers.IO) {
        try {
            val loader = ImageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(imageUrl).allowHardware(false).build()
            val result = loader.execute(request)
            val bitmap = (result as? SuccessResult)?.drawable?.let { (it as? BitmapDrawable)?.bitmap }
                ?: return@withContext false
            val filename = "RSS_${System.currentTimeMillis()}.jpg"
            val cv = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/RSS_Chsu")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }
            val resolver = context.contentResolver
            val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, cv)
                ?: return@withContext false
            resolver.openOutputStream(imageUri)?.use { out ->
                if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)) return@withContext false
            } ?: return@withContext false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                cv.clear(); cv.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(imageUri, cv, null, null)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace(); false
        }
    }