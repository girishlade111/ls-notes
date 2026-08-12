package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalHapticFeedback
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
    val haptic = LocalHapticFeedback.current
    val sdf = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm:ss a", Locale.getDefault())
    
    val wordCount = if (note.content.isBlank()) 0 else note.content.trim().split("\\s+".toRegex()).count { it.isNotBlank() }
    val charCount = note.content.length

    val glassBorder = BorderStroke(
        width = 1.dp,
        brush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.55f),
                Color.White.copy(alpha = 0.2f),
                MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
            )
        )
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Header Title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Column {
                    Text(
                        text = "Note Metadata & Info",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = note.title.ifBlank { "Untitled Note" },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    MetadataTile(
                        icon = Icons.Outlined.Folder,
                        label = "Notebook / Folder",
                        value = note.notebookName.ifBlank { "Uncategorized" },
                        glassBorder = glassBorder
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MetadataTile(
                            icon = Icons.Outlined.TextFields,
                            label = "Word Count",
                            value = "$wordCount words",
                            modifier = Modifier.weight(1f),
                            glassBorder = glassBorder
                        )
                        MetadataTile(
                            icon = Icons.Outlined.Abc,
                            label = "Character Count",
                            value = "$charCount chars",
                            modifier = Modifier.weight(1f),
                            glassBorder = glassBorder
                        )
                    }
                }

                item {
                    MetadataTile(
                        icon = Icons.Outlined.Event,
                        label = "Created Date",
                        value = sdf.format(Date(note.createdTimestamp)),
                        glassBorder = glassBorder
                    )
                }

                item {
                    MetadataTile(
                        icon = Icons.Outlined.Update,
                        label = "Last Modified Date",
                        value = sdf.format(Date(note.updatedTimestamp)),
                        glassBorder = glassBorder
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MetadataTile(
                            icon = Icons.Outlined.Category,
                            label = "Note Type",
                            value = note.type.name,
                            modifier = Modifier.weight(1f),
                            glassBorder = glassBorder
                        )
                        MetadataTile(
                            icon = Icons.Outlined.Security,
                            label = "Privacy",
                            value = if (note.isPrivate) "PrivateSafe 🔒" else "Public",
                            modifier = Modifier.weight(1f),
                            glassBorder = glassBorder
                        )
                    }
                }

                if (note.tagsCsv.isNotBlank()) {
                    item {
                        MetadataTile(
                            icon = Icons.Outlined.Tag,
                            label = "Tags",
                            value = note.tagsCsv.split(",").joinToString(" ") { "#${it.trim()}" },
                            glassBorder = glassBorder
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text("Done")
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun MetadataTile(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    glassBorder: BorderStroke
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
        border = glassBorder
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
