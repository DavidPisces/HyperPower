package com.reiraku.hyperpower.ui.theme

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.edit

enum class ThemeMode(val label: String) {
    SYSTEM("跟随系统"),
    LIGHT("浅色"),
    DARK("深色"),
}

data class ThemeState(
    val mode: ThemeMode = ThemeMode.SYSTEM,
)

val LocalThemeState = staticCompositionLocalOf {
    mutableStateOf(ThemeState())
}

object ThemePreferences {
    private const val PREFERENCES_NAME = "theme_preferences"
    private const val THEME_MODE_KEY = "theme_mode"

    fun load(context: Context): ThemeState {
        val value = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .getString(THEME_MODE_KEY, ThemeMode.SYSTEM.name)
        return ThemeState(
            mode = ThemeMode.entries.firstOrNull { it.name == value } ?: ThemeMode.SYSTEM,
        )
    }

    fun saveMode(context: Context, mode: ThemeMode) {
        context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit { putString(THEME_MODE_KEY, mode.name) }
    }
}

@Composable
fun rememberThemeState(): MutableState<ThemeState> {
    val context = LocalContext.current
    return remember { mutableStateOf(ThemePreferences.load(context)) }
}
