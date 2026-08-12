package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R

class QuickActionWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateQuickWidget(context, appWidgetManager, appWidgetId)
        }
        super.onUpdate(context, appWidgetManager, appWidgetIds)
    }

    companion object {
        fun updateQuickWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_quick_action)

            // Text Note Shortcut
            views.setOnClickPendingIntent(
                R.id.btn_quick_text,
                createQuickPendingIntent(context, "TEXT", 101)
            )

            // Checklist Shortcut
            views.setOnClickPendingIntent(
                R.id.btn_quick_checklist,
                createQuickPendingIntent(context, "CHECKLIST", 102)
            )

            // Photo Note Shortcut
            views.setOnClickPendingIntent(
                R.id.btn_quick_photo,
                createQuickPendingIntent(context, "PHOTO", 106)
            )

            // Voice Note Shortcut
            views.setOnClickPendingIntent(
                R.id.btn_quick_voice,
                createQuickPendingIntent(context, "VOICE", 103)
            )

            // Sketch/Drawing Note Shortcut
            views.setOnClickPendingIntent(
                R.id.btn_quick_draw,
                createQuickPendingIntent(context, "DRAWING", 104)
            )

            // Search Shortcut
            val searchIntent = Intent(context, MainActivity::class.java).apply {
                action = "ACTION_SEARCH"
                putExtra(PinnedNotesWidgetProvider.EXTRA_ACTION, "ACTION_SEARCH")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val searchPendingIntent = PendingIntent.getActivity(
                context,
                105,
                searchIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.btn_quick_search, searchPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }

        private fun createQuickPendingIntent(
            context: Context,
            noteTypeStr: String,
            requestCode: Int
        ): PendingIntent {
            val intent = Intent(context, MainActivity::class.java).apply {
                action = PinnedNotesWidgetProvider.ACTION_CREATE_NOTE
                putExtra(PinnedNotesWidgetProvider.EXTRA_ACTION, PinnedNotesWidgetProvider.ACTION_CREATE_NOTE)
                putExtra(PinnedNotesWidgetProvider.EXTRA_CREATE_NOTE_TYPE, noteTypeStr)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            return PendingIntent.getActivity(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }
    }
}
