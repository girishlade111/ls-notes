package com.example.data.local

import androidx.room.*
import com.example.data.model.NoteHistory
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteHistoryDao {

    // --- Create / Insert ---

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: NoteHistory): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllHistory(histories: List<NoteHistory>): List<Long>

    // --- Read / Retrieve ---

    @Query("SELECT * FROM note_history WHERE noteId = :noteId ORDER BY timestamp DESC")
    fun getHistoryForNote(noteId: Long): Flow<List<NoteHistory>>

    @Query("SELECT * FROM note_history WHERE noteId = :noteId ORDER BY timestamp DESC")
    suspend fun getHistoryListForNote(noteId: Long): List<NoteHistory>

    @Query("SELECT * FROM note_history WHERE id = :id")
    suspend fun getHistoryById(id: Long): NoteHistory?

    @Query("SELECT * FROM note_history WHERE noteId = :noteId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestHistoryForNote(noteId: Long): NoteHistory?

    @Query("SELECT * FROM note_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<NoteHistory>>

    // --- Update ---

    @Update
    suspend fun updateHistory(history: NoteHistory)

    // --- Delete / Prune ---

    @Delete
    suspend fun deleteHistory(history: NoteHistory)

    @Query("DELETE FROM note_history WHERE id = :id")
    suspend fun deleteHistoryById(id: Long)

    @Query("DELETE FROM note_history WHERE noteId = :noteId")
    suspend fun deleteHistoryForNote(noteId: Long)

    @Query("DELETE FROM note_history WHERE noteId = :noteId AND id NOT IN (SELECT id FROM note_history WHERE noteId = :noteId ORDER BY timestamp DESC LIMIT :keepCount)")
    suspend fun deleteOlderHistoryItems(noteId: Long, keepCount: Int)

    @Query("DELETE FROM note_history WHERE noteId = :noteId AND timestamp < :beforeTimestamp")
    suspend fun deleteHistoryBeforeTimestamp(noteId: Long, beforeTimestamp: Long)
}

