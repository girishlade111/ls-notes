package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.model.Note
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ExportDialog(
    note: Note?,
    onExportConfirmed: (format: String, filename: String) -> Unit,
    onDismiss: () -> Unit
) {
    val formats = listOf("PDF Document (.pdf)", "Plain Text (.txt)", "Markdown (.md)", "HTML Web Page (.html)", "JSON Data (.json)", "ZIP Package (.zip)")
    var selectedFormat by remember { mutableStateOf(formats[0]) }

    val dateStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }
    val defaultTitle = note?.title?.replace("[^a-zA-Z0-9]".toRegex(), "_") ?: "LS_Notes_Collection"
    var exportFilename by remember { mutableStateOf("LS_Notes_${defaultTitle}_$dateStr") }

    var includeAttachments by remember { mutableStateOf(true) }
    var includeMetadata by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.FileUpload, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text("Export Note") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Export format:", style = MaterialTheme.typography.labelMedium)
                var showDropdown by remember { mutableStateOf(false) }
                Box {
                    OutlinedButton(
                        onClick = { showDropdown = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(selectedFormat)
                    }
                    DropdownMenu(expanded = showDropdown, onDismissRequest = { showDropdown = false }) {
                        formats.forEach { fmt ->
                            DropdownMenuItem(
                                text = { Text(fmt) },
                                onClick = { selectedFormat = fmt; showDropdown = false }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = exportFilename,
                    onValueChange = { exportFilename = it },
                    label = { Text("Export Filename") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Checkbox(checked = includeAttachments, onCheckedChange = { includeAttachments = it })
                    Text("Include file attachments", style = MaterialTheme.typography.bodyMedium)
                }

                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Checkbox(checked = includeMetadata, onCheckedChange = { includeMetadata = it })
                    Text("Include tags, dates & folder info", style = MaterialTheme.typography.bodyMedium)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val ext = when {
                        selectedFormat.contains("PDF") -> ".pdf"
                        selectedFormat.contains("Markdown") -> ".md"
                        selectedFormat.contains("HTML") -> ".html"
                        selectedFormat.contains("JSON") -> ".json"
                        selectedFormat.contains("ZIP") -> ".zip"
                        else -> ".txt"
                    }
                    onExportConfirmed(selectedFormat, "$exportFilename$ext")
                    onDismiss()
                }
            ) {
                Text("Export File")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
