package com.example.rss_chsu.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rss_chsu.ui.theme.DarkPrimary
import com.example.rss_chsu.ui.theme.LightCardBorder

@Composable
fun TopAppLogoBar(
    modifier: Modifier = Modifier,
    rightAction: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .drawBehind {
                drawLine(
                    color = LightCardBorder,
                    start = Offset(0f, size.height),
                    end = Offset(size.width, size.height),
                    strokeWidth = 1.5.dp.toPx()
                )
            }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = RssIcons.Subscriptions,
                contentDescription = null,
                tint = DarkPrimary,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = "RSS-reader",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = DarkPrimary
            )
        }

        rightAction?.invoke()
    }
}
