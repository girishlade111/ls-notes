package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun NoteCard(
    note: Note,
    isListView: Boolean = false,
    showTimeMode: TimeDisplayMode = TimeDisplayMode.SHOW_BOTH,
    revealPrivateContent: Boolean = false,
    onClick: () -> Unit,
    onPinToggle: () -> Unit,
    onFavoriteToggle: () -> Unit,
    onDuplicate: () -> Unit,
    onTogglePrivate: () -> Unit,
    onDelete: () -> Unit,
    onExport: () -> Unit,
    onShare: () -> Unit,
    onInfo: () -> Unit,
    onChangeColor: (String) -> Unit,
    onViewHistory: (() -> Unit)? = null,
    onToggleArchive: (() -> Unit)? = null
) {
    var showMenu by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }

    val baseColor = remember(note.colorHex) {
        runCatching { Color(android.graphics.Color.parseColor(note.colorHex)) }
            .getOrDefault(Color(0xFFEDE7F6))
    }

    // Glassmorphism card surface colors
    val cardBackground = baseColor.copy(alpha = 0.85f)
    val glassBorder = BorderStroke(
        width = 1.dp,
        brush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.6f),
                Color.White.copy(alpha = 0.2f),
                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
            )
        )
    )

    // Checklist progress calculation
    val checklistStats = remember(note.checklistJson) {
        runCatching {
            val jsonArray = JSONArray(note.checklistJson)
            val total = jsonArray.length()
            var checked = 0
            for (i in 0 until total) {
                if (jsonArray.getJSONObject(i).optBoolean("isChecked", false)) checked++
            }
            Pair(checked, total)
        }.getOrDefault(Pair(0, 0))
    }

    // Attachments count
    val attachmentsCount = remember(note.attachmentsJson) {
        runCatching { JSONArray(note.attachmentsJson).length() }.getOrDefault(0)
    }

    // Uniform size container for strict grid/list formatting
    val cardModifier = if (isListView) {
        Modifier
            .fillMaxWidth()
            .heightIn(min = 130.dp, max = 150.dp)
            .padding(horizontal = 6.dp, vertical = 5.dp)
    } else {
        Modifier
            .fillMaxWidth()
            .height(210.dp)
            .padding(6.dp)
    }

    Surface(
        modifier = cardModifier.clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        color = cardBackground,
        border = glassBorder,
        shadowElevation = 3.dp,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Row: Notebook name, Lock/Favorite, Pin, Menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = note.notebookName.ifBlank { "Uncategorized" },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (note.isPrivate) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Private Note",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    IconButton(
                        onClick = onPinToggle,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = if (note.isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                            contentDescription = "Pin Note",
                            tint = if (note.isPinned) MaterialTheme.colorScheme.primary else Color.Gray,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More actions",
                                modifier = Modifier.size(16.dp),
                                tint = Color.DarkGray
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Open & Edit") },
                                onClick = { showMenu = false; onClick() },
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text(if (note.isPinned) "Unpin Note" else "Pin Note") },
                                onClick = { showMenu = false; onPinToggle() },
                                leadingIcon = { Icon(Icons.Default.PushPin, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Change Note Color") },
                                onClick = { showMenu = false; showColorPicker = true },
                                leadingIcon = { Icon(Icons.Default.Palette, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Duplicate") },
                                onClick = { showMenu = false; onDuplicate() },
                                leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text(if (note.isPrivate) "Remove from PrivateSafe" else "Move to PrivateSafe") },
                                onClick = { showMenu = false; onTogglePrivate() },
                                leadingIcon = { Icon(Icons.Default.Security, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Export Note") },
                                onClick = { showMenu = false; onExport() },
                                leadingIcon = { Icon(Icons.Default.FileDownload, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Share") },
                                onClick = { showMenu = false; onShare() },
                                leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Note Info") },
                                onClick = { showMenu = false; onInfo() },
                                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
                            )
                            onViewHistory?.let { action ->
                                DropdownMenuItem(
                                    text = { Text("Version History") },
                                    onClick = { showMenu = false; action() },
                                    leadingIcon = { Icon(Icons.Default.History, contentDescription = null) }
                                )
                            }
                            onToggleArchive?.let { action ->
                                DropdownMenuItem(
                                    text = { Text(if (note.isArchived) "Unarchive Note" else "Archive Note") },
                                    onClick = { showMenu = false; action() },
                                    leadingIcon = { Icon(Icons.Default.Archive, contentDescription = null) }
                                )
                            }
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Move to Trash", color = MaterialTheme.colorScheme.error) },
                                onClick = { showMenu = false; onDelete() },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Title - Strict 1 Line in Grid / 1 Line in List
            Text(
                text = note.title.ifBlank { "Untitled Note" },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = Color.Black.copy(alpha = 0.88f)
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Content Preview - Weighted so all cards align perfectly
            val displayContent = if (note.isPrivate && !revealPrivateContent) {
                "🔒 Content protected inside PrivateSafe"
            } else {
                note.content.ifBlank { "No content" }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.Top
            ) {
                Text(
                    text = displayContent,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = if (isListView) 2 else 3,
                    overflow = TextOverflow.Ellipsis,
                    color = Color.Black.copy(alpha = 0.68f),
                    lineHeight = 18.sp
                )

                // Checklist indicator if available
                if (checklistStats.second > 0) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckBox,
                            contentDescription = "Checklist progress",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${checklistStats.first}/${checklistStats.second} done",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Tags Row if available
            if (note.tagsCsv.isNotBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    note.tagsCsv.split(",").take(2).forEach { tag ->
                        val trimmed = tag.trim()
                        if (trimmed.isNotBlank()) {
                            Surface(
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text(
                                    text = "#$trimmed",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Footer Row: Uniform time & attachments alignment
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (showTimeMode != TimeDisplayMode.HIDE) {
                    val sdf = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }
                    val timeStr = when (showTimeMode) {
                        TimeDisplayMode.SHOW_CREATED -> sdf.format(Date(note.createdTimestamp))
                        TimeDisplayMode.SHOW_EDITED, TimeDisplayMode.SHOW_BOTH, TimeDisplayMode.HIDE -> sdf.format(Date(note.updatedTimestamp))
                    }
                    Text(
                        text = timeStr,
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Black.copy(alpha = 0.5f)
                    )
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                if (attachmentsCount > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AttachFile,
                            contentDescription = "Attachments",
                            modifier = Modifier.size(12.dp),
                            tint = Color.Gray
                        )
                        Text(
                            text = "$attachmentsCount",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }

    // Note Color Picker Dialog
    if (showColorPicker) {
        AlertDialog(
            onDismissRequest = { showColorPicker = false },
            title = { Text("Choose Note Color") },
            text = {
                Column {
                    val colorList = listOf(
                        "#FFFFFF", "#EDE7F6", "#E3F2FD", "#E8F5E9",
                        "#FFF3E0", "#FCE4EC", "#FFFDE7", "#F3E5F5", "#E0F2F1"
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        colorList.take(5).forEach { colorHex ->
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(android.graphics.Color.parseColor(colorHex)))
                                    .clickable {
                                        onChangeColor(colorHex)
                                        showColorPicker = false
                                    }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        colorList.drop(5).forEach { colorHex ->
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(android.graphics.Color.parseColor(colorHex)))
                                    .clickable {
                                        onChangeColor(colorHex)
                                        showColorPicker = false
                                    }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showColorPicker = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
