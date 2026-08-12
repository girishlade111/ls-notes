package com.example.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.example.data.local.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LsNotesRepository(
    private val context: Context,
    private val noteDao: NoteDao = LsNotesDatabase.getInstance(context).noteDao(),
    private val notebookDao: NotebookDao = LsNotesDatabase.getInstance(context).notebookDao(),
    private val tagDao: TagDao = LsNotesDatabase.getInstance(context).tagDao(),
    private val noteHistoryDao: NoteHistoryDao = LsNotesDatabase.getInstance(context).noteHistoryDao(),
    val settingsManager: SettingsManager = SettingsManager(context)
) {
    private val database: LsNotesDatabase by lazy { LsNotesDatabase.getInstance(context) }
    val noteImportService: NoteImportService by lazy { NoteImportService(context, this) }

    suspend fun <T> runInTransaction(block: suspend () -> T): T {
        return database.withTransaction(block)
    }

    /**
     * Executes a batch import of notes inside a single Room database transaction to ensure
     * data integrity. If any error occurs during note insertion or snapshot generation,
     * the entire transaction is rolled back.
     */
    suspend fun importNotesBatchInTransaction(
        notes: List<Note>,
        targetNotebookName: String
    ): List<Note> {
        return database.withTransaction {
            val notebookId = notebookDao.insertNotebook(
                Notebook(name = targetNotebookName, colorHex = "#7C4DFF")
            )

            val now = System.currentTimeMillis()
            val preparedNotes = notes.map { note ->
                note.copy(
                    notebookId = notebookId,
                    notebookName = targetNotebookName,
                    createdTimestamp = if (note.createdTimestamp == 0L) now else note.createdTimestamp,
                    updatedTimestamp = if (note.updatedTimestamp == 0L) now else note.updatedTimestamp
                )
            }

            val insertedIds = noteDao.insertNotes(preparedNotes)
            val savedNotes = preparedNotes.mapIndexed { index, note ->
                val savedId = insertedIds.getOrElse(index) { 0L }
                val finalNote = note.copy(id = savedId)

                if (finalNote.id != 0L) {
                    saveNoteHistorySnapshot(finalNote, "Imported from file")
                }
                finalNote
            }
            savedNotes
        }
    }

    val allActiveNotes: Flow<List<Note>> = noteDao.getAllActiveNotes()
    val pinnedNotes: Flow<List<Note>> = noteDao.getPinnedNotes()
    val recentNotes: Flow<List<Note>> = noteDao.getRecentNotes()
    val privateNotes: Flow<List<Note>> = noteDao.getPrivateNotes()
    val trashNotes: Flow<List<Note>> = noteDao.getTrashNotes()
    val archivedNotes: Flow<List<Note>> = noteDao.getArchivedNotes()
    val allNotebooks: Flow<List<Notebook>> = notebookDao.getAllNotebooks()
    val allTags: Flow<List<Tag>> = tagDao.getAllTags()

    val notesSortedByLastModified: Flow<List<Note>> = noteDao.getNotesSortedByUpdatedDesc()
    val notesSortedByTitle: Flow<List<Note>> = noteDao.getNotesSortedByTitleAsc()
    val notesSortedByDateCreated: Flow<List<Note>> = noteDao.getNotesSortedByCreatedDesc()

    fun getSortedNotes(sortOrder: NoteSortOrder = NoteSortOrder.DEFAULT): Flow<List<Note>> {
        return when (sortOrder) {
            NoteSortOrder.LAST_MODIFIED_DESC -> noteDao.getNotesSortedByUpdatedDesc()
            NoteSortOrder.LAST_MODIFIED_ASC -> noteDao.getNotesSortedByUpdatedAsc()
            NoteSortOrder.TITLE_ASC -> noteDao.getNotesSortedByTitleAsc()
            NoteSortOrder.TITLE_DESC -> noteDao.getNotesSortedByTitleDesc()
            NoteSortOrder.DATE_CREATED_DESC -> noteDao.getNotesSortedByCreatedDesc()
            NoteSortOrder.DATE_CREATED_ASC -> noteDao.getNotesSortedByCreatedAsc()
            else -> allActiveNotes.map { it.sortNotes(sortOrder) }
        }
    }

    fun getSortedNotesByNotebook(notebookId: Long, sortOrder: NoteSortOrder = NoteSortOrder.DEFAULT): Flow<List<Note>> {
        return getNotesByNotebook(notebookId).map { it.sortNotes(sortOrder) }
    }

    fun getSortedNotesByTag(tag: String, sortOrder: NoteSortOrder = NoteSortOrder.DEFAULT): Flow<List<Note>> {
        return getNotesByTag(tag).map { it.sortNotes(sortOrder) }
    }

    suspend fun getNoteById(id: Long): Note? = noteDao.getNoteById(id)

    fun observeNoteById(id: Long): Flow<Note?> = noteDao.observeNoteById(id)

    fun getNotesByNotebook(notebookId: Long): Flow<List<Note>> = noteDao.getNotesByNotebook(notebookId)

    fun getNotesByTag(tag: String): Flow<List<Note>> = noteDao.getNotesByTag(tag)

    suspend fun saveNote(note: Note): Long {
        val now = System.currentTimeMillis()
        val noteToSave = if (note.id == 0L) {
            note.copy(createdTimestamp = now, updatedTimestamp = now)
        } else {
            note.copy(updatedTimestamp = now)
        }
        val savedId = noteDao.insertNote(noteToSave)
        val finalNote = if (note.id == 0L) noteToSave.copy(id = savedId) else noteToSave
        if (finalNote.title.isNotBlank() || finalNote.content.isNotBlank()) {
            saveNoteHistorySnapshot(finalNote, if (note.id == 0L) "Created Note" else "Saved Edits")
        }
        return savedId
    }

    fun getHistoryForNote(noteId: Long): Flow<List<NoteHistory>> = noteHistoryDao.getHistoryForNote(noteId)

    suspend fun getHistoryById(historyId: Long): NoteHistory? = noteHistoryDao.getHistoryById(historyId)

    suspend fun saveNoteHistorySnapshot(note: Note, summary: String = "Version snapshot"): Long {
        if (note.id == 0L) return 0L
        val latest = noteHistoryDao.getLatestHistoryForNote(note.id)
        if (latest != null && latest.title == note.title && latest.content == note.content &&
            latest.checklistJson == note.checklistJson && latest.tagsCsv == note.tagsCsv) {
            return latest.id
        }
        val snapshot = NoteHistory(
            noteId = note.id,
            timestamp = System.currentTimeMillis(),
            title = note.title,
            content = note.content,
            checklistJson = note.checklistJson,
            attachmentsJson = note.attachmentsJson,
            tagsCsv = note.tagsCsv,
            colorHex = note.colorHex,
            fontName = note.fontName,
            fontSizeSp = note.fontSizeSp,
            changeSummary = summary
        )
        return noteHistoryDao.insertHistory(snapshot)
    }

    suspend fun revertNoteToHistory(noteId: Long, historyId: Long): Note? {
        val currentNote = getNoteById(noteId) ?: return null
        val history = getHistoryById(historyId) ?: return null

        // Save current note state as backup history snapshot before reverting
        saveNoteHistorySnapshot(currentNote, summary = "Pre-restore Backup")

        val restoredNote = currentNote.copy(
            title = history.title,
            content = history.content,
            checklistJson = history.checklistJson,
            attachmentsJson = history.attachmentsJson,
            tagsCsv = history.tagsCsv,
            colorHex = history.colorHex,
            fontName = history.fontName,
            fontSizeSp = history.fontSizeSp,
            updatedTimestamp = System.currentTimeMillis()
        )
        noteDao.updateNote(restoredNote)
        return restoredNote
    }

    suspend fun softDeleteNote(note: Note) {
        val trashedNote = note.copy(
            isInTrash = true,
            trashedTimestamp = System.currentTimeMillis()
        )
        noteDao.updateNote(trashedNote)
    }

    suspend fun softDeleteNoteById(id: Long) {
        moveNoteToTrash(id)
    }

    suspend fun deleteNote(note: Note) {
        softDeleteNote(note)
    }

    suspend fun moveNoteToTrash(id: Long) {
        val note = getNoteById(id) ?: return
        softDeleteNote(note)
    }

    suspend fun restoreNoteFromTrash(id: Long) {
        val note = getNoteById(id) ?: return
        noteDao.updateNote(
            note.copy(
                isInTrash = false,
                trashedTimestamp = 0L
            )
        )
    }

    suspend fun deleteNotePermanently(id: Long) {
        noteDao.deleteNoteById(id)
    }

    suspend fun emptyTrash() {
        noteDao.emptyTrash()
    }

    suspend fun purgeOldTrash(daysOld: Int = 30) {
        val cutoff = System.currentTimeMillis() - (daysOld * 24L * 60L * 60L * 1000L)
        noteDao.purgeOldTrash(cutoff)
    }

    suspend fun togglePinNote(id: Long) {
        val note = getNoteById(id) ?: return
        noteDao.updateNote(note.copy(isPinned = !note.isPinned))
    }

    suspend fun toggleFavoriteNote(id: Long) {
        val note = getNoteById(id) ?: return
        noteDao.updateNote(note.copy(isFavorite = !note.isFavorite))
    }

    suspend fun toggleArchiveNote(id: Long) {
        val note = getNoteById(id) ?: return
        noteDao.updateNote(
            note.copy(
                isArchived = !note.isArchived,
                isPinned = false,
                updatedTimestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun archiveNote(id: Long) {
        val note = getNoteById(id) ?: return
        noteDao.updateNote(
            note.copy(
                isArchived = true,
                isPinned = false,
                updatedTimestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun unarchiveNote(id: Long) {
        val note = getNoteById(id) ?: return
        noteDao.updateNote(
            note.copy(
                isArchived = false,
                updatedTimestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun setNotePrivacy(id: Long, isPrivate: Boolean) {
        val note = getNoteById(id) ?: return
        noteDao.updateNote(note.copy(isPrivate = isPrivate, updatedTimestamp = System.currentTimeMillis()))
    }

    suspend fun duplicateNote(id: Long): Long {
        val note = getNoteById(id) ?: return 0L
        val copyNote = note.copy(
            id = 0,
            title = "${note.title} (Copy)",
            createdTimestamp = System.currentTimeMillis(),
            updatedTimestamp = System.currentTimeMillis()
        )
        return noteDao.insertNote(copyNote)
    }

    suspend fun moveNoteToNotebook(noteId: Long, notebookId: Long?, notebookName: String) {
        val note = getNoteById(noteId) ?: return
        noteDao.updateNote(note.copy(notebookId = notebookId, notebookName = notebookName, updatedTimestamp = System.currentTimeMillis()))
    }

    // Notebook operations
    suspend fun saveNotebook(notebook: Notebook): Long = notebookDao.insertNotebook(notebook)
    suspend fun deleteNotebook(notebook: Notebook) = notebookDao.deleteNotebook(notebook)

    suspend fun renameNotebook(notebook: Notebook, newName: String, newColorHex: String = notebook.colorHex) {
        val updated = notebook.copy(name = newName, colorHex = newColorHex)
        database.withTransaction {
            notebookDao.updateNotebook(updated)
            noteDao.updateNotesNotebookName(notebook.id, newName)
        }
    }

    suspend fun saveNotebookOrder(orderedNotebooks: List<Notebook>) {
        database.withTransaction {
            val reordered = orderedNotebooks.mapIndexed { index, notebook ->
                notebook.copy(orderIndex = index)
            }
            notebookDao.updateNotebooks(reordered)
        }
    }

    suspend fun deleteNotebookWithAction(
        notebookId: Long,
        action: DeleteNotebookAction,
        targetNotebookId: Long? = null,
        targetNotebookName: String = "Uncategorized"
    ) {
        database.withTransaction {
            when (action) {
                DeleteNotebookAction.MOVE_TO_UNCATEGORIZED -> {
                    noteDao.moveNotesToNotebook(
                        sourceNotebookId = notebookId,
                        targetNotebookId = null,
                        targetNotebookName = "Uncategorized"
                    )
                }
                DeleteNotebookAction.MOVE_TO_OTHER_NOTEBOOK -> {
                    noteDao.moveNotesToNotebook(
                        sourceNotebookId = notebookId,
                        targetNotebookId = targetNotebookId,
                        targetNotebookName = targetNotebookName
                    )
                }
                DeleteNotebookAction.MOVE_TO_TRASH -> {
                    noteDao.moveNotesInNotebookToTrash(
                        notebookId = notebookId,
                        trashedTimestamp = System.currentTimeMillis()
                    )
                }
            }
            notebookDao.deleteNotebookById(notebookId)
        }
    }

    // Tag operations
    suspend fun saveTag(tag: Tag): Long = tagDao.insertTag(tag)
    suspend fun deleteTag(tagId: Long) = tagDao.deleteTagById(tagId)

    suspend fun seedInitialDataIfEmpty() {
        noteDao.deleteDemoNotes()
        notebookDao.deleteDemoNotebooks()
    }
}
