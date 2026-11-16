package com.example.rss_chsu.ui.screens

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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
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

    val realFolders by RssRepository.folders.collectAsState()
    val realSources by RssRepository.sources.collectAsState()
    val recommended by sourcesViewModel.recommendedSources.collectAsState()
    val subscribing by sourcesViewModel.subscribing.collectAsState()

    // Пробная загрузка лент каталога, чтобы оценить, какие из них подходят пользователю
    // (не чаще раза в несколько часов, лент за раз ограниченное число)
    LaunchedEffect(Unit) { sourcesViewModel.loadPreviews() }

    val folderNames = remember(realFolders) {
        listOf("Все") + realFolders.map { it.name }
    }

    // Объединяем подписки пользователя и каталог популярных источников
    val allSearchableSources = remember(realSources) {
        val catalog = PopularSources.topFeeds.map { catalogSource ->
            val userSource = realSources.find { userSrc ->
                userSrc.domain.equals(catalogSource.domain, ignoreCase = true) ||
                        (extractDomainKeywords(userSrc.domain).isNotEmpty() &&
                                extractDomainKeywords(userSrc.domain) == extractDomainKeywords(catalogSource.domain))
            }
            if (userSource != null) {
                // Если мы уже подписаны, берём данные из реальных подписок, НО СОХРАНЯЕМ аккуратное имя из каталога
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

    // Обзор по категориям показываем, только когда пользователь не ищет и не открыл папку
    val showDiscovery = !isSearching && selectedFolderName == "Все"

    // Плоский список: результаты поиска или содержимое выбранной папки
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

    // Источники по категориям (ключ null: добавлены по ссылке, в каталоге их нет)
    val categorized = remember(allSearchableSources) {
        allSearchableSources.groupBy { SourceCategories.of(it) }
    }

    val onSubscribe: (Source) -> Unit = { sourcesViewModel.subscribe(it) }

    // Позиция строки с чипами категорий в списке: нужна, чтобы после выбора категории показать её сверху
    val chipsIndex = (if (realFolders.isNotEmpty()) 1 else 0) +
            (if (recommended.isNotEmpty()) 1 + recommended.size else 0)

    LaunchedEffect(scrollRequest) {
        if (scrollRequest > 0) listState.scrollToItem(chipsIndex)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Шапка в газетном стиле как в "Новости", включающая строку поиска
        TopAppLogoBar(
            content = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ПОИСК",
                        fontFamily = FontFamily.Serif,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = DarkPrimary,
                        letterSpacing = 2.sp
                    )

                    Spacer(modifier = Modifier.width(10.dp))

                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .height(32.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, ChipUnselected)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )

                            Spacer(modifier = Modifier.width(6.dp))

                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (searchQuery.isEmpty()) {
                                    Text(
                                        text = "Поиск по источникам",
                                        fontSize = 13.sp,
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
                                        fontSize = 13.sp,
                                        color = TextPrimary
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            if (searchQuery.isNotEmpty()) {
                                IconButton(
                                    onClick = { searchQuery = "" },
                                    modifier = Modifier.size(20.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Очистить",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        )

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            // User Folder Chips Filter Row (Show only if user has folders)
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
                // 1. Рекомендации: по закладкам, прочитанному, подпискам и категориям
                if (recommended.isNotEmpty()) {
                    item(key = "rec_header") {
                        SectionHeader(title = "Рекомендуем вам")
                    }
                    items(recommended, key = { "rec|" + it.source.domain + "|" + it.source.name }) { rec ->
                        SourceRow(
                            source = rec.source,
                            hint = recommendationHint(rec),
                            subscribing = subscribing,
                            onSubscribe = onSubscribe
                        )
                    }
                }

                // 2. Чипы категорий
                item(key = "category_chips") {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
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
                        items(SourceCategories.inOrder.filter { !categorized[it].isNullOrEmpty() }) { category ->
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

                // 3. Источники: все категории разделами или одна выбранная целиком
                val category = selectedCategory
                if (category == null) {
                    SourceCategories.inOrder.forEach { section ->
                        val list = categorized[section].orEmpty()
                        if (list.isEmpty()) return@forEach

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
                // Поиск или содержимое папки: плоский список, как раньше
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
    }
}

/** Почему источник рекомендован: категория и причина. */
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

/** Строка источника. Подписка идёт через ViewModel, поэтому индикатор сбрасывается и при ошибке. */
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
    // Если передан, индикатор загрузки управляется снаружи; иначе, как раньше, включается по нажатию
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
                        color = DarkPrimary,
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
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Добавить",
                            tint = Color.White,
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