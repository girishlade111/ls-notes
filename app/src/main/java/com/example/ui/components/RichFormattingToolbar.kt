package com.example.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.EditorMode
import com.example.ui.theme.AvailableFonts
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun RichFormattingToolbar(
    editorMode: EditorMode,
    currentFont: String,
    currentFontSizeSp: Float,
    onFormatAction: (String) -> Unit,
    onFontChange: (String) -> Unit,
    onFontSizeChange: (Float) -> Unit,
    onAddAttachment: () -> Unit
) {
    var showFontDialog by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 4.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 2.dp)) {
            // Main Formatting Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Attachments button
                IconButton(onClick = onAddAttachment) {
                    Icon(Icons.Default.AddCircleOutline, contentDescription = "Add Content / Attachment", tint = MaterialTheme.colorScheme.primary)
                }

                VerticalDivider(modifier = Modifier.height(24.dp))

                IconButton(onClick = { onFormatAction("**") }) {
                    Text("B", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }

                IconButton(onClick = { onFormatAction("*") }) {
                    Text("I", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                }

                IconButton(onClick = { onFormatAction("<u>") }) {
                    Text("U", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                }

                IconButton(onClick = { onFormatAction("- ") }) {
                    Icon(Icons.Default.List, contentDescription = "Bullet List")
                }

                IconButton(onClick = { onFormatAction("1. ") }) {
                    Icon(Icons.Default.FormatListNumbered, contentDescription = "Numbered List")
                }

                IconButton(onClick = { onFormatAction("- [ ] ") }) {
                    Icon(Icons.Default.CheckBox, contentDescription = "Checklist Item")
                }

                IconButton(onClick = { showFontDialog = true }) {
                    Icon(Icons.Default.TextFields, contentDescription = "Font Settings")
                }

                if (editorMode == EditorMode.ADVANCED) {
                    VerticalDivider(modifier = Modifier.height(24.dp))

                    IconButton(onClick = { onFormatAction("# ") }) {
                        Text("H1", fontWeight = FontWeight.Bold)
                    }

                    IconButton(onClick = { onFormatAction("## ") }) {
                        Text("H2", fontWeight = FontWeight.Bold)
                    }

                    IconButton(onClick = { onFormatAction("> ") }) {
                        Icon(Icons.Default.FormatQuote, contentDescription = "Quote")
                    }

                    IconButton(onClick = { onFormatAction("```\n\n```") }) {
                        Icon(Icons.Default.Code, contentDescription = "Code Block")
                    }

                    IconButton(onClick = { onFormatAction("\n| Col 1 | Col 2 |\n| --- | --- |\n| Cell 1 | Cell 2 |\n") }) {
                        Icon(Icons.Default.TableChart, contentDescription = "Insert Table")
                    }

                    IconButton(onClick = {
                        val dateStr = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())
                        onFormatAction(dateStr)
                    }) {
                        Icon(Icons.Default.AccessTime, contentDescription = "Insert Date & Time")
                    }

                    IconButton(onClick = { onFormatAction("\n---\n") }) {
                        Icon(Icons.Default.HorizontalRule, contentDescription = "Horizontal Rule")
                    }
                }
            }
        }
    }

    if (showFontDialog) {
        AlertDialog(
            onDismissRequest = { showFontDialog = false },
            title = { Text("Editor Font Settings") },
            text = {
                Column {
                    Text("Font Family", style = MaterialTheme.typography.labelLarge)
                    Spacer(modifier = Modifier.height(6.dp))
                    AvailableFonts.forEach { font ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = font.equals(currentFont, ignoreCase = true),
                                onClick = { onFontChange(font) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(font)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Font Size: ${currentFontSizeSp.toInt()} sp", style = MaterialTheme.typography.labelLarge)
                    Slider(
                        value = currentFontSizeSp,
                        onValueChange = onFontSizeChange,
                        valueRange = 12f..28f,
                        steps = 8
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showFontDialog = false }) {
                    Text("Done")
                }
            }
        )
    }
}
