package com.example.ui.screens

import androidx.compose.foundation.layout.*
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
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import com.example.data.model.Note
import com.example.data.model.NoteType
import com.example.ui.components.ExportBottomSheet
import com.example.ui.components.ExportDialog
import com.example.ui.components.NoteInfoSheet
import com.example.ui.viewmodel.NavSection
import com.example.ui.viewmodel.NotesViewModel

@Composable
fun MainScreen(
    viewModel: NotesViewModel = viewModel()
) {
    val currentSection by viewModel.currentSection.collectAsState()
    val allNotes by viewModel.allNotes.collectAsState()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var activeNoteIdForEdit by remember { mutableStateOf<Long?>(null) }
    var activeNoteIdForHistory by remember { mutableStateOf<Long?>(null) }
    var activeNewNoteType by remember { mutableStateOf(NoteType.TEXT) }

    var selectedNoteForInfo by remember { mutableStateOf<Note?>(null) }
    var selectedNoteForExport by remember { mutableStateOf<Note?>(null) }

    // User Toast messages
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        viewModel.userMessage.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    // Widget navigation shortcuts listener
    LaunchedEffect(Unit) {
        viewModel.widgetNavEvent.collect { event ->
            when (event) {
                is NotesViewModel.WidgetNavigationEvent.OpenNote -> {
                    activeNoteIdForHistory = null
                    activeNoteIdForEdit = event.noteId
                }
                is NotesViewModel.WidgetNavigationEvent.CreateNote -> {
                    activeNoteIdForHistory = null
                    activeNewNoteType = event.noteType
                    activeNoteIdForEdit = 0L
                }
                is NotesViewModel.WidgetNavigationEvent.Search -> {
                    activeNoteIdForHistory = null
                    activeNoteIdForEdit = null
                }
            }
        }
    }

    if (activeNoteIdForHistory != null) {
        NoteHistoryScreen(
            noteId = activeNoteIdForHistory!!,
            viewModel = viewModel,
            onBack = { activeNoteIdForHistory = null },
            onVersionRestored = { restored ->
                activeNoteIdForHistory = null
                activeNoteIdForEdit = restored.id
            }
        )
    } else if (activeNoteIdForEdit != null) {
        NoteEditorScreen(
            noteId = activeNoteIdForEdit!!,
            initialType = activeNewNoteType,
            viewModel = viewModel,
            onBack = { activeNoteIdForEdit = null }
        )
    } else {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet {
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .statusBarsPadding()
                            .padding(16.dp)
                    ) {
                        // Header
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 16.dp)
                        ) {
                            Surface(
                                color = MaterialTheme.colorScheme.primary,
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Icon(
                                    imageVector = Icons.Default.EditNote,
                                    contentDescription = "LS Notes Icon",
                                    tint = Color.White,
                                    modifier = Modifier.padding(8.dp).size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "LS Notes",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Artistic & Private Workspace",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.Gray
                                )
                            }
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        // Nav Items
                        NavigationDrawerItem(
                            label = { Text("All Notes") },
                            selected = currentSection == NavSection.ALL_NOTES,
                            onClick = {
                                viewModel.selectSection(NavSection.ALL_NOTES)
                                scope.launch { drawerState.close() }
                            },
                            icon = { Icon(Icons.Default.Notes, contentDescription = null) }
                        )

                        NavigationDrawerItem(
                            label = { Text("Notebooks & Folders") },
                            selected = currentSection == NavSection.NOTEBOOKS,
                            onClick = {
                                viewModel.selectSection(NavSection.NOTEBOOKS)
                                scope.launch { drawerState.close() }
                            },
                            icon = { Icon(Icons.Default.Folder, contentDescription = null) }
                        )

                        NavigationDrawerItem(
                            label = { Text("Tags & Labels") },
                            selected = currentSection == NavSection.TAGS,
                            onClick = {
                                viewModel.selectSection(NavSection.TAGS)
                                scope.launch { drawerState.close() }
                            },
                            icon = { Icon(Icons.Default.Tag, contentDescription = null) }
                        )

                        NavigationDrawerItem(
                            label = { Text("Pinned Notes") },
                            selected = currentSection == NavSection.PINNED,
                            onClick = {
                                viewModel.selectSection(NavSection.PINNED)
                                scope.launch { drawerState.close() }
                            },
                            icon = { Icon(Icons.Default.PushPin, contentDescription = null) }
                        )

                        NavigationDrawerItem(
                            label = { Text("Recent Notes") },
                            selected = currentSection == NavSection.RECENT,
                            onClick = {
                                viewModel.selectSection(NavSection.RECENT)
                                scope.launch { drawerState.close() }
                            },
                            icon = { Icon(Icons.Default.History, contentDescription = null) }
                        )

                        NavigationDrawerItem(
                            label = { Text("Archive") },
                            selected = currentSection == NavSection.ARCHIVE,
                            onClick = {
                                viewModel.selectSection(NavSection.ARCHIVE)
                                scope.launch { drawerState.close() }
                            },
                            icon = { Icon(Icons.Default.Archive, contentDescription = null) }
                        )

                        NavigationDrawerItem(
                            label = { Text("PrivateSafe") },
                            selected = currentSection == NavSection.PRIVATE_SAFE,
                            onClick = {
                                viewModel.selectSection(NavSection.PRIVATE_SAFE)
                                scope.launch { drawerState.close() }
                            },
                            icon = { Icon(Icons.Default.Security, contentDescription = null) }
                        )

                        NavigationDrawerItem(
                            label = { Text("Trash Bin") },
                            selected = currentSection == NavSection.TRASH,
                            onClick = {
                                viewModel.selectSection(NavSection.TRASH)
                                scope.launch { drawerState.close() }
                            },
                            icon = { Icon(Icons.Default.Delete, contentDescription = null) }
                        )

                        Spacer(modifier = Modifier.weight(1f))
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        NavigationDrawerItem(
                            label = { Text("Import & Export") },
                            selected = currentSection == NavSection.IMPORT_EXPORT,
                            onClick = {
                                viewModel.selectSection(NavSection.IMPORT_EXPORT)
                                scope.launch { drawerState.close() }
                            },
                            icon = { Icon(Icons.Default.ImportExport, contentDescription = null) }
                        )

                        NavigationDrawerItem(
                            label = { Text("Settings") },
                            selected = currentSection == NavSection.SETTINGS,
                            onClick = {
                                viewModel.selectSection(NavSection.SETTINGS)
                                scope.launch { drawerState.close() }
                            },
                            icon = { Icon(Icons.Default.Settings, contentDescription = null) }
                        )
                    }
                }
            }
        ) {
            Scaffold(
                snackbarHost = { SnackbarHost(snackbarHostState) },
                contentWindowInsets = WindowInsets.statusBars
            ) { paddingValues ->
                Box(modifier = Modifier.padding(paddingValues)) {
                    when (currentSection) {
                        NavSection.ALL_NOTES -> AllNotesScreen(
                            viewModel = viewModel,
                            onNoteClick = { noteId -> activeNoteIdForEdit = noteId },
                            onCreateNote = { type ->
                                activeNewNoteType = type
                                activeNoteIdForEdit = 0L
                            },
                            onOpenDrawer = { scope.launch { drawerState.open() } },
                            onOpenInfo = { note -> selectedNoteForInfo = note },
                            onExportNote = { note -> selectedNoteForExport = note },
                            onViewHistory = { noteId -> activeNoteIdForHistory = noteId }
                        )
                        NavSection.NOTEBOOKS -> NotebooksScreen(
                            viewModel = viewModel,
                            onNotebookClick = {
                                viewModel.selectSection(NavSection.ALL_NOTES)
                            },
                            onOpenDrawer = { scope.launch { drawerState.open() } }
                        )
                        NavSection.MANAGE_NOTEBOOKS -> ManageNotebooksScreen(
                            viewModel = viewModel,
                            onBack = { viewModel.selectSection(NavSection.NOTEBOOKS) }
                        )
                        NavSection.TAGS -> TagsScreen(
                            viewModel = viewModel,
                            onTagClick = {
                                viewModel.selectSection(NavSection.ALL_NOTES)
                            },
                            onOpenDrawer = { scope.launch { drawerState.open() } }
                        )
                        NavSection.PINNED -> PinnedNotesScreen(
                            viewModel = viewModel,
                            onNoteClick = { noteId -> activeNoteIdForEdit = noteId },
                            onOpenDrawer = { scope.launch { drawerState.open() } },
                            onOpenInfo = { note -> selectedNoteForInfo = note },
                            onExportNote = { note -> selectedNoteForExport = note }
                        )
                        NavSection.RECENT -> RecentNotesScreen(
                            viewModel = viewModel,
                            onNoteClick = { noteId -> activeNoteIdForEdit = noteId },
                            onOpenDrawer = { scope.launch { drawerState.open() } },
                            onOpenInfo = { note -> selectedNoteForInfo = note },
                            onExportNote = { note -> selectedNoteForExport = note }
                        )
                        NavSection.ARCHIVE -> ArchiveScreen(
                            viewModel = viewModel,
                            onNoteClick = { noteId -> activeNoteIdForEdit = noteId },
                            onOpenDrawer = { scope.launch { drawerState.open() } },
                            onOpenInfo = { note -> selectedNoteForInfo = note },
                            onExportNote = { note -> selectedNoteForExport = note }
                        )
                        NavSection.PRIVATE_SAFE -> PrivateSafeScreen(
                            viewModel = viewModel,
                            onNoteClick = { noteId -> activeNoteIdForEdit = noteId },
                            onOpenDrawer = { scope.launch { drawerState.open() } },
                            onOpenInfo = { note -> selectedNoteForInfo = note },
                            onExportNote = { note -> selectedNoteForExport = note }
                        )
                        NavSection.TRASH -> TrashScreen(
                            viewModel = viewModel,
                            onOpenDrawer = { scope.launch { drawerState.open() } }
                        )
                        NavSection.IMPORT_EXPORT -> ImportExportScreen(
                            viewModel = viewModel,
                            onOpenDrawer = { scope.launch { drawerState.open() } }
                        )
                        NavSection.SETTINGS -> SettingsScreen(
                            viewModel = viewModel,
                            onOpenDrawer = { scope.launch { drawerState.open() } }
                        )
                    }
                }
            }
        }
    }

    // Info Sheet
    selectedNoteForInfo?.let { note ->
        NoteInfoSheet(
            note = note,
            onDismiss = { selectedNoteForInfo = null }
        )
    }

    // Export Bottom Sheet
    selectedNoteForExport?.let { note ->
        ExportBottomSheet(
            note = note,
            allNotes = allNotes,
            onExportSuccess = { filename ->
                viewModel.emitToast("Successfully exported file as $filename")
            },
            onDismiss = { selectedNoteForExport = null }
        )
    }
}
