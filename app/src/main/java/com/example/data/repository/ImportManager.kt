package com.example.data.repository

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import com.example.data.model.Note
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Data class representing the result summary of a batch import operation.
 */
data class ImportSummary(
    val totalFilesProcessed: Int,
    val totalNotesImported: Int,
    val successfulCount: Int,
    val failedCount: Int,
    val skippedCount: Int = 0,
    val importedNotebookName: String,
    val errors: List<String> = emptyList()
)

/**
 * Dedicated repository-layer helper class that processes JSON and Markdown files
 * selected via ActivityResultContracts.OpenDocument or OpenMultipleDocuments,
 * executes batch Room database transactions, and returns a detailed ImportSummary.
 */
class ImportManager(
    private val context: Context,
    private val repository: LsNotesRepository
) {
    private val importService = NoteImportService(context, repository)

    /**
     * MIME types supported for selection via ActivityResultContracts.OpenDocument.
     */
    fun getSupportedMimeTypes(): Array<String> {
        return arrayOf(
            "application/json",
            "text/markdown",
            "text/x-markdown",
            "text/plain",
            "text/html",
            "*/*"
        )
    }

    /**
     * Processes a single Android Content URI picked via OpenDocument.
     */
    suspend fun importSingleFile(
        uri: Uri,
        targetNotebookName: String = "Imported Notes"
    ): ImportSummary = withContext(Dispatchers.IO) {
        importFiles(listOf(uri), targetNotebookName)
    }

    /**
     * Processes a list of Android Content URIs picked via OpenDocument / OpenMultipleDocuments.
     * All notes are parsed and written inside a batch database transaction.
     */
    suspend fun importFiles(
        uris: List<Uri>,
        targetNotebookName: String = "Imported Notes"
    ): ImportSummary = withContext(Dispatchers.IO) {
        if (uris.isEmpty()) {
            return@withContext ImportSummary(
                totalFilesProcessed = 0,
                totalNotesImported = 0,
                successfulCount = 0,
                failedCount = 0,
                importedNotebookName = targetNotebookName,
                errors = listOf("No file URIs provided for import.")
            )
        }

        var successfulFiles = 0
        var failedFiles = 0
        val errors = mutableListOf<String>()
        val parsedNotesBatch = mutableListOf<Note>()

        for (uri in uris) {
            val fileName = getFileNameFromUri(uri) ?: "Imported_File"
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                    ?: throw IllegalArgumentException("Unable to open file stream for $fileName")

                val content = inputStream.use { stream ->
                    stream.bufferedReader(Charsets.UTF_8).readText()
                }

                val notes = importService.parseContent(
                    content = content,
                    fileName = fileName,
                    targetNotebookName = targetNotebookName
                )

                if (notes.isNotEmpty()) {
                    parsedNotesBatch.addAll(notes)
                    successfulFiles++
                } else {
                    failedFiles++
                    errors.add("No valid note content extracted from file: $fileName")
                }
            } catch (e: Exception) {
                failedFiles++
                errors.add("Failed to import '$fileName': ${e.localizedMessage}")
            }
        }

        var totalImportedNotes = 0
        if (parsedNotesBatch.isNotEmpty()) {
            try {
                // Batch Room database transaction
                val savedNotes = repository.importNotesBatchInTransaction(parsedNotesBatch, targetNotebookName)
                totalImportedNotes = savedNotes.size
            } catch (e: Exception) {
                errors.add("Database batch transaction error: ${e.localizedMessage}")
            }
        }

        ImportSummary(
            totalFilesProcessed = uris.size,
            totalNotesImported = totalImportedNotes,
            successfulCount = successfulFiles,
            failedCount = failedFiles,
            importedNotebookName = targetNotebookName,
            errors = errors
        )
    }

    private fun getFileNameFromUri(uri: Uri): String? {
        var name: String? = null
        if (uri.scheme == "content") {
            runCatching {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val displayNameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (displayNameIndex != -1) {
                            name = cursor.getString(displayNameIndex)
                        }
                    }
                }
            }
        }
        if (name == null) {
            name = uri.lastPathSegment
        }
        return name
    }
}
