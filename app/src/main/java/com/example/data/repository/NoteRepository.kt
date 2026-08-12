package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.LsNotesDatabase
import com.example.data.local.NoteDao
import com.example.data.local.NoteHistoryDao
import com.example.data.local.NotebookDao
import com.example.data.model.Note
import com.example.data.model.NoteHistory
import com.example.data.model.NoteSortOrder
import com.example.data.model.sortNotes
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface NoteRepository {
    val allActiveNotes: Flow<List<Note>>
    val pinnedNotes: Flow<List<Note>>
    val recentNotes: Flow<List<Note>>
    val privateNotes: Flow<List<Note>>
    val trashNotes: Flow<List<Note>>
    val archivedNotes: Flow<List<Note>>

    val notesSortedByLastModified: Flow<List<Note>>
    val notesSortedByTitle: Flow<List<Note>>
    val notesSortedByDateCreated: Flow<List<Note>>

    fun getSortedNotes(sortOrder: NoteSortOrder = NoteSortOrder.DEFAULT): Flow<List<Note>>
    fun getSortedNotesByNotebook(notebookId: Long, sortOrder: NoteSortOrder = NoteSortOrder.DEFAULT): Flow<List<Note>>
    fun getSortedNotesByTag(tag: String, sortOrder: NoteSortOrder = NoteSortOrder.DEFAULT): Flow<List<Note>>

    suspend fun getNoteById(id: Long): Note?
    fun observeNoteById(id: Long): Flow<Note?>
    fun getNotesByNotebook(notebookId: Long): Flow<List<Note>>
    fun getNotesByTag(tag: String): Flow<List<Note>>

    suspend fun saveNote(note: Note): Long
    suspend fun updateNote(note: Note)
    suspend fun deleteNote(note: Note)
    suspend fun softDeleteNote(note: Note)
    suspend fun softDeleteNoteById(id: Long)
    suspend fun moveNoteToTrash(id: Long)
    suspend fun restoreNoteFromTrash(id: Long)
    suspend fun deleteNotePermanently(id: Long)
    suspend fun emptyTrash()
    suspend fun purgeOldTrash(daysOld: Int = 30)
    suspend fun togglePinNote(id: Long)
    suspend fun toggleFavoriteNote(id: Long)
    suspend fun toggleArchiveNote(id: Long)
    suspend fun archiveNote(id: Long)
    suspend fun unarchiveNote(id: Long)
    suspend fun setNotePrivacy(id: Long, isPrivate: Boolean)
    suspend fun duplicateNote(id: Long): Long
    suspend fun moveNoteToNotebook(noteId: Long, notebookId: Long?, notebookName: String)

    fun getHistoryForNote(noteId: Long): Flow<List<NoteHistory>>
    suspend fun getHistoryById(historyId: Long): NoteHistory?
    suspend fun saveNoteHistorySnapshot(note: Note, summary: String = "Version snapshot"): Long
    suspend fun revertNoteToHistory(noteId: Long, historyId: Long): Note?

    suspend fun <R> runInTransaction(block: suspend () -> R): R
    suspend fun importNotesBatchInTransaction(notes: List<Note>, targetNotebookName: String): List<Note>
}

class NoteRepositoryImpl(
    private val noteDao: NoteDao,
    private val noteHistoryDao: NoteHistoryDao,
    private val notebookDao: NotebookDao? = null,
    private val database: LsNotesDatabase? = null
) : NoteRepository {

    override suspend fun <R> runInTransaction(block: suspend () -> R): R {
        return if (database != null) {
            database.withTransaction(block)
        } else {
            block()
        }
    }

    override suspend fun importNotesBatchInTransaction(
        notes: List<Note>,
        targetNotebookName: String
    ): List<Note> {
        val action = suspend {
            val notebookId = notebookDao?.insertNotebook(
                com.example.data.model.Notebook(name = targetNotebookName, colorHex = "#7C4DFF")
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

        return if (database != null) {
            database.withTransaction(action)
        } else {
            action()
        }
    }
    override val allActiveNotes: Flow<List<Note>> = noteDao.getAllActiveNotes()
    override val pinnedNotes: Flow<List<Note>> = noteDao.getPinnedNotes()
    override val recentNotes: Flow<List<Note>> = noteDao.getRecentNotes()
    override val privateNotes: Flow<List<Note>> = noteDao.getPrivateNotes()
    override val trashNotes: Flow<List<Note>> = noteDao.getTrashNotes()
    override val archivedNotes: Flow<List<Note>> = noteDao.getArchivedNotes()

    override val notesSortedByLastModified: Flow<List<Note>> = noteDao.getNotesSortedByUpdatedDesc()
    override val notesSortedByTitle: Flow<List<Note>> = noteDao.getNotesSortedByTitleAsc()
    override val notesSortedByDateCreated: Flow<List<Note>> = noteDao.getNotesSortedByCreatedDesc()

    override fun getSortedNotes(sortOrder: NoteSortOrder): Flow<List<Note>> {
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

    override fun getSortedNotesByNotebook(notebookId: Long, sortOrder: NoteSortOrder): Flow<List<Note>> {
        return getNotesByNotebook(notebookId).map { it.sortNotes(sortOrder) }
    }

    override fun getSortedNotesByTag(tag: String, sortOrder: NoteSortOrder): Flow<List<Note>> {
        return getNotesByTag(tag).map { it.sortNotes(sortOrder) }
    }

    override suspend fun getNoteById(id: Long): Note? = noteDao.getNoteById(id)

    override fun observeNoteById(id: Long): Flow<Note?> = noteDao.observeNoteById(id)

    override fun getNotesByNotebook(notebookId: Long): Flow<List<Note>> = noteDao.getNotesByNotebook(notebookId)

    override fun getNotesByTag(tag: String): Flow<List<Note>> = noteDao.getNotesByTag(tag)

    override suspend fun saveNote(note: Note): Long {
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

    override suspend fun updateNote(note: Note) {
        val noteToUpdate = note.copy(updatedTimestamp = System.currentTimeMillis())
        noteDao.updateNote(noteToUpdate)
        saveNoteHistorySnapshot(noteToUpdate, "Updated Note")
    }

    override fun getHistoryForNote(noteId: Long): Flow<List<NoteHistory>> = noteHistoryDao.getHistoryForNote(noteId)

    override suspend fun getHistoryById(historyId: Long): NoteHistory? = noteHistoryDao.getHistoryById(historyId)

    override suspend fun saveNoteHistorySnapshot(note: Note, summary: String): Long {
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

    override suspend fun revertNoteToHistory(noteId: Long, historyId: Long): Note? {
        val currentNote = getNoteById(noteId) ?: return null
        val history = getHistoryById(historyId) ?: return null

        // Save current note state as backup history before reverting
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

    override suspend fun softDeleteNote(note: Note) {
        val trashedNote = note.copy(
            isInTrash = true,
            trashedTimestamp = System.currentTimeMillis()
        )
        noteDao.updateNote(trashedNote)
    }

    override suspend fun softDeleteNoteById(id: Long) {
        moveNoteToTrash(id)
    }

    override suspend fun deleteNote(note: Note) {
        softDeleteNote(note)
    }

    override suspend fun moveNoteToTrash(id: Long) {
        val note = getNoteById(id) ?: return
        softDeleteNote(note)
    }

    override suspend fun restoreNoteFromTrash(id: Long) {
        val note = getNoteById(id) ?: return
        noteDao.updateNote(
            note.copy(
                isInTrash = false,
                trashedTimestamp = 0L
            )
        )
    }

    override suspend fun deleteNotePermanently(id: Long) {
        noteDao.deleteNoteById(id)
    }

    override suspend fun emptyTrash() {
        noteDao.emptyTrash()
    }

    override suspend fun purgeOldTrash(daysOld: Int) {
        val cutoff = System.currentTimeMillis() - (daysOld * 24L * 60L * 60L * 1000L)
        noteDao.purgeOldTrash(cutoff)
    }

    override suspend fun togglePinNote(id: Long) {
        val note = getNoteById(id) ?: return
        noteDao.updateNote(note.copy(isPinned = !note.isPinned))
    }

    override suspend fun toggleFavoriteNote(id: Long) {
        val note = getNoteById(id) ?: return
        noteDao.updateNote(note.copy(isFavorite = !note.isFavorite))
    }

    override suspend fun toggleArchiveNote(id: Long) {
        val note = getNoteById(id) ?: return
        noteDao.updateNote(
            note.copy(
                isArchived = !note.isArchived,
                isPinned = false,
                updatedTimestamp = System.currentTimeMillis()
            )
        )
    }

    override suspend fun archiveNote(id: Long) {
        val note = getNoteById(id) ?: return
        noteDao.updateNote(
            note.copy(
                isArchived = true,
                isPinned = false,
                updatedTimestamp = System.currentTimeMillis()
            )
        )
    }

    override suspend fun unarchiveNote(id: Long) {
        val note = getNoteById(id) ?: return
        noteDao.updateNote(
            note.copy(
                isArchived = false,
                updatedTimestamp = System.currentTimeMillis()
            )
        )
    }

    override suspend fun setNotePrivacy(id: Long, isPrivate: Boolean) {
        val note = getNoteById(id) ?: return
        noteDao.updateNote(note.copy(isPrivate = isPrivate, updatedTimestamp = System.currentTimeMillis()))
    }

    override suspend fun duplicateNote(id: Long): Long {
        val note = getNoteById(id) ?: return 0L
        val copyNote = note.copy(
            id = 0,
            title = "${note.title} (Copy)",
            createdTimestamp = System.currentTimeMillis(),
            updatedTimestamp = System.currentTimeMillis()
        )
        return noteDao.insertNote(copyNote)
    }

    override suspend fun moveNoteToNotebook(noteId: Long, notebookId: Long?, notebookName: String) {
        val note = getNoteById(noteId) ?: return
        noteDao.updateNote(
            note.copy(
                notebookId = notebookId,
                notebookName = notebookName,
                updatedTimestamp = System.currentTimeMillis()
            )
        )
    }
}
