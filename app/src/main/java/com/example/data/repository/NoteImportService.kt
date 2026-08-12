package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.data.model.ChecklistItem
import com.example.data.model.Note
import com.example.data.model.NoteType
import com.example.data.model.Notebook
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.util.UUID

/**
 * Repository-layer service for file selection, parsing, and ingestion logic
 * for importing notes from various offline sources (Markdown, TXT, JSON, Keep, ENEX).
 * Operates 100% offline using local parsing.
 */
class NoteImportService(
    private val context: Context,
    private val repository: LsNotesRepository
) {

    /**
     * Ingests a file from an Android Content Uri, detects its format, parses the content,
     * and persists the imported notes directly to local Room storage.
     */
    suspend fun importFromUri(
        uri: Uri,
        originalFileName: String?,
        targetNotebookName: String = "Imported Notes"
    ): Result<List<Note>> {
        return runCatching {
            val fileName = originalFileName ?: getFileNameFromUri(uri) ?: "Imported_Note.txt"
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: throw IllegalArgumentException("Cannot open file stream for URI: $uri")

            val content = inputStream.use { stream ->
                BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).readText()
            }

            val parsedNotes = parseContent(
                content = content,
                fileName = fileName,
                targetNotebookName = targetNotebookName
            )

            ingestParsedNotes(parsedNotes, targetNotebookName)
        }
    }

    /**
     * Ingests multiple files from a list of Android Content URIs operating 100% offline.
     * All files are processed inside a single atomic database transaction to prevent partial imports.
     */
    suspend fun importFromUris(
        uris: List<Uri>,
        targetNotebookName: String = "Imported Notes"
    ): Result<List<Note>> {
        return runCatching {
            repository.runInTransaction {
                val allImportedNotes = mutableListOf<Note>()
                for (uri in uris) {
                    val fileName = getFileNameFromUri(uri)
                    val inputStream = context.contentResolver.openInputStream(uri)
                        ?: throw IllegalArgumentException("Cannot open file stream for URI: $uri")

                    val content = inputStream.use { stream ->
                        BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).readText()
                    }

                    val parsedNotes = parseContent(
                        content = content,
                        fileName = fileName ?: "Imported_Note.txt",
                        targetNotebookName = targetNotebookName
                    )

                    val savedNotes = ingestParsedNotes(parsedNotes, targetNotebookName)
                    allImportedNotes.addAll(savedNotes)
                }
                allImportedNotes
            }
        }
    }

    private fun getFileNameFromUri(uri: Uri): String? {
        var name: String? = null
        if (uri.scheme == "content") {
            runCatching {
                context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val displayNameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
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

    /**
     * Ingests content directly from an InputStream and persists notes locally.
     */
    suspend fun importFromInputStream(
        inputStream: InputStream,
        fileName: String,
        targetNotebookName: String = "Imported Notes"
    ): Result<List<Note>> {
        return runCatching {
            val content = inputStream.use { stream ->
                BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).readText()
            }

            val parsedNotes = parseContent(
                content = content,
                fileName = fileName,
                targetNotebookName = targetNotebookName
            )

            ingestParsedNotes(parsedNotes, targetNotebookName)
        }
    }

    /**
     * Parses raw text content based on file extension / content format.
     */
    fun parseContent(
        content: String,
        fileName: String,
        targetNotebookName: String
    ): List<Note> {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return when {
            extension == "enex" || content.contains("<en-export") -> {
                parseEnexNotes(content, targetNotebookName)
            }
            extension == "json" || content.trim().startsWith("{") || content.trim().startsWith("[") -> {
                parseJsonNotes(content, targetNotebookName)
            }
            extension == "md" || extension == "markdown" -> {
                listOf(parseMarkdownNote(content, fileName, targetNotebookName))
            }
            extension == "html" || extension == "htm" -> {
                listOf(parseHtmlNote(content, fileName, targetNotebookName))
            }
            else -> {
                listOf(parsePlainTextNote(content, fileName, targetNotebookName))
            }
        }
    }

    /**
     * Parses Markdown content into a structured Note model.
     * Supports Frontmatter (`---` tags/title), `# Title` headings, and `- [ ]` checklist parsing.
     */
    fun parseMarkdownNote(
        rawContent: String,
        fileName: String,
        targetNotebookName: String
    ): Note {
        var cleanTitle: String? = null
        var tagsCsv = "Markdown, Imported"
        var bodyContent = rawContent
        val checklistItems = mutableListOf<ChecklistItem>()

        // 1. Frontmatter extraction (YAML header)
        if (rawContent.startsWith("---")) {
            val endIdx = rawContent.indexOf("---", 3)
            if (endIdx > 3) {
                val frontmatter = rawContent.substring(3, endIdx)
                bodyContent = rawContent.substring(endIdx + 3).trim()

                frontmatter.lineSequence().forEach { line ->
                    when {
                        line.startsWith("title:", ignoreCase = true) -> {
                            cleanTitle = line.substringAfter("title:").trim().removeSurrounding("\"").removeSurrounding("'")
                        }
                        line.startsWith("tags:", ignoreCase = true) -> {
                            val rawTags = line.substringAfter("tags:").trim().removeSurrounding("[").removeSurrounding("]")
                            if (rawTags.isNotBlank()) {
                                tagsCsv = "$tagsCsv, $rawTags"
                            }
                        }
                    }
                }
            }
        }

        val lines = bodyContent.lines()

        // 2. Extract title from first H1 `# Title` if not set in frontmatter
        if (cleanTitle == null) {
            val firstH1Line = lines.firstOrNull { it.trim().startsWith("# ") }
            if (firstH1Line != null) {
                cleanTitle = firstH1Line.trim().removePrefix("#").trim()
            }
        }

        // Fallback title from file name
        val finalTitle = cleanTitle.takeUnless { it.isNullOrBlank() }
            ?: fileName.substringBeforeLast('.').replace('_', ' ').replace('-', ' ').trim()

        // 3. Checklist items detection
        var noteType = NoteType.TEXT
        lines.forEach { line ->
            val trimmed = line.trim()
            if (trimmed.startsWith("- [ ]") || trimmed.startsWith("- [x]") || trimmed.startsWith("- [X]")) {
                val isChecked = trimmed.contains("[x]", ignoreCase = true)
                val itemText = trimmed.substring(5).trim()
                if (itemText.isNotBlank()) {
                    checklistItems.add(
                        ChecklistItem(
                            id = UUID.randomUUID().toString(),
                            text = itemText,
                            isChecked = isChecked
                        )
                    )
                }
            }
        }

        val checklistJson = if (checklistItems.isNotEmpty()) {
            noteType = NoteType.CHECKLIST
            val jsonArray = JSONArray()
            checklistItems.forEach { item ->
                val obj = JSONObject().apply {
                    put("id", item.id)
                    put("text", item.text)
                    put("isChecked", item.isChecked)
                    put("indentLevel", item.indentLevel)
                }
                jsonArray.put(obj)
            }
            jsonArray.toString()
        } else {
            "[]"
        }

        return Note(
            title = finalTitle,
            content = bodyContent,
            type = noteType,
            notebookName = targetNotebookName,
            tagsCsv = tagsCsv,
            checklistJson = checklistJson,
            createdTimestamp = System.currentTimeMillis(),
            updatedTimestamp = System.currentTimeMillis()
        )
    }

    /**
     * Parses plain text (.txt) files.
     * Uses the first line as the note title and the remaining text as body content.
     */
    fun parsePlainTextNote(
        rawContent: String,
        fileName: String,
        targetNotebookName: String
    ): Note {
        val lines = rawContent.lines().filter { it.isNotBlank() }
        val extractedTitle = lines.firstOrNull()?.take(60)
            ?: fileName.substringBeforeLast('.').replace('_', ' ').trim()

        return Note(
            title = extractedTitle,
            content = rawContent,
            type = NoteType.TEXT,
            notebookName = targetNotebookName,
            tagsCsv = "TXT, Imported",
            createdTimestamp = System.currentTimeMillis(),
            updatedTimestamp = System.currentTimeMillis()
        )
    }

    /**
     * Parses HTML files, stripping basic HTML markup for text notes.
     */
    fun parseHtmlNote(
        rawContent: String,
        fileName: String,
        targetNotebookName: String
    ): Note {
        val titleMatch = Regex("(?i)<title>(.*?)</title>").find(rawContent)
        val extractedTitle = titleMatch?.groupValues?.get(1)?.trim()
            ?: fileName.substringBeforeLast('.').replace('_', ' ').trim()

        // Strip HTML tags for clean text content
        val plainBody = rawContent
            .replace(Regex("(?i)<br\\s*/?>"), "\n")
            .replace(Regex("(?i)</p>"), "\n\n")
            .replace(Regex("<[^>]*>"), "")
            .trim()

        return Note(
            title = extractedTitle,
            content = plainBody,
            type = NoteType.TEXT,
            notebookName = targetNotebookName,
            tagsCsv = "HTML, Imported",
            createdTimestamp = System.currentTimeMillis(),
            updatedTimestamp = System.currentTimeMillis()
        )
    }

    /**
     * Parses Evernote XML export files (.enex) offline into structured Notes.
     */
    fun parseEnexNotes(
        enexXml: String,
        targetNotebookName: String
    ): List<Note> {
        val notesList = mutableListOf<Note>()
        val noteRegex = Regex("(?s)<note>(.*?)</note>")
        val noteMatches = noteRegex.findAll(enexXml)

        for (match in noteMatches) {
            val noteXml = match.groupValues[1]
            val titleMatch = Regex("(?s)<title>(.*?)</title>").find(noteXml)
            val title = titleMatch?.groupValues?.get(1)?.trim() ?: "Imported ENEX Note"

            val tagMatches = Regex("(?s)<tag>(.*?)</tag>").findAll(noteXml)
            val tags = tagMatches.map { it.groupValues[1].trim() }.filter { it.isNotBlank() }.toList()
            val tagsCsv = if (tags.isNotEmpty()) "Evernote, ${tags.joinToString(", ")}" else "Evernote, Imported"

            val contentMatch = Regex("(?s)<content>(.*?)</content>").find(noteXml)
            val rawContent = contentMatch?.groupValues?.get(1) ?: ""
            val cleanContent = rawContent
                .replace("<![CDATA[", "")
                .replace("]]>", "")
                .replace(Regex("(?i)<br\\s*/?>"), "\n")
                .replace(Regex("(?i)</p>"), "\n\n")
                .replace(Regex("<[^>]*>"), "")
                .trim()

            notesList.add(
                Note(
                    title = title,
                    content = cleanContent,
                    type = NoteType.TEXT,
                    notebookName = targetNotebookName,
                    tagsCsv = tagsCsv,
                    createdTimestamp = System.currentTimeMillis(),
                    updatedTimestamp = System.currentTimeMillis()
                )
            )
        }
        return if (notesList.isNotEmpty()) notesList else listOf(parsePlainTextNote(enexXml, "Evernote_Note.txt", targetNotebookName))
    }

    /**
     * Parses JSON backup/export payloads (LS Notes JSON backups or Google Keep JSON exports).
     */
    fun parseJsonNotes(
        jsonString: String,
        targetNotebookName: String
    ): List<Note> {
        val notesList = mutableListOf<Note>()
        val trimmed = jsonString.trim()

        if (trimmed.startsWith("[")) {
            val jsonArray = JSONArray(trimmed)
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.optJSONObject(i) ?: continue
                parseSingleJsonNote(obj, targetNotebookName)?.let { notesList.add(it) }
            }
        } else if (trimmed.startsWith("{")) {
            val jsonObject = JSONObject(trimmed)
            if (jsonObject.has("notes") && jsonObject.get("notes") is JSONArray) {
                val jsonArray = jsonObject.getJSONArray("notes")
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.optJSONObject(i) ?: continue
                    parseSingleJsonNote(obj, targetNotebookName)?.let { notesList.add(it) }
                }
            } else {
                parseSingleJsonNote(jsonObject, targetNotebookName)?.let { notesList.add(it) }
            }
        }

        return notesList
    }

    private fun parseSingleJsonNote(obj: JSONObject, targetNotebookName: String): Note? {
        val title = obj.optString("title", "").ifBlank {
            obj.optString("titleText", "Imported JSON Note")
        }
        val content = obj.optString("content", "").ifBlank {
            obj.optString("textContent", "")
        }

        val typeStr = obj.optString("type", "TEXT")
        val noteType = runCatching { NoteType.valueOf(typeStr) }.getOrDefault(NoteType.TEXT)

        val tagsCsv = obj.optString("tagsCsv", "JSON, Imported")
        val colorHex = obj.optString("colorHex", "#FFFFFF")
        val checklistJson = obj.optString("checklistJson", "[]")
        val attachmentsJson = obj.optString("attachmentsJson", "[]")

        return Note(
            title = title,
            content = content,
            type = noteType,
            notebookName = targetNotebookName,
            tagsCsv = tagsCsv,
            colorHex = colorHex,
            checklistJson = checklistJson,
            attachmentsJson = attachmentsJson,
            isPinned = obj.optBoolean("isPinned", false),
            isFavorite = obj.optBoolean("isFavorite", false),
            createdTimestamp = obj.optLong("createdTimestamp", System.currentTimeMillis()),
            updatedTimestamp = obj.optLong("updatedTimestamp", System.currentTimeMillis())
        )
    }

    /**
     * Persists a list of parsed Note entities into local storage inside an atomic Room transaction.
     */
    suspend fun ingestParsedNotes(
        notes: List<Note>,
        targetNotebookName: String
    ): List<Note> {
        return repository.importNotesBatchInTransaction(notes, targetNotebookName)
    }
}
