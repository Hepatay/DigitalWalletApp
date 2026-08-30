package com.epatay.digitalwallet.util

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

object ThemeManager {
    const val THEME_SYSTEM = 0
    const val THEME_LIGHT = 1
    const val THEME_DARK = 2

    private const val PREFS_KEY_THEME = "app_theme_mode"

    fun getThemeMode(context: Context): Int {
        val prefs = context.getSharedPreferences("wallet_prefs", Context.MODE_PRIVATE)
        return prefs.getInt(PREFS_KEY_THEME, THEME_SYSTEM)
    }

    fun applyTheme(context: Context) {
        val mode = getThemeMode(context)
        val nightMode = when (mode) {
            THEME_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            THEME_DARK -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(nightMode)
    }

    fun setThemeMode(context: Context, mode: Int) {
        val prefs = context.getSharedPreferences("wallet_prefs", Context.MODE_PRIVATE)
        prefs.edit().putInt(PREFS_KEY_THEME, mode).apply()
        val nightMode = when (mode) {
            THEME_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            THEME_DARK -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(nightMode)
    }
}