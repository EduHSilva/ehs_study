package com.studyflow.app.domain.notes

import org.junit.Assert.assertEquals
import org.junit.Test

class NoteFormatTest {

    @Test
    fun `preview achata espacos e corta com reticencias`() {
        val content = "  Primeira   linha\n\nsegunda   linha  "
        assertEquals("Primeira linha segunda linha", NoteFormat.preview(content))

        val long = "a".repeat(NoteFormat.PREVIEW_MAX_LENGTH + 5)
        assertEquals(
            "a".repeat(NoteFormat.PREVIEW_MAX_LENGTH) + "…",
            NoteFormat.preview(long),
        )
    }

    @Test
    fun `preview de conteudo vazio e vazio`() {
        assertEquals("", NoteFormat.preview(null))
        assertEquals("", NoteFormat.preview("   \n  "))
    }

    @Test
    fun `conta palavras ignorando espacos`() {
        assertEquals(0, NoteFormat.wordCount(""))
        assertEquals(0, NoteFormat.wordCount("   \n "))
        assertEquals(3, NoteFormat.wordCount("um  dois\n três"))
    }

    @Test
    fun `titulo vazio cai no fallback`() {
        assertEquals("Sem título", NoteFormat.title(null, "Sem título"))
        assertEquals("Sem título", NoteFormat.title("   ", "Sem título"))
        assertEquals("Minha nota", NoteFormat.title("Minha nota", "Sem título"))
    }
}
