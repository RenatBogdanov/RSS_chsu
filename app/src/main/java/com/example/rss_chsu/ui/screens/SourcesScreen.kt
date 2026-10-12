package com.example.rss_chsu.ui.screens

import androidx.compose.runtime.Composable

@Composable
fun SourcesScreen(
    onReadClick: () -> Unit = {},
    onNotesClick: () -> Unit = {}
) {
    SubscriptionsScreen(
        onAddSourceClick = { },
        onMenuClick = { }
    )
}
