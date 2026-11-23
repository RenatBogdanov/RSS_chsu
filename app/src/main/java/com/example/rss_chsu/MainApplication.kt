package com.example.rss_chsu

import android.app.Application
import com.example.rss_chsu.ui.model.RssRepository
import com.example.rss_chsu.ui.theme.ThemeManager

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        RssRepository.init(this)
        ThemeManager.init(this)
    }
}
