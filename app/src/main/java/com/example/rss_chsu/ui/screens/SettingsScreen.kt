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
import com.example.rss_chsu.ui.components.TopAppLogoBar
import com.example.rss_chsu.ui.model.RssRepository
import com.example.rss_chsu.ui.theme.*

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    var showDeleteAppDialog by remember { mutableStateOf(false) }
    val isDarkTheme by ThemeManager.isDarkTheme.collectAsState()
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

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
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
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ThemeOptionItem(
                        title = "Светлая",
                        isDarkThemeOption = false,
                        isSelected = !isDarkTheme,
                        onClick = { ThemeManager.setDarkTheme(context, false) }
                    )
                    Spacer(modifier = Modifier.width(48.dp))
                    ThemeOptionItem(
                        title = "Тёмная",
                        isDarkThemeOption = true,
                        isSelected = isDarkTheme,
                        onClick = { ThemeManager.setDarkTheme(context, true) }
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

            // Показываем объём полезных данных статей
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
                onClick = {
                    RssRepository.addAllPopularSources()
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.Red
                )
            ) {
                Text(
                    "Подписаться на 50 источников",
                    color = Color.Red,
                    fontWeight = FontWeight.Medium
                )
            }

            OutlinedButton(
                onClick = {
                    // Полная очистка всех данных (подписки, статьи, папки).
                    RssRepository.clearAllData()
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.Red
                )
            ) {
                Text(
                    "Сбросить все данные",
                    color = Color.Red,
                    fontWeight = FontWeight.Medium
                )
            }

            OutlinedButton(
                onClick = {
                    showDeleteAppDialog = true
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.Red
                )
            ) {
                Text(
                    "Удалить приложение",
                    color = Color.Red,
                    fontWeight = FontWeight.Medium
                )
            }
        }

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
    isDarkThemeOption: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val primaryThemeColor = TextPrimary
    val surfaceThemeColor = LightSurface

    val circleBgColor = if (isDarkThemeOption) primaryThemeColor else surfaceThemeColor
    val wedgeColor = if (isDarkThemeOption) surfaceThemeColor else primaryThemeColor

    val mainBorderColor = if (isSelected) primaryThemeColor else LightCardBorder

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
    ) {
        Box(
            modifier = Modifier.size(64.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .border(
                        width = if (isSelected) 2.5.dp else 1.dp,
                        color = mainBorderColor,
                        shape = CircleShape
                    )
                    .background(circleBgColor),
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
                    drawPath(
                        path = path,
                        color = wedgeColor
                    )
                }
            }

            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .align(Alignment.BottomEnd)
                        .clip(CircleShape)
                        .background(primaryThemeColor)
                        .border(
                            width = 1.dp,
                            color = surfaceThemeColor,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = surfaceThemeColor,
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