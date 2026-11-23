package com.example.rss_chsu.ui.theme

import android.app.UiModeManager
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object ThemeManager {
    private const val PREFS_NAME = "app_theme_prefs"
    private const val KEY_IS_DARK_THEME = "is_dark_theme"

    private val _isDarkTheme = MutableStateFlow(false)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isFirstLaunch = !prefs.contains(KEY_IS_DARK_THEME)

        val isDark = if (isFirstLaunch) {
            val nightModeFlags =
                context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            val systemIsDark = nightModeFlags == Configuration.UI_MODE_NIGHT_YES
            prefs.edit().putBoolean(KEY_IS_DARK_THEME, systemIsDark).apply()
            systemIsDark
        } else {
            prefs.getBoolean(KEY_IS_DARK_THEME, false)
        }

        _isDarkTheme.value = isDark
        syncSystemNightMode(context, isDark)
    }

    fun setDarkTheme(context: Context, isDark: Boolean) {
        if (_isDarkTheme.value == isDark) return

        _isDarkTheme.value = isDark
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_IS_DARK_THEME, isDark).apply()

        syncSystemNightMode(context, isDark)
    }

    fun applyPendingIconChange(context: Context) {
        val pm = context.packageManager
        val isDark = _isDarkTheme.value
        val lightAlias = ComponentName(context, "${context.packageName}.MainActivityLight")
        val darkAlias = ComponentName(context, "${context.packageName}.MainActivityDark")

        val darkState = pm.getComponentEnabledSetting(darkAlias)
        val isDarkCurrentlyEnabled = darkState == PackageManager.COMPONENT_ENABLED_STATE_ENABLED

        if (isDark != isDarkCurrentlyEnabled) {
            pm.setComponentEnabledSetting(
                if (isDark) darkAlias else lightAlias,
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP
            )
            pm.setComponentEnabledSetting(
                if (isDark) lightAlias else darkAlias,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP
            )
        }
    }

    private fun syncSystemNightMode(context: Context, isDark: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val uiModeManager = context.getSystemService(UiModeManager::class.java)
            uiModeManager?.setApplicationNightMode(
                if (isDark) UiModeManager.MODE_NIGHT_YES else UiModeManager.MODE_NIGHT_NO
            )
        } else {
            AppCompatDelegate.setDefaultNightMode(
                if (isDark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            )
        }
    }
}