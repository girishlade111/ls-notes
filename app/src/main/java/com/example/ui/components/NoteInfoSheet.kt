package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Note
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteInfoSheet(
    note: Note,
    onDismiss: () -> Unit
) {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    val wordCount = note.content.split("\\s+".toRegex()).filter { it.isNotBlank() }.size
    val charCount = note.content.length

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Note Information", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            InfoRow("Title", note.title.ifBlank { "Untitled Note" })
            InfoRow("Notebook / Folder", note.notebookName)
            InfoRow("Note Type", note.type.name)
            InfoRow("Tags", note.tagsCsv.ifBlank { "None" })
            InfoRow("Date Created", sdf.format(Date(note.createdTimestamp)))
            InfoRow("Last Modified", sdf.format(Date(note.updatedTimestamp)))
            InfoRow("Word Count", "$wordCount words")
            InfoRow("Character Count", "$charCount characters")
            InfoRow("Privacy Status", if (note.isPrivate) "🔒 PrivateSafe Protected" else "Standard Local Note")
            InfoRow("Storage Location", "Internal App Storage (Offline Local)")

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Close")
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}
