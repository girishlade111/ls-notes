package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "notebooks",
    indices = [
        Index(value = ["name"])
    ]
)
data class Notebook(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val colorHex: String = "#7C4DFF",
    val iconName: String = "folder",
    val coverStyle: String = "abstract_gradient",
    val orderIndex: Int = 0,
    val createdTimestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "tags",
    indices = [
        Index(value = ["name"], unique = true)
    ]
)
data class Tag(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val colorHex: String = "#FFB74D",
    val noteCount: Int = 0
)

enum class ThemeMode { LIGHT, DARK, SYSTEM }
enum class TimeDisplayMode { SHOW_CREATED, SHOW_EDITED, SHOW_BOTH, HIDE }
enum class NoteViewMode { GRID, LIST }
enum class EditorMode { BASIC, ADVANCED }
enum class NoteColorMode { RANDOM, THEME, CHOOSE_COLOR }

enum class DeleteNotebookAction {
    MOVE_TO_UNCATEGORIZED,
    MOVE_TO_OTHER_NOTEBOOK,
    MOVE_TO_TRASH
}

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accentColorHex: String = "#7C4DFF",
    val defaultNoteColorMode: NoteColorMode = NoteColorMode.RANDOM,
    val chosenDefaultColorHex: String = "#FFFFFF",
    val timeDisplayMode: TimeDisplayMode = TimeDisplayMode.SHOW_BOTH,
    val defaultNoteViewMode: NoteViewMode = NoteViewMode.GRID,
    val defaultEditorMode: EditorMode = EditorMode.BASIC,
    val editorFontFamily: String = "Roboto",
    val editorFontSizeSp: Float = 16f,
    val spellCheckEnabled: Boolean = true,
    val autoSaveSeconds: Int = 2,
    val privateSafePasscode: String = "",
    val autoLockMinutes: Int = 1,
    val trashRetentionDays: Int = 30
)
