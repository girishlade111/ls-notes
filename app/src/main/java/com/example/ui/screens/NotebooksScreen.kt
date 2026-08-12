package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Notebook
import com.example.ui.viewmodel.NotesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotebooksScreen(
    viewModel: NotesViewModel,
    onNotebookClick: (Long) -> Unit,
    onOpenDrawer: () -> Unit
) {
    val notebooks by viewModel.allNotebooks.collectAsState()
    val allNotes by viewModel.rawNotes.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var newNotebookName by remember { mutableStateOf("") }
    var selectedColorHex by remember { mutableStateOf("#7C4DFF") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notebooks & Folders") },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Open Menu")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.selectSection(com.example.ui.viewmodel.NavSection.MANAGE_NOTEBOOKS) }
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Manage")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Notebook")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            if (notebooks.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No Notebooks created yet. Tap + to add one.")
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(notebooks, key = { it.id }) { notebook ->
                        val count = allNotes.count { it.notebookId == notebook.id }
                        val cardBg = runCatching { Color(android.graphics.Color.parseColor(notebook.colorHex)) }
                            .getOrDefault(MaterialTheme.colorScheme.primaryContainer)

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                                .clickable {
                                    viewModel.setNotebookFilter(notebook.id)
                                    onNotebookClick(notebook.id)
                                },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = cardBg.copy(alpha = 0.25f)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Folder,
                                        contentDescription = null,
                                        tint = cardBg,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    IconButton(
                                        onClick = { viewModel.deleteNotebook(notebook) }
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray, modifier = Modifier.size(18.dp))
                                    }
                                }

                                Column {
                                    Text(
                                        text = notebook.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "$count notes",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        val colors = listOf("#7C4DFF", "#FF5722", "#00B0FF", "#4CAF50", "#E91E63", "#FF9800")
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Create Notebook") },
            text = {
                Column {
                    OutlinedTextField(
                        value = newNotebookName,
                        onValueChange = { newNotebookName = it },
                        label = { Text("Notebook Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Theme Color", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        colors.forEach { hex ->
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(Color(android.graphics.Color.parseColor(hex)), shape = RoundedCornerShape(16.dp))
                                    .clickable { selectedColorHex = hex }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newNotebookName.isNotBlank()) {
                            viewModel.createNotebook(newNotebookName, selectedColorHex)
                            newNotebookName = ""
                            showCreateDialog = false
                        }
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
