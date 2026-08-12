package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.*
import com.example.ui.components.PrivateSafeAuthDialog
import com.example.ui.theme.AvailableFonts
import com.example.ui.viewmodel.NotesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: NotesViewModel,
    onOpenDrawer: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()
    var showPasscodeDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("App Settings & Customization") },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = "Open Drawer")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 1: Appearance & Theme
            SettingsSectionHeader(title = "Appearance & Theme", icon = Icons.Default.Palette)

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Theme Mode", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        ThemeMode.values().forEach { mode ->
                            FilterChip(
                                selected = settings.themeMode == mode,
                                onClick = { viewModel.updateSettings(settings.copy(themeMode = mode)) },
                                label = { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Default Note Color Mode", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        NoteColorMode.values().forEach { mode ->
                            FilterChip(
                                selected = settings.defaultNoteColorMode == mode,
                                onClick = { viewModel.updateSettings(settings.copy(defaultNoteColorMode = mode)) },
                                label = { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }) }
                            )
                        }
                    }

                    if (settings.defaultNoteColorMode == NoteColorMode.CHOOSE_COLOR) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Selected Default Color", style = MaterialTheme.typography.labelMedium)
                        val choices = listOf("#FFFFFF", "#EDE7F6", "#E3F2FD", "#E8F5E9", "#FFF3E0", "#FCE4EC")
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            choices.forEach { hex ->
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(android.graphics.Color.parseColor(hex)))
                                        .clickable {
                                            viewModel.updateSettings(settings.copy(chosenDefaultColorHex = hex))
                                        }
                                )
                            }
                        }
                    }
                }
            }

            // Section 2: Note Display & Layout
            SettingsSectionHeader(title = "Note Display & Layout", icon = Icons.Default.ViewAgenda)

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Note Card Time Indicator", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    TimeDisplayMode.values().forEach { timeMode ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .clickable { viewModel.updateSettings(settings.copy(timeDisplayMode = timeMode)) },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = settings.timeDisplayMode == timeMode,
                                onClick = { viewModel.updateSettings(settings.copy(timeDisplayMode = timeMode)) }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(timeMode.name.replace("_", " "))
                        }
                    }
                }
            }

            // Section 3: Typography & Editor
            SettingsSectionHeader(title = "Typography & Editor Defaults", icon = Icons.Default.TextFields)

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Default Font Family", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    var showFontMenu by remember { mutableStateOf(false) }
                    Box {
                        OutlinedButton(
                            onClick = { showFontMenu = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(settings.editorFontFamily)
                        }
                        DropdownMenu(expanded = showFontMenu, onDismissRequest = { showFontMenu = false }) {
                            AvailableFonts.forEach { font ->
                                DropdownMenuItem(
                                    text = { Text(font) },
                                    onClick = {
                                        viewModel.updateSettings(settings.copy(editorFontFamily = font))
                                        showFontMenu = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Default Font Size: ${settings.editorFontSizeSp.toInt()} sp", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Slider(
                        value = settings.editorFontSizeSp,
                        onValueChange = { viewModel.updateSettings(settings.copy(editorFontSizeSp = it)) },
                        valueRange = 12f..28f,
                        steps = 8
                    )
                }
            }

            // Section 4: Security & PrivateSafe
            SettingsSectionHeader(title = "Security & PrivateSafe", icon = Icons.Default.Security)

            val context = androidx.compose.ui.platform.LocalContext.current
            val bioMessage = remember(context) { com.example.util.BiometricAuthManager.getBiometricStatusMessage(context) }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("PrivateSafe Security", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (settings.privateSafePasscode.isEmpty()) "PIN status: Not set" else "PIN status: Active",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = bioMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = { showPasscodeDialog = true }) {
                        Text(if (settings.privateSafePasscode.isEmpty()) "Set Passcode" else "Change Passcode")
                    }
                }
            }

            // Section 5: Privacy Guarantee
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("LS Notes Privacy Guarantee", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "100% Offline & Private Local Storage. Your notes, sketches, and attachments never leave this device without your express consent.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }

    if (showPasscodeDialog) {
        PrivateSafeAuthDialog(
            isSetPasscodeMode = true,
            onConfirm = { newCode ->
                viewModel.setPrivateSafePasscode(newCode)
                showPasscodeDialog = false
            },
            onDismiss = { showPasscodeDialog = false }
        )
    }
}

@Composable
private fun SettingsSectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}
