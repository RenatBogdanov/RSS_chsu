package com.example.rss_chsu.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rss_chsu.ui.model.RssRepository
import com.example.rss_chsu.ui.model.Source
import com.example.rss_chsu.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun SearchSourcesScreen(
    onHelpClick: () -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedTagIndex by remember { mutableIntStateOf(0) }
    var showHelpDialog by remember { mutableStateOf(false) }

    val realFolders by RssRepository.folders.collectAsState()
    val realSources by RssRepository.sources.collectAsState()

    val folderNames = remember(realFolders) {
        listOf("Все") + realFolders.map { it.name }
    }

    var selectedFolderForAdd by remember(realFolders) {
        mutableStateOf(realFolders.firstOrNull()?.name)
    }

    var rssUrlInput by remember { mutableStateOf("") }
    var isAddingFeed by remember { mutableStateOf(false) }
    var addStatusMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    val filteredSources = remember(searchQuery, selectedTagIndex, realSources, folderNames) {
        val selectedFolderName = if (selectedTagIndex in folderNames.indices) folderNames[selectedTagIndex] else "Все"
        realSources.filter { source ->
            val matchesSearch = searchQuery.isBlank() ||
                source.name.contains(searchQuery, ignoreCase = true) ||
                source.domain.contains(searchQuery, ignoreCase = true) ||
                source.description.contains(searchQuery, ignoreCase = true)

            val matchesFolder = selectedFolderName == "Все" ||
                source.folderName == selectedFolderName ||
                source.description.contains(selectedFolderName, ignoreCase = true)

            matchesSearch && matchesFolder
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            // Title & Subtitle with ? button
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Найти источник",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Surface(
                            onClick = {
                                showHelpDialog = true
                                onHelpClick()
                            },
                            shape = CircleShape,
                            color = Color.Transparent,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "?",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DarkPrimary
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Соберите свою ленту из любимых изданий",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }
            }

            // Add custom RSS feed card (cleaned up text without "например...")
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = LightSurfaceContainer
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Добавить RSS ленту",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = rssUrlInput,
                            onValueChange = { rssUrlInput = it },
                            placeholder = { Text("https://...", fontSize = 13.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Show folder selector only if user has created folders
                        if (realFolders.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Папка назначения:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(realFolders) { folder ->
                                    val isSelected = selectedFolderForAdd == folder.name
                                    FilterChipPill(
                                        text = folder.name,
                                        isSelected = isSelected,
                                        onClick = { selectedFolderForAdd = folder.name }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = {
                                if (rssUrlInput.isNotBlank()) {
                                    isAddingFeed = true
                                    addStatusMessage = null
                                    coroutineScope.launch {
                                        val targetFolderToUse = if (realFolders.isNotEmpty()) selectedFolderForAdd else null
                                        val success = RssRepository.addFeed(rssUrlInput.trim(), targetFolderToUse)
                                        isAddingFeed = false
                                        if (success) {
                                            addStatusMessage = if (targetFolderToUse != null) "Лента успешно добавлена в «$targetFolderToUse»!" else "Лента успешно добавлена!"
                                            rssUrlInput = ""
                                        } else {
                                            addStatusMessage = "Не удалось загрузить RSS. Проверьте ссылку."
                                        }
                                    }
                                }
                            },
                            enabled = !isAddingFeed,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = DarkPrimary)
                        ) {
                            if (isAddingFeed) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text("Добавить источник")
                        }

                        if (addStatusMessage != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = addStatusMessage!!,
                                fontSize = 13.sp,
                                color = if (addStatusMessage!!.contains("успешно")) Color(0xFF2E7D32) else Color(0xFFC62828)
                            )
                        }
                    }
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
                        .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Подключенные источники",
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
                            text = if (realSources.isEmpty()) "📡 Подключенных источников пока нет. Добавьте RSS-ссылку выше." else "Источников в этой папке не найдено.",
                            fontSize = 14.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                    }
                }
            } else {
                items(filteredSources) { source ->
                    SourceSearchResultRow(
                        source = source,
                        onToggleSubscribe = { }
                    )
                }
            }
        }
    }

    // Help Dialog Instructions
    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            containerColor = LightSurface,
            title = {
                Text(
                    text = "💡 Как найти RSS-ссылку?",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "1. Откройте нужный новостной сайт или блог в браузере.\n" +
                            "2. Найдите значок RSS 🛜 или ссылку «RSS» в подвале сайта (футере) или меню.\n" +
                            "3. Скопируйте прямую ссылку на RSS или Atom поток и вставьте её в поле ввода.\n\n" +
                            "Нажмите на любой пример ниже, чтобы вставить его:",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        lineHeight = 18.sp
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = ChipUnselected,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                rssUrlInput = "https://lenta.ru/rss/news"
                                showHelpDialog = false
                            }
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(text = "Лента.ру", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text(text = "https://lenta.ru/rss/news", fontSize = 11.sp, color = DarkPrimary)
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = ChipUnselected,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                rssUrlInput = "https://www.interfax.ru/rss.asp"
                                showHelpDialog = false
                            }
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(text = "Интерфакс", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text(text = "https://www.interfax.ru/rss.asp", fontSize = 11.sp, color = DarkPrimary)
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = ChipUnselected,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                rssUrlInput = "https://rssexport.rbc.ru/rbcnews/news/30/full.rss"
                                showHelpDialog = false
                            }
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(text = "РБК", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text(text = "https://rssexport.rbc.ru/rbcnews/news/30/full.rss", fontSize = 11.sp, color = DarkPrimary)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showHelpDialog = false }) {
                    Text("Понятно", fontWeight = FontWeight.Bold, color = DarkPrimary)
                }
            }
        )
    }
}

@Composable
fun SourceSearchResultRow(
    source: Source,
    onToggleSubscribe: () -> Unit
) {
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

                Column {
                    Text(
                        text = source.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = source.domain,
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.clickable { onToggleSubscribe() }
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = DarkPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "В «${source.folderName ?: "Подписках"}»",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = source.description,
            fontSize = 13.sp,
            color = TextSecondary,
            lineHeight = 18.sp
        )
    }
}
