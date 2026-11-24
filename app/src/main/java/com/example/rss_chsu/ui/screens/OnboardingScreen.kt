package com.example.rss_chsu.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rss_chsu.ui.components.NavTab
import com.example.rss_chsu.ui.components.RssIcons
import com.example.rss_chsu.ui.model.OnboardingManager
import com.example.rss_chsu.ui.model.PopularSources
import com.example.rss_chsu.ui.model.RssRepository
import com.example.rss_chsu.ui.model.SourceCategories
import com.example.rss_chsu.ui.theme.*
import kotlinx.coroutines.launch

private const val PAGE_COUNT = 4
private const val SOURCES_PER_CATEGORY = 3

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val pagerState = rememberPagerState(initialPage = 0, pageCount = { PAGE_COUNT })

    var selectedCategories by remember { mutableStateOf<Set<String>>(emptySet()) }
    var isFinishing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { RssRepository.init(context) }

    val availableCategories = remember {
        val seen = linkedSetOf<String>()
        PopularSources.topFeeds.forEach { src ->
            SourceCategories.of(src)?.title?.let { seen.add(it) }
        }
        seen.toList()
    }

    val isLastPage = pagerState.currentPage == PAGE_COUNT - 1
    val canProceed = !isFinishing && (
            pagerState.currentPage != 2 || selectedCategories.isNotEmpty()
            )

    val handleFinish: suspend () -> Unit = {
        if (selectedCategories.isNotEmpty()) {
            val toSubscribe = PopularSources.topFeeds
                .filter { src -> SourceCategories.of(src)?.title in selectedCategories }
                .groupBy { src -> SourceCategories.of(src)?.title }
                .values
                .flatMap { it.take(SOURCES_PER_CATEGORY) }
            try {
                RssRepository.subscribeToCatalogSources(toSubscribe)
            } catch (_: Throwable) {
            }
        }
        OnboardingManager.setCompleted(context)
        onComplete()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!isLastPage) {
                TextButton(
                    onClick = {
                        if (!isFinishing) {
                            coroutineScope.launch {
                                isFinishing = true
                                handleFinish()
                            }
                        }
                    },
                    enabled = !isFinishing
                ) {
                    Text(
                        "Пропустить",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(80.dp))
            }

            Spacer(modifier = Modifier.weight(1f))

            PageIndicator(
                pageCount = PAGE_COUNT,
                currentPage = pagerState.currentPage
            )
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            userScrollEnabled = !isFinishing
        ) { page ->
            when (page) {
                0 -> WelcomePage()
                1 -> NavigationPage()
                2 -> InterestsPage(
                    categories = availableCategories,
                    selected = selectedCategories,
                    onToggle = { title ->
                        selectedCategories = if (title in selectedCategories) {
                            selectedCategories - title
                        } else {
                            selectedCategories + title
                        }
                    }
                )
                3 -> DonePage(categoriesCount = selectedCategories.size)
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Button(
                onClick = {
                    if (isLastPage) {
                        if (!isFinishing) {
                            coroutineScope.launch {
                                isFinishing = true
                                handleFinish()
                            }
                        }
                    } else {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                },
                enabled = canProceed,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = DarkPrimary,
                    contentColor = OnDarkPrimary,
                    disabledContainerColor = ChipUnselected,
                    disabledContentColor = TextSecondary
                )
            ) {
                if (isFinishing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = OnDarkPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text(
                        text = when (pagerState.currentPage) {
                            3 -> "Перейти к ленте"
                            else -> "Далее"
                        },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (pagerState.currentPage == 2 && selectedCategories.isEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Выберите хотя бы одну тему или нажмите «Пропустить»",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun PageIndicator(pageCount: Int, currentPage: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pageCount) { index ->
            val isActive = index == currentPage
            Box(
                modifier = Modifier
                    .height(6.dp)
                    .width(if (isActive) 22.dp else 6.dp)
                    .clip(CircleShape)
                    .background(if (isActive) DarkPrimary else ChipUnselected)
            )
        }
    }
}

@Composable
private fun WelcomePage() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF2F3542), Color(0xFF1E1E24))
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = RssIcons.Subscriptions,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(56.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Добро пожаловать",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Все интересные новости в одном месте. Настроим ленту под вас за 30 секунд — а дальше только читать.",
            fontSize = 15.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )

        Spacer(modifier = Modifier.height(40.dp))

        FeatureRow("Персональные рекомендации", "Подборка «Для вас» на основе того, что вы читаете")
        Spacer(modifier = Modifier.height(14.dp))
        FeatureRow("Удобное чтение", "Режим чтения, похожие статьи, сохранение в закладки")
        Spacer(modifier = Modifier.height(14.dp))
        FeatureRow("Гибкая настройка", "Темы, шрифты, папки — всё подстраивается под вас")

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun FeatureRow(title: String, description: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(DarkPrimary),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = OnDarkPrimary,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                fontSize = 12.sp,
                color = TextSecondary,
                lineHeight = 16.sp
            )
        }
    }
}

// ── Исправлено: страница навигации берёт всё из NavTab и красит иконки однотонно ──

@Composable
private fun NavigationPage() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Как всё устроено",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Пять вкладок внизу — вот что делает каждая.",
            fontSize = 14.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(28.dp))

        NavTab.entries.forEachIndexed { index, tab ->
            NavigationItem(
                icon = tab.icon,                 // иконка как в нижней панели
                title = tab.title,               // название как в нижней панели
                description = navDescriptionFor(tab)
            )
            if (index < NavTab.entries.lastIndex) {
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun NavigationItem(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(ChipUnselected.copy(alpha = 0.5f))
            .border(1.dp, LightCardBorder.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(DarkPrimary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = DarkPrimary,              // ч/б, один цвет на всех
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                fontSize = 12.sp,
                color = TextSecondary,
                lineHeight = 16.sp
            )
        }
    }
}

private fun navDescriptionFor(tab: NavTab): String = when (tab) {
    NavTab.TODAY ->
        "Персональная лента «Для вас» и все статьи из подписок. Здесь начинается чтение."
    NavTab.SUBSCRIPTIONS ->
        "Ваши RSS-источники, папки и ручное добавление лент по ссылке."
    NavTab.SEARCH ->
        "Каталог популярных изданий и рекомендации новых источников."
    NavTab.SAVED ->
        "Сохранённые статьи. Смахивайте влево, чтобы удалить."
    NavTab.SETTINGS ->
        "Темы, шрифты, размер карточек, экспериментальные функции и кэш."
}

// ──────────────────────────────────────────────────────────────────────

@Composable
private fun InterestsPage(
    categories: List<String>,
    selected: Set<String>,
    onToggle: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Что вам интересно?",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Выберите одну или несколько тем — мы подпишем вас на $SOURCES_PER_CATEGORY источника из каждой. Всё можно поменять в «Подписках».",
            fontSize = 14.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (categories.isEmpty()) {
            Text(
                text = "Каталог источников пуст. Пропустите этот шаг и добавьте RSS-ленты вручную.",
                fontSize = 13.sp,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp,
                modifier = Modifier.padding(vertical = 24.dp)
            )
        } else {
            categories.forEach { title ->
                CategoryCard(
                    title = title,
                    isSelected = title in selected,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onToggle(title) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
private fun CategoryCard(
    title: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) DarkPrimary else LightCardBorder
    val bgColor = if (isSelected) {
        DarkPrimary.copy(alpha = 0.08f)
    } else {
        ChipUnselected.copy(alpha = 0.4f)
    }

    Row(
        modifier = modifier
            .heightIn(min = 52.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(bgColor)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(if (isSelected) DarkPrimary else Color.Transparent)
                .border(
                    width = if (isSelected) 0.dp else 1.5.dp,
                    color = TextSecondary.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(6.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = OnDarkPrimary,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) TextPrimary else TextSecondary,
            modifier = Modifier.weight(1f),
            lineHeight = 18.sp
        )
    }
}

@Composable
private fun DonePage(categoriesCount: Int) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        Box(
            modifier = Modifier
                .size(110.dp)
                .clip(CircleShape)
                .background(DarkPrimary),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = OnDarkPrimary,
                modifier = Modifier.size(56.dp)
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = "Всё готово",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = if (categoriesCount > 0) {
                "Подписали вас на источники по $categoriesCount ${pluralizeCategory(categoriesCount)}. Лента начнёт наполняться в течение минуты."
            } else {
                "Можно начинать. Первые статьи появятся, как только вы добавите источники во вкладке «Подписки» или через «Поиск»."
            },
            fontSize = 15.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Всё это можно изменить в «Настройках».",
            fontSize = 13.sp,
            color = TextSecondary.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))
    }
}

private fun pluralizeCategory(n: Int): String {
    val mod10 = n % 10
    val mod100 = n % 100
    return when {
        mod10 == 1 && mod100 != 11 -> "теме"
        else -> "темам"
    }
}