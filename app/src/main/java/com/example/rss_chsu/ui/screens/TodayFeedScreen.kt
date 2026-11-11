package com.example.rss_chsu.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.foundation.lazy.LazyRow
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import com.example.rss_chsu.ui.components.RssIcons
import com.example.rss_chsu.ui.components.TopAppLogoBar
import com.example.rss_chsu.ui.model.Article
import com.example.rss_chsu.ui.model.Folder
import com.example.rss_chsu.ui.model.PopularSources
import com.example.rss_chsu.ui.model.RssRepository
import com.example.rss_chsu.ui.model.Source
import com.example.rss_chsu.ui.model.matchesArticle
import com.example.rss_chsu.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun TodayFeedScreen(
    articlesList: List<Article>,
    personalizedArticles: List<Article>,
    sourcesList: List<Source>,
    foldersList: List<Folder>,
    onArticleClick: (Article) -> Unit,
    onBookmarkClick: (Article) -> Unit,
    onFilterClick: () -> Unit = {}
) {
    var selectedFilterIndex by remember { mutableIntStateOf(0) }
    val listState = rememberLazyListState()

    var isRefreshing by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    var isAscendingSort by remember { mutableStateOf(false) } // false = сначала новые, true = сначала старые
    var selectedFolders by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedSources by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showFilterSheet by remember { mutableStateOf(false) }

    val unreadCount = remember(articlesList) { articlesList.count { !it.isRead } }

    val filterOptions = remember(articlesList.size, personalizedArticles.size, unreadCount) {
        listOf("Все (${articlesList.size})", "Для вас (${personalizedArticles.size})", "Новые ($unreadCount)")
    }

    // Ограничиваем количество статей на главном экране во избежание OutOfMemory при тысячах элементов
    val displayLimit by remember { mutableIntStateOf(500) }

    // 1. Сырой список для текущей вкладки
    val rawTabArticles = remember(selectedFilterIndex, articlesList, personalizedArticles, selectedFolders, selectedSources, sourcesList) {
        val baseList = when (selectedFilterIndex) {
            1 -> personalizedArticles // Для вас
            2 -> articlesList.filter { !it.isFeatured && !it.isRead } // Новые (непрочитанные)
            else -> articlesList.filter { !it.isFeatured } // Все
        }
        
        if (selectedFolders.isEmpty() && selectedSources.isEmpty()) {
            baseList
        } else {
            baseList.filter { article ->
                // Находим источник статьи
                val source = sourcesList.find { it.matchesArticle(article) }
                val matchesFolder = selectedFolders.isNotEmpty() && source?.folderName in selectedFolders
                val matchesSource = selectedSources.isNotEmpty() && source?.name in selectedSources
                matchesFolder || matchesSource
            }
        }
    }

    // 2. Находим главную статью в зависимости от вкладки и сортировки
    val featured = remember(rawTabArticles, selectedFilterIndex, isAscendingSort, articlesList) {
        if (selectedFilterIndex == 0 && !isAscendingSort) {
            // Для "Все" и "Сначала новые" берем специальную главную
            articlesList.find { it.isFeatured } ?: articlesList.firstOrNull()
        } else {
            // Для остальных вкладок и сортировок - просто берем первую сверху
            if (isAscendingSort) rawTabArticles.lastOrNull() else rawTabArticles.firstOrNull()
        }
    }

    // 3. Убираем главную статью из списка и применяем лимит
    val filteredArticles = remember(rawTabArticles, featured, displayLimit) {
        rawTabArticles.filter { it.id != featured?.id }.take(displayLimit)
    }

    // 4. Применяем сортировку 
    val sortedArticles = remember(filteredArticles, isAscendingSort, selectedFilterIndex) {
        // Если вкладка "Для вас", не переворачиваем (там сортировка по релевантности)
        if (isAscendingSort && selectedFilterIndex != 1) filteredArticles.reversed() else filteredArticles
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppLogoBar(
            rightAction = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { showFilterSheet = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = RssIcons.Tune,
                            contentDescription = "Фильтр",
                            tint = DarkPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        )

        if (isRefreshing) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = DarkPrimary)
        }

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
                        text = "Перейдите во вкладку «Подписки», чтобы добавить RSS-источник.",
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
                // Filter and Sort Segmented Control
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Filter Segmented Control
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .background(ChipUnselected, RoundedCornerShape(14.dp))
                                .height(40.dp)
                                .padding(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            filterOptions.forEachIndexed { index, optionText ->
                                val isSelected = selectedFilterIndex == index
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent)
                                        .clickable { selectedFilterIndex = index },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = optionText, 
                                        fontSize = 12.sp, 
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, 
                                        color = if (isSelected) TextPrimary else TextSecondary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
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

                // Article List Items
                items(sortedArticles, key = { it.id }) { article ->
                    FeedArticleRow(
                        article = article,
                        sourcesList = sourcesList,
                        onClick = { onArticleClick(article) }
                    )
                }

                // Индикатор конца списка
                item(key = "end_of_list_indicator") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp, horizontal = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = TextSecondary.copy(alpha = 0.5f),
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Вы просмотрели все доступные новости.",
                            fontSize = 14.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        if (showFilterSheet) {
            @OptIn(ExperimentalMaterial3Api::class)
            ModalBottomSheet(
                onDismissRequest = { showFilterSheet = false },
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .padding(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Настройки ленты", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    
                    // Сортировка по времени
                    Text("Сортировка по времени", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ChipUnselected, RoundedCornerShape(12.dp))
                            .height(40.dp)
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (!isAscendingSort) MaterialTheme.colorScheme.surface else Color.Transparent)
                                .clickable { isAscendingSort = false },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Сначала новые", fontSize = 13.sp, fontWeight = if (!isAscendingSort) FontWeight.Bold else FontWeight.Medium, color = if (!isAscendingSort) TextPrimary else TextSecondary)
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isAscendingSort) MaterialTheme.colorScheme.surface else Color.Transparent)
                                .clickable { isAscendingSort = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Сначала старые", fontSize = 13.sp, fontWeight = if (isAscendingSort) FontWeight.Bold else FontWeight.Medium, color = if (isAscendingSort) TextPrimary else TextSecondary)
                        }
                    }

                    // Фильтр по папкам
                    if (foldersList.isNotEmpty()) {
                        Text("Показывать папки", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(foldersList, key = { it.id }) { folder ->
                                val isSelected = selectedFolders.contains(folder.name)
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = if (isSelected) DarkPrimary else ChipUnselected,
                                    modifier = Modifier.clickable {
                                        selectedFolders = if (isSelected) selectedFolders - folder.name else selectedFolders + folder.name
                                    }
                                ) {
                                    Text(
                                        text = folder.name, 
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), 
                                        color = if (isSelected) Color.White else TextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                    
                    // Фильтр по источникам
                    if (sourcesList.isNotEmpty()) {
                        Text("Показывать источники", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(sourcesList, key = { it.id }) { source ->
                                val isSelected = selectedSources.contains(source.name)
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = if (isSelected) DarkPrimary else ChipUnselected,
                                    modifier = Modifier.clickable {
                                        selectedSources = if (isSelected) selectedSources - source.name else selectedSources + source.name
                                    }
                                ) {
                                    Text(
                                        text = source.name, 
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), 
                                        color = if (isSelected) Color.White else TextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { showFilterSheet = false },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DarkPrimary)
                    ) {
                        Text("Применить", color = Color.White, fontWeight = FontWeight.SemiBold)
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

fun cleanSourceName(rawName: String, sourcesList: List<Source> = emptyList()): String {
    if (rawName.isBlank()) return "Источник"
    
    val matched = sourcesList.find { it.name.equals(rawName, ignoreCase = true) || it.domain.equals(rawName, ignoreCase = true) }
        ?: PopularSources.topFeeds.find { it.name.equals(rawName, ignoreCase = true) || it.domain.equals(rawName, ignoreCase = true) }
    if (matched != null) {
        return matched.name
    }

    var clean = rawName.trim()
        .removePrefix("Официальный сайт города ")
        .removePrefix("Официальный сайт ")
        .removePrefix("Новости портала ")
        .removePrefix("Последние новости ")
        .removePrefix("Лента новостей ")
        .removePrefix("Новости ")
        .trim()

    val colonIndex = clean.indexOf(':')
    if (colonIndex > 0) {
        clean = clean.substring(0, colonIndex).trim()
    }

    if (clean.length > 22) {
        clean = clean.take(22).trimEnd() + "..."
    }

    return if (clean.isBlank()) rawName.take(22) else clean
}

@Composable
fun FeaturedArticleCard(
    article: Article,
    onClick: () -> Unit,
    onBookmarkClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 6.dp)
                .clip(RoundedCornerShape(12.dp))
                .clickable { onClick() }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFA0A0A0)),
                contentAlignment = Alignment.Center
            ) {
                if (article.imageUrl.isNotEmpty()) {
                    SubcomposeAsyncImage(
                        model = article.imageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        error = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(0xFFA0A0A0)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = RssIcons.Subscriptions,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.6f),
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFFA0A0A0)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = RssIcons.Subscriptions,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(28.dp)
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
                        .padding(8.dp)
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.9f))
                ) {
                    Icon(
                        imageVector = if (article.isBookmarked) RssIcons.BookmarkFilled else RssIcons.Bookmark,
                        contentDescription = "В закладки",
                        tint = DarkPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                val displaySource = remember(article.sourceName) {
                    cleanSourceName(article.sourceName)
                }
                Text(
                    text = displaySource,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Text(
                    text = " · ${article.date}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    maxLines = 1
                )
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = article.title,
                fontSize = 16.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                lineHeight = 21.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (article.subtitle.isNotEmpty()) {
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = article.subtitle,
                    fontSize = 13.sp,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 17.sp
                )
            }

            Spacer(modifier = Modifier.height(4.dp))
        }
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            thickness = 0.5.dp,
            color = ChipUnselected.copy(alpha = 0.6f)
        )
    }
}

@Composable
fun FeedArticleRow(
    article: Article,
    sourcesList: List<Source> = emptyList(),
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(horizontal = 16.dp, vertical = 6.dp),
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
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val displaySource = remember(article.sourceName, sourcesList) {
                            cleanSourceName(article.sourceName, sourcesList)
                        }
                        Text(
                            text = displaySource,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Text(
                            text = " · ${article.date}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            maxLines = 1
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = article.title,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 18.sp
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Thumbnail
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFA0A0A0)),
                    contentAlignment = Alignment.Center
                ) {
                    if (article.imageUrl.isNotEmpty()) {
                        SubcomposeAsyncImage(
                            model = article.imageUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            error = {
                                Icon(
                                    imageVector = RssIcons.Subscriptions,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.6f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        )
                    } else {
                        Icon(
                            imageVector = RssIcons.Subscriptions,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(horizontal = 16.dp),
            thickness = 0.5.dp,
            color = ChipUnselected.copy(alpha = 0.6f)
        )
    }
}
