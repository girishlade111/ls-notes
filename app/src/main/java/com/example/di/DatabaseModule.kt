package com.example.di

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.FolderDao
import com.example.data.local.LocalFileManager
import com.example.data.local.NoteDao
import com.example.data.local.NoteHistoryDao
import com.example.data.local.NotebookDao
import com.example.data.local.SettingsManager
import com.example.data.local.TagDao
import com.example.data.repository.LsNotesRepository
import com.example.data.repository.NoteRepository
import com.example.data.repository.NoteRepositoryImpl

/**
 * Dependency Injection container & Provider module for Room Database and Repository instances.
 */
object DatabaseModule {

    @Volatile
    private var databaseInstance: AppDatabase? = null

    @Volatile
    private var repositoryInstance: LsNotesRepository? = null

    @Volatile
    private var noteRepositoryInstance: NoteRepository? = null

    fun provideAppDatabase(context: Context): AppDatabase {
        return databaseInstance ?: synchronized(this) {
            databaseInstance ?: AppDatabase(context.applicationContext).also { databaseInstance = it }
        }
    }

    fun provideNoteDao(context: Context): NoteDao {
        return provideAppDatabase(context).noteDao()
    }

    fun provideNotebookDao(context: Context): NotebookDao {
        return provideAppDatabase(context).notebookDao()
    }

    fun provideFolderDao(context: Context): FolderDao {
        return provideAppDatabase(context).notebookDao()
    }

    fun provideTagDao(context: Context): TagDao {
        return provideAppDatabase(context).tagDao()
    }

    fun provideNoteHistoryDao(context: Context): NoteHistoryDao {
        return provideAppDatabase(context).noteHistoryDao()
    }

    fun provideSettingsManager(context: Context): SettingsManager {
        return SettingsManager(context.applicationContext)
    }

    fun provideLocalFileManager(context: Context): LocalFileManager {
        return LocalFileManager(context.applicationContext)
    }

    fun provideNoteRepository(context: Context): NoteRepository {
        return noteRepositoryInstance ?: synchronized(this) {
            noteRepositoryInstance ?: NoteRepositoryImpl(
                provideNoteDao(context),
                provideNoteHistoryDao(context)
            ).also { noteRepositoryInstance = it }
        }
    }

    fun provideLsNotesRepository(context: Context): LsNotesRepository {
        return repositoryInstance ?: synchronized(this) {
            repositoryInstance ?: LsNotesRepository(context.applicationContext).also { repositoryInstance = it }
        }
    }

    fun provideNoteImportService(context: Context): com.example.data.repository.NoteImportService {
        return provideLsNotesRepository(context).noteImportService
    }
}
