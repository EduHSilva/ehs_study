package com.studyflow.app.domain.notes

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NoteMarkdownTest {

    @Test
    fun `classifica titulos listas citacao codigo e regua`() {
        val blocks = NoteMarkdown.parse(
            """
            # Título
            Texto do parágrafo.

            - Primeiro
            * Segundo
            1. Numerado
            2) Outro

            > Citação
            > em duas linhas

            ```
            val x = 1
            ```

            ---
            """.trimIndent(),
        )

        assertEquals(MarkdownBlock.Heading(1, "Título"), blocks[0])
        assertEquals(MarkdownBlock.Paragraph("Texto do parágrafo."), blocks[1])
        assertEquals(MarkdownBlock.BulletItem("Primeiro"), blocks[2])
        assertEquals(MarkdownBlock.BulletItem("Segundo"), blocks[3])
        assertEquals(MarkdownBlock.OrderedItem(1, "Numerado"), blocks[4])
        assertEquals(MarkdownBlock.OrderedItem(2, "Outro"), blocks[5])
        assertEquals(MarkdownBlock.Quote("Citação em duas linhas"), blocks[6])
        assertEquals(MarkdownBlock.Code("val x = 1"), blocks[7])
        assertTrue(blocks[8] is MarkdownBlock.Divider)
    }

    @Test
    fun `classifica checklist e alterna pelo indice`() {
        val markdown = "- [ ] Aula 1\n- [x] **Aula 2**\n1. [ ] Ler"
        assertEquals(
            listOf(
                MarkdownBlock.CheckItem(checked = false, index = 0, text = "Aula 1"),
                MarkdownBlock.CheckItem(checked = true, index = 1, text = "**Aula 2**"),
                MarkdownBlock.CheckItem(checked = false, index = 2, text = "Ler"),
            ),
            NoteMarkdown.parse(markdown),
        )
        assertEquals(
            "- [x] Aula 1\n- [x] **Aula 2**\n1. [ ] Ler",
            NoteMarkdown.toggleCheckItem(markdown, 0),
        )
        assertEquals(
            "- [ ] Aula 1\n- [x] **Aula 2**\n1. [x] Ler",
            NoteMarkdown.toggleCheckItem(markdown, 2),
        )
        assertEquals(markdown, NoteMarkdown.toggleCheckItem(markdown, 9))
    }

    @Test
    fun `junta linhas soltas em um paragrafo`() {
        assertEquals(
            listOf(MarkdownBlock.Paragraph("Primeira linha segunda linha")),
            NoteMarkdown.parse("Primeira linha\nsegunda linha"),
        )
    }

    @Test
    fun `interpreta enfase codigo e link inline`() {
        assertEquals(
            listOf(
                MarkdownSpan.Bold("negrito"),
                MarkdownSpan.Text(" e "),
                MarkdownSpan.Italic("itálico"),
                MarkdownSpan.Text(" com "),
                MarkdownSpan.Link("link", "https://exemplo.com"),
                MarkdownSpan.Text(" e "),
                MarkdownSpan.Code("código"),
                MarkdownSpan.Text(" e "),
                MarkdownSpan.Strikethrough("riscado"),
            ),
            NoteMarkdown.inline("**negrito** e *itálico* com [link](https://exemplo.com) e `código` e ~~riscado~~"),
        )
    }

    @Test
    fun `texto puro remove marcadores e preserva o conteudo do codigo`() {
        val markdown = "# Título\n\n- **Um**\n- [Dois](https://exemplo.com)\n\n```\nfun main() {}\n```"
        assertEquals("Título Um Dois fun main() {}", NoteMarkdown.plainText(markdown))
    }

    @Test
    fun `markers sem fechamento continuam literais`() {
        assertEquals(listOf(MarkdownSpan.Text("**aberto e *solto")), NoteMarkdown.inline("**aberto e *solto"))
    }
}
