package com.example.data.local

import android.content.Context

typealias AppDatabase = LsNotesDatabase

fun AppDatabase(context: Context): AppDatabase {
    return LsNotesDatabase.getInstance(context)
}
