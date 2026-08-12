package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.*
import com.example.ui.components.NoteCard
import com.example.ui.viewmodel.NotesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllNotesScreen(
    viewModel: NotesViewModel,
    onNoteClick: (Long) -> Unit,
    onCreateNote: (NoteType) -> Unit,
    onOpenDrawer: () -> Unit,
    onOpenInfo: (Note) -> Unit,
    onExportNote: (Note) -> Unit,
    onViewHistory: ((Long) -> Unit)? = null
) {
    val filterState by viewModel.filterState.collectAsState()
    val notes by viewModel.filteredNotes.collectAsState()
    val notebooks by viewModel.allNotebooks.collectAsState()
    val tags by viewModel.allTags.collectAsState()
    val settings by viewModel.settings.collectAsState()

    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    var showCreateFabMenu by remember { mutableStateOf(false) }

    androidx.activity.compose.BackHandler(enabled = filterState.searchQuery.isNotEmpty()) {
        viewModel.updateSearchQuery("")
    }

    Scaffold(
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 4.dp)
            ) {
                // Global Search Bar
                OutlinedTextField(
                    value = filterState.searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search title, text, tags, folders...") },
                    leadingIcon = {
                        IconButton(onClick = onOpenDrawer) {
                            Icon(Icons.Default.Menu, contentDescription = "Open Drawer Navigation")
                        }
                    },
                    trailingIcon = {
                        Row {
                            IconButton(onClick = { viewModel.toggleViewMode() }) {
                                Icon(
                                    imageVector = if (filterState.viewMode == NoteViewMode.GRID) Icons.Default.ViewList else Icons.Default.GridView,
                                    contentDescription = "Toggle Grid/List View"
                                )
                            }
                            if (filterState.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear search")
                                }
                            }
                        }
                    },
                    shape = RoundedCornerShape(28.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Notebook & Tag Filter Chips Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Notebook Selector Dropdown Chip
                    FilterChip(
                        selected = filterState.selectedNotebookId != null,
                        onClick = { viewModel.setNotebookFilter(null) },
                        label = {
                            val activeName = notebooks.find { it.id == filterState.selectedNotebookId }?.name ?: "All Notebooks"
                            Text(activeName)
                        },
                        trailingIcon = {
                            if (filterState.selectedNotebookId != null) {
                                Icon(Icons.Default.Close, contentDescription = "Clear Notebook Filter", modifier = Modifier.size(16.dp))
                            }
                        }
                    )

                    // Tag Selector Chip
                    if (tags.isNotEmpty()) {
                        FilterChip(
                            selected = filterState.selectedTagId != null,
                            onClick = { viewModel.setTagFilter(null) },
                            label = {
                                val activeTag = tags.find { it.id == filterState.selectedTagId }?.name ?: "All Tags"
                                Text(activeTag)
                            },
                            trailingIcon = {
                                if (filterState.selectedTagId != null) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear Tag Filter", modifier = Modifier.size(16.dp))
                                }
                            }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End) {
                AnimatedVisibility(
                    visible = showCreateFabMenu,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        ExtendedFloatingActionButton(
                            text = { Text("Text Note") },
                            icon = { Icon(Icons.Default.Notes, contentDescription = null) },
                            onClick = {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                showCreateFabMenu = false; onCreateNote(NoteType.TEXT)
                            },
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                        ExtendedFloatingActionButton(
                            text = { Text("Checklist") },
                            icon = { Icon(Icons.Default.CheckBox, contentDescription = null) },
                            onClick = {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                showCreateFabMenu = false; onCreateNote(NoteType.CHECKLIST)
                            },
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                        ExtendedFloatingActionButton(
                            text = { Text("Sketch / Drawing") },
                            icon = { Icon(Icons.Default.Brush, contentDescription = null) },
                            onClick = {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                showCreateFabMenu = false; onCreateNote(NoteType.SKETCH)
                            },
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                        ExtendedFloatingActionButton(
                            text = { Text("Smart Card") },
                            icon = { Icon(Icons.Default.Bookmark, contentDescription = null) },
                            onClick = {
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                                showCreateFabMenu = false; onCreateNote(NoteType.SMART_CARD)
                            },
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    }
                }

                FloatingActionButton(
                    onClick = {
                        haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                        showCreateFabMenu = !showCreateFabMenu
                    },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        imageVector = if (showCreateFabMenu) Icons.Default.Close else Icons.Default.Add,
                        contentDescription = "Create Note"
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 8.dp)
        ) {
            if (notes.isEmpty()) {
                // Empty State
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.NoteAdd,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No notes yet",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Tap + to capture your first idea, checklist, or sketch in LS Notes.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            } else {
                if (filterState.viewMode == NoteViewMode.GRID) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(bottom = 80.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(notes, key = { it.id }) { note ->
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
                                },
                                onViewHistory = onViewHistory?.let { action -> { action(note.id) } },
                                onToggleArchive = { viewModel.toggleArchiveNote(note.id) }
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 80.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(notes, key = { it.id }) { note ->
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
                                onViewHistory = onViewHistory?.let { action -> { action(note.id) } },
                                onToggleArchive = { viewModel.toggleArchiveNote(note.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}
