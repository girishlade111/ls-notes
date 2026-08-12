package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("ls_notes_settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private fun loadSettings(): AppSettings {
        val themeModeStr = prefs.getString("theme_mode", ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
        val accentHex = prefs.getString("accent_color", "#7C4DFF") ?: "#7C4DFF"
        val noteColorModeStr = prefs.getString("note_color_mode", NoteColorMode.RANDOM.name) ?: NoteColorMode.RANDOM.name
        val chosenDefaultColorHex = prefs.getString("chosen_default_color", "#FFFFFF") ?: "#FFFFFF"
        val timeDisplayModeStr = prefs.getString("time_display_mode", TimeDisplayMode.SHOW_BOTH.name) ?: TimeDisplayMode.SHOW_BOTH.name
        val viewModeStr = prefs.getString("view_mode", NoteViewMode.GRID.name) ?: NoteViewMode.GRID.name
        val editorModeStr = prefs.getString("editor_mode", EditorMode.BASIC.name) ?: EditorMode.BASIC.name
        val font = prefs.getString("editor_font", "Roboto") ?: "Roboto"
        val fontSize = prefs.getFloat("editor_font_size", 16f)
        val spellCheck = prefs.getBoolean("spell_check", true)
        val passcode = prefs.getString("private_safe_passcode", "") ?: ""
        val autoLock = prefs.getInt("auto_lock_minutes", 1)

        return AppSettings(
            themeMode = runCatching { ThemeMode.valueOf(themeModeStr) }.getOrDefault(ThemeMode.SYSTEM),
            accentColorHex = accentHex,
            defaultNoteColorMode = runCatching { NoteColorMode.valueOf(noteColorModeStr) }.getOrDefault(NoteColorMode.RANDOM),
            chosenDefaultColorHex = chosenDefaultColorHex,
            timeDisplayMode = runCatching { TimeDisplayMode.valueOf(timeDisplayModeStr) }.getOrDefault(TimeDisplayMode.SHOW_BOTH),
            defaultNoteViewMode = runCatching { NoteViewMode.valueOf(viewModeStr) }.getOrDefault(NoteViewMode.GRID),
            defaultEditorMode = runCatching { EditorMode.valueOf(editorModeStr) }.getOrDefault(EditorMode.BASIC),
            editorFontFamily = font,
            editorFontSizeSp = fontSize,
            spellCheckEnabled = spellCheck,
            privateSafePasscode = passcode,
            autoLockMinutes = autoLock
        )
    }

    fun updateSettings(newSettings: AppSettings) {
        prefs.edit()
            .putString("theme_mode", newSettings.themeMode.name)
            .putString("accent_color", newSettings.accentColorHex)
            .putString("note_color_mode", newSettings.defaultNoteColorMode.name)
            .putString("chosen_default_color", newSettings.chosenDefaultColorHex)
            .putString("time_display_mode", newSettings.timeDisplayMode.name)
            .putString("view_mode", newSettings.defaultNoteViewMode.name)
            .putString("editor_mode", newSettings.defaultEditorMode.name)
            .putString("editor_font", newSettings.editorFontFamily)
            .putFloat("editor_font_size", newSettings.editorFontSizeSp)
            .putBoolean("spell_check", newSettings.spellCheckEnabled)
            .putString("private_safe_passcode", newSettings.privateSafePasscode)
            .putInt("auto_lock_minutes", newSettings.autoLockMinutes)
            .apply()

        _settings.value = newSettings
    }

    fun setPrivateSafePasscode(passcode: String) {
        val updated = _settings.value.copy(privateSafePasscode = passcode)
        updateSettings(updated)
    }
}
