package com.example.rss_chsu.ui.screens

import android.text.format.Formatter
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rss_chsu.ui.components.ScrollToTopFab
import com.example.rss_chsu.ui.components.TopAppLogoBar
import com.example.rss_chsu.ui.model.RssRepository
import com.example.rss_chsu.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    var showDeleteAppDialog by remember { mutableStateOf(false) }
    val currentTheme by ThemeManager.appTheme.collectAsState()
    val isLargeCards by ThemeManager.isLargeCards.collectAsState()

    LaunchedEffect(context) {
        RssRepository.init(context)
    }

    val cacheSizeBytes by RssRepository.cacheSizeBytes.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        TopAppLogoBar(
            title = "НАСТРОЙКИ"
        )

        // ── Кнопка «Наверх» ────────────────────────────────────────────
        val scrollState = rememberScrollState()
        val coroutineScope = rememberCoroutineScope()
        val showScrollToTop = remember {
            derivedStateOf { scrollState.value > 0 }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Оформление",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = ChipUnselected.copy(alpha = 0.6f),
                    border = BorderStroke(1.dp, LightCardBorder.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp, horizontal = 12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ThemeOptionItem(
                            title = "Светлая",
                            primarySwatch = Color(0xFFFFFFFF),
                            secondarySwatch = Color(0xFF111113),
                            isSelected = currentTheme == AppTheme.LIGHT,
                            onClick = { ThemeManager.setAppTheme(context, AppTheme.LIGHT) }
                        )
                        ThemeOptionItem(
                            title = "Тёмная",
                            primarySwatch = Color(0xFF1E1E1E),
                            secondarySwatch = Color(0xFFFFFFFF),
                            isSelected = currentTheme == AppTheme.DARK,
                            onClick = { ThemeManager.setAppTheme(context, AppTheme.DARK) }
                        )
                        ThemeOptionItem(
                            title = "GTA VI",
                            primarySwatch = Color(0xFF1D1238),
                            secondarySwatch = Color(0xFFFF3E9D),
                            isSelected = currentTheme == AppTheme.GTA_VI,
                            onClick = { ThemeManager.setAppTheme(context, AppTheme.GTA_VI) }
                        )
                    }
                }

                Text(
                    text = "Вид карточек в ленте",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

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
                            .background(if (!isLargeCards) MaterialTheme.colorScheme.surface else Color.Transparent)
                            .clickable { ThemeManager.setLargeCards(context, false) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Компактный",
                            fontSize = 13.sp,
                            fontWeight = if (!isLargeCards) FontWeight.Bold else FontWeight.Medium,
                            color = if (!isLargeCards) TextPrimary else TextSecondary
                        )
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isLargeCards) MaterialTheme.colorScheme.surface else Color.Transparent)
                            .clickable { ThemeManager.setLargeCards(context, true) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Крупный",
                            fontSize = 13.sp,
                            fontWeight = if (isLargeCards) FontWeight.Bold else FontWeight.Medium,
                            color = if (isLargeCards) TextPrimary else TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Размер кэша статей",
                        fontSize = 14.sp,
                        color = TextPrimary.copy(alpha = 0.75f)
                    )
                    Text(
                        text = Formatter.formatShortFileSize(context, cacheSizeBytes),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                }

                OutlinedButton(
                    onClick = { RssRepository.addAllPopularSources() },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                ) {
                    Text(
                        "Подписаться на 50 источников",
                        color = Color.Red,
                        fontWeight = FontWeight.Medium
                    )
                }

                OutlinedButton(
                    onClick = { RssRepository.clearAllData() },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                ) {
                    Text(
                        "Сбросить все данные",
                        color = Color.Red,
                        fontWeight = FontWeight.Medium
                    )
                }

                OutlinedButton(
                    onClick = { showDeleteAppDialog = true },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red)
                ) {
                    Text(
                        "Удалить приложение",
                        color = Color.Red,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            ScrollToTopFab(
                visible = { showScrollToTop.value },
                onClick = { coroutineScope.launch { scrollState.animateScrollTo(0) } }
            )
        }
        // ───────────────────────────────────────────────────────────────

        if (showDeleteAppDialog) {
            AlertDialog(
                onDismissRequest = { showDeleteAppDialog = false },
                containerColor = LightSurface,
                title = {
                    Text(
                        text = "Удалить приложение?",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                },
                text = {
                    Text(
                        text = "Вы уверены, что хотите удалить приложение?",
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                },
                confirmButton = {
                    TextButton(onClick = { showDeleteAppDialog = false }) {
                        Text("Нет", fontWeight = FontWeight.Bold, color = DarkPrimary)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteAppDialog = false }) {
                        Text("Нет", fontWeight = FontWeight.Bold, color = TextSecondary)
                    }
                }
            )
        }
    }
}

@Composable
private fun ThemeOptionItem(
    title: String,
    primarySwatch: Color,
    secondarySwatch: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val activeBorderColor = if (isSelected) DarkPrimary else LightCardBorder
    val badgeBgColor = DarkPrimary
    val badgeIconColor = OnDarkPrimary

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
    ) {
        Box(modifier = Modifier.size(64.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .border(
                        width = if (isSelected) 2.5.dp else 1.dp,
                        color = activeBorderColor,
                        shape = CircleShape
                    )
                    .background(primarySwatch),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val path = Path().apply {
                        moveTo(size.width * 0.72f, 0f)
                        lineTo(size.width, 0f)
                        lineTo(size.width, size.height * 0.28f)
                        arcTo(
                            rect = Rect(0f, 0f, size.width, size.height),
                            startAngleDegrees = 0f,
                            sweepAngleDegrees = -90f,
                            forceMoveTo = false
                        )
                        close()
                    }
                    drawPath(path = path, color = secondarySwatch)
                }
            }

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .align(Alignment.BottomEnd)
                        .clip(CircleShape)
                        .background(badgeBgColor)
                        .border(width = 1.dp, color = LightSurface, shape = CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = badgeIconColor,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) TextPrimary else TextSecondary
        )
    }
}