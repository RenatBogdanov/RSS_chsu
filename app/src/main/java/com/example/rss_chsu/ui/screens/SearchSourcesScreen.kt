package com.example.rss_chsu.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import com.example.rss_chsu.ui.model.RssRepository
import androidx.compose.foundation.lazy.rememberLazyListState
import com.example.rss_chsu.ui.model.Source
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
        val subscribedUrls = realSources.map { it.domain }.toSet()
        val catalog = com.example.rss_chsu.ui.model.PopularSources.topFeeds.map { source ->
            if (subscribedUrls.contains(source.domain)) {
                // Если мы уже подписаны, берём данные из реальных подписок (с именем папки и т.д.)
                realSources.first { it.domain == source.domain }
            } else {
                source
            }
        }
        
        // Добавляем те источники пользователя, которых изначально не было в каталоге
        val catalogUrls = catalog.map { it.domain }.toSet()
        val customUserSources = realSources.filter { !catalogUrls.contains(it.domain) }
        
        customUserSources + catalog
    }

    val filteredSources = remember(searchQuery, selectedTagIndex, allSearchableSources, folderNames) {
        val selectedFolderName = if (selectedTagIndex in folderNames.indices) folderNames[selectedTagIndex] else "Все"
        allSearchableSources.filter { source ->
            val matchesSearch = searchQuery.isBlank() ||
                source.name.contains(searchQuery, ignoreCase = true) ||
                source.domain.contains(searchQuery, ignoreCase = true) ||
                source.description.contains(searchQuery, ignoreCase = true)

            val matchesFolder = selectedFolderName == "Все" ||
                source.folderName == selectedFolderName ||
                (source.folderName == null && selectedFolderName == "Все")

            matchesSearch && matchesFolder
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            // Title & Subtitle
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 8.dp)
                ) {
                    Text(
                        text = "Найти источник",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    /*Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Соберите свою ленту из любимых изданий",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )*/
                }
            }

            // Search Bar Input
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = CardDefaults.outlinedCardBorder()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Поиск по источникам", fontSize = 14.sp, color = TextSecondary) },
                            singleLine = true,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            )
                        )

                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { searchQuery = "" },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Очистить",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // User Folder Chips Filter Row (Show only if user has folders)
            if (realFolders.isNotEmpty()) {
                item {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
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
                        .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (searchQuery.isEmpty()) "Популярные источники" else "Результаты поиска",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Найдено ${filteredSources.size}",
                        fontSize = 13.sp,
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
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(ChipUnselected),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = source.logoText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = source.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.clickable { clipboardManager.setText(AnnotatedString(source.domain)) }
                    ) {
                        Text(
                            text = source.domain,
                            fontSize = 12.sp,
                            color = DarkPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                }
            }

            if (source.isSubscribed || source.folderName != null) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.clickable { onToggleSubscribe() }.padding(start = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = DarkPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "В «Подписках»",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                    }
                }
            } else {
                Button(
                    onClick = {
                        isAdding = true
                        onToggleSubscribe()
                    },
                    enabled = !isAdding,
                    colors = ButtonDefaults.buttonColors(containerColor = DarkPrimary),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(32.dp).padding(start = 8.dp)
                ) {
                    if (isAdding) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Добавить", fontSize = 13.sp, color = Color.White)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = source.description,
            fontSize = 13.sp,
            color = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
