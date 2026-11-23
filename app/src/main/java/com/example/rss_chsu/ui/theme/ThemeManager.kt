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
    private const val KEY_APP_THEME = "app_theme"
    private const val KEY_IS_LARGE_CARDS = "is_large_cards"

    private val _appTheme = MutableStateFlow(AppTheme.LIGHT)
    val appTheme: StateFlow<AppTheme> = _appTheme.asStateFlow()

    private val _isLargeCards = MutableStateFlow(false)
    val isLargeCards: StateFlow<Boolean> = _isLargeCards.asStateFlow()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedThemeName = prefs.getString(KEY_APP_THEME, null)

        val resolvedTheme = if (savedThemeName != null) {
            runCatching { AppTheme.valueOf(savedThemeName) }.getOrDefault(AppTheme.LIGHT)
        } else {
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
            val migrated = if (isDark) AppTheme.DARK else AppTheme.LIGHT
            prefs.edit().putString(KEY_APP_THEME, migrated.name).apply()
            migrated
        }

        _appTheme.value = resolvedTheme
        _isLargeCards.value = prefs.getBoolean(KEY_IS_LARGE_CARDS, false)
        syncSystemNightMode(context, resolvedTheme != AppTheme.LIGHT)
    }

    fun setAppTheme(context: Context, theme: AppTheme) {
        if (_appTheme.value == theme) return

        _appTheme.value = theme
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_APP_THEME, theme.name)
            .putBoolean(KEY_IS_DARK_THEME, theme != AppTheme.LIGHT)
            .apply()

        syncSystemNightMode(context, theme != AppTheme.LIGHT)
    }

    fun setLargeCards(context: Context, isLarge: Boolean) {
        if (_isLargeCards.value == isLarge) return

        _isLargeCards.value = isLarge
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_IS_LARGE_CARDS, isLarge).apply()
    }

    fun applyPendingIconChange(context: Context) {
        val pm = context.packageManager
        val isDark = _appTheme.value != AppTheme.LIGHT
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