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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import com.example.rss_chsu.ui.components.RssIcons
import com.example.rss_chsu.ui.model.Article
import com.example.rss_chsu.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    var showFullScreenImage by remember(article.id) { mutableStateOf(false) }

    val context = LocalContext.current
    val articleUrl = remember(article.id) {
        if (article.id.startsWith("http://") || article.id.startsWith("https://")) article.id else null
    }
    val browserIntent = remember(articleUrl) {
        articleUrl?.let {
            try {
                Intent(Intent.ACTION_VIEW, it.toUri())
            } catch (_: Exception) {
                null
            }
        }
    }

    // Полноэкранный просмотрщик изображения
    if (showFullScreenImage && article.imageUrl.isNotBlank()) {
        FullScreenImageViewer(
            imageUrl = article.imageUrl,
            onDismiss = { showFullScreenImage = false }
        )
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
// Иллюстрация (полностью убирается, если её нет или она не загрузилась)
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
                val message = if (success) {
                    "Изображение сохранено в галерею"
                } else {
                    "Не удалось сохранить изображение"
                }
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            }
        }
    }
// Запрос разрешения только для Android 9 (API 28) и ниже
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startSaving()
        } else {
            Toast.makeText(context, "Нет разрешения на сохранение в память", Toast.LENGTH_SHORT).show()
        }
    }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = {
                            if (scale > 1f) {
                                scale = 1f
                                offset = Offset.Zero
                            } else {
                                scale = 2.5f
                            }
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        val newScale = (scale * zoom).coerceIn(1f, 5f)
                        scale = newScale
                        offset = if (newScale > 1f) {
                            offset + pan
                        } else {
                            Offset.Zero
                        }
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
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y
                    ),
                loading = {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color.White)
                    }
                }
            )
// Верхняя панель управления в просмотрщике
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
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Закрыть",
                        tint = Color.White
                    )
                }
                IconButton(
                    onClick = {
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
                            ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        } else {
                            startSaving()
                        }
                    },
                    enabled = !isSaving,
                    modifier = Modifier
                        .size(42.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = DownloadIcon,
                            contentDescription = "Сохранить в галерею",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}
private val DownloadIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "Download",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).apply {
        path(
            fill = SolidColor(Color.White),
            pathFillType = PathFillType.NonZero,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter
        ) {
            moveTo(19f, 9f)
            horizontalLineToRelative(-4f)
            verticalLineTo(3f)
            horizontalLineTo(9f)
            verticalLineToRelative(6f)
            horizontalLineTo(5f)
            lineToRelative(7f, 7f)
            lineToRelative(7f, -7f)
            close()
            moveTo(5f, 18f)
            verticalLineToRelative(2f)
            horizontalLineToRelative(14f)
            verticalLineToRelative(-2f)
            horizontalLineTo(5f)
            close()
        }
    }.build()
}
private suspend fun saveImageToGallery(context: Context, imageUrl: String): Boolean =
    withContext(Dispatchers.IO) {
        try {
            val loader = ImageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(imageUrl)
                .allowHardware(false)
                .build()
            val result = loader.execute(request)
            val bitmap = (result as? SuccessResult)?.drawable?.let { drawable ->
                (drawable as? BitmapDrawable)?.bitmap
            } ?: return@withContext false
            val filename = "RSS_${System.currentTimeMillis()}.jpg"
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/RSS_Chsu")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }
            val resolver = context.contentResolver
            val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                ?: return@withContext false
            resolver.openOutputStream(imageUri)?.use { out ->
                if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)) {
                    return@withContext false
                }
            } ?: return@withContext false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(imageUri, contentValues, null, null)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }