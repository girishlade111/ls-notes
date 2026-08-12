package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import com.example.ui.components.DrawingCanvas
import com.example.ui.components.NoteHistoryDialog
import com.example.ui.components.PrivateSafeAuthDialog
import com.example.ui.components.RichFormattingToolbar
import com.example.ui.theme.getFontFamilyByName
import com.example.ui.viewmodel.NotesViewModel
import com.example.util.BiometricAuthManager
import org.json.JSONArray
import org.json.JSONObject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    noteId: Long,
    initialType: NoteType = NoteType.TEXT,
    viewModel: NotesViewModel,
    onBack: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()
    val notebooks by viewModel.allNotebooks.collectAsState()
    val tags by viewModel.allTags.collectAsState()

    var loadedNote by remember { mutableStateOf<Note?>(null) }

    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var selectedNotebookId by remember { mutableStateOf<Long?>(null) }
    var selectedNotebookName by remember { mutableStateOf("Uncategorized") }
    var selectedTagsCsv by remember { mutableStateOf("") }
    var noteColorHex by remember { mutableStateOf("#FFFFFF") }
    var isPinned by remember { mutableStateOf(false) }
    var isPrivate by remember { mutableStateOf(false) }
    var editorMode by remember { mutableStateOf(settings.defaultEditorMode) }
    var noteFont by remember { mutableStateOf(settings.editorFontFamily) }
    var noteFontSize by remember { mutableStateOf(settings.editorFontSizeSp) }

    // Checklists state
    val checklistItems = remember { mutableStateListOf<ChecklistItem>() }

    // Attachments state
    val attachmentsList = remember { mutableStateListOf<NoteAttachment>() }

    // Sketch Canvas state
    var showSketchCanvas by remember { mutableStateOf(false) }

    // History Screen & Dialog state
    var showHistoryScreen by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }

    val isPrivateSafeUnlocked by viewModel.isPrivateSafeUnlocked.collectAsState()
    var showAuthDialogForPrivateNote by remember { mutableStateOf(false) }

    // Load Note Data
    LaunchedEffect(noteId) {
        if (noteId > 0) {
            val note = viewModel.repository.getNoteById(noteId)
            note?.let { n ->
                if (n.isPrivate && !isPrivateSafeUnlocked) {
                    showAuthDialogForPrivateNote = true
                }
                loadedNote = n
                title = n.title
                content = n.content
                selectedNotebookId = n.notebookId
                selectedNotebookName = n.notebookName
                selectedTagsCsv = n.tagsCsv
                noteColorHex = n.colorHex
                isPinned = n.isPinned
                isPrivate = n.isPrivate
                noteFont = n.fontName ?: settings.editorFontFamily
                noteFontSize = n.fontSizeSp ?: settings.editorFontSizeSp

                // Parse Checklist JSON
                runCatching {
                    val arr = JSONArray(n.checklistJson)
                    checklistItems.clear()
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        checklistItems.add(
                            ChecklistItem(
                                id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                                text = obj.optString("text", ""),
                                isChecked = obj.optBoolean("isChecked", false),
                                indentLevel = obj.optInt("indentLevel", 0)
                            )
                        )
                    }
                }

                // Parse Attachments JSON
                runCatching {
                    val arr = JSONArray(n.attachmentsJson)
                    attachmentsList.clear()
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        attachmentsList.add(
                            NoteAttachment(
                                id = obj.optString("id"),
                                name = obj.optString("name"),
                                uriOrPath = obj.optString("uriOrPath"),
                                mimeType = obj.optString("mimeType"),
                                sizeBytes = obj.optLong("sizeBytes")
                            )
                        )
                    }
                }
            }
        } else {
            // New Note
            noteColorHex = when (settings.defaultNoteColorMode) {
                NoteColorMode.CHOOSE_COLOR -> settings.chosenDefaultColorHex
                NoteColorMode.THEME -> "#EDE7F6"
                NoteColorMode.RANDOM -> listOf("#EDE7F6", "#E3F2FD", "#E8F5E9", "#FFF3E0", "#FCE4EC").random()
            }
            if (initialType == NoteType.CHECKLIST && checklistItems.isEmpty()) {
                checklistItems.add(ChecklistItem(text = ""))
            }
        }
    }

    // Auto-save logic
    fun saveCurrentState() {
        val checklistJson = JSONArray().apply {
            checklistItems.forEach { item ->
                put(JSONObject().apply {
                    put("id", item.id)
                    put("text", item.text)
                    put("isChecked", item.isChecked)
                    put("indentLevel", item.indentLevel)
                })
            }
        }.toString()

        val attachmentsJson = JSONArray().apply {
            attachmentsList.forEach { att ->
                put(JSONObject().apply {
                    put("id", att.id)
                    put("name", att.name)
                    put("uriOrPath", att.uriOrPath)
                    put("mimeType", att.mimeType)
                    put("sizeBytes", att.sizeBytes)
                })
            }
        }.toString()

        val noteToSave = Note(
            id = loadedNote?.id ?: 0L,
            title = title,
            content = content,
            type = if (checklistItems.isNotEmpty()) NoteType.CHECKLIST else initialType,
            notebookId = selectedNotebookId,
            notebookName = selectedNotebookName,
            tagsCsv = selectedTagsCsv,
            colorHex = noteColorHex,
            isPinned = isPinned,
            isPrivate = isPrivate,
            checklistJson = checklistJson,
            attachmentsJson = attachmentsJson,
            fontName = noteFont,
            fontSizeSp = noteFontSize,
            createdTimestamp = loadedNote?.createdTimestamp ?: System.currentTimeMillis()
        )

        viewModel.saveNote(noteToSave)
    }

    val parsedBgColor = remember(noteColorHex) {
        runCatching { Color(android.graphics.Color.parseColor(noteColorHex)) }
            .getOrDefault(Color(0xFFEDE7F6))
    }

    // File / Attachment Pickers
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val fileName = it.lastPathSegment ?: "Attachment File"
            attachmentsList.add(
                NoteAttachment(
                    name = fileName,
                    uriOrPath = it.toString(),
                    mimeType = "file/*"
                )
            )
            viewModel.emitToast("Attached file: $fileName")
        }
    }

    if (showSketchCanvas) {
        DrawingCanvas(
            onSaveDrawing = { sketchMarker ->
                content += "\n\n![Sketch Note]($sketchMarker)"
                showSketchCanvas = false
                viewModel.emitToast("Sketch added to note")
            },
            onCancel = { showSketchCanvas = false }
        )
    } else if (showHistoryScreen) {
        NoteHistoryScreen(
            noteId = loadedNote?.id ?: noteId,
            viewModel = viewModel,
            onBack = { showHistoryScreen = false },
            onVersionRestored = { restored ->
                loadedNote = restored
                title = restored.title
                content = restored.content
                selectedNotebookId = restored.notebookId
                selectedNotebookName = restored.notebookName
                selectedTagsCsv = restored.tagsCsv
                noteColorHex = restored.colorHex
                isPinned = restored.isPinned
                isPrivate = restored.isPrivate
                noteFont = restored.fontName ?: settings.editorFontFamily
                noteFontSize = restored.fontSizeSp ?: settings.editorFontSizeSp

                runCatching {
                    val arr = JSONArray(restored.checklistJson)
                    checklistItems.clear()
                    for (i in 0 until arr.length()) {
                        val obj = arr.getJSONObject(i)
                        checklistItems.add(
                            ChecklistItem(
                                id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                                text = obj.optString("text", ""),
                                isChecked = obj.optBoolean("isChecked", false),
                                indentLevel = obj.optInt("indentLevel", 0)
                            )
                        )
                    }
                }
                showHistoryScreen = false
            }
        )
    } else {
        androidx.activity.compose.BackHandler {
            saveCurrentState()
            onBack()
        }
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = if (title.isBlank()) "New Note" else title,
                            maxLines = 1,
                            style = MaterialTheme.typography.titleMedium
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            saveCurrentState()
                            onBack()
                        }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Save and Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { isPinned = !isPinned }) {
                            Icon(
                                imageVector = if (isPinned) Icons.Default.PushPin else Icons.Outlined.PushPin,
                                contentDescription = "Pin Note",
                                tint = if (isPinned) MaterialTheme.colorScheme.primary else Color.Gray
                            )
                        }

                        IconButton(onClick = {
                            editorMode = if (editorMode == EditorMode.BASIC) EditorMode.ADVANCED else EditorMode.BASIC
                            viewModel.emitToast("Switched to ${editorMode.name} Editor")
                        }) {
                            Icon(
                                imageVector = if (editorMode == EditorMode.ADVANCED) Icons.Default.Code else Icons.Default.TextFields,
                                contentDescription = "Toggle Basic/Advanced Editor"
                            )
                        }

                        IconButton(onClick = {
                            saveCurrentState()
                            showHistoryScreen = true
                        }) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "View Version History"
                            )
                        }

                        IconButton(onClick = {
                            saveCurrentState()
                            viewModel.emitToast("Note saved")
                        }) {
                            Icon(Icons.Default.Check, contentDescription = "Save Note", tint = MaterialTheme.colorScheme.primary)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = parsedBgColor)
                )
            },
            bottomBar = {
                RichFormattingToolbar(
                    editorMode = editorMode,
                    currentFont = noteFont,
                    currentFontSizeSp = noteFontSize,
                    onFormatAction = { action ->
                        content += action
                    },
                    onFontChange = { newFont ->
                        noteFont = newFont
                        viewModel.emitToast("Font: $newFont")
                    },
                    onFontSizeChange = { newSize ->
                        noteFontSize = newSize
                    },
                    onAddAttachment = {
                        filePickerLauncher.launch("*/*")
                    }
                )
            },
            containerColor = parsedBgColor
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // Folder & Tags Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Folder Dropdown
                    var showFolderMenu by remember { mutableStateOf(false) }
                    Box {
                        AssistChip(
                            onClick = { showFolderMenu = true },
                            label = { Text(selectedNotebookName) },
                            leadingIcon = { Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        )
                        DropdownMenu(expanded = showFolderMenu, onDismissRequest = { showFolderMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Uncategorized") },
                                onClick = {
                                    selectedNotebookId = null
                                    selectedNotebookName = "Uncategorized"
                                    showFolderMenu = false
                                }
                            )
                            notebooks.forEach { nb ->
                                DropdownMenuItem(
                                    text = { Text(nb.name) },
                                    onClick = {
                                        selectedNotebookId = nb.id
                                        selectedNotebookName = nb.name
                                        showFolderMenu = false
                                    }
                                )
                            }
                        }
                    }

                    // PrivateSafe Toggle
                    FilterChip(
                        selected = isPrivate,
                        onClick = {
                            isPrivate = !isPrivate
                            viewModel.emitToast(if (isPrivate) "Moved to PrivateSafe" else "Removed from PrivateSafe")
                        },
                        label = { Text(if (isPrivate) "🔒 Private" else "Public") },
                        leadingIcon = { Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Title Input
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("Note Title", style = MaterialTheme.typography.titleLarge) },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = getFontFamilyByName(noteFont),
                        fontWeight = FontWeight.Bold
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    singleLine = true
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = Color.Gray.copy(alpha = 0.3f))

                // Checklist Items Section
                if (checklistItems.isNotEmpty()) {
                    Text("Checklist", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))

                    checklistItems.forEachIndexed { index, item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = item.isChecked,
                                onCheckedChange = { checked ->
                                    checklistItems[index] = item.copy(isChecked = checked)
                                }
                            )

                            OutlinedTextField(
                                value = item.text,
                                onValueChange = { newText ->
                                    checklistItems[index] = item.copy(text = newText)
                                },
                                modifier = Modifier.weight(1f),
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    textDecoration = if (item.isChecked) TextDecoration.LineThrough else TextDecoration.None
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent
                                ),
                                singleLine = true
                            )

                            IconButton(onClick = { checklistItems.removeAt(index) }) {
                                Icon(Icons.Default.Close, contentDescription = "Delete Item", modifier = Modifier.size(18.dp))
                            }
                        }
                    }

                    TextButton(onClick = { checklistItems.add(ChecklistItem(text = "")) }) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add checklist item")
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = Color.Gray.copy(alpha = 0.3f))
                }

                // Main Content Body Input
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    placeholder = { Text("Start typing your note here...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = getFontFamilyByName(noteFont),
                        fontSize = noteFontSize.sp
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    )
                )

                // Attachments List
                if (attachmentsList.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Attachments (${attachmentsList.size})", style = MaterialTheme.typography.labelLarge)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        attachmentsList.forEach { att ->
                            AssistChip(
                                onClick = { viewModel.emitToast("Opening attachment: ${att.name}") },
                                label = { Text(att.name) },
                                leadingIcon = { Icon(Icons.Default.AttachFile, contentDescription = null) },
                                trailingIcon = {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Remove",
                                        modifier = Modifier
                                            .size(16.dp)
                                            .clickable { attachmentsList.remove(att) }
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }

        if (showHistoryDialog) {
            NoteHistoryDialog(
                noteId = loadedNote?.id ?: noteId,
                viewModel = viewModel,
                onDismiss = { showHistoryDialog = false },
                onVersionRestored = { restored ->
                    loadedNote = restored
                    title = restored.title
                    content = restored.content
                    selectedNotebookId = restored.notebookId
                    selectedNotebookName = restored.notebookName
                    selectedTagsCsv = restored.tagsCsv
                    noteColorHex = restored.colorHex
                    isPinned = restored.isPinned
                    isPrivate = restored.isPrivate
                    noteFont = restored.fontName ?: settings.editorFontFamily
                    noteFontSize = restored.fontSizeSp ?: settings.editorFontSizeSp

                    runCatching {
                        val arr = JSONArray(restored.checklistJson)
                        checklistItems.clear()
                        for (i in 0 until arr.length()) {
                            val obj = arr.getJSONObject(i)
                            checklistItems.add(
                                ChecklistItem(
                                    id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                                    text = obj.optString("text", ""),
                                    isChecked = obj.optBoolean("isChecked", false),
                                    indentLevel = obj.optInt("indentLevel", 0)
                                )
                            )
                        }
                    }
                    showHistoryDialog = false
                }
            )
        }

        if (showAuthDialogForPrivateNote) {
            val context = LocalContext.current
            val fragmentActivity = context as? FragmentActivity
            val canUseBiometrics = remember(context) { BiometricAuthManager.canAuthenticate(context) }

            PrivateSafeAuthDialog(
                isSetPasscodeMode = settings.privateSafePasscode.isEmpty(),
                onBiometricClick = if (canUseBiometrics && fragmentActivity != null) {
                    {
                        BiometricAuthManager.showBiometricPrompt(
                            activity = fragmentActivity,
                            title = "Unlock PrivateSafe Note",
                            subtitle = "Authenticate to view and edit this private note",
                            negativeButtonText = "Cancel",
                            onSuccess = {
                                viewModel.unlockPrivateSafeWithBiometrics()
                                showAuthDialogForPrivateNote = false
                            },
                            onError = {
                                showAuthDialogForPrivateNote = false
                                onBack()
                            }
                        )
                    }
                } else null,
                onConfirm = { code ->
                    if (settings.privateSafePasscode.isEmpty()) {
                        viewModel.setPrivateSafePasscode(code)
                        showAuthDialogForPrivateNote = false
                    } else {
                        if (viewModel.unlockPrivateSafe(code)) {
                            showAuthDialogForPrivateNote = false
                        }
                    }
                },
                onDismiss = {
                    showAuthDialogForPrivateNote = false
                    onBack()
                }
            )
        }
    }
}
