package com.example.rss_chsu.ui.screens

import android.text.format.Formatter
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rss_chsu.ui.components.TopAppLogoBar
import com.example.rss_chsu.ui.model.RssRepository
import com.example.rss_chsu.ui.theme.*

@Composable
fun ProfileScreen() {
    val context = LocalContext.current

    // На случай, если экран профиля открылся раньше инициализации репозитория.
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
            title = "ПРОФИЛЬ"
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Настройки",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            OutlinedButton(
                onClick = { /* TODO */ },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Оформление и тема", color = TextPrimary, fontWeight = FontWeight.Medium)
            }

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
                    // Полная очистка всех данных (подписки, статьи, папки).
                    RssRepository.clearAllData()
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = androidx.compose.ui.graphics.Color.Red
                )
            ) {
                Text(
                    "Сбросить все данные (Тест)",
                    color = androidx.compose.ui.graphics.Color.Red,
                    fontWeight = FontWeight.Medium
                )
            }

            OutlinedButton(
                onClick = { /* TODO */ },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Уведомления", color = TextPrimary, fontWeight = FontWeight.Medium)
            }
        }
    }
}
