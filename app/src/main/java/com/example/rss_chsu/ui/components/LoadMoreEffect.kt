package com.example.rss_chsu.ui.components

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState

/**
 * Вызывает [onLoadMore], когда пользователь доскроллил до конца списка (осталось не больше [buffer] элементов).
 *
 * Использование внутри экрана со списком:
 *
 *     val listState = rememberLazyListState()
 *     LoadMoreEffect(listState, enabled = hasMore, onLoadMore = onLoadMore)
 *     LazyColumn(state = listState) { ... }
 *
 * Если после подгрузки список всё ещё короткий и конец остаётся на экране, подгрузка повторяется:
 * эффект перезапускается при изменении числа элементов.
 */
@Composable
fun LoadMoreEffect(
    listState: LazyListState,
    enabled: Boolean = true,
    buffer: Int = 5,
    onLoadMore: () -> Unit
) {
    val currentOnLoadMore by rememberUpdatedState(onLoadMore)

    val totalCount by remember(listState) {
        derivedStateOf { listState.layoutInfo.totalItemsCount }
    }
    val nearEnd by remember(listState, buffer) {
        derivedStateOf {
            val info = listState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            info.totalItemsCount > 0 && lastVisible >= info.totalItemsCount - 1 - buffer
        }
    }

    LaunchedEffect(nearEnd, enabled, totalCount) {
        if (nearEnd && enabled) currentOnLoadMore()
    }
}