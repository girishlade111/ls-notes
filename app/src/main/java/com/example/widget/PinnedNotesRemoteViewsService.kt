package com.example.widget

import android.content.Context
import android.content.Intent
import android.text.format.DateFormat
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.example.R
import com.example.data.local.LsNotesDatabase
import com.example.data.model.Note
import com.example.data.model.NoteType
import kotlinx.coroutines.runBlocking
import java.util.Date

class PinnedNotesRemoteViewsService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return PinnedNotesRemoteViewsFactory(this.applicationContext)
    }
}

class PinnedNotesRemoteViewsFactory(
    private val context: Context
) : RemoteViewsService.RemoteViewsFactory {

    private var notesList: List<Note> = emptyList()

    override fun onCreate() {
        // Initial setup
    }

    override fun onDataSetChanged() {
        // Called on a background thread by AppWidgetManager
        runBlocking {
            runCatching {
                val db = LsNotesDatabase.getInstance(context)
                val pinned = db.noteDao().getPinnedNotesDirect()
                notesList = if (pinned.isNotEmpty()) {
                    pinned
                } else {
                    db.noteDao().getRecentNotesDirect()
                }
            }.onFailure {
                notesList = emptyList()
            }
        }
    }

    override fun onDestroy() {
        notesList = emptyList()
    }

    override fun getCount(): Int = notesList.size

    override fun getViewAt(position: Int): RemoteViews? {
        if (position < 0 || position >= notesList.size) return null

        val note = notesList[position]
        val views = RemoteViews(context.packageName, R.layout.widget_pinned_notes_item)

        val displayTitle = note.title.ifBlank { "Untitled Note" }
        views.setTextViewText(R.id.widget_item_title, displayTitle)

        val snippetText = if (note.isPrivate) {
            "🔒 PrivateSafe Note"
        } else {
            when (note.type) {
                NoteType.CHECKLIST -> "☑️ Checklist item(s)"
                NoteType.AUDIO -> "🎙️ Voice recording note"
                NoteType.SKETCH -> "🎨 Sketch drawing note"
                else -> note.content.ifBlank { "Empty note" }
            }
        }
        views.setTextViewText(R.id.widget_item_snippet, snippetText)

        val formattedDate = DateFormat.format("MMM dd, yyyy", Date(note.updatedTimestamp)).toString()
        views.setTextViewText(R.id.widget_item_date, formattedDate)

        // Set FillInIntent for row click handling
        val fillInIntent = Intent().apply {
            putExtra(PinnedNotesWidgetProvider.EXTRA_NOTE_ID, note.id)
        }
        views.setOnClickFillInIntent(R.id.widget_item_container, fillInIntent)

        return views
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long {
        return if (position < notesList.size) notesList[position].id else position.toLong()
    }

    override fun hasStableIds(): Boolean = true
}
