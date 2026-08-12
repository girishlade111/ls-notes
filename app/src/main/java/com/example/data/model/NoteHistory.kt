package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "note_history",
    foreignKeys = [
        ForeignKey(
            entity = Note::class,
            parentColumns = ["id"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["noteId"]),
        Index(value = ["timestamp"])
    ]
)
data class NoteHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val noteId: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val title: String,
    val content: String = "",
    val checklistJson: String = "[]",
    val attachmentsJson: String = "[]",
    val tagsCsv: String = "",
    val colorHex: String = "#FFFFFF",
    val fontName: String? = null,
    val fontSizeSp: Float? = null,
    val changeSummary: String = "Version snapshot"
)
