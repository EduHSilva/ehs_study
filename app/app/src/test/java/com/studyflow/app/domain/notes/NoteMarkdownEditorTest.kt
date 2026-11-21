package com.studyflow.app.domain.notes

import org.junit.Assert.assertEquals
import org.junit.Test

class NoteMarkdownEditorTest {

    @Test
    fun `envolve a selecao e mantem o texto selecionado`() {
        assertEquals(
            MarkdownEdit("um **negrito** aqui", 5, 12),
            NoteMarkdownEditor.wrap("um negrito aqui", 3, 10, "**"),
        )
    }

    @Test
    fun `sem selecao posiciona o cursor entre os marcadores`() {
        assertEquals(
            MarkdownEdit("um **** aqui", 5, 5),
            NoteMarkdownEditor.wrap("um  aqui", 3, 3, "**"),
        )
    }

    @Test
    fun `prefixa todas as linhas tocadas pela selecao`() {
        assertEquals(
            MarkdownEdit("- um\n- dois", 0, 11),
            NoteMarkdownEditor.prefixLines("um\ndois", 0, 7, "- "),
        )
    }

    @Test
    fun `prefixo existente e removido e substitui outro marcador`() {
        assertEquals(
            MarkdownEdit("um\ndois", 0, 7),
            NoteMarkdownEditor.prefixLines("- um\n- dois", 0, 11, "- "),
        )
        assertEquals(
            MarkdownEdit("- um\n- dois", 0, 11),
            NoteMarkdownEditor.prefixLines("# um\n> dois", 0, 10, "- "),
        )
    }

    @Test
    fun `titulo alterna entre niveis e remove marcadores antigos`() {
        assertEquals(
            MarkdownEdit("## um\n## dois", 0, 13),
            NoteMarkdownEditor.heading("# um\ndois", 0, 8, 2),
        )
        assertEquals(
            MarkdownEdit("um\n## dois", 0, 10),
            NoteMarkdownEditor.heading("## um\ndois", 0, 9, 2),
        )
        assertEquals(
            MarkdownEdit("# um\n# dois", 0, 11),
            NoteMarkdownEditor.heading("um\ndois", 0, 7, 1),
        )
    }

    @Test
    fun `selecao comeca depois da ultima quebra de linha`() {
        assertEquals(
            MarkdownEdit("a\n- b", 2, 5),
            NoteMarkdownEditor.prefixLines("a\nb", 2, 3, "- "),
        )
    }
}
