package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.DeleteNotebookAction
import com.example.data.model.Notebook
import com.example.ui.viewmodel.NotesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageNotebooksScreen(
    viewModel: NotesViewModel,
    onBack: () -> Unit
) {
    val haptic = androidx.compose.ui.platform.LocalHapticFeedback.current
    val notebooks by viewModel.allNotebooks.collectAsState()
    val allNotes by viewModel.rawNotes.collectAsState()

    var editingNotebook by remember { mutableStateOf<Notebook?>(null) }
    var deletingNotebook by remember { mutableStateOf<Notebook?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }

    var newNotebookName by remember { mutableStateOf("") }
    var selectedColorHex by remember { mutableStateOf("#7C4DFF") }

    val presetColors = listOf(
        "#7C4DFF", "#FF5722", "#00B0FF", "#4CAF50",
        "#E91E63", "#FF9800", "#9C27B0", "#009688", "#3F51B5"
    )

    val glassBorder = androidx.compose.foundation.BorderStroke(
        width = 1.dp,
        brush = androidx.compose.ui.graphics.Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.55f),
                Color.White.copy(alpha = 0.2f),
                MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
            )
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manage Notebooks & Folders") },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("manage_notebooks_back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                            showCreateDialog = true
                        },
                        modifier = Modifier.testTag("add_notebook_header_button")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Notebook")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                    showCreateDialog = true
                },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("New Notebook") },
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("create_notebook_fab")
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
                shape = RoundedCornerShape(16.dp),
                border = glassBorder,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Reorder, rename, or delete notebooks. Deleting a notebook allows you to safely relocate its notes.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (notebooks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No notebooks found. Tap + to add one.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(notebooks, key = { _, item -> item.id }) { index, notebook ->
                        val noteCount = allNotes.count { it.notebookId == notebook.id }
                        val themeColor = runCatching { Color(android.graphics.Color.parseColor(notebook.colorHex)) }
                            .getOrDefault(MaterialTheme.colorScheme.primary)

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("notebook_item_${notebook.id}"),
                            shape = RoundedCornerShape(16.dp),
                            border = glassBorder,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.4f)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Reorder controls (Up & Down buttons)
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    IconButton(
                                        onClick = { viewModel.moveNotebookUp(notebook, notebooks) },
                                        enabled = index > 0,
                                        modifier = Modifier.size(32.dp).testTag("move_up_notebook_${notebook.id}")
                                    ) {
                                        Icon(
                                            Icons.Default.KeyboardArrowUp,
                                            contentDescription = "Move Up",
                                            tint = if (index > 0) MaterialTheme.colorScheme.onSurface else Color.LightGray
                                        )
                                    }
                                    Text(
                                        text = "#${index + 1}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Gray
                                    )
                                    IconButton(
                                        onClick = { viewModel.moveNotebookDown(notebook, notebooks) },
                                        enabled = index < notebooks.size - 1,
                                        modifier = Modifier.size(32.dp).testTag("move_down_notebook_${notebook.id}")
                                    ) {
                                        Icon(
                                            Icons.Default.KeyboardArrowDown,
                                            contentDescription = "Move Down",
                                            tint = if (index < notebooks.size - 1) MaterialTheme.colorScheme.onSurface else Color.LightGray
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // Folder Icon
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(themeColor.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Folder,
                                        contentDescription = null,
                                        tint = themeColor,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                // Name & Count
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = notebook.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "$noteCount note(s)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )
                                }

                                // Action Buttons: Edit & Delete
                                IconButton(
                                    onClick = { editingNotebook = notebook },
                                    modifier = Modifier.testTag("edit_notebook_${notebook.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Rename",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }

                                IconButton(
                                    onClick = { deletingNotebook = notebook },
                                    modifier = Modifier.testTag("delete_notebook_${notebook.id}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // CREATE NOTEBOOK DIALOG
    if (showCreateDialog) {
        var createName by remember { mutableStateOf("") }
        var createColorHex by remember { mutableStateOf("#7C4DFF") }

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Create Notebook") },
            text = {
                Column {
                    OutlinedTextField(
                        value = createName,
                        onValueChange = { createName = it },
                        label = { Text("Notebook Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("create_notebook_name_input")
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Theme Color", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        presetColors.take(5).forEach { hex ->
                            val color = runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(Color.Magenta)
                            val isSelected = createColorHex.equals(hex, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (isSelected) 3.dp else 0.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { createColorHex = hex }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (createName.isNotBlank()) {
                            viewModel.createNotebook(createName, createColorHex)
                            showCreateDialog = false
                        }
                    },
                    modifier = Modifier.testTag("confirm_create_notebook_button")
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

    // RENAME / EDIT NOTEBOOK DIALOG
    editingNotebook?.let { notebook ->
        var editName by remember { mutableStateOf(notebook.name) }
        var editColorHex by remember { mutableStateOf(notebook.colorHex) }

        AlertDialog(
            onDismissRequest = { editingNotebook = null },
            title = { Text("Edit Notebook") },
            text = {
                Column {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Notebook Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("edit_notebook_name_input")
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Theme Color", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        presetColors.take(5).forEach { hex ->
                            val color = runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(Color.Magenta)
                            val isSelected = editColorHex.equals(hex, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (isSelected) 3.dp else 0.dp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { editColorHex = hex }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editName.isNotBlank()) {
                            viewModel.renameNotebook(notebook, editName, editColorHex)
                            editingNotebook = null
                        }
                    },
                    modifier = Modifier.testTag("save_edited_notebook_button")
                ) {
                    Text("Save Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingNotebook = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // DELETE NOTEBOOK & HANDLE NOTES DIALOG
    deletingNotebook?.let { notebook ->
        val noteCount = allNotes.count { it.notebookId == notebook.id }
        var selectedAction by remember { mutableStateOf(DeleteNotebookAction.MOVE_TO_UNCATEGORIZED) }
        
        val otherNotebooks = notebooks.filter { it.id != notebook.id }
        var selectedTargetNotebook by remember { mutableStateOf(otherNotebooks.firstOrNull()) }

        AlertDialog(
            onDismissRequest = { deletingNotebook = null },
            title = { Text("Delete '${notebook.name}'?") },
            text = {
                Column {
                    if (noteCount > 0) {
                        Text(
                            text = "This notebook contains $noteCount note(s). Where would you like to move them?",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        // Option 1: Move to Uncategorized
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedAction = DeleteNotebookAction.MOVE_TO_UNCATEGORIZED }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = selectedAction == DeleteNotebookAction.MOVE_TO_UNCATEGORIZED,
                                onClick = { selectedAction = DeleteNotebookAction.MOVE_TO_UNCATEGORIZED }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Move to 'Uncategorized'", style = MaterialTheme.typography.bodyMedium)
                        }

                        // Option 2: Move to another folder
                        if (otherNotebooks.isNotEmpty()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedAction = DeleteNotebookAction.MOVE_TO_OTHER_NOTEBOOK }
                                    .padding(vertical = 4.dp)
                            ) {
                                RadioButton(
                                    selected = selectedAction == DeleteNotebookAction.MOVE_TO_OTHER_NOTEBOOK,
                                    onClick = { selectedAction = DeleteNotebookAction.MOVE_TO_OTHER_NOTEBOOK }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Move to another notebook:", style = MaterialTheme.typography.bodyMedium)
                            }

                            if (selectedAction == DeleteNotebookAction.MOVE_TO_OTHER_NOTEBOOK) {
                                Column(modifier = Modifier.padding(start = 32.dp, top = 4.dp)) {
                                    otherNotebooks.forEach { target ->
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { selectedTargetNotebook = target }
                                                .padding(vertical = 4.dp)
                                        ) {
                                            RadioButton(
                                                selected = selectedTargetNotebook?.id == target.id,
                                                onClick = { selectedTargetNotebook = target }
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(target.name, style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                }
                            }
                        }

                        // Option 3: Move to Trash
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedAction = DeleteNotebookAction.MOVE_TO_TRASH }
                                .padding(vertical = 4.dp)
                        ) {
                            RadioButton(
                                selected = selectedAction == DeleteNotebookAction.MOVE_TO_TRASH,
                                onClick = { selectedAction = DeleteNotebookAction.MOVE_TO_TRASH }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Move all $noteCount notes to Trash", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                        }
                    } else {
                        Text("Are you sure you want to delete this empty notebook?")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val targetId = if (selectedAction == DeleteNotebookAction.MOVE_TO_OTHER_NOTEBOOK) selectedTargetNotebook?.id else null
                        val targetName = if (selectedAction == DeleteNotebookAction.MOVE_TO_OTHER_NOTEBOOK) (selectedTargetNotebook?.name ?: "Uncategorized") else "Uncategorized"

                        viewModel.deleteNotebookWithAction(
                            notebookId = notebook.id,
                            action = selectedAction,
                            targetNotebookId = targetId,
                            targetNotebookName = targetName
                        )
                        deletingNotebook = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_delete_notebook_button")
                ) {
                    Text("Delete Notebook")
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingNotebook = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
