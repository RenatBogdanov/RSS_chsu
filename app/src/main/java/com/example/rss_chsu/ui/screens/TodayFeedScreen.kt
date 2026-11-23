package com.example.rss_chsu.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.foundation.lazy.LazyRow
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import coil.compose.AsyncImage
import com.example.rss_chsu.ui.components.RssIcons
import com.example.rss_chsu.ui.components.TopAppLogoBar
import com.example.rss_chsu.ui.model.Article
import com.example.rss_chsu.ui.model.FeedViewModel
import com.example.rss_chsu.ui.model.Folder
import com.example.rss_chsu.ui.model.Source
import com.example.rss_chsu.ui.theme.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

@Composable
fun TodayFeedScreen(
    personalizedArticles: List<Article>,
    totalArticleCount: Int,
    unreadArticleCount: Int,
    sourcesList: List<Source>,
    foldersList: List<Folder>,
    feedViewModel: FeedViewModel,
    onArticleClick: (Article) -> Unit,
    onBookmarkClick: (Article) -> Unit,
    @Suppress("UNUSED_PARAMETER") onFilterClick: () -> Unit = {}
) {
    var selectedFilterIndex by remember { mutableIntStateOf(0) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var isAscendingSort by remember { mutableStateOf(false) }
    var selectedFolders by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedSources by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showFilterSheet by remember { mutableStateOf(false) }

    val showScrollToTop by remember {
        derivedStateOf { listState.canScrollBackward }
    }

    val filterOptions = remember(totalArticleCount, personalizedArticles.size, unreadArticleCount) {
        listOf(
            "Все ($totalArticleCount)",
            "Для вас (${personalizedArticles.size})",
            "Новые ($unreadArticleCount)"
        )
    }

    val sourceFolderEntries = remember(sourcesList) {
        sourcesList.map { it.domain to it.folderName }
    }
    val sourceFolderByDomain = remember(sourceFolderEntries) {
        sourceFolderEntries.toMap()
    }
    val sourceNameEntries = remember(sourcesList) {
        sourcesList.map { it.name to it.domain }
    }
    val sourceNameIndex = remember(sourceNameEntries) {
        SourceNameResolver.indexForEntries(sourceNameEntries)
    }
    val selectedSourceDomains = remember(selectedSources) {
        selectedSources.toList()
    }
    val folderNames = remember(selectedFolders) { selectedFolders.toList() }

    val pagingFlow = remember(
        selectedFilterIndex,
        isAscendingSort,
        selectedSourceDomains,
        folderNames
    ) {
        if (selectedFilterIndex == 1) {
            flowOf(PagingData.empty<Article>())
        } else {
            feedViewModel.pagedArticles(
                onlyUnread = selectedFilterIndex == 2,
                oldestFirst = isAscendingSort,
                sourceDomains = selectedSourceDomains,
                folderNames = folderNames
            )
        }
    }
    val pagedArticles = pagingFlow.collectAsLazyPagingItems()

    val filteredPersonalized = remember(
        personalizedArticles,
        selectedFolders,
        selectedSources,
        sourceFolderByDomain
    ) {
        if (selectedFolders.isEmpty() && selectedSources.isEmpty()) {
            personalizedArticles
        } else {
            personalizedArticles.filter { article ->
                val folderName = sourceFolderByDomain[article.sourceId]
                val matchesFolder = selectedFolders.isNotEmpty() && folderName in selectedFolders
                val matchesSource = article.sourceId in selectedSources
                matchesFolder || matchesSource
            }
        }
    }

    LaunchedEffect(selectedFilterIndex, isAscendingSort, selectedFolders, selectedSources) {
        listState.scrollToItem(0)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppLogoBar(
            rightAction = {
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
        )

        val refreshState = if (selectedFilterIndex == 1) LoadState.NotLoading(false) else pagedArticles.loadState.refresh
        if (refreshState is LoadState.Loading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = DarkPrimary)
        }

        val hasVisibleItems = if (selectedFilterIndex == 1) {
            filteredPersonalized.isNotEmpty()
        } else {
            pagedArticles.itemCount > 0
        }

        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 160.dp)
            ) {
                item(key = "feed_controls") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
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

                if (!hasVisibleItems && refreshState is LoadState.Error) {
                    item(key = "feed_refresh_error") {
                        TextButton(
                            onClick = { pagedArticles.retry() },
                            modifier = Modifier.fillMaxWidth().padding(24.dp)
                        ) { Text("Не удалось загрузить ленту. Повторить", color = DarkPrimary) }
                    }
                } else if (!hasVisibleItems && refreshState is LoadState.NotLoading) {
                    item(key = "feed_empty_state") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 260.dp)
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (totalArticleCount == 0) "Лента пока пуста" else "Нет публикаций по выбранным условиям",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = if (totalArticleCount == 0)
                                        "Перейдите во вкладку «Подписки», чтобы добавить RSS-источник."
                                    else
                                        "Измените фильтр или выберите другую вкладку.",
                                    fontSize = 14.sp,
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else if (selectedFilterIndex == 1) {
                    itemsIndexed(
                        filteredPersonalized,
                        key = { _, article -> "personalized_${article.id}" },
                        contentType = { _, _ -> "article" }
                    ) { index, article ->
                        if (index == 0) {
                            FeaturedArticleCard(
                                article = article.copy(isFeatured = true),
                                onClick = { onArticleClick(article) },
                                onBookmarkClick = { onBookmarkClick(article) },
                                sourceNameIndex = sourceNameIndex
                            )
                        } else {
                            FeedArticleRow(
                                article = article,
                                sourceNameIndex = sourceNameIndex,
                                onClick = { onArticleClick(article) }
                            )
                        }
                    }
                    if (filteredPersonalized.isNotEmpty()) {
                        item(key = "personalized_end") { EndOfFeedIndicator() }
                    }
                } else {
                    items(
                        count = pagedArticles.itemCount,
                        key = pagedArticles.itemKey { it.id },
                        contentType = pagedArticles.itemContentType { "article" }
                    ) { index ->
                        val article = pagedArticles[index]
                        if (article != null) {
                            if (index == 0) {
                                FeaturedArticleCard(
                                    article = article.copy(isFeatured = true),
                                    onClick = { onArticleClick(article) },
                                    onBookmarkClick = { onBookmarkClick(article) },
                                    sourceNameIndex = sourceNameIndex
                                )
                            } else {
                                FeedArticleRow(
                                    article = article,
                                    sourceNameIndex = sourceNameIndex,
                                    onClick = { onArticleClick(article) }
                                )
                            }
                        }
                    }

                    if (pagedArticles.loadState.append is LoadState.Loading) {
                        item(key = "paging_append_loading") {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(20.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = DarkPrimary)
                            }
                        }
                    }
                    if (pagedArticles.loadState.append is LoadState.Error) {
                        item(key = "paging_append_error") {
                            TextButton(
                                onClick = { pagedArticles.retry() },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Не удалось загрузить. Повторить", color = DarkPrimary) }
                        }
                    }
                    val appendState = pagedArticles.loadState.append
                    if (pagedArticles.itemCount > 0 && appendState is LoadState.NotLoading && appendState.endOfPaginationReached) {
                        item(key = "end_of_list_indicator") { EndOfFeedIndicator() }
                    }
                }
            }

            ScrollToTopFab(
                visible = showScrollToTop,
                onClick = {
                    coroutineScope.launch {
                        listState.animateScrollToItem(0)
                    }
                }
            )
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

                    if (foldersList.isNotEmpty()) {
                        Text("Показывать папки", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(foldersList, key = { it.id }) { folder ->
                                val isSelected = folder.name in selectedFolders
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

                    if (sourcesList.isNotEmpty()) {
                        Text("Показывать источники", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(sourcesList, key = { it.id }) { source ->
                                val isSelected = source.domain in selectedSources
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = if (isSelected) DarkPrimary else ChipUnselected,
                                    modifier = Modifier.clickable {
                                        selectedSources = if (isSelected) selectedSources - source.domain else selectedSources + source.domain
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
private fun BoxScope.ScrollToTopFab(
    visible: Boolean,
    onClick: () -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(end = 16.dp, bottom = 16.dp),
        enter = fadeIn() + scaleIn(initialScale = 0.8f),
        exit = fadeOut() + scaleOut(targetScale = 0.8f)
    ) {
        SmallFloatingActionButton(
            onClick = onClick,
            containerColor = DarkPrimary,
            contentColor = Color.White
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = "Наверх",
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun EndOfFeedIndicator() {
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

@Suppress("unused")
fun cleanSourceName(rawName: String, sourcesList: List<Source> = emptyList()): String =
    SourceNameResolver.clean(rawName, SourceNameResolver.indexFor(sourcesList))

@Composable
fun FeaturedArticleCard(
    article: Article,
    onClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    sourceNameIndex: Map<String, String> = SourceNameResolver.popularIndex()
) {
    Column(modifier = Modifier.fillMaxWidth()) {
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
                Icon(
                    imageVector = RssIcons.Subscriptions,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.size(28.dp)
                )
                if (article.imageUrl.isNotEmpty()) {
                    AsyncImage(
                        model = article.imageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.1f)))
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
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                val displaySource = remember(article.sourceName, sourceNameIndex) {
                    SourceNameResolver.clean(article.sourceName, sourceNameIndex)
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
                Text(" · ${article.date}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary, maxLines = 1)
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
    onClick: () -> Unit,
    sourceNameIndex: Map<String, String>? = null
) {
    val sourceNameEntries = remember(sourcesList) {
        sourcesList.map { it.name to it.domain }
    }
    val fallbackSourceNameIndex = remember(sourceNameEntries) {
        SourceNameResolver.indexForEntries(sourceNameEntries)
    }
    val effectiveSourceNameIndex = sourceNameIndex ?: fallbackSourceNameIndex

    Column(modifier = Modifier.fillMaxWidth()) {
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
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        val displaySource = remember(article.sourceName, effectiveSourceNameIndex) {
                            SourceNameResolver.clean(article.sourceName, effectiveSourceNameIndex)
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
                        Text(" · ${article.date}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary, maxLines = 1)
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
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFA0A0A0)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = RssIcons.Subscriptions,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.size(18.dp)
                    )
                    if (article.imageUrl.isNotEmpty()) {
                        AsyncImage(
                            model = article.imageUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
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