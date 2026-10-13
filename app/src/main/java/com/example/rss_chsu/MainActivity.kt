package com.example.rss_chsu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.rss_chsu.ui.screens.MainScreen
import com.example.rss_chsu.ui.theme.RSS_chsuTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            RSS_chsuTheme {
                MainScreen()
            }
        }
    }
}
