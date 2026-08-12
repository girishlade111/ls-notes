package com.example.data.local

import androidx.room.*
import com.example.data.model.Notebook
import kotlinx.coroutines.flow.Flow

@Dao
interface NotebookDao {
    @Query("SELECT * FROM notebooks ORDER BY orderIndex ASC, name ASC")
    fun getAllNotebooks(): Flow<List<Notebook>>

    @Query("SELECT * FROM notebooks WHERE id = :id")
    suspend fun getNotebookById(id: Long): Notebook?

    @Query("SELECT * FROM notebooks WHERE id = :id")
    fun observeNotebookById(id: Long): Flow<Notebook?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotebook(notebook: Notebook): Long

    @Update
    suspend fun updateNotebook(notebook: Notebook)

    @Update
    suspend fun updateNotebooks(notebooks: List<Notebook>)

    @Delete
    suspend fun deleteNotebook(notebook: Notebook)

    @Query("DELETE FROM notebooks WHERE id = :id")
    suspend fun deleteNotebookById(id: Long)
}

typealias FolderDao = NotebookDao

