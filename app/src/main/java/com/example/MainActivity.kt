package com.example

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.MainScreen
import com.example.ui.theme.LsNotesTheme
import com.example.ui.viewmodel.NotesViewModel

class MainActivity : FragmentActivity() {

    private var currentNotesViewModel: NotesViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val notesViewModel: NotesViewModel = viewModel()
            currentNotesViewModel = notesViewModel
            val settings by notesViewModel.settings.collectAsState()

            LaunchedEffect(intent) {
                notesViewModel.handleWidgetIntent(intent)
            }

            LsNotesTheme(themeMode = settings.themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(viewModel = notesViewModel)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        currentNotesViewModel?.handleWidgetIntent(intent)
    }
}
