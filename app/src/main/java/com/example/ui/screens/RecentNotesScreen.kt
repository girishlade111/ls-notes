package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ui.components.NoteCard
import com.example.ui.viewmodel.NotesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentNotesScreen(
    viewModel: NotesViewModel,
    onNoteClick: (Long) -> Unit,
    onOpenDrawer: () -> Unit,
    onOpenInfo: (com.example.data.model.Note) -> Unit,
    onExportNote: (com.example.data.model.Note) -> Unit
) {
    val recentNotes by viewModel.recentNotes.collectAsState()
    val settings by viewModel.settings.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recent Notes Timeline") },
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
            if (recentNotes.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No recent activity yet.")
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(recentNotes, key = { it.id }) { note ->
                        NoteCard(
                            note = note,
                            isListView = true,
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
