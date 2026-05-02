package dk.itu.moapd.x9.s25134.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// View model to save the user's dark mode preferences.
// Extensible with other preferences in the future.
class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "SettingsViewModel"
        private const val PREFS_NAME = "x9_prefs"
        private const val KEY_DARK_MODE = "dark_mode"
    }

    private val prefs = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // null = no user preference saved yet; falls back to isSystemInDarkTheme() in MainActivity.
    private val _isDarkMode = MutableStateFlow(
        if (prefs.contains(KEY_DARK_MODE)) prefs.getBoolean(KEY_DARK_MODE, false) else null
    )
    val isDarkMode: StateFlow<Boolean?> = _isDarkMode.asStateFlow()

    fun setDarkMode(enabled: Boolean) {
        Log.d(TAG, "Dark mode set to $enabled")
        _isDarkMode.value = enabled
        prefs.edit { putBoolean(KEY_DARK_MODE, enabled) }
    }
}
