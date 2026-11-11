package com.example.rss_chsu.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rss_chsu.ui.theme.DarkPrimary
import com.example.rss_chsu.ui.theme.TextSecondary

enum class NavTab(val title: String, val icon: ImageVector) {
    TODAY("Новости", RssIcons.Today),
    SUBSCRIPTIONS("Подписки", RssIcons.Subscriptions),
    SEARCH("Поиск", Icons.Default.Search),
    SAVED("Закладки", RssIcons.Bookmark),
    PROFILE("Профиль", RssIcons.Profile)
}

@Composable
fun RSSBottomBar(
    currentTab: NavTab,
    onTabSelected: (NavTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Тонкая черта сверху меню приложения
            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                thickness = 1.dp,
                color = DarkPrimary
            )

            // Компактный ряд иконок меню приложения
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                NavTab.entries.forEach { tab ->
                    val isSelected = currentTab == tab
                    val color = if (isSelected) DarkPrimary else TextSecondary

                    Column(
                        modifier = Modifier
                            .clickable { onTabSelected(tab) }
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = tab.icon,
                            contentDescription = tab.title,
                            tint = color,
                            modifier = Modifier.size(20.dp)
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = tab.title,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = color,
                            maxLines = 1
                        )
                    }
                }
            }

            // Тонкая черта снизу меню приложения (отделяет от системного меню телефона)
            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                thickness = 1.dp,
                color = DarkPrimary
            )

            // Системный отступ под кнопки/жесты телефона
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsBottomHeight(WindowInsets.navigationBars)
            )
        }
    }
}
