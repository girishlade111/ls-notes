package com.example.ui.components

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import android.provider.OpenableColumns
import com.example.data.model.Note
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportBottomSheet(
    note: Note? = null,
    allNotes: List<Note> = emptyList(),
    onDismiss: () -> Unit,
    onExportSuccess: (String) -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Export format: "TXT", "PDF", or "JSON"
    var exportFormat by remember { mutableStateOf("TXT") }
    
    // Scope option: 0 -> Single Note, 1 -> Selected Notes
    var exportScope by remember { mutableStateOf(if (note != null) 0 else 1) }
    
    // Multi-selection state if exporting multiple
    val selectedNoteIds = remember {
        mutableStateListOf<Long>().apply {
            if (note != null) add(note.id) else addAll(allNotes.map { it.id })
        }
    }

    var includeMetadata by remember { mutableStateOf(true) }
    var includeHeader by remember { mutableStateOf(true) }

    val activeNotesToExport = remember(exportScope, note, selectedNoteIds.toList(), allNotes) {
        if (exportScope == 0 && note != null) {
            listOf(note)
        } else {
            allNotes.filter { it.id in selectedNoteIds }
        }
    }

    val defaultTitle = when {
        exportScope == 0 && note != null -> note.title.ifBlank { "Untitled_Note" }.replace("[^a-zA-Z0-9]".toRegex(), "_")
        else -> "Notes_Export_Collection"
    }
    
    val dateFormatter = remember { SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()) }
    val suggestedFileName = remember(defaultTitle, exportFormat) {
        val ext = when (exportFormat) {
            "PDF" -> ".pdf"
            "JSON" -> ".json"
            else -> ".txt"
        }
        "${defaultTitle}_${dateFormatter.format(Date())}$ext"
    }

    // Document file creation launcher using Storage Access Framework (ActivityResultContracts.CreateDocument)
    val mimeType = when (exportFormat) {
        "PDF" -> "application/pdf"
        "JSON" -> "application/json"
        else -> "text/plain"
    }
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(mimeType)
    ) { uri: Uri? ->
        if (uri != null) {
            val success = exportToUri(
                context = context,
                uri = uri,
                notes = activeNotesToExport,
                exportFormat = exportFormat,
                includeHeader = includeHeader,
                includeMetadata = includeMetadata
            )
            if (success) {
                var fileName = suggestedFileName
                try {
                    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                            if (nameIndex != -1) {
                                fileName = cursor.getString(nameIndex) ?: suggestedFileName
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                onExportSuccess(fileName)
                onDismiss()
            } else {
                Toast.makeText(context, "Failed to write file export.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = Modifier.testTag("export_bottom_sheet")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.FileUpload,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Export File Options",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Format Selection
            Text("Select File Format", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                FilterChip(
                    selected = exportFormat == "TXT",
                    onClick = { exportFormat = "TXT" },
                    label = { Text("Plain Text (.txt)") },
                    leadingIcon = { Icon(Icons.Default.Description, contentDescription = null) },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = exportFormat == "PDF",
                    onClick = { exportFormat = "PDF" },
                    label = { Text("PDF Document (.pdf)") },
                    leadingIcon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Scope Selection
            if (note != null && allNotes.size > 1) {
                Text("Export Selection", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FilterChip(
                        selected = exportScope == 0,
                        onClick = { exportScope = 0 },
                        label = { Text("Current Note") }
                    )
                    FilterChip(
                        selected = exportScope == 1,
                        onClick = { exportScope = 1 },
                        label = { Text("Multiple Notes (${activeNotesToExport.size})") }
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Note List if Multi-Select Scope
            if (exportScope == 1 && allNotes.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 140.dp)
                ) {
                    LazyColumn(modifier = Modifier.padding(8.dp)) {
                        items(allNotes, key = { it.id }) { item ->
                            val isChecked = item.id in selectedNoteIds
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (isChecked) selectedNoteIds.remove(item.id)
                                        else selectedNoteIds.add(item.id)
                                    }
                                    .padding(vertical = 4.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = isChecked,
                                    onCheckedChange = { checked ->
                                        if (checked) selectedNoteIds.add(item.id)
                                        else selectedNoteIds.remove(item.id)
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = item.title.ifBlank { "Untitled Note" },
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Custom Options
            Text("Content Options", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { includeHeader = !includeHeader },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(checked = includeHeader, onCheckedChange = { includeHeader = it })
                Text("Include document header banner", style = MaterialTheme.typography.bodyMedium)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { includeMetadata = !includeMetadata },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(checked = includeMetadata, onCheckedChange = { includeMetadata = it })
                Text("Include note creation date, tags & folder info", style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Real-Time Stats Summary Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val totalWords = activeNotesToExport.sumOf { n ->
                    n.content.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }.size
                }
                val totalChars = activeNotesToExport.sumOf { it.content.length }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Notes", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Text("${activeNotesToExport.size}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Words", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Text("$totalWords", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Characters", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                        Text("$totalChars", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Button
            Button(
                onClick = {
                    if (activeNotesToExport.isEmpty()) {
                        Toast.makeText(context, "Please select at least one note to export.", Toast.LENGTH_SHORT).show()
                    } else {
                        createDocumentLauncher.launch(suggestedFileName)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("confirm_export_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.SaveAlt, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save to Local Device ($exportFormat)")
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

private fun exportToUri(
    context: Context,
    uri: Uri,
    notes: List<Note>,
    isPdf: Boolean,
    includeHeader: Boolean,
    includeMetadata: Boolean
): Boolean {
    return try {
        val outputStream: OutputStream? = context.contentResolver.openOutputStream(uri)
        if (outputStream == null) return false

        if (isPdf) {
            writePdfToStream(outputStream, notes, includeHeader, includeMetadata)
        } else {
            writeTxtToStream(outputStream, notes, includeHeader, includeMetadata)
        }
        true
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
}

private fun writeTxtToStream(
    outputStream: OutputStream,
    notes: List<Note>,
    includeHeader: Boolean,
    includeMetadata: Boolean
) {
    val sb = java.lang.StringBuilder()
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    if (includeHeader) {
        sb.append("=========================================\n")
        sb.append("         LS NOTES EXPORT COLLECTION       \n")
        sb.append(" Export Date: ").append(dateFormat.format(Date())).append("\n")
        sb.append(" Total Notes: ").append(notes.size).append("\n")
        sb.append("=========================================\n\n")
    }

    notes.forEachIndexed { index, note ->
        sb.append("--- NOTE #").append(index + 1).append(" ---\n")
        sb.append("Title: ").append(note.title.ifBlank { "Untitled" }).append("\n")

        if (includeMetadata) {
            sb.append("Created: ").append(dateFormat.format(Date(note.createdTimestamp))).append("\n")
            sb.append("Modified: ").append(dateFormat.format(Date(note.updatedTimestamp))).append("\n")
            if (!note.notebookName.isNull_or_blank()) {
                sb.append("Notebook: ").append(note.notebookName).append("\n")
            }
            if (note.tagsCsv.isNotBlank()) {
                sb.append("Tags: ").append(note.tagsCsv).append("\n")
            }
        }
        sb.append("-----------------------------------------\n")
        sb.append(note.content).append("\n\n")
    }

    outputStream.write(sb.toString().toByteArray(Charsets.UTF_8))
    outputStream.flush()
    outputStream.close()
}

private fun String?.isNull_or_blank(): Boolean {
    return this == null || this.isBlank()
}

private fun writePdfToStream(
    outputStream: OutputStream,
    notes: List<Note>,
    includeHeader: Boolean,
    includeMetadata: Boolean
) {
    val pdfDocument = PdfDocument()
    val pageWidth = 595 // A4 standard width in points
    val pageHeight = 842 // A4 standard height in points
    val margin = 40f
    val contentWidth = pageWidth - (margin * 2)

    var pageNumber = 1
    var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
    var page = pdfDocument.startPage(pageInfo)
    var canvas: Canvas = page.canvas

    val titlePaint = Paint().apply {
        color = android.graphics.Color.BLACK
        textSize = 18f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    val headerPaint = Paint().apply {
        color = android.graphics.Color.DKGRAY
        textSize = 14f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    val metaPaint = Paint().apply {
        color = android.graphics.Color.GRAY
        textSize = 10f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
    }

    val bodyPaint = Paint().apply {
        color = android.graphics.Color.BLACK
        textSize = 12f
        typeface = Typeface.DEFAULT
    }

    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    var currentY = margin + 10f

    fun checkNewPage(neededHeight: Float) {
        if (currentY + neededHeight > pageHeight - margin) {
            pdfDocument.finishPage(page)
            pageNumber++
            pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            page = pdfDocument.startPage(pageInfo)
            canvas = page.canvas
            currentY = margin + 10f
        }
    }

    if (includeHeader) {
        canvas.drawText("LS Notes Collection", margin, currentY, titlePaint)
        currentY += 24f
        canvas.drawText("Exported on ${dateFormat.format(Date())} | Total notes: ${notes.size}", margin, currentY, metaPaint)
        currentY += 20f
        canvas.drawLine(margin, currentY, pageWidth - margin, currentY, Paint().apply { color = android.graphics.Color.LTGRAY; strokeWidth = 1f })
        currentY += 16f
    }

    notes.forEachIndexed { index, note ->
        checkNewPage(60f)

        val noteTitle = "${index + 1}. ${note.title.ifBlank { "Untitled Note" }}"
        canvas.drawText(noteTitle, margin, currentY, headerPaint)
        currentY += 18f

        if (includeMetadata) {
            val metaStr = "Created: ${dateFormat.format(Date(note.createdTimestamp))}" +
                    (if (note.notebookName.isNull_or_blank()) "" else " | Notebook: ${note.notebookName}") +
                    (if (note.tagsCsv.isBlank()) "" else " | Tags: ${note.tagsCsv}")
            canvas.drawText(metaStr, margin, currentY, metaPaint)
            currentY += 16f
        }

        val lines = note.content.split("\n")
        for (line in lines) {
            if (line.isBlank()) {
                currentY += 12f
                checkNewPage(14f)
                continue
            }
            val words = line.split(" ")
            var currentLineText = ""
            for (word in words) {
                val testText = if (currentLineText.isEmpty()) word else "$currentLineText $word"
                if (bodyPaint.measureText(testText) > contentWidth) {
                    checkNewPage(16f)
                    canvas.drawText(currentLineText, margin, currentY, bodyPaint)
                    currentY += 16f
                    currentLineText = word
                } else {
                    currentLineText = testText
                }
            }
            if (currentLineText.isNotEmpty()) {
                checkNewPage(16f)
                canvas.drawText(currentLineText, margin, currentY, bodyPaint)
                currentY += 16f
            }
        }

        currentY += 16f
        checkNewPage(20f)
        canvas.drawLine(margin, currentY, pageWidth - margin, currentY, Paint().apply { color = android.graphics.Color.LTGRAY; strokeWidth = 0.8f })
        currentY += 16f
    }

    pdfDocument.finishPage(page)
    pdfDocument.writeTo(outputStream)
    pdfDocument.close()
    outputStream.flush()
    outputStream.close()
}
