package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.ExportBottomSheet
import com.example.ui.components.ImportDialog
import com.example.ui.viewmodel.NotesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportExportScreen(
    viewModel: NotesViewModel,
    onOpenDrawer: () -> Unit
) {
    val allNotes by viewModel.allNotes.collectAsState()
    var showImportDialog by remember { mutableStateOf(false) }
    var showExportBottomSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Import & Migration Center") },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Open Drawer")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Import Card
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.FileDownload, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Import Notes & Data", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Import from Evernote, OneNote, Google Keep, Pocket, or Markdown", style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { showImportDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Start Import Wizard")
                    }
                }
            }

            // Export Card
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.FileUpload, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Export Notes & Archives", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Export notes as PDF, Markdown, Plain Text, HTML, or JSON Backup", style = MaterialTheme.typography.bodySmall)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { showExportBottomSheet = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export Notes Collection")
                    }
                }
            }
        }
    }

    if (showImportDialog) {
        ImportDialog(
            onImportConfirmed = { sourceType, notebookName, count ->
                viewModel.importLocalNotes(sourceType, notebookName, count)
            },
            onDismiss = { showImportDialog = false }
        )
    }

    if (showExportBottomSheet) {
        ExportBottomSheet(
            note = null,
            allNotes = allNotes,
            onExportSuccess = { filename ->
                viewModel.emitToast("Successfully exported notes as $filename")
            },
            onDismiss = { showExportBottomSheet = false }
        )
    }
}
