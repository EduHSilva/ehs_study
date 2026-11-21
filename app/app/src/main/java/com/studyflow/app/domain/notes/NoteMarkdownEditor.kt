package com.studyflow.app.domain.notes

/** Resultado de uma ação de formatação: novo texto e seleção a restaurar no campo. */
data class MarkdownEdit(
    val text: String,
    val selectionStart: Int,
    val selectionEnd: Int,
)

/** Ações da barra de formatação, iguais às de `useNoteEditor.ts` no web. */
object NoteMarkdownEditor {
    private val LINE_MARKER = Regex("^(#{1,6}\\s+|[-*+]\\s+|\\d+\\.\\s+|>\\s?)")

    fun wrap(text: String, selectionStart: Int, selectionEnd: Int, marker: String): MarkdownEdit {
        val start = selectionStart.coerceIn(0, text.length)
        val end = selectionEnd.coerceIn(start, text.length)
        val selected = text.substring(start, end)
        val next = text.substring(0, start) + marker + selected + marker + text.substring(end)
        val cursor = start + marker.length
        return MarkdownEdit(next, cursor, cursor + selected.length)
    }

    fun prefixLines(text: String, selectionStart: Int, selectionEnd: Int, prefix: String): MarkdownEdit =
        mapSelectedLines(text, selectionStart, selectionEnd) { line ->
            if (line.startsWith(prefix)) {
                line.substring(prefix.length)
            } else {
                prefix + line.replaceFirst(LINE_MARKER, "")
            }
        }

    fun heading(text: String, selectionStart: Int, selectionEnd: Int, level: Int): MarkdownEdit {
        val marker = "#".repeat(level.coerceIn(1, 6)) + " "
        return mapSelectedLines(text, selectionStart, selectionEnd) { line ->
            if (line.startsWith(marker)) {
                line.substring(marker.length)
            } else {
                marker + line.replaceFirst(LINE_MARKER, "")
            }
        }
    }

    // Aplica a transformação nas linhas tocadas pela seleção, para títulos e listas funcionarem
    // mesmo sem selecionar o texto todo.
    private fun mapSelectedLines(
        text: String,
        selectionStart: Int,
        selectionEnd: Int,
        transform: (String) -> String,
    ): MarkdownEdit {
        val start = selectionStart.coerceIn(0, text.length)
        val end = selectionEnd.coerceIn(start, text.length)
        val lineStart = text.substring(0, start).lastIndexOf('\n') + 1
        val nextBreak = text.indexOf('\n', end)
        val lineEnd = if (nextBreak == -1) text.length else nextBreak
        val block = text.substring(lineStart, lineEnd)
        val mapped = block.split('\n').joinToString("\n", transform = transform)
        val next = text.substring(0, lineStart) + mapped + text.substring(lineEnd)
        return MarkdownEdit(next, lineStart, lineStart + mapped.length)
    }
}
