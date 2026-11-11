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
import com.example.rss_chsu.ui.components.TopAppLogoBar
import com.example.rss_chsu.ui.model.PopularSources
import com.example.rss_chsu.ui.model.RssRepository
import com.example.rss_chsu.ui.model.Source
import com.example.rss_chsu.ui.model.extractDomainKeywords
import com.example.rss_chsu.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun SearchSourcesScreen() {
    var searchQuery by remember { mutableStateOf("") }
    var selectedTagIndex by remember { mutableIntStateOf(0) }
    val listState = rememberLazyListState()

    val realFolders by RssRepository.folders.collectAsState()
    val realSources by RssRepository.sources.collectAsState()
    
    val coroutineScope = rememberCoroutineScope()

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

    val filteredSources = remember(searchQuery, selectedTagIndex, allSearchableSources, folderNames) {
        val selectedFolderName = if (selectedTagIndex in folderNames.indices) folderNames[selectedTagIndex] else "Все"
        allSearchableSources.filter { source ->
            val matchesSearch = searchQuery.isBlank() ||
                source.name.contains(searchQuery, ignoreCase = true) ||
                source.domain.contains(searchQuery, ignoreCase = true) ||
                source.description.contains(searchQuery, ignoreCase = true)

            val matchesFolder = selectedFolderName == "Все" || source.folderName == selectedFolderName

            matchesSearch && matchesFolder
        }
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
                item {
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

            // Section Header: Источники | Найдено
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (searchQuery.isEmpty()) "Популярные источники" else "Результаты поиска",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Найдено ${filteredSources.size}",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }

            if (filteredSources.isEmpty()) {
                item {
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
                items(filteredSources) { source ->
                    var localSourceState by remember(source) { mutableStateOf(source) }
                    
                    SourceSearchResultRow(
                        source = localSourceState,
                        onToggleSubscribe = {
                            if (!localSourceState.isSubscribed && localSourceState.folderName == null) {
                                coroutineScope.launch {
                                    val success = RssRepository.addFeed(localSourceState.domain, null)
                                    if (success) {
                                        localSourceState = localSourceState.copy(isSubscribed = true)
                                    }
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun SourceSearchResultRow(
    source: Source,
    onToggleSubscribe: () -> Unit
) {
    var isAdding by remember { mutableStateOf(false) }
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
                        .clickable(enabled = !isAdding) {
                            isAdding = true
                            onToggleSubscribe()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isAdding) {
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
    }
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        thickness = 0.5.dp,
        color = ChipUnselected.copy(alpha = 0.6f)
    )
}
