package com.example.rss_chsu.ui.model

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Флаг «первичная настройка пройдена». Хранится в отдельном SharedPreferences,
 * чтобы не мешать ThemeManager. MainActivity читает isCompleted и решает, что
 * показывать: OnboardingScreen или MainScreen.
 */
object OnboardingManager {
    private const val PREFS_NAME = "app_onboarding_prefs"
    private const val KEY_COMPLETED = "is_completed"

    private val _isCompleted = MutableStateFlow(false)
    val isCompleted: StateFlow<Boolean> = _isCompleted.asStateFlow()

    fun init(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        _isCompleted.value = prefs.getBoolean(KEY_COMPLETED, false)
    }

    fun setCompleted(context: Context) {
        _isCompleted.value = true
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_COMPLETED, true).apply()
    }

    fun reset(context: Context) {
        _isCompleted.value = false
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_COMPLETED, false).apply()
    }
}