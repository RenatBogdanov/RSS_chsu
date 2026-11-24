package com.example.rss_chsu.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.rss_chsu.ui.components.ScrollToTopFab
import com.example.rss_chsu.ui.components.TopAppLogoBar
import com.example.rss_chsu.ui.model.PopularSources
import com.example.rss_chsu.ui.model.RecommendedSource
import com.example.rss_chsu.ui.model.RssRepository
import com.example.rss_chsu.ui.model.Source
import com.example.rss_chsu.ui.model.SourceCategories
import com.example.rss_chsu.ui.model.SourceCategory
import com.example.rss_chsu.ui.model.SourceRecommendationsViewModel
import com.example.rss_chsu.ui.model.extractDomainKeywords
import com.example.rss_chsu.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Сколько источников показывать в каждой категории, пока она не раскрыта целиком. */
private const val SECTION_PREVIEW_COUNT = 5

@Composable
fun SearchSourcesScreen(
    sourcesViewModel: SourceRecommendationsViewModel = viewModel()
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedTagIndex by remember { mutableIntStateOf(0) }
    var selectedCategory by remember { mutableStateOf<SourceCategory?>(null) }
    var scrollRequest by remember { mutableIntStateOf(0) }
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // ── Кнопка «Наверх» ────────────────────────────────────────────────
    val showScrollToTop = remember {
        derivedStateOf { listState.canScrollBackward }
    }
    // ───────────────────────────────────────────────────────────────────

    val realFolders by RssRepository.folders.collectAsState()
    val realSources by RssRepository.sources.collectAsState()
    val recommended by sourcesViewModel.recommendedSources.collectAsState()
    val subscribing by sourcesViewModel.subscribing.collectAsState()

    var previousSubscribing by remember { mutableStateOf<Set<String>>(emptySet()) }
    var bannerError by remember { mutableStateOf<String?>(null) }
    var bannerTrigger by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) { sourcesViewModel.loadPreviews() }

    // Отслеживаем завершение подписки: если источник не добавился — показываем плашку
    LaunchedEffect(subscribing, realSources) {
        val finishedDomains = previousSubscribing - subscribing
        finishedDomains.forEach { domain ->
            val isNowSubscribed = realSources.any {
                it.domain.equals(domain, ignoreCase = true) ||
                        (extractDomainKeywords(it.domain).isNotEmpty() &&
                                extractDomainKeywords(it.domain) == extractDomainKeywords(domain))
            }
            if (!isNowSubscribed) {
                bannerError = "Не удалось добавить источник ($domain)"
                bannerTrigger++
            }
        }
        previousSubscribing = subscribing
    }

    // Автоматически скрываем плашку через 3 секунды
    LaunchedEffect(bannerTrigger) {
        if (bannerTrigger > 0 && bannerError != null) {
            delay(3000L)
            bannerError = null
        }
    }

    val folderNames = remember(realFolders) {
        listOf("Все") + realFolders.map { it.name }
    }

    val allSearchableSources = remember(realSources) {
        val catalog = PopularSources.topFeeds.map { catalogSource ->
            val userSource = realSources.find { userSrc ->
                userSrc.domain.equals(catalogSource.domain, ignoreCase = true) ||
                        (extractDomainKeywords(userSrc.domain).isNotEmpty() &&
                                extractDomainKeywords(userSrc.domain) == extractDomainKeywords(catalogSource.domain))
            }
            if (userSource != null) {
                userSource.copy(
                    name = catalogSource.name,
                    description = if (catalogSource.description.isNotBlank()) catalogSource.description else userSource.description,
                    logoText = catalogSource.logoText,
                    isSubscribed = true
                )
            } else {
                catalogSource
            }
        }

        val catalogKeywords = PopularSources.topFeeds.flatMap { extractDomainKeywords(it.domain) }.toSet()
        val customUserSources = realSources.filter { userSrc ->
            extractDomainKeywords(userSrc.domain).none { catalogKeywords.contains(it) }
        }

        customUserSources + catalog
    }

    val selectedFolderName = if (selectedTagIndex in folderNames.indices) folderNames[selectedTagIndex] else "Все"
    val isSearching = searchQuery.isNotBlank()
    val showDiscovery = !isSearching && selectedFolderName == "Все"

    val filteredSources = remember(searchQuery, selectedFolderName, allSearchableSources) {
        allSearchableSources.filter { source ->
            val matchesSearch = searchQuery.isBlank() ||
                    source.name.contains(searchQuery, ignoreCase = true) ||
                    source.domain.contains(searchQuery, ignoreCase = true) ||
                    source.description.contains(searchQuery, ignoreCase = true)

            val matchesFolder = selectedFolderName == "Все" || source.folderName == selectedFolderName

            matchesSearch && matchesFolder
        }
    }

    val categorized = remember(allSearchableSources) {
        allSearchableSources.groupBy { SourceCategories.of(it) }
    }

    val activeCategories = remember(categorized) {
        SourceCategories.inOrder.filter { !categorized[it].isNullOrEmpty() }
    }

    val onSubscribe: (Source) -> Unit = { sourcesViewModel.subscribe(it) }

    val targetSectionIndex = remember(realFolders.isNotEmpty(), recommended.isNotEmpty(), activeCategories, selectedCategory) {
        val folderOffset = if (realFolders.isNotEmpty()) 1 else 0
        val chipsOffset = 1
        val recOffset = if (recommended.isNotEmpty()) 2 else 0
        val baseOffset = folderOffset + chipsOffset + recOffset

        if (selectedCategory == null) {
            baseOffset
        } else {
            baseOffset
        }
    }

    LaunchedEffect(scrollRequest) {
        if (scrollRequest > 0) {
            listState.animateScrollToItem(targetSectionIndex.coerceAtLeast(0))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppLogoBar(
            content = {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, ChipUnselected)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Поиск по источникам",
                                    fontSize = 16.sp,
                                    color = TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                singleLine = true,
                                textStyle = LocalTextStyle.current.copy(
                                    fontSize = 16.sp,
                                    color = TextPrimary
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { searchQuery = "" },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Очистить",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        )

        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {
                if (realFolders.isNotEmpty()) {
                    item(key = "folder_chips") {
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(folderNames.size) { index ->
                                val isSelected = selectedTagIndex == index
                                FilterChipPill(
                                    text = folderNames[index],
                                    isSelected = isSelected,
                                    onClick = { selectedTagIndex = index }
                                )
                            }
                        }
                    }
                }
                if (showDiscovery) {
                    item(key = "category_chips") {
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            item {
                                FilterChipPill(
                                    text = "Все категории",
                                    isSelected = selectedCategory == null,
                                    onClick = {
                                        selectedCategory = null
                                        scrollRequest++
                                    }
                                )
                            }
                            items(activeCategories, key = { "chip_${it.name}" }) { category ->
                                FilterChipPill(
                                    text = category.title,
                                    isSelected = selectedCategory == category,
                                    onClick = {
                                        selectedCategory = category
                                        scrollRequest++
                                    }
                                )
                            }
                        }
                    }
                    if (recommended.isNotEmpty()) {
                        item(key = "rec_header") {
                            SectionHeader(
                                title = "Рекомендуем вам",
                                trailing = "${recommended.size}"
                            )
                        }
                        item(key = "rec_carousel") {
                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(
                                    recommended,
                                    key = { "rec|${it.source.domain}|${it.source.name}" }
                                ) { rec ->
                                    RecommendedSourceCard(
                                        rec = rec,
                                        isLoading = rec.source.domain in subscribing,
                                        onSubscribe = { onSubscribe(rec.source) }
                                    )
                                }
                            }
                        }
                    }
                    val category = selectedCategory
                    if (category == null) {
                        activeCategories.forEach { section ->
                            val list = categorized[section].orEmpty()
                            item(key = "sec_${section.name}") {
                                SectionHeader(title = section.title, trailing = "${list.size}")
                            }
                            items(
                                list.take(SECTION_PREVIEW_COUNT),
                                key = { "sec_${section.name}|${it.domain}|${it.name}" }
                            ) { source ->
                                SourceRow(source = source, subscribing = subscribing, onSubscribe = onSubscribe)
                            }
                            if (list.size > SECTION_PREVIEW_COUNT) {
                                item(key = "more_${section.name}") {
                                    ShowAllRow(
                                        count = list.size,
                                        onClick = {
                                            selectedCategory = section
                                            scrollRequest++
                                        }
                                    )
                                }
                            }
                        }
                        val others = categorized[null].orEmpty()
                        if (others.isNotEmpty()) {
                            item(key = "sec_custom") {
                                SectionHeader(title = "Добавленные вами", trailing = "${others.size}")
                            }
                            items(others, key = { "sec_custom|${it.domain}|${it.name}" }) { source ->
                                SourceRow(source = source, subscribing = subscribing, onSubscribe = onSubscribe)
                            }
                        }
                    } else {
                        val list = categorized[category].orEmpty()
                        item(key = "sec_selected_header") {
                            SectionHeader(title = category.title, trailing = "${list.size}")
                        }
                        items(list, key = { "cat|${it.domain}|${it.name}" }) { source ->
                            SourceRow(source = source, subscribing = subscribing, onSubscribe = onSubscribe)
                        }
                    }
                } else {
                    item(key = "flat_header") {
                        SectionHeader(
                            title = if (isSearching) "Результаты поиска" else selectedFolderName,
                            trailing = "Найдено ${filteredSources.size}"
                        )
                    }
                    if (filteredSources.isEmpty()) {
                        item(key = "flat_empty") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Источников не найдено.",
                                    fontSize = 14.sp,
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 20.sp
                                )
                            }
                        }
                    } else {
                        items(filteredSources, key = { "flat|${it.domain}|${it.name}" }) { source ->
                            SourceRow(source = source, subscribing = subscribing, onSubscribe = onSubscribe)
                        }
                    }
                }
            }

            // Плашка ошибки поверх ленты (BoxScope-расширение, поэтому align работает корректно)
            SubscriptionErrorBanner(
                message = bannerError,
                modifier = Modifier.align(Alignment.TopCenter),
                onDismiss = { bannerError = null }
            )

            // ── Кнопка «Наверх» ────────────────────────────────────────
            ScrollToTopFab(
                visible = { showScrollToTop.value },
                onClick = {
                    coroutineScope.launch {
                        listState.animateScrollToItem(0)
                    }
                }
            )
            // ───────────────────────────────────────────────────────────
        }
    }
}

/**
 * Плашка ошибки подписки.
 * Объявлена как расширение на BoxScope — это гарантирует, что AnimatedVisibility
 * и Modifier.align относятся к BoxScope, а не к ColumnScope.
 * Поддерживает смахивание в сторону для досрочного закрытия.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BoxScope.SubscriptionErrorBanner(
    message: String?,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value != SwipeToDismissBoxValue.Settled) {
                onDismiss()
                true
            } else false
        }
    )

    LaunchedEffect(message) {
        if (message != null) {
            dismissState.reset()
        }
    }

    AnimatedVisibility(
        visible = message != null,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
    ) {
        SwipeToDismissBox(
            state = dismissState,
            modifier = Modifier.fillMaxWidth(),
            backgroundContent = {},
            enableDismissFromStartToEnd = true,
            enableDismissFromEndToStart = true,
            gesturesEnabled = message != null
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFFFECEE),
                border = BorderStroke(1.dp, Color(0xFFF5B8BF)),
                shadowElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Color(0xFFB3261E),
                        modifier = Modifier.size(17.dp)
                    )
                    Text(
                        text = message.orEmpty(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF7D1A15),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun RecommendedSourceCard(
    rec: RecommendedSource,
    isLoading: Boolean,
    onSubscribe: () -> Unit
) {
    val source = rec.source
    val isAdded = source.isSubscribed || source.folderName != null
    Surface(
        modifier = Modifier
            .width(248.dp)
            .height(116.dp),
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, ChipUnselected)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(ChipUnselected),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = source.logoText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = source.name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = source.domain,
                            fontSize = 11.sp,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(if (isAdded) ChipUnselected else DarkPrimary)
                        .clickable(enabled = !isAdded && !isLoading, onClick = onSubscribe),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(13.dp),
                            color = OnDarkPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = if (isAdded) Icons.Default.Check else Icons.Default.Add,
                            contentDescription = if (isAdded) "В подписках" else "Добавить",
                            tint = if (isAdded) TextPrimary else OnDarkPrimary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
            Text(
                text = source.description.ifBlank { recommendationHint(rec) },
                fontSize = 12.sp,
                color = TextSecondary,
                minLines = 2,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 16.sp
            )
            Text(
                text = recommendationHint(rec),
                fontSize = 11.sp,
                color = DarkPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun recommendationHint(rec: RecommendedSource): String {
    val reason = when {
        rec.basedOnInterests && rec.score > 0.0 -> "Подходит под ваши интересы"
        rec.score > 0.0 -> "Похоже на ваши подписки"
        else -> "Популярный источник"
    }
    val category = SourceCategories.of(rec.source)?.title
    return if (category != null) "$category · $reason" else reason
}

@Composable
private fun SectionHeader(title: String, trailing: String? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        if (trailing != null) {
            Text(
                text = trailing,
                fontSize = 12.sp,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun ShowAllRow(count: Int, onClick: () -> Unit) {
    Text(
        text = "Показать все ($count)",
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        color = DarkPrimary,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp)
    )
}

@Composable
private fun SourceRow(
    source: Source,
    hint: String? = null,
    subscribing: Set<String>,
    onSubscribe: (Source) -> Unit
) {
    SourceSearchResultRow(
        source = source,
        onToggleSubscribe = {
            if (!source.isSubscribed && source.folderName == null) onSubscribe(source)
        },
        hint = hint,
        isLoading = source.domain in subscribing
    )
}

@Composable
fun SourceSearchResultRow(
    source: Source,
    onToggleSubscribe: () -> Unit,
    hint: String? = null,
    isLoading: Boolean? = null
) {
    var isAdding by remember { mutableStateOf(false) }
    val adding = isLoading ?: isAdding
    val clipboardManager = LocalClipboardManager.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(ChipUnselected),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = source.logoText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = source.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = source.domain,
                        fontSize = 11.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable { clipboardManager.setText(AnnotatedString(source.domain)) }
                    )
                }
            }
            if (source.isSubscribed || source.folderName != null) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(ChipUnselected)
                        .clickable { onToggleSubscribe() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "В подписках",
                        tint = TextPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(DarkPrimary)
                        .clickable(enabled = !adding) {
                            if (isLoading == null) isAdding = true
                            onToggleSubscribe()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (adding) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            color = OnDarkPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Добавить",
                            tint = OnDarkPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
        if (source.description.isNotBlank()) {
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = source.description,
                fontSize = 12.sp,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (hint != null) {
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = hint,
                fontSize = 11.sp,
                color = DarkPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        thickness = 0.5.dp,
        color = ChipUnselected.copy(alpha = 0.6f)
    )
}