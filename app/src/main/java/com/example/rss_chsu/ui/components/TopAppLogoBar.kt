package com.example.rss_chsu.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rss_chsu.ui.theme.DarkPrimary

@Composable
fun TopAppLogoBar(
    modifier: Modifier = Modifier,
    title: String = "НОВОСТИ",
    rightAction: @Composable (() -> Unit)? = null,
    content: (@Composable RowScope.() -> Unit)? = null
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column {
            // Тонкая линия сверху (газетный стиль)
            HorizontalDivider(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                thickness = 1.dp,
                color = DarkPrimary
            )
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (content != null) {
                    content()
                } else {
                    Text(
                        text = title,
                        fontFamily = FontFamily.Serif,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = DarkPrimary,
                        letterSpacing = 2.sp
                    )

                    rightAction?.invoke()
                }
            }
            
            // Двойная линия снизу (классический газетный стиль)
            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                thickness = 2.dp,
                color = DarkPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                thickness = 1.dp,
                color = DarkPrimary
            )
        }
    }
}
