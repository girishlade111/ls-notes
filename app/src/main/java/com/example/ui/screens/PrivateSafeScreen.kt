package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.example.ui.components.NoteCard
import com.example.ui.components.PrivateSafeAuthDialog
import com.example.ui.viewmodel.NotesViewModel
import com.example.util.BiometricAuthManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivateSafeScreen(
    viewModel: NotesViewModel,
    onNoteClick: (Long) -> Unit,
    onOpenDrawer: () -> Unit,
    onOpenInfo: (com.example.data.model.Note) -> Unit,
    onExportNote: (com.example.data.model.Note) -> Unit
) {
    val context = LocalContext.current
    val fragmentActivity = context as? FragmentActivity
    val isUnlocked by viewModel.isPrivateSafeUnlocked.collectAsState()
    val privateNotes by viewModel.privateNotes.collectAsState()
    val settings by viewModel.settings.collectAsState()

    var showAuthDialog by remember { mutableStateOf(false) }
    var isSetPasscodeMode by remember { mutableStateOf(false) }

    val canUseBiometrics = remember(context) { BiometricAuthManager.canAuthenticate(context) }

    val glassBorder = BorderStroke(
        width = 1.dp,
        brush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.5f),
                Color.White.copy(alpha = 0.15f),
                MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
            )
        )
    )

    fun triggerBiometricUnlock() {
        if (fragmentActivity != null && canUseBiometrics) {
            BiometricAuthManager.showBiometricPrompt(
                activity = fragmentActivity,
                title = "Unlock PrivateSafe",
                subtitle = "Authenticate using fingerprint or face unlock to view encrypted notes",
                negativeButtonText = "Use Passcode",
                onSuccess = {
                    viewModel.unlockPrivateSafeWithBiometrics()
                    showAuthDialog = false
                },
                onError = { err ->
                    if (err != "Cancelled by user") {
                        viewModel.emitToast(err)
                    }
                    isSetPasscodeMode = settings.privateSafePasscode.isEmpty()
                    showAuthDialog = true
                }
            )
        } else {
            isSetPasscodeMode = settings.privateSafePasscode.isEmpty()
            showAuthDialog = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PrivateSafe Workspace") },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Open Drawer")
                    }
                },
                actions = {
                    if (isUnlocked) {
                        IconButton(onClick = { viewModel.lockPrivateSafe() }) {
                            Icon(Icons.Default.Lock, contentDescription = "Lock PrivateSafe", tint = MaterialTheme.colorScheme.error)
                        }
                    } else {
                        IconButton(onClick = { triggerBiometricUnlock() }) {
                            Icon(Icons.Default.LockOpen, contentDescription = "Unlock PrivateSafe", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            if (!isUnlocked) {
                // Glassmorphism Locked View Container
                Surface(
                    modifier = Modifier.fillMaxWidth(0.92f),
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.55f),
                    border = glassBorder,
                    shadowElevation = 12.dp,
                    tonalElevation = 6.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Surface(
                            modifier = Modifier.size(96.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
                        ) {
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = "Security Shield",
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "PrivateSafe is Locked",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Protected with biometric authentication and local PIN passcode.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(28.dp))

                        if (canUseBiometrics && settings.privateSafePasscode.isNotEmpty()) {
                            Button(
                                onClick = { triggerBiometricUnlock() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("unlock_biometrics_button")
                            ) {
                                Icon(Icons.Default.Fingerprint, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Unlock with Biometrics")
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedButton(
                                onClick = {
                                    isSetPasscodeMode = false
                                    showAuthDialog = true
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("unlock_pin_button")
                            ) {
                                Text("Unlock with PIN Passcode")
                            }
                        } else {
                            Button(
                                onClick = {
                                    isSetPasscodeMode = settings.privateSafePasscode.isEmpty()
                                    showAuthDialog = true
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp)
                                    .testTag("unlock_privatesafe_main_button")
                            ) {
                                Text(if (settings.privateSafePasscode.isEmpty()) "Set PrivateSafe Passcode" else "Unlock PrivateSafe")
                            }
                        }
                    }
                }
            } else {
                // Unlocked View
                if (privateNotes.isEmpty()) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(0.9f),
                        shape = RoundedCornerShape(24.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f),
                        border = glassBorder,
                        shadowElevation = 6.dp
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.LockOpen,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "PrivateSafe is Empty",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "To move notes here, toggle 'Private' on any note card or in the editor.",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(bottom = 16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(privateNotes, key = { it.id }) { note ->
                            NoteCard(
                                note = note,
                                isListView = false,
                                showTimeMode = settings.timeDisplayMode,
                                revealPrivateContent = true,
                                onClick = { onNoteClick(note.id) },
                                onPinToggle = { viewModel.togglePinNote(note.id) },
                                onFavoriteToggle = { viewModel.toggleFavoriteNote(note.id) },
                                onDuplicate = { viewModel.duplicateNote(note.id) },
                                onTogglePrivate = { viewModel.setNotePrivacy(note.id, !note.isPrivate) },
                                onDelete = { viewModel.moveNoteToTrash(note.id) },
                                onExport = { onExportNote(note) },
                                onShare = { viewModel.emitToast("Sharing note: ${note.title}") },
                                onInfo = { onOpenInfo(note) },
                                onChangeColor = { colorHex ->
                                    viewModel.saveNote(note.copy(colorHex = colorHex))
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAuthDialog) {
        PrivateSafeAuthDialog(
            isSetPasscodeMode = isSetPasscodeMode,
            onBiometricClick = if (canUseBiometrics && !isSetPasscodeMode && fragmentActivity != null) {
                { triggerBiometricUnlock() }
            } else null,
            onConfirm = { code ->
                if (isSetPasscodeMode) {
                    viewModel.setPrivateSafePasscode(code)
                    showAuthDialog = false
                } else {
                    if (viewModel.unlockPrivateSafe(code)) {
                        showAuthDialog = false
                    }
                }
            },
            onDismiss = { showAuthDialog = false }
        )
    }
}
