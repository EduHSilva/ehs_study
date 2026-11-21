package com.studyflow.app.domain.notes

/** Regras de texto dos cartões de nota, iguais às do web (`NotesPage.vue`). */
object NoteFormat {
    const val PREVIEW_MAX_LENGTH = 110

    private val whitespace = Regex("\\s+")

    /** Achata os espaços e corta em [maxLength] caracteres, com reticências. */
    fun preview(content: String?, maxLength: Int = PREVIEW_MAX_LENGTH): String {
        val text = NoteMarkdown.plainText(content.orEmpty())
        return if (text.length > maxLength) text.take(maxLength) + "…" else text
    }

    fun wordCount(content: String?): Int =
        NoteMarkdown.plainText(content.orEmpty()).split(whitespace).count { it.isNotEmpty() }

    fun title(title: String?, untitled: String): String =
        title?.takeIf { it.isNotBlank() } ?: untitled
}
