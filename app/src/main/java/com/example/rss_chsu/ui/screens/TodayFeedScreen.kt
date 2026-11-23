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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.rss_chsu.data.local.ArticleCounts
import com.example.rss_chsu.data.local.ArticlePagingRepository
import com.example.rss_chsu.data.local.ArticleStatus
import com.example.rss_chsu.data.local.SourceNameIndex
import com.example.rss_chsu.ui.components.RssIcons
import com.example.rss_chsu.ui.components.TopAppLogoBar
import com.example.rss_chsu.ui.model.Article
import com.example.rss_chsu.ui.model.FeedViewModel
import com.example.rss_chsu.ui.model.Folder
import com.example.rss_chsu.ui.model.RssRepository
import com.example.rss_chsu.ui.model.Source
import com.example.rss_chsu.ui.theme.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
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
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 3 })
    val coroutineScope = rememberCoroutineScope()

    var isAscendingSort by remember { mutableStateOf(false) }
    var selectedFolders by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selectedSources by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }

    val articleStatuses by feedViewModel.articleStatuses.collectAsState()
    val isLargeCards by ThemeManager.isLargeCards.collectAsState()

    val sourceNamePairs = remember(sourcesList) { sourcesList.map { it.name to it.domain } }
    val sourceNameIndex = remember(sourceNamePairs) {
        SourceNameIndex(SourceNameResolver.indexForEntries(sourceNamePairs))
    }

    val sourceFolderPairs = remember(sourcesList) { sourcesList.map { it.domain to it.folderName } }
    val sourceFolderByDomain = remember(sourceFolderPairs) { sourceFolderPairs.toMap() }

    val selectedSourceDomains = remember(selectedSources) { selectedSources.toList() }
    val folderNames = remember(selectedFolders) { selectedFolders.toList() }

    val filteredPersonalized = remember(personalizedArticles, selectedFolders, selectedSources, sourceFolderByDomain) {
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

    val hasActiveFilter = selectedFolders.isNotEmpty() || selectedSources.isNotEmpty()

    val filteredDbCounts by remember(selectedSourceDomains, folderNames) {
        ArticlePagingRepository.observeFilteredArticleCounts(
            sourceDomains = selectedSourceDomains,
            folderNames = folderNames
        )
    }.collectAsState(
        initial = ArticleCounts(
            total = totalArticleCount,
            unread = unreadArticleCount
        )
    )

    val displayedTotalCount = if (hasActiveFilter) filteredDbCounts.total else totalArticleCount
    val displayedUnreadCount = if (hasActiveFilter) filteredDbCounts.unread else unreadArticleCount
    val displayedPersonalizedCount = filteredPersonalized.size

    val filterOptions = remember(displayedTotalCount, displayedPersonalizedCount, displayedUnreadCount) {
        listOf(
            "Все ($displayedTotalCount)",
            "Для вас ($displayedPersonalizedCount)",
            "Новые ($displayedUnreadCount)"
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppLogoBar(
            rightAction = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = {
                            if (!isRefreshing) {
                                coroutineScope.launch {
                                    isRefreshing = true
                                    RssRepository.refreshAllSources()
                                    isRefreshing = false
                                }
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

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
        ) {
            if (isRefreshing) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkPrimary
                )
            }
        }

        CustomTabRow(
            pagerState = pagerState,
            filterOptions = filterOptions,
            onTabClick = { index ->
                coroutineScope.launch { pagerState.animateScrollToPage(index) }
            }
        )

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            userScrollEnabled = !showFilterSheet
        ) { page ->
            FeedPage(
                pageIndex = page,
                filteredPersonalized = filteredPersonalized,
                articleStatuses = articleStatuses,
                isLargeCards = isLargeCards,
                isAscendingSort = isAscendingSort,
                selectedSourceDomains = selectedSourceDomains,
                folderNames = folderNames,
                feedViewModel = feedViewModel,
                sourceNameIndex = sourceNameIndex,
                onArticleClick = onArticleClick,
                onBookmarkClick = onBookmarkClick,
                totalArticleCount = displayedTotalCount,
                isRefreshing = isRefreshing
            )
        }

        if (showFilterSheet) {
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
                            Text(
                                "Сначала новые",
                                fontSize = 13.sp,
                                fontWeight = if (!isAscendingSort) FontWeight.Bold else FontWeight.Medium,
                                color = if (!isAscendingSort) TextPrimary else TextSecondary
                            )
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
                            Text(
                                "Сначала старые",
                                fontSize = 13.sp,
                                fontWeight = if (isAscendingSort) FontWeight.Bold else FontWeight.Medium,
                                color = if (isAscendingSort) TextPrimary else TextSecondary
                            )
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
                                        color = if (isSelected) OnDarkPrimary else TextPrimary,
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
                                        color = if (isSelected) OnDarkPrimary else TextPrimary,
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
                        colors = ButtonDefaults.buttonColors(containerColor = DarkPrimary, contentColor = OnDarkPrimary)
                    ) {
                        Text("Применить", color = OnDarkPrimary, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomTabRow(
    pagerState: androidx.compose.foundation.pager.PagerState,
    filterOptions: List<String>,
    onTabClick: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        var rowWidthPx by remember { mutableStateOf(0) }
        val tabWidthPx = if (filterOptions.isEmpty()) 0f else rowWidthPx.toFloat() / filterOptions.size
        val tabWidthDp = with(LocalDensity.current) { tabWidthPx.toDp() }
        val currentPage = pagerState.currentPage

        Box(
            modifier = Modifier
                .weight(1f)
                .onSizeChanged { rowWidthPx = it.width }
                .background(ChipUnselected, RoundedCornerShape(14.dp))
                .height(40.dp)
                .padding(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(tabWidthDp)
                    .fillMaxHeight()
                    .graphicsLayer {
                        translationX = tabWidthPx *
                                (pagerState.currentPage + pagerState.currentPageOffsetFraction)
                    }
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surface)
            )

            Row(modifier = Modifier.fillMaxSize()) {
                filterOptions.forEachIndexed { index, optionText ->
                    val isSelected = currentPage == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable { onTabClick(index) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = optionText,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isSelected) TextPrimary else TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FeedPage(
    pageIndex: Int,
    filteredPersonalized: List<Article>,
    articleStatuses: Map<String, ArticleStatus>,
    isLargeCards: Boolean,
    isAscendingSort: Boolean,
    selectedSourceDomains: List<String>,
    folderNames: List<String>,
    feedViewModel: FeedViewModel,
    sourceNameIndex: SourceNameIndex,
    onArticleClick: (Article) -> Unit,
    onBookmarkClick: (Article) -> Unit,
    totalArticleCount: Int,
    isRefreshing: Boolean = false
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val showScrollToTop = remember {
        derivedStateOf { listState.canScrollBackward }
    }

    val pagingFlow = remember(
        pageIndex,
        isAscendingSort,
        selectedSourceDomains,
        folderNames
    ) {
        if (pageIndex == 1) {
            flowOf(PagingData.empty())
        } else {
            feedViewModel.pagedArticles(
                onlyUnread = pageIndex == 2,
                oldestFirst = isAscendingSort,
                sourceDomains = selectedSourceDomains,
                folderNames = folderNames
            )
        }
    }
    val pagedArticles = pagingFlow.collectAsLazyPagingItems()

    val refreshState = if (pageIndex == 1) LoadState.NotLoading(false) else pagedArticles.loadState.refresh
    val hasVisibleItems = if (pageIndex == 1) filteredPersonalized.isNotEmpty() else pagedArticles.itemCount > 0

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 160.dp)
        ) {
            if (!hasVisibleItems && refreshState is LoadState.Error) {
                item(key = "error_$pageIndex", contentType = "error") {
                    TextButton(
                        onClick = { pagedArticles.retry() },
                        modifier = Modifier.fillMaxWidth().padding(24.dp)
                    ) { Text("Не удалось загрузить ленту. Повторить", color = DarkPrimary) }
                }
            } else if (!hasVisibleItems && refreshState is LoadState.NotLoading) {
                item(key = "empty_$pageIndex", contentType = "empty") {
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
                                text = if (totalArticleCount == 0) "Перейдите во вкладку «Подписки», чтобы добавить RSS-источник." else "Измените фильтр или выберите другую вкладку.",
                                fontSize = 14.sp,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else if (pageIndex == 1) {
                itemsIndexed(
                    filteredPersonalized,
                    key = { _, article -> "personalized_${article.id}" },
                    contentType = { index, _ -> if (isLargeCards || index == 0) "featured" else "row" }
                ) { index, article ->
                    val status = articleStatuses[article.id]
                    val effectiveBookmarked = status?.isBookmarked ?: article.isBookmarked
                    if (isLargeCards || index == 0) {
                        FeaturedArticleCard(
                            article = article,
                            isBookmarked = effectiveBookmarked,
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
                    item(key = "personalized_end_$pageIndex", contentType = "end") {
                        EndOfFeedIndicator()
                    }
                }
            } else {
                items(
                    count = pagedArticles.itemCount,
                    key = pagedArticles.itemKey { it.id },
                    contentType = { index -> if (isLargeCards || index == 0) "featured" else "row" }
                ) { index ->
                    val article = pagedArticles[index]
                    if (article != null) {
                        if (isLargeCards || index == 0) {
                            val status = articleStatuses[article.id]
                            val effectiveBookmarked = status?.isBookmarked ?: article.isBookmarked
                            FeaturedArticleCard(
                                article = article,
                                isBookmarked = effectiveBookmarked,
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
                    item(key = "paging_append_loading_$pageIndex", contentType = "loading") {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = DarkPrimary)
                        }
                    }
                }
                if (pagedArticles.loadState.append is LoadState.Error) {
                    item(key = "paging_append_error_$pageIndex", contentType = "error") {
                        TextButton(
                            onClick = { pagedArticles.retry() },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Не удалось загрузить. Повторить", color = DarkPrimary) }
                    }
                }
                val appendState = pagedArticles.loadState.append
                if (pagedArticles.itemCount > 0 && appendState is LoadState.NotLoading && appendState.endOfPaginationReached) {
                    item(key = "end_of_list_indicator_$pageIndex", contentType = "end") {
                        EndOfFeedIndicator()
                    }
                }
            }
        }

        ScrollToTopFab(
            visible = { showScrollToTop.value },
            onClick = {
                coroutineScope.launch {
                    listState.animateScrollToItem(0)
                }
            }
        )
    }
}

@Composable
private fun BoxScope.ScrollToTopFab(
    visible: () -> Boolean,
    onClick: () -> Unit
) {
    AnimatedVisibility(
        visible = visible(),
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(end = 16.dp, bottom = 16.dp),
        enter = fadeIn() + scaleIn(initialScale = 0.8f),
        exit = fadeOut() + scaleOut(targetScale = 0.8f)
    ) {
        SmallFloatingActionButton(
            onClick = onClick,
            containerColor = DarkPrimary,
            contentColor = OnDarkPrimary
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = "Наверх",
                tint = OnDarkPrimary,
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
                color = if (isSelected) OnDarkPrimary else TextPrimary
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
    isBookmarked: Boolean,
    onClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    sourceNameIndex: SourceNameIndex = SourceNameIndex()
) {
    val context = LocalContext.current

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
                    val imageModel = remember(article.imageUrl) {
                        ImageRequest.Builder(context)
                            .data(article.imageUrl)
                            .size(600)
                            .crossfade(100)
                            .build()
                    }
                    AsyncImage(
                        model = imageModel,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.1f)))
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                        .clickable { onBookmarkClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isBookmarked) RssIcons.BookmarkFilled else RssIcons.Bookmark,
                        contentDescription = "В закладки",
                        tint = TextPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                val displaySource = remember(article.sourceName, sourceNameIndex) {
                    SourceNameResolver.clean(article.sourceName, sourceNameIndex.map)
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
    sourceNameIndex: SourceNameIndex,
    onClick: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp),
                verticalArrangement = Arrangement.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    val displaySource = remember(article.sourceName, sourceNameIndex) {
                        SourceNameResolver.clean(article.sourceName, sourceNameIndex.map)
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

            if (article.imageUrl.isNotEmpty()) {
                val imageModel = remember(article.imageUrl) {
                    ImageRequest.Builder(context)
                        .data(article.imageUrl)
                        .size(120)
                        .crossfade(100)
                        .build()
                }
                AsyncImage(
                    model = imageModel,
                    contentDescription = null,
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFA0A0A0)),
                    contentScale = ContentScale.Crop
                )
            } else {
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