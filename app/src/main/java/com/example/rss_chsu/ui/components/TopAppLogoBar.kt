package com.example.rss_chsu.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rss_chsu.ui.theme.DarkPrimary

@Composable
fun TopAppLogoBar(
    modifier: Modifier = Modifier,
    rightAction: @Composable (() -> Unit)? = null
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = RssIcons.Subscriptions,
                    contentDescription = null,
                    tint = DarkPrimary.copy(alpha = 0.85f),
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "RSS-читалка",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkPrimary.copy(alpha = 0.85f)
                )
            }

            rightAction?.invoke()
        }
    }
}
