package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.Note
import com.example.data.model.NoteHistory
import com.example.data.model.Notebook
import com.example.data.model.Tag

@Database(
    entities = [Note::class, Notebook::class, Tag::class, NoteHistory::class],
    version = 4,
    exportSchema = false
)
abstract class LsNotesDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun notebookDao(): NotebookDao
    abstract fun tagDao(): TagDao
    abstract fun noteHistoryDao(): NoteHistoryDao

    companion object {
        @Volatile
        private var INSTANCE: LsNotesDatabase? = null

        fun getInstance(context: Context): LsNotesDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    LsNotesDatabase::class.java,
                    "ls_notes_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
