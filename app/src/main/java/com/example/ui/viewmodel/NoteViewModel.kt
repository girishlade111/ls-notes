package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.Note
import com.example.data.model.NoteHistory
import com.example.data.model.NoteType
import com.example.data.repository.NoteRepository
import com.example.di.DatabaseModule
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class NoteUiState(
    val isLoading: Boolean = false,
    val notes: List<Note> = emptyList(),
    val pinnedNotes: List<Note> = emptyList(),
    val recentNotes: List<Note> = emptyList(),
    val privateNotes: List<Note> = emptyList(),
    val trashNotes: List<Note> = emptyList(),
    val selectedNote: Note? = null,
    val searchQuery: String = "",
    val userMessage: String? = null
)

class NoteViewModel(
    application: Application,
    private val noteRepository: NoteRepository = DatabaseModule.provideNoteRepository(application)
) : AndroidViewModel(application) {

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedNoteId = MutableStateFlow<Long?>(null)
    
    val selectedNote: StateFlow<Note?> = _selectedNoteId
        .flatMapLatest { id ->
            if (id == null) flowOf(null)
            else noteRepository.observeNoteById(id)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val allActiveNotes: StateFlow<List<Note>> = noteRepository.allActiveNotes
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val pinnedNotes: StateFlow<List<Note>> = noteRepository.pinnedNotes
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val recentNotes: StateFlow<List<Note>> = noteRepository.recentNotes
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val privateNotes: StateFlow<List<Note>> = noteRepository.privateNotes
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val trashNotes: StateFlow<List<Note>> = noteRepository.trashNotes
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val filteredNotes: StateFlow<List<Note>> = combine(allActiveNotes, searchQuery) { notes, query ->
        if (query.isBlank()) {
            notes
        } else {
            val q = query.lowercase()
            notes.filter { note ->
                note.title.lowercase().contains(q) ||
                note.content.lowercase().contains(q) ||
                note.tagsCsv.lowercase().contains(q) ||
                note.notebookName.lowercase().contains(q)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _userMessage = MutableSharedFlow<String>()
    val userMessage = _userMessage.asSharedFlow()

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectNoteById(id: Long?) {
        _selectedNoteId.value = id
    }

    fun saveNote(note: Note, onComplete: ((Long) -> Unit)? = null) {
        viewModelScope.launch {
            val id = noteRepository.saveNote(note)
            _userMessage.emit(if (note.id == 0L) "Note created" else "Note saved")
            onComplete?.invoke(id)
        }
    }

    fun updateNote(note: Note) {
        viewModelScope.launch {
            noteRepository.updateNote(note)
            _userMessage.emit("Note updated")
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            noteRepository.deleteNote(note)
            _userMessage.emit("Note deleted")
        }
    }

    fun moveNoteToTrash(id: Long) {
        viewModelScope.launch {
            noteRepository.moveNoteToTrash(id)
            _userMessage.emit("Note moved to Trash")
        }
    }

    fun restoreNoteFromTrash(id: Long) {
        viewModelScope.launch {
            noteRepository.restoreNoteFromTrash(id)
            _userMessage.emit("Note restored")
        }
    }

    fun deleteNotePermanently(id: Long) {
        viewModelScope.launch {
            noteRepository.deleteNotePermanently(id)
            _userMessage.emit("Note deleted permanently")
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            noteRepository.emptyTrash()
            _userMessage.emit("Trash emptied")
        }
    }

    fun togglePinNote(id: Long) {
        viewModelScope.launch {
            noteRepository.togglePinNote(id)
            _userMessage.emit("Note pin status updated")
        }
    }

    fun toggleFavoriteNote(id: Long) {
        viewModelScope.launch {
            noteRepository.toggleFavoriteNote(id)
            _userMessage.emit("Note favorite status updated")
        }
    }

    fun setNotePrivacy(id: Long, isPrivate: Boolean) {
        viewModelScope.launch {
            noteRepository.setNotePrivacy(id, isPrivate)
            _userMessage.emit(if (isPrivate) "Moved to PrivateSafe" else "Removed from PrivateSafe")
        }
    }

    fun duplicateNote(id: Long) {
        viewModelScope.launch {
            noteRepository.duplicateNote(id)
            _userMessage.emit("Note duplicated")
        }
    }

    fun moveNoteToNotebook(noteId: Long, notebookId: Long?, notebookName: String) {
        viewModelScope.launch {
            noteRepository.moveNoteToNotebook(noteId, notebookId, notebookName)
            _userMessage.emit("Note moved to $notebookName")
        }
    }

    fun getHistoryForNote(noteId: Long): Flow<List<NoteHistory>> {
        return noteRepository.getHistoryForNote(noteId)
    }

    fun revertNoteToHistory(noteId: Long, historyId: Long, onRestored: ((Note) -> Unit)? = null) {
        viewModelScope.launch {
            val restored = noteRepository.revertNoteToHistory(noteId, historyId)
            if (restored != null) {
                _userMessage.emit("Restored note version from history")
                onRestored?.invoke(restored)
            } else {
                _userMessage.emit("Failed to restore note version")
            }
        }
    }

    fun saveManualHistorySnapshot(note: Note, summary: String = "Manual Snapshot") {
        viewModelScope.launch {
            noteRepository.saveNoteHistorySnapshot(note, summary)
            _userMessage.emit("Version snapshot saved")
        }
    }
}
