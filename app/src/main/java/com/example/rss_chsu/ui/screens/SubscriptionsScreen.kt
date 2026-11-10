package com.example.rss_chsu.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
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
import com.example.rss_chsu.ui.components.RssIcons
import com.example.rss_chsu.ui.model.Folder
import com.example.rss_chsu.ui.model.RssRepository
import com.example.rss_chsu.ui.model.Source
import com.example.rss_chsu.ui.theme.*
import kotlinx.coroutines.launch

import androidx.compose.foundation.lazy.rememberLazyListState

@Composable
fun SubscriptionsScreen(
    onAddSourceClick: () -> Unit = {},
    onMenuClick: () -> Unit = {}
) {
    val realFolders by RssRepository.folders.collectAsState()
    val realSources by RssRepository.sources.collectAsState()
    val realArticles by RssRepository.articles.collectAsState()

    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var newFolderNameInput by remember { mutableStateOf("") }

    var selectedFolderForAdd by remember(realFolders) {
        mutableStateOf(realFolders.firstOrNull()?.name)
    }

    var isAddingFeed by remember { mutableStateOf(false) }
    var addStatusMessage by remember { mutableStateOf<String?>(null) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var sourceToMove by remember { mutableStateOf<Source?>(null) }
    
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var rssUrlInput by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp)
        ) {
            // Header Title & Subtitle
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 8.dp, top = 2.dp, bottom = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Подписки",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            /*IconButton(
                                onClick = { showHelpDialog = true },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Text(
                                    text = "?",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DarkPrimary
                                )
                            }*/
                            IconButton(onClick = onMenuClick) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "Меню",
                                    tint = DarkPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Источников: ${realSources.size} · статей: ${realArticles.size}",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }
            }

            // Add custom RSS feed card
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = LightSurfaceContainer
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Добавить RSS ленту",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            IconButton(
                                onClick = { showHelpDialog = true },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Text(
                                    text = "?",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DarkPrimary
                                )
                            }
                        }
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

            // All Articles Banner Card
            /*item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clip(RoundedCornerShape(14.dp)),
                    color = LightSurfaceContainer
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = RssIcons.Subscriptions,
                                contentDescription = null,
                                tint = DarkPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Все статьи",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = ChipUnselected
                        ) {
                            Text(
                                text = "${realArticles.size}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }*/

            if (realFolders.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Ваши папки",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                }

                items(
                    items = realFolders,
                    key = { it.id }
                ) { folder ->
                    val sourcesInFolder = realSources.filter { it.folderName == folder.name }
                    SwipeToDeleteContainer(
                        onDelete = { RssRepository.deleteFolder(folder.id) }
                    ) {
                        FolderItemRow(
                            folder = folder,
                            sourcesInFolder = sourcesInFolder,
                            onToggleExpand = {
                                RssRepository.toggleFolderExpanded(folder.id)
                            },
                            onDeleteSource = { sourceId -> 
                                RssRepository.deleteSource(sourceId) 
                            },
                            onMoveSource = { source ->
                                sourceToMove = source
                            }
                        )
                    }
                }

                val unassignedSources = realSources.filter { it.folderName == null }
                if (unassignedSources.isNotEmpty()) {
                    item {
                        Text(
                            text = "Без папки",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp)
                        )
                    }
                    items(
                        items = unassignedSources,
                        key = { it.id }
                    ) { source ->
                        SwipeToDeleteContainer(
                            onDelete = { RssRepository.deleteSource(source.domain) }
                        ) {
                            SourceSubItemRow(
                                source = source, 
                                onMoveClick = { sourceToMove = source }
                            )
                        }
                    }
                }
            } else if (realSources.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "У вас пока нет источников.\nДобавьте RSS-ленту выше, чтобы подгрузить новости.",
                            fontSize = 14.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                    }
                }
            } else {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Ваши источники",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                }

                items(
                    items = realSources,
                    key = { it.id }
                ) { source ->
                    SwipeToDeleteContainer(
                        onDelete = { RssRepository.deleteSource(source.domain) }
                    ) {
                        SourceSubItemRow(
                            source = source,
                            onMoveClick = { sourceToMove = source }
                        )
                    }
                }
            }
        }

        // Floating Bottom Pill Button: "+ Создать новую папку"
        Button(
            onClick = { showCreateFolderDialog = true },
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(containerColor = DarkPrimary),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp)
                .height(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Создать новую папку",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }

        // Create Folder Dialog
        if (showCreateFolderDialog) {
            AlertDialog(
                onDismissRequest = { showCreateFolderDialog = false },
                containerColor = LightSurface,
                title = { Text("Создать новую папку", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary) },
                text = {
                    OutlinedTextField(
                        value = newFolderNameInput,
                        onValueChange = { newFolderNameInput = it },
                        placeholder = { Text("Название папки", color = TextSecondary) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DarkPrimary,
                            unfocusedBorderColor = LightCardBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (newFolderNameInput.isNotBlank()) {
                                RssRepository.createFolder(newFolderNameInput.trim())
                                newFolderNameInput = ""
                                showCreateFolderDialog = false
                            }
                        }
                    ) {
                        Text("Создать", fontWeight = FontWeight.Bold, color = DarkPrimary)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCreateFolderDialog = false }) {
                        Text("Отмена", color = TextSecondary)
                    }
                }
            )
        }

        // Help Dialog Instructions
        if (showHelpDialog) {
            AlertDialog(
                onDismissRequest = { showHelpDialog = false },
                containerColor = LightSurface,
                title = {
                    Text(
                        text = "Как найти RSS-ссылку?",
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
                                "2. Найдите значок RSS или ссылку «RSS» в подвале сайта (футере) или меню.\n" +
                                "3. Скопируйте прямую ссылку на RSS и вставьте её в поле ввода.\n\n" +
                                "Или нажмите на любой пример ниже, чтобы вставить его:",
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

        // Dialog for moving a source to a folder
        if (sourceToMove != null) {
            AlertDialog(
                onDismissRequest = { sourceToMove = null },
                containerColor = LightSurface,
                title = { Text("Переместить источник", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary) },
                text = {
                    Column {
                        Text(
                            text = "Выберите папку для «${sourceToMove?.name}»",
                            fontSize = 14.sp,
                            color = TextSecondary,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp)
                        ) {
                            item {
                                Text(
                                    text = "Без папки",
                                    fontSize = 16.sp,
                                    color = TextPrimary,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            RssRepository.updateSourceFolder(sourceToMove!!.domain, null)
                                            sourceToMove = null
                                        }
                                        .padding(vertical = 12.dp, horizontal = 8.dp)
                                )
                            }
                            items(realFolders) { folder ->
                                Text(
                                    text = folder.name,
                                    fontSize = 16.sp,
                                    color = TextPrimary,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            RssRepository.updateSourceFolder(sourceToMove!!.domain, folder.name)
                                            sourceToMove = null
                                        }
                                        .padding(vertical = 12.dp, horizontal = 8.dp)
                                )
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { sourceToMove = null }) {
                        Text("Отмена", color = TextSecondary)
                    }
                }
            )
        }
    }
}

@Composable
fun FolderItemRow(
    folder: Folder,
    sourcesInFolder: List<Source>,
    onToggleExpand: () -> Unit,
    onDeleteSource: (String) -> Unit,
    onMoveSource: (Source) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleExpand() }
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = RssIcons.Folder,
                    contentDescription = null,
                    tint = TextPrimary,
                    modifier = Modifier.size(20.dp)
                )

                Column {
                    Text(
                        text = folder.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "${sourcesInFolder.size} источников",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (folder.isExpanded) Icons.Default.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Sub-sources inside expanded folder
        AnimatedVisibility(visible = folder.isExpanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 48.dp, end = 16.dp)
            ) {
                if (sourcesInFolder.isEmpty()) {
                    Text(
                        text = "В этой папке пока нет источников",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                } else {
                    sourcesInFolder.forEach { source ->
                        SwipeToDeleteContainer(
                            onDelete = { onDeleteSource(source.domain) }
                        ) {
                            SourceSubItemRow(source = source, horizontalPadding = 0.dp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SourceSubItemRow(
    source: Source,
    horizontalPadding: androidx.compose.ui.unit.Dp = 16.dp,
    onMoveClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onMoveClick?.invoke() }
            .padding(horizontal = horizontalPadding, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Logo Badge
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
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
                    color = TextPrimary
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

        if (source.unreadCount > 0) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = ChipUnselected
            ) {
                Text(
                    text = "${source.unreadCount}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeToDeleteContainer(
    onDelete: () -> Unit,
    content: @Composable () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else {
                false
            }
        },
        positionalThreshold = { it * 0.5f }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            val color by androidx.compose.animation.animateColorAsState(
                targetValue = when (dismissState.targetValue) {
                    SwipeToDismissBoxValue.EndToStart -> Color(0xFFFF5252)
                    else -> MaterialTheme.colorScheme.background
                }, label = "color"
            )
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color)
                    .padding(end = 16.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) {
                    Text(
                        text = "Удалить",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        content = {
            Box(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
                content()
            }
        }
    )
}
