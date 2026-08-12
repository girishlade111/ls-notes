package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

data class DrawnPath(
    val path: Path,
    val color: Color,
    val strokeWidth: Float,
    val isEraser: Boolean = false
)

enum class PaperStyle { BLANK, DOTTED, RULED, GRID }

@Composable
fun DrawingCanvas(
    onSaveDrawing: (String) -> Unit,
    onCancel: () -> Unit
) {
    val paths = remember { mutableStateListOf<DrawnPath>() }
    val undonePaths = remember { mutableStateListOf<DrawnPath>() }

    var currentPath by remember { mutableStateOf<Path?>(null) }
    var selectedColor by remember { mutableStateOf(Color.Black) }
    var strokeWidth by remember { mutableStateOf(8f) }
    var isEraser by remember { mutableStateOf(false) }
    var paperStyle by remember { mutableStateOf(PaperStyle.BLANK) }

    val colorOptions = listOf(
        Color.Black, Color(0xFF7C4DFF), Color(0xFF1E88E5), Color(0xFF43A047),
        Color(0xFFFB8C00), Color(0xFFE53935), Color(0xFF8E24AA)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp)
    ) {
        // Toolbar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onCancel) {
                Icon(Icons.Default.Close, contentDescription = "Cancel")
            }

            Text("Sketch Note", style = MaterialTheme.typography.titleMedium)

            Row {
                IconButton(
                    onClick = {
                        if (paths.isNotEmpty()) {
                            undonePaths.add(paths.removeAt(paths.size - 1))
                        }
                    },
                    enabled = paths.isNotEmpty()
                ) {
                    Icon(Icons.Default.Undo, contentDescription = "Undo")
                }

                IconButton(
                    onClick = {
                        if (undonePaths.isNotEmpty()) {
                            paths.add(undonePaths.removeAt(undonePaths.size - 1))
                        }
                    },
                    enabled = undonePaths.isNotEmpty()
                ) {
                    Icon(Icons.Default.Redo, contentDescription = "Redo")
                }

                IconButton(onClick = { paths.clear(); undonePaths.clear() }) {
                    Icon(Icons.Default.Delete, contentDescription = "Clear Canvas")
                }

                Button(
                    onClick = { onSaveDrawing("[Sketch Note Canvas]") },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Save Sketch")
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Tool Options Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            FilterChip(
                selected = !isEraser,
                onClick = { isEraser = false },
                label = { Text("Pen") },
                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
            )
            FilterChip(
                selected = isEraser,
                onClick = { isEraser = true },
                label = { Text("Eraser") },
                leadingIcon = { Icon(Icons.Default.AutoFixHigh, contentDescription = null) }
            )

            // Paper Style Dropdown
            var showPaperMenu by remember { mutableStateOf(false) }
            Box {
                OutlinedButton(onClick = { showPaperMenu = true }) {
                    Text("Paper: ${paperStyle.name.lowercase().replaceFirstChar { it.uppercase() }}")
                }
                DropdownMenu(expanded = showPaperMenu, onDismissRequest = { showPaperMenu = false }) {
                    PaperStyle.values().forEach { style ->
                        DropdownMenuItem(
                            text = { Text(style.name.lowercase().replaceFirstChar { it.uppercase() }) },
                            onClick = { paperStyle = style; showPaperMenu = false }
                        )
                    }
                }
            }
        }

        // Color Palette Row
        if (!isEraser) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                colorOptions.forEach { color ->
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(color)
                            .clickable { selectedColor = color }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Drawing Canvas Box
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(12.dp),
            color = Color.White,
            tonalElevation = 2.dp,
            shadowElevation = 4.dp
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(isEraser, selectedColor, strokeWidth) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                currentPath = Path().apply { moveTo(offset.x, offset.y) }
                                undonePaths.clear()
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                currentPath?.let { p ->
                                    val newOffset = change.position
                                    p.lineTo(newOffset.x, newOffset.y)
                                    // Trigger recomposition
                                    val temp = currentPath
                                    currentPath = null
                                    currentPath = temp
                                }
                            },
                            onDragEnd = {
                                currentPath?.let { p ->
                                    paths.add(
                                        DrawnPath(
                                            path = p,
                                            color = if (isEraser) Color.White else selectedColor,
                                            strokeWidth = if (isEraser) strokeWidth * 2 else strokeWidth,
                                            isEraser = isEraser
                                        )
                                    )
                                }
                                currentPath = null
                            }
                        )
                    }
            ) {
                // Draw Paper Background Pattern
                when (paperStyle) {
                    PaperStyle.RULED -> {
                        var y = 60f
                        while (y < size.height) {
                            drawLine(Color.LightGray.copy(alpha = 0.5f), Offset(0f, y), Offset(size.width, y), strokeWidth = 2f)
                            y += 60f
                        }
                    }
                    PaperStyle.GRID -> {
                        var x = 60f
                        while (x < size.width) {
                            drawLine(Color.LightGray.copy(alpha = 0.3f), Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
                            x += 60f
                        }
                        var y = 60f
                        while (y < size.height) {
                            drawLine(Color.LightGray.copy(alpha = 0.3f), Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
                            y += 60f
                        }
                    }
                    PaperStyle.DOTTED -> {
                        var x = 40f
                        while (x < size.width) {
                            var y = 40f
                            while (y < size.height) {
                                drawCircle(Color.LightGray.copy(alpha = 0.6f), radius = 3f, center = Offset(x, y))
                                y += 40f
                            }
                            x += 40f
                        }
                    }
                    PaperStyle.BLANK -> {}
                }

                // Draw existing paths
                paths.forEach { drawnPath ->
                    drawPath(
                        path = drawnPath.path,
                        color = drawnPath.color,
                        style = Stroke(
                            width = drawnPath.strokeWidth,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }

                // Draw current active path
                currentPath?.let { p ->
                    drawPath(
                        path = p,
                        color = if (isEraser) Color.White else selectedColor,
                        style = Stroke(
                            width = if (isEraser) strokeWidth * 2 else strokeWidth,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }
        }
    }
}
