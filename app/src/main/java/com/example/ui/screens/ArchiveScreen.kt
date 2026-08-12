package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.NoteCard
import com.example.ui.viewmodel.NotesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArchiveScreen(
    viewModel: NotesViewModel,
    onNoteClick: (Long) -> Unit,
    onOpenDrawer: () -> Unit,
    onOpenInfo: (com.example.data.model.Note) -> Unit,
    onExportNote: (com.example.data.model.Note) -> Unit
) {
    val archivedNotes by viewModel.archivedNotes.collectAsState()
    val settings by viewModel.settings.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Archive") },
                navigationIcon = {
                    IconButton(
                        onClick = onOpenDrawer,
                        modifier = Modifier.testTag("archive_menu_button")
                    ) {
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
            if (archivedNotes.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Archive,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No archived notes",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Notes you archive will appear here, hidden from All Notes.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(archivedNotes, key = { it.id }) { note ->
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
                            },
                            onToggleArchive = { viewModel.unarchiveNote(note.id) }
                        )
                    }
                }
            }
        }
    }
}
