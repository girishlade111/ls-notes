package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ui.components.NoteCard
import com.example.ui.viewmodel.NotesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PinnedNotesScreen(
    viewModel: NotesViewModel,
    onNoteClick: (Long) -> Unit,
    onOpenDrawer: () -> Unit,
    onOpenInfo: (com.example.data.model.Note) -> Unit,
    onExportNote: (com.example.data.model.Note) -> Unit
) {
    val pinnedNotes by viewModel.pinnedNotes.collectAsState()
    val settings by viewModel.settings.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pinned & Key Notes") },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Open Drawer")
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 8.dp)
        ) {
            if (pinnedNotes.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No pinned notes yet. Pin notes to keep them at the top.")
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(pinnedNotes, key = { it.id }) { note ->
                        NoteCard(
                            note = note,
                            isListView = false,
                            showTimeMode = settings.timeDisplayMode,
                            onClick = { onNoteClick(note.id) },
                            onPinToggle = { viewModel.togglePinNote(note.id) },
                            onFavoriteToggle = { viewModel.toggleFavoriteNote(note.id) },
                            onDuplicate = { viewModel.duplicateNote(note.id) },
                            onTogglePrivate = { viewModel.setNotePrivacy(note.id, !note.isPrivate) },
                            onDelete = { viewModel.moveNoteToTrash(note.id) },
                            onExport = { onExportNote(note) },
                            onShare = { viewModel.emitToast("Sharing note: ${note.title}") },
                            onInfo = { onOpenInfo(note) },
                            onChangeColor = { colorHex ->
                                viewModel.saveNote(note.copy(colorHex = colorHex))
                            }
                        )
                    }
                }
            }
        }
    }
}
