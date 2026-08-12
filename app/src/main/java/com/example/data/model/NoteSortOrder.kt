package com.example.data.model

enum class NoteSortCriteria {
    LAST_MODIFIED,
    TITLE,
    DATE_CREATED
}

enum class SortDirection {
    ASCENDING,
    DESCENDING
}

data class NoteSortOrder(
    val criteria: NoteSortCriteria = NoteSortCriteria.LAST_MODIFIED,
    val direction: SortDirection = SortDirection.DESCENDING
) {
    companion object {
        val DEFAULT = NoteSortOrder(NoteSortCriteria.LAST_MODIFIED, SortDirection.DESCENDING)
        val LAST_MODIFIED_DESC = NoteSortOrder(NoteSortCriteria.LAST_MODIFIED, SortDirection.DESCENDING)
        val LAST_MODIFIED_ASC = NoteSortOrder(NoteSortCriteria.LAST_MODIFIED, SortDirection.ASCENDING)
        val TITLE_ASC = NoteSortOrder(NoteSortCriteria.TITLE, SortDirection.ASCENDING)
        val TITLE_DESC = NoteSortOrder(NoteSortCriteria.TITLE, SortDirection.DESCENDING)
        val DATE_CREATED_DESC = NoteSortOrder(NoteSortCriteria.DATE_CREATED, SortDirection.DESCENDING)
        val DATE_CREATED_ASC = NoteSortOrder(NoteSortCriteria.DATE_CREATED, SortDirection.ASCENDING)
    }
}

fun List<Note>.sortNotes(sortOrder: NoteSortOrder): List<Note> {
    return when (sortOrder.criteria) {
        NoteSortCriteria.LAST_MODIFIED -> {
            if (sortOrder.direction == SortDirection.DESCENDING) {
                sortedWith(compareByDescending<Note> { it.isPinned }.thenByDescending { it.updatedTimestamp })
            } else {
                sortedWith(compareByDescending<Note> { it.isPinned }.thenBy { it.updatedTimestamp })
            }
        }
        NoteSortCriteria.TITLE -> {
            if (sortOrder.direction == SortDirection.ASCENDING) {
                sortedWith(compareByDescending<Note> { it.isPinned }.thenBy(String.CASE_INSENSITIVE_ORDER) { it.title })
            } else {
                sortedWith(compareByDescending<Note> { it.isPinned }.thenByDescending(String.CASE_INSENSITIVE_ORDER) { it.title })
            }
        }
        NoteSortCriteria.DATE_CREATED -> {
            if (sortOrder.direction == SortDirection.DESCENDING) {
                sortedWith(compareByDescending<Note> { it.isPinned }.thenByDescending { it.createdTimestamp })
            } else {
                sortedWith(compareByDescending<Note> { it.isPinned }.thenBy { it.createdTimestamp })
            }
        }
    }
}
