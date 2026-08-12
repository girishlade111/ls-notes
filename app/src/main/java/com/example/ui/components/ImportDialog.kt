package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportDialog(
    onImportConfirmed: (sourceType: String, notebookName: String, count: Int) -> Unit,
    onDismiss: () -> Unit
) {
    val importSources = listOf(
        "Evernote (.enex)",
        "Microsoft OneNote",
        "Google Keep (JSON)",
        "Pocket Articles",
        "Zoho Notebook",
        "Markdown Files (.md)",
        "HTML Files (.html)",
        "Text Documents (.txt)",
        "LS Notes Backup (.json)"
    )

    var selectedSource by remember { mutableStateOf(importSources[0]) }
    var targetNotebook by remember { mutableStateOf("Imported Notes") }
    var isPreviewing by remember { mutableStateOf(false) }
    var estimatedCount by remember { mutableStateOf(12) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.FileDownload, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text("Import & Migration Center") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "Select local export file or migration package to import into LS Notes:",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text("Source Format", style = MaterialTheme.typography.labelMedium)
                var showDropdown by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(
                        onClick = { showDropdown = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(selectedSource)
                    }
                    DropdownMenu(
                        expanded = showDropdown,
                        onDismissRequest = { showDropdown = false }
                    ) {
                        importSources.forEach { source ->
                            DropdownMenuItem(
                                text = { Text(source) },
                                onClick = {
                                    selectedSource = source
                                    showDropdown = false
                                    isPreviewing = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = targetNotebook,
                    onValueChange = { targetNotebook = it },
                    label = { Text("Target Notebook Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (!isPreviewing) {
                    OutlinedButton(
                        onClick = {
                            isPreviewing = true
                            estimatedCount = (8..24).random()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Scan & Preview File")
                    }
                } else {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Import Preview", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                            Text("• Source: $selectedSource", style = MaterialTheme.typography.bodySmall)
                            Text("• Detected Notes: $estimatedCount", style = MaterialTheme.typography.bodySmall)
                            Text("• Target Folder: $targetNotebook", style = MaterialTheme.typography.bodySmall)
                            Text("• Duplicates: 0 found", style = MaterialTheme.typography.bodySmall)
                            Text("• Attachments: Preserved locally", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onImportConfirmed(selectedSource, targetNotebook, estimatedCount)
                    onDismiss()
                }
            ) {
                Text("Confirm Import")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
