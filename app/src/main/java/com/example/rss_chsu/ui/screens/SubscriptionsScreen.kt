package com.example.rss_chsu.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import com.example.rss_chsu.ui.components.TopAppLogoBar
import com.example.rss_chsu.ui.model.Folder
import com.example.rss_chsu.ui.model.RssRepository
import com.example.rss_chsu.ui.model.Source
import com.example.rss_chsu.ui.model.matchesArticle
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppLogoBar(
            title = "ПОДПИСКИ (${realSources.size})",
            rightAction = {
                IconButton(
                    onClick = onMenuClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Меню",
                        tint = DarkPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        )

        // Pinned Top Compact Add RSS Bar
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, ChipUnselected)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Help Button "?"
                IconButton(
                    onClick = { showHelpDialog = true },
                    modifier = Modifier.size(28.dp)
                ) {
                    Text(
                        text = "?",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkPrimary
                    )
                }

                Spacer(modifier = Modifier.width(4.dp))

                // TextField for RSS URL
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (rssUrlInput.isEmpty()) {
                        Text(
                            text = "Ссылка на RSS (https://...)",
                            fontSize = 13.sp,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    BasicTextField(
                        value = rssUrlInput,
                        onValueChange = { rssUrlInput = it },
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(
                            fontSize = 13.sp,
                            color = TextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Plus IconButton in a Circle
                IconButton(
                    onClick = {
                        if (rssUrlInput.isNotBlank()) {
                            isAddingFeed = true
                            addStatusMessage = null
                            coroutineScope.launch {
                                val targetFolderToUse = if (realFolders.isNotEmpty()) selectedFolderForAdd else null
                                val success = RssRepository.addFeed(rssUrlInput.trim(), targetFolderToUse)
                                isAddingFeed = false
                                if (success) {
                                    addStatusMessage = "Лента успешно добавлена!"
                                    rssUrlInput = ""
                                } else {
                                    addStatusMessage = "Не удалось загрузить RSS."
                                }
                            }
                        }
                    },
                    enabled = !isAddingFeed && rssUrlInput.isNotBlank(),
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(if (rssUrlInput.isNotBlank()) DarkPrimary else ChipUnselected)
                ) {
                    if (isAddingFeed) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Добавить",
                            tint = if (rssUrlInput.isNotBlank()) Color.White else TextSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        if (addStatusMessage != null) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (addStatusMessage!!.contains("успешно")) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                border = BorderStroke(1.dp, if (addStatusMessage!!.contains("успешно")) Color(0xFF81C784) else Color(0xFFE57373)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp)
            ) {
                Text(
                    text = addStatusMessage!!,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (addStatusMessage!!.contains("успешно")) Color(0xFF2E7D32) else Color(0xFFC62828),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
        }

        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 120.dp)
            ) {

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
                            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Ваши папки",
                            fontSize = 14.sp,
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
                            articlesList = realArticles,
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
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 4.dp)
                        )
                    }
                    items(
                        items = unassignedSources,
                        key = { it.id }
                    ) { source ->
                        val count = realArticles.count { source.matchesArticle(it) }
                        SwipeToDeleteContainer(
                            onDelete = { RssRepository.deleteSource(source.domain) }
                        ) {
                            SourceSubItemRow(
                                source = source, 
                                articleCount = count,
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
                            .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Ваши источники",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                }

                items(
                    items = realSources,
                    key = { it.id }
                ) { source ->
                    val count = realArticles.count { source.matchesArticle(it) }
                    SwipeToDeleteContainer(
                        onDelete = { RssRepository.deleteSource(source.domain) }
                    ) {
                        SourceSubItemRow(
                            source = source,
                            articleCount = count,
                            onMoveClick = { sourceToMove = source }
                        )
                    }
                }
            }
        }

        // Floating Bottom Pill Button: "+ Создать папку"
        Button(
            onClick = { showCreateFolderDialog = true },
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(containerColor = DarkPrimary),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp)
                .height(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Создать папку",
                fontSize = 13.sp,
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
                                    rssUrlInput = "https://cherinfo.ru/rss/news"
                                    showHelpDialog = false
                                }
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Text(text = "ЧерИнфо", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text(text = "https://cherinfo.ru/rss/news", fontSize = 11.sp, color = DarkPrimary)
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
                                Text(
                                    text = "https://rssexport.rbc.ru/rbcnews/news/30/full.rss",
                                    fontSize = 11.sp,
                                    color = DarkPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
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
}

@Composable
fun FolderItemRow(
    folder: Folder,
    sourcesInFolder: List<Source>,
    articlesList: List<com.example.rss_chsu.ui.model.Article> = emptyList(),
    onToggleExpand: () -> Unit,
    onDeleteSource: (String) -> Unit,
    onMoveSource: (Source) -> Unit
) {
    val totalFolderArticles = remember(sourcesInFolder, articlesList) {
        articlesList.count { article -> sourcesInFolder.any { source -> source.matchesArticle(article) } }
    }

    Column(
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleExpand() }
                .padding(horizontal = 16.dp, vertical = 6.dp),
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
                        text = "${sourcesInFolder.size} источников · $totalFolderArticles статей",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = ChipUnselected
                ) {
                    Text(
                        text = "$totalFolderArticles",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
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
                        val count = articlesList.count { source.matchesArticle(it) }
                        SwipeToDeleteContainer(
                            onDelete = { onDeleteSource(source.domain) }
                        ) {
                            SourceSubItemRow(
                                source = source, 
                                articleCount = count,
                                horizontalPadding = 0.dp
                            )
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
    articleCount: Int = 0,
    horizontalPadding: androidx.compose.ui.unit.Dp = 16.dp,
    onMoveClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onMoveClick?.invoke() }
            .padding(horizontal = horizontalPadding, vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
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

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = ChipUnselected,
            modifier = Modifier.padding(start = 8.dp)
        ) {
            Text(
                text = "$articleCount",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
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
