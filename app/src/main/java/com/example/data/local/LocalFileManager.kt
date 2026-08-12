package com.example.data.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LocalFileManager(private val context: Context) {

    private val attachmentsDir: File
        get() = File(context.filesDir, "note_attachments").also { if (!it.exists()) it.mkdirs() }

    private val backupsDir: File
        get() = File(context.filesDir, "note_backups").also { if (!it.exists()) it.mkdirs() }

    fun saveAttachment(inputStream: InputStream, originalFileName: String): String {
        val extension = originalFileName.substringAfterLast('.', "bin")
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "ATT_${timestamp}_${System.currentTimeMillis()}.$extension"
        val targetFile = File(attachmentsDir, fileName)

        FileOutputStream(targetFile).use { outputStream ->
            inputStream.copyTo(outputStream)
        }
        return targetFile.absolutePath
    }

    fun saveBitmap(bitmap: Bitmap, format: Bitmap.CompressFormat = Bitmap.CompressFormat.JPEG): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val extension = if (format == Bitmap.CompressFormat.PNG) "png" else "jpg"
        val fileName = "IMG_${timestamp}.$extension"
        val targetFile = File(attachmentsDir, fileName)

        FileOutputStream(targetFile).use { out ->
            bitmap.compress(format, 90, out)
        }
        return targetFile.absolutePath
    }

    fun loadBitmap(filePath: String): Bitmap? {
        val file = File(filePath)
        if (!file.exists()) return null
        return BitmapFactory.decodeFile(file.absolutePath)
    }

    fun deleteAttachment(filePath: String): Boolean {
        val file = File(filePath)
        return if (file.exists()) file.delete() else false
    }

    fun exportBackupJson(jsonString: String): String {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "ls_notes_backup_$timestamp.json"
        val targetFile = File(backupsDir, fileName)

        targetFile.writeText(jsonString)
        return targetFile.absolutePath
    }

    fun getLocalBackups(): List<File> {
        return backupsDir.listFiles()?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    fun readBackupFile(file: File): String {
        return file.readText()
    }
}
