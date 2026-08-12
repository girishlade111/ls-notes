package com.example.data.local

import androidx.room.*
import com.example.data.model.Note
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE isInTrash = 0 AND isPrivate = 0 AND isArchived = 0 ORDER BY isPinned DESC, updatedTimestamp DESC")
    fun getAllActiveNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE isInTrash = 0 AND isPrivate = 0 AND isArchived = 0 ORDER BY isPinned DESC, updatedTimestamp DESC")
    fun getNotesSortedByUpdatedDesc(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE isInTrash = 0 AND isPrivate = 0 AND isArchived = 0 ORDER BY isPinned DESC, updatedTimestamp ASC")
    fun getNotesSortedByUpdatedAsc(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE isInTrash = 0 AND isPrivate = 0 AND isArchived = 0 ORDER BY isPinned DESC, title COLLATE NOCASE ASC")
    fun getNotesSortedByTitleAsc(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE isInTrash = 0 AND isPrivate = 0 AND isArchived = 0 ORDER BY isPinned DESC, title COLLATE NOCASE DESC")
    fun getNotesSortedByTitleDesc(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE isInTrash = 0 AND isPrivate = 0 AND isArchived = 0 ORDER BY isPinned DESC, createdTimestamp DESC")
    fun getNotesSortedByCreatedDesc(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE isInTrash = 0 AND isPrivate = 0 AND isArchived = 0 ORDER BY isPinned DESC, createdTimestamp ASC")
    fun getNotesSortedByCreatedAsc(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE isInTrash = 0 AND isPrivate = 0 AND isArchived = 0 AND isPinned = 1 ORDER BY updatedTimestamp DESC")
    fun getPinnedNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE isInTrash = 0 AND isPrivate = 0 AND isArchived = 0 AND isPinned = 1 ORDER BY updatedTimestamp DESC")
    suspend fun getPinnedNotesDirect(): List<Note>

    @Query("SELECT * FROM notes WHERE isInTrash = 0 AND isPrivate = 0 AND isArchived = 0 ORDER BY updatedTimestamp DESC LIMIT 10")
    fun getRecentNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE isInTrash = 0 AND isPrivate = 0 AND isArchived = 0 ORDER BY updatedTimestamp DESC LIMIT 10")
    suspend fun getRecentNotesDirect(): List<Note>

    @Query("SELECT * FROM notes WHERE isInTrash = 0 AND isPrivate = 0 AND isArchived = 1 ORDER BY updatedTimestamp DESC")
    fun getArchivedNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE isInTrash = 0 AND isPrivate = 1 ORDER BY isPinned DESC, updatedTimestamp DESC")
    fun getPrivateNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE isInTrash = 1 ORDER BY trashedTimestamp DESC")
    fun getTrashNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: Long): Note?

    @Query("SELECT * FROM notes WHERE id = :id")
    fun observeNoteById(id: Long): Flow<Note?>

    @Query("SELECT * FROM notes WHERE isInTrash = 0 AND isPrivate = 0 AND notebookId = :notebookId ORDER BY isPinned DESC, updatedTimestamp DESC")
    fun getNotesByNotebook(notebookId: Long): Flow<List<Note>>

    @Query("UPDATE notes SET notebookId = :targetNotebookId, notebookName = :targetNotebookName WHERE notebookId = :sourceNotebookId")
    suspend fun moveNotesToNotebook(sourceNotebookId: Long, targetNotebookId: Long?, targetNotebookName: String)

    @Query("UPDATE notes SET notebookName = :newName WHERE notebookId = :notebookId")
    suspend fun updateNotesNotebookName(notebookId: Long, newName: String)

    @Query("UPDATE notes SET isInTrash = 1, trashedTimestamp = :trashedTimestamp WHERE notebookId = :notebookId")
    suspend fun moveNotesInNotebookToTrash(notebookId: Long, trashedTimestamp: Long = System.currentTimeMillis())

    @Query("SELECT * FROM notes WHERE isInTrash = 0 AND isPrivate = 0 AND tagsCsv LIKE '%' || :tag || '%' ORDER BY isPinned DESC, updatedTimestamp DESC")
    fun getNotesByTag(tag: String): Flow<List<Note>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotes(notes: List<Note>): List<Long>

    @Update
    suspend fun updateNote(note: Note)

    @Delete
    suspend fun deleteNote(note: Note)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteNoteById(id: Long)

    @Query("DELETE FROM notes WHERE isInTrash = 1")
    suspend fun emptyTrash()

    @Query("DELETE FROM notes WHERE isInTrash = 1 AND trashedTimestamp < :cutoffTimestamp")
    suspend fun purgeOldTrash(cutoffTimestamp: Long)

    @Query("DELETE FROM notes WHERE title IN ('Welcome to LS Notes', 'Daily Focus Checklist', 'PrivateSafe Notes Instructions', 'Welcome to LS Notes! 🚀', 'Project Ideas 💡')")
    suspend fun deleteDemoNotes()
}
