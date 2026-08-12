package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class NoteType {
    TEXT,
    CHECKLIST,
    PHOTO,
    AUDIO,
    SKETCH,
    FILE,
    SMART_CARD,
    LINK_BOOKMARK,
    CODE,
    TABLE,
    DOCUMENT_SCAN
}

data class ChecklistItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    var text: String = "",
    var isChecked: Boolean = false,
    var indentLevel: Int = 0
)

data class NoteAttachment(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val uriOrPath: String,
    val mimeType: String,
    val sizeBytes: Long = 0L,
    val addedTimestamp: Long = System.currentTimeMillis()
)

data class SmartCardMeta(
    val url: String = "",
    val title: String = "",
    val description: String = "",
    val iconUrl: String? = null,
    val coverImageUrl: String? = null,
    val category: String = "Bookmark"
)

data class TableData(
    val rows: Int = 2,
    val cols: Int = 2,
    val cells: List<List<String>> = listOf(listOf("", ""), listOf("", ""))
)

@Entity(
    tableName = "notes",
    indices = [
        Index(value = ["notebookId"]),
        Index(value = ["isPinned"]),
        Index(value = ["isFavorite"]),
        Index(value = ["isArchived"]),
        Index(value = ["isPrivate"]),
        Index(value = ["isInTrash"]),
        Index(value = ["updatedTimestamp"]),
        Index(value = ["createdTimestamp"])
    ],
    foreignKeys = [
        ForeignKey(
            entity = Notebook::class,
            parentColumns = ["id"],
            childColumns = ["notebookId"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String = "",
    val type: NoteType = NoteType.TEXT,
    val notebookId: Long? = null,
    val notebookName: String = "Uncategorized",
    val tagsCsv: String = "", // Comma separated tag names
    val colorHex: String = "#FFFFFF",
    val isPinned: Boolean = false,
    val isFavorite: Boolean = false,
    val isArchived: Boolean = false,
    val isPrivate: Boolean = false,
    val isInTrash: Boolean = false,
    val trashedTimestamp: Long = 0L,
    val createdTimestamp: Long = System.currentTimeMillis(),
    val updatedTimestamp: Long = System.currentTimeMillis(),
    val checklistJson: String = "[]",
    val attachmentsJson: String = "[]",
    val sketchPath: String? = null,
    val smartCardMetaJson: String? = null,
    val tableDataJson: String? = null,
    val fontName: String? = null,
    val fontSizeSp: Float? = null
)
