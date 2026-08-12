package com.example.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.repository.LsNotesRepository
import com.example.widget.PinnedNotesWidgetProvider
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

enum class NavSection {
    ALL_NOTES,
    NOTEBOOKS,
    MANAGE_NOTEBOOKS,
    TAGS,
    PINNED,
    RECENT,
    ARCHIVE,
    PRIVATE_SAFE,
    TRASH,
    SETTINGS,
    IMPORT_EXPORT
}

data class FilterState(
    val searchQuery: String = "",
    val selectedNotebookId: Long? = null,
    val selectedTag: String? = null,
    val selectedType: NoteType? = null,
    val viewMode: NoteViewMode = NoteViewMode.GRID,
    val sortOrder: NoteSortOrder = NoteSortOrder.DEFAULT
)

class NotesViewModel(application: Application) : AndroidViewModel(application) {
    val repository = LsNotesRepository(application)

    val settings: StateFlow<AppSettings> = repository.settingsManager.settings

    private val _currentSection = MutableStateFlow(NavSection.ALL_NOTES)
    val currentSection: StateFlow<NavSection> = _currentSection.asStateFlow()

    private val _filterState = MutableStateFlow(FilterState())
    val filterState: StateFlow<FilterState> = _filterState.asStateFlow()

    private val _isPrivateSafeUnlocked = MutableStateFlow(false)
    val isPrivateSafeUnlocked: StateFlow<Boolean> = _isPrivateSafeUnlocked.asStateFlow()

    sealed class WidgetNavigationEvent {
        data class OpenNote(val noteId: Long) : WidgetNavigationEvent()
        data class CreateNote(val noteType: NoteType) : WidgetNavigationEvent()
        object Search : WidgetNavigationEvent()
    }

    private val _widgetNavEvent = MutableSharedFlow<WidgetNavigationEvent>()
    val widgetNavEvent = _widgetNavEvent.asSharedFlow()

    fun handleWidgetIntent(intent: android.content.Intent?) {
        if (intent == null) return
        val noteId = intent.getLongExtra(PinnedNotesWidgetProvider.EXTRA_NOTE_ID, -1L)
        if (noteId > 0L) {
            viewModelScope.launch {
                _widgetNavEvent.emit(WidgetNavigationEvent.OpenNote(noteId))
            }
            return
        }

        val action = intent.getStringExtra(PinnedNotesWidgetProvider.EXTRA_ACTION)
        if (action == PinnedNotesWidgetProvider.ACTION_CREATE_NOTE) {
            val typeStr = intent.getStringExtra(PinnedNotesWidgetProvider.EXTRA_CREATE_NOTE_TYPE) ?: "TEXT"
            val noteType = when (typeStr) {
                "CHECKLIST" -> NoteType.CHECKLIST
                "PHOTO", "IMAGE", "CAMERA" -> NoteType.PHOTO
                "VOICE", "AUDIO" -> NoteType.AUDIO
                "DRAWING", "SKETCH" -> NoteType.SKETCH
                else -> NoteType.TEXT
            }
            viewModelScope.launch {
                _widgetNavEvent.emit(WidgetNavigationEvent.CreateNote(noteType))
            }
        } else if (action == "ACTION_SEARCH") {
            selectSection(NavSection.ALL_NOTES)
            viewModelScope.launch {
                _widgetNavEvent.emit(WidgetNavigationEvent.Search)
            }
        }
    }

    private val _userMessage = MutableSharedFlow<String>()
    val userMessage = _userMessage.asSharedFlow()

    val allNotebooks: StateFlow<List<Notebook>> = repository.allNotebooks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTags: StateFlow<List<Tag>> = repository.allTags
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val rawNotes: StateFlow<List<Note>> = repository.allActiveNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allNotes: StateFlow<List<Note>> = rawNotes

    val pinnedNotes: StateFlow<List<Note>> = repository.pinnedNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentNotes: StateFlow<List<Note>> = repository.recentNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val privateNotes: StateFlow<List<Note>> = repository.privateNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val trashNotes: StateFlow<List<Note>> = repository.trashNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val archivedNotes: StateFlow<List<Note>> = repository.archivedNotes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notesSortedByLastModified: StateFlow<List<Note>> = repository.notesSortedByLastModified
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notesSortedByTitle: StateFlow<List<Note>> = repository.notesSortedByTitle
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notesSortedByDateCreated: StateFlow<List<Note>> = repository.notesSortedByDateCreated
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredNotes: StateFlow<List<Note>> = combine(rawNotes, filterState) { notes, filter ->
        notes.filter { note ->
            val matchesQuery = if (filter.searchQuery.isBlank()) true else {
                val q = filter.searchQuery.lowercase()
                note.title.lowercase().contains(q) ||
                note.content.lowercase().contains(q) ||
                note.tagsCsv.lowercase().contains(q) ||
                note.notebookName.lowercase().contains(q)
            }
            val matchesNotebook = filter.selectedNotebookId == null || note.notebookId == filter.selectedNotebookId
            val matchesTag = filter.selectedTag == null || note.tagsCsv.contains(filter.selectedTag)
            val matchesType = filter.selectedType == null || note.type == filter.selectedType

            matchesQuery && matchesNotebook && matchesTag && matchesType
        }.sortNotes(filter.sortOrder)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            repository.seedInitialDataIfEmpty()
            repository.purgeOldTrash(30)
        }
    }

    fun selectSection(section: NavSection) {
        _currentSection.value = section
    }

    fun updateSearchQuery(query: String) {
        _filterState.value = _filterState.value.copy(searchQuery = query)
    }

    fun setNotebookFilter(notebookId: Long?) {
        _filterState.value = _filterState.value.copy(selectedNotebookId = notebookId)
    }

    fun setTagFilter(tag: String?) {
        _filterState.value = _filterState.value.copy(selectedTag = tag)
    }

    fun setTypeFilter(type: NoteType?) {
        _filterState.value = _filterState.value.copy(selectedType = type)
    }

    fun setSortOrder(sortOrder: NoteSortOrder) {
        _filterState.value = _filterState.value.copy(sortOrder = sortOrder)
    }

    fun toggleViewMode() {
        val current = _filterState.value.viewMode
        val newMode = if (current == NoteViewMode.GRID) NoteViewMode.LIST else NoteViewMode.GRID
        _filterState.value = _filterState.value.copy(viewMode = newMode)
    }

    fun unlockPrivateSafe(passcode: String): Boolean {
        val storedPasscode = settings.value.privateSafePasscode
        if (storedPasscode.isEmpty() || passcode == storedPasscode) {
            _isPrivateSafeUnlocked.value = true
            emitToast("PrivateSafe unlocked")
            return true
        } else {
            emitToast("Incorrect PrivateSafe passcode")
            return false
        }
    }

    fun unlockPrivateSafeWithBiometrics() {
        _isPrivateSafeUnlocked.value = true
        emitToast("PrivateSafe unlocked via Biometrics")
    }

    fun lockPrivateSafe() {
        _isPrivateSafeUnlocked.value = false
        emitToast("PrivateSafe locked")
    }

    private fun notifyWidgets() {
        runCatching {
            PinnedNotesWidgetProvider.updateAllWidgets(getApplication())
        }
    }

    fun saveNote(note: Note, onComplete: ((Long) -> Unit)? = null) {
        viewModelScope.launch {
            val id = repository.saveNote(note)
            emitToast(if (note.id == 0L) "Note created" else "Note saved")
            notifyWidgets()
            onComplete?.invoke(id)
        }
    }

    fun togglePinNote(noteId: Long) {
        viewModelScope.launch {
            repository.togglePinNote(noteId)
            emitToast("Note pinned status updated")
            notifyWidgets()
        }
    }

    fun toggleFavoriteNote(noteId: Long) {
        viewModelScope.launch {
            repository.toggleFavoriteNote(noteId)
            emitToast("Note favorite status updated")
            notifyWidgets()
        }
    }

    fun softDeleteNote(note: Note) {
        viewModelScope.launch {
            repository.softDeleteNote(note)
            emitToast("Note moved to Trash")
            notifyWidgets()
        }
    }

    fun moveNoteToTrash(noteId: Long) {
        viewModelScope.launch {
            repository.moveNoteToTrash(noteId)
            emitToast("Note moved to Trash")
            notifyWidgets()
        }
    }

    fun purgeOldTrash(daysOld: Int = 30) {
        viewModelScope.launch {
            repository.purgeOldTrash(daysOld)
            notifyWidgets()
        }
    }

    fun restoreNoteFromTrash(noteId: Long) {
        viewModelScope.launch {
            repository.restoreNoteFromTrash(noteId)
            emitToast("Note restored from Trash")
            notifyWidgets()
        }
    }

    fun deleteNotePermanently(noteId: Long) {
        viewModelScope.launch {
            repository.deleteNotePermanently(noteId)
            emitToast("Note permanently deleted")
            notifyWidgets()
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            repository.emptyTrash()
            emitToast("Trash emptied")
            notifyWidgets()
        }
    }

    fun duplicateNote(noteId: Long) {
        viewModelScope.launch {
            repository.duplicateNote(noteId)
            emitToast("Note duplicated")
        }
    }

    fun toggleArchiveNote(noteId: Long) {
        viewModelScope.launch {
            repository.toggleArchiveNote(noteId)
            emitToast("Note archive status updated")
        }
    }

    fun archiveNote(noteId: Long) {
        viewModelScope.launch {
            repository.archiveNote(noteId)
            emitToast("Note archived")
        }
    }

    fun unarchiveNote(noteId: Long) {
        viewModelScope.launch {
            repository.unarchiveNote(noteId)
            emitToast("Note unarchived")
        }
    }

    fun setNotePrivacy(noteId: Long, isPrivate: Boolean) {
        viewModelScope.launch {
            repository.setNotePrivacy(noteId, isPrivate)
            if (isPrivate) {
                emitToast("Note moved to PrivateSafe")
            } else {
                emitToast("Note removed from PrivateSafe")
            }
        }
    }

    fun createNotebook(name: String, colorHex: String) {
        viewModelScope.launch {
            repository.saveNotebook(Notebook(name = name, colorHex = colorHex))
            emitToast("Notebook '$name' created")
        }
    }

    fun renameNotebook(notebook: Notebook, newName: String, newColorHex: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            repository.renameNotebook(notebook, newName, newColorHex)
            emitToast("Notebook updated to '$newName'")
        }
    }

    fun reorderNotebooks(orderedList: List<Notebook>) {
        viewModelScope.launch {
            repository.saveNotebookOrder(orderedList)
        }
    }

    fun moveNotebookUp(notebook: Notebook, currentList: List<Notebook>) {
        val index = currentList.indexOfFirst { it.id == notebook.id }
        if (index > 0) {
            val mutable = currentList.toMutableList()
            val item = mutable.removeAt(index)
            mutable.add(index - 1, item)
            reorderNotebooks(mutable)
        }
    }

    fun moveNotebookDown(notebook: Notebook, currentList: List<Notebook>) {
        val index = currentList.indexOfFirst { it.id == notebook.id }
        if (index >= 0 && index < currentList.size - 1) {
            val mutable = currentList.toMutableList()
            val item = mutable.removeAt(index)
            mutable.add(index + 1, item)
            reorderNotebooks(mutable)
        }
    }

    fun deleteNotebookWithAction(
        notebookId: Long,
        action: DeleteNotebookAction,
        targetNotebookId: Long? = null,
        targetNotebookName: String = "Uncategorized"
    ) {
        viewModelScope.launch {
            repository.deleteNotebookWithAction(
                notebookId = notebookId,
                action = action,
                targetNotebookId = targetNotebookId,
                targetNotebookName = targetNotebookName
            )
            emitToast("Notebook deleted successfully")
        }
    }

    fun deleteNotebook(notebook: Notebook) {
        viewModelScope.launch {
            repository.deleteNotebookWithAction(
                notebookId = notebook.id,
                action = DeleteNotebookAction.MOVE_TO_UNCATEGORIZED
            )
            emitToast("Notebook '${notebook.name}' deleted")
        }
    }

    fun createTag(name: String, colorHex: String) {
        viewModelScope.launch {
            repository.saveTag(Tag(name = name, colorHex = colorHex))
            emitToast("Tag '$name' created")
        }
    }

    fun deleteTag(tagId: Long) {
        viewModelScope.launch {
            repository.deleteTag(tagId)
            emitToast("Tag deleted")
        }
    }

    fun updateSettings(newSettings: AppSettings) {
        repository.settingsManager.updateSettings(newSettings)
        emitToast("Settings updated")
    }

    fun setPrivateSafePasscode(passcode: String) {
        repository.settingsManager.setPrivateSafePasscode(passcode)
        _isPrivateSafeUnlocked.value = true
        emitToast("PrivateSafe passcode set")
    }

    fun importFromFileUri(
        uri: Uri,
        fileName: String?,
        targetNotebookName: String = "Imported Notes"
    ) {
        viewModelScope.launch {
            repository.noteImportService.importFromUri(uri, fileName, targetNotebookName)
                .onSuccess { savedNotes ->
                    emitToast("Imported ${savedNotes.size} note(s) offline successfully")
                }
                .onFailure { error ->
                    emitToast("Failed to import file: ${error.localizedMessage}")
                }
        }
    }

    fun importFromFileUris(
        uris: List<Uri>,
        targetNotebookName: String = "Imported Notes"
    ) {
        viewModelScope.launch {
            repository.noteImportService.importFromUris(uris, targetNotebookName)
                .onSuccess { savedNotes ->
                    emitToast("Imported ${savedNotes.size} note(s) across ${uris.size} file(s) offline")
                }
                .onFailure { error ->
                    emitToast("Failed to import files: ${error.localizedMessage}")
                }
        }
    }

    fun importFromTextContent(
        content: String,
        fileName: String,
        sourceType: String,
        targetNotebookName: String = "Imported Notes"
    ) {
        viewModelScope.launch {
            val parsedNotes = repository.noteImportService.parseContent(
                content = content,
                fileName = fileName,
                targetNotebookName = targetNotebookName
            )
            val savedNotes = repository.noteImportService.ingestParsedNotes(parsedNotes, targetNotebookName)
            emitToast("Imported ${savedNotes.size} note(s) from $sourceType")
        }
    }

    fun importLocalNotes(
        sourceType: String,
        targetNotebookName: String,
        importedCount: Int
    ) {
        viewModelScope.launch {
            val dummyContent = buildString {
                appendLine("# Imported Notes Summary")
                appendLine("Source: $sourceType")
                appendLine("Date: ${java.util.Date()}")
                appendLine()
                for (i in 1..importedCount) {
                    appendLine("## Section Note $i")
                    appendLine("- [ ] Task 1 for note $i")
                    appendLine("- [x] Completed item for note $i")
                    appendLine("Sample imported note content for entry $i from $sourceType.")
                    appendLine()
                }
            }

            val parsedNotes = repository.noteImportService.parseContent(
                content = dummyContent,
                fileName = "Imported_$sourceType.md",
                targetNotebookName = targetNotebookName
            )
            val saved = repository.noteImportService.ingestParsedNotes(parsedNotes, targetNotebookName)
            emitToast("Import completed: ${saved.size} note(s) added")
        }
    }

    fun getHistoryForNote(noteId: Long): Flow<List<NoteHistory>> {
        return repository.getHistoryForNote(noteId)
    }

    fun revertNoteToHistory(noteId: Long, historyId: Long, onRestored: (Note) -> Unit) {
        viewModelScope.launch {
            val restored = repository.revertNoteToHistory(noteId, historyId)
            if (restored != null) {
                emitToast("Restored version from history")
                onRestored(restored)
            } else {
                emitToast("Failed to restore version")
            }
        }
    }

    fun saveManualHistorySnapshot(note: Note, summary: String = "Manual Snapshot") {
        viewModelScope.launch {
            repository.saveNoteHistorySnapshot(note, summary)
            emitToast("Version snapshot saved")
        }
    }

    fun emitToast(message: String) {
        viewModelScope.launch {
            _userMessage.emit(message)
        }
    }
}
