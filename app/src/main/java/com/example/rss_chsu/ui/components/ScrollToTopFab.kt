package com.example.rss_chsu.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.rss_chsu.ui.theme.DarkPrimary
import com.example.rss_chsu.ui.theme.OnDarkPrimary

/**
 * Круглая кнопка «Наверх». Должна вызываться внутри Box — сама позиционируется
 * в правом нижнем углу с отступом 16 dp. Показывается только когда visible()
 * возвращает true.
 *
 * Параметр visible — лямбда, а не Boolean: чтение состояния (canScrollBackward /
 * scrollState.value) происходит внутри AnimatedVisibility, а не в родительском
 * композабле. Это избавляет от лишних рекомпозиций при скролле.
 */
@Composable
fun BoxScope.ScrollToTopFab(
    visible: () -> Boolean,
    onClick: () -> Unit
) {
    AnimatedVisibility(
        visible = visible(),
        modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(end = 16.dp, bottom = 16.dp),
        enter = fadeIn() + scaleIn(initialScale = 0.8f),
        exit = fadeOut() + scaleOut(targetScale = 0.8f)
    ) {
        SmallFloatingActionButton(
            onClick = onClick,
            containerColor = DarkPrimary,
            contentColor = OnDarkPrimary
        ) {
            Icon(
                imageVector = Icons.Default.KeyboardArrowUp,
                contentDescription = "Наверх",
                tint = OnDarkPrimary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}