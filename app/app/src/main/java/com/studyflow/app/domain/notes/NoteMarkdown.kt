package com.studyflow.app.domain.notes

/** Bloco de Markdown já classificado, pronto para a prévia renderizar no Compose. */
sealed interface MarkdownBlock {
    data class Heading(val level: Int, val text: String) : MarkdownBlock
    data class Paragraph(val text: String) : MarkdownBlock
    data class BulletItem(val text: String) : MarkdownBlock
    data class OrderedItem(val number: Int, val text: String) : MarkdownBlock
    data class Quote(val text: String) : MarkdownBlock
    data class Code(val code: String) : MarkdownBlock
    data object Divider : MarkdownBlock
}

/** Trecho inline: o texto continua legível mesmo quando a interface não aplica o estilo. */
sealed interface MarkdownSpan {
    data class Text(val text: String) : MarkdownSpan
    data class Bold(val text: String) : MarkdownSpan
    data class Italic(val text: String) : MarkdownSpan
    data class Strikethrough(val text: String) : MarkdownSpan
    data class Code(val text: String) : MarkdownSpan
    data class Link(val text: String, val url: String) : MarkdownSpan
}

/**
 * Subconjunto de Markdown aceito pelo editor de notas, espelhando o que o web renderiza com
 * `marked`: títulos, listas, citação, código, régua e ênfase/links inline.
 */
object NoteMarkdown {
    private val heading = Regex("^(#{1,6})\\s+(.*)$")
    private val ordered = Regex("^(\\d+)[.)]\\s+(.*)$")
    private val bullet = Regex("^[-*+]\\s+(.*)$")
    private val quote = Regex("^>\\s?(.*)$")
    private val divider = Regex("^([-*_])(\\s*\\1){2,}\\s*$")
    private val whitespace = Regex("\\s+")

    fun parse(markdown: String): List<MarkdownBlock> {
        val blocks = mutableListOf<MarkdownBlock>()
        val lines = markdown.replace("\r\n", "\n").replace('\r', '\n').split('\n')
        val paragraph = StringBuilder()
        var index = 0

        fun flushParagraph() {
            val text = paragraph.toString().trim()
            paragraph.clear()
            if (text.isNotEmpty()) blocks += MarkdownBlock.Paragraph(text)
        }

        while (index < lines.size) {
            val line = lines[index]
            val trimmed = line.trim()
            when {
                trimmed.startsWith("```") -> {
                    flushParagraph()
                    val code = StringBuilder()
                    index++
                    while (index < lines.size && !lines[index].trim().startsWith("```")) {
                        if (code.isNotEmpty()) code.append('\n')
                        code.append(lines[index])
                        index++
                    }
                    blocks += MarkdownBlock.Code(code.toString())
                }

                trimmed.isEmpty() -> flushParagraph()

                else -> {
                    val headingMatch = heading.matchEntire(trimmed)
                    val orderedMatch = ordered.matchEntire(trimmed)
                    val bulletMatch = bullet.matchEntire(trimmed)
                    val quoteMatch = quote.matchEntire(trimmed)
                    when {
                        headingMatch != null -> {
                            flushParagraph()
                            blocks += MarkdownBlock.Heading(
                                level = headingMatch.groupValues[1].length,
                                text = headingMatch.groupValues[2].trim(),
                            )
                        }

                        divider.matches(trimmed) -> {
                            flushParagraph()
                            blocks += MarkdownBlock.Divider
                        }

                        quoteMatch != null -> {
                            flushParagraph()
                            val quoted = mutableListOf<String>()
                            var cursor = index
                            while (cursor < lines.size) {
                                val match = quote.matchEntire(lines[cursor].trim()) ?: break
                                quoted += match.groupValues[1].trim()
                                cursor++
                            }
                            blocks += MarkdownBlock.Quote(quoted.joinToString(" ").trim())
                            index = cursor - 1
                        }

                        bulletMatch != null -> {
                            flushParagraph()
                            blocks += MarkdownBlock.BulletItem(bulletMatch.groupValues[1].trim())
                        }

                        orderedMatch != null -> {
                            flushParagraph()
                            blocks += MarkdownBlock.OrderedItem(
                                number = orderedMatch.groupValues[1].toIntOrNull() ?: 1,
                                text = orderedMatch.groupValues[2].trim(),
                            )
                        }

                        else -> {
                            if (paragraph.isNotEmpty()) paragraph.append(' ')
                            paragraph.append(trimmed)
                        }
                    }
                }
            }
            index++
        }
        flushParagraph()
        return blocks
    }

    /** Texto exibido em cartões, prévias e contagem de palavras, sem a sintaxe do Markdown. */
    fun plainText(markdown: String): String =
        parse(markdown)
            .joinToString(" ") { block ->
                when (block) {
                    is MarkdownBlock.Heading -> visibleText(block.text)
                    is MarkdownBlock.Paragraph -> visibleText(block.text)
                    is MarkdownBlock.BulletItem -> visibleText(block.text)
                    is MarkdownBlock.OrderedItem -> visibleText(block.text)
                    is MarkdownBlock.Quote -> visibleText(block.text)
                    is MarkdownBlock.Code -> block.code
                    MarkdownBlock.Divider -> ""
                }
            }
            .replace(whitespace, " ")
            .trim()

    fun inline(text: String): List<MarkdownSpan> {
        val spans = mutableListOf<MarkdownSpan>()
        val literal = StringBuilder()
        var index = 0

        fun flushLiteral() {
            if (literal.isNotEmpty()) {
                spans += MarkdownSpan.Text(literal.toString())
                literal.clear()
            }
        }

        while (index < text.length) {
            val strong = when {
                text.startsWith("**", index) -> "**"
                text.startsWith("__", index) -> "__"
                else -> null
            }
            if (strong != null) {
                val end = text.indexOf(strong, index + strong.length)
                if (end > index + strong.length) {
                    flushLiteral()
                    spans += MarkdownSpan.Bold(text.substring(index + strong.length, end))
                    index = end + strong.length
                    continue
                }
            }

            if (text.startsWith("~~", index)) {
                val end = text.indexOf("~~", index + 2)
                if (end > index + 2) {
                    flushLiteral()
                    spans += MarkdownSpan.Strikethrough(text.substring(index + 2, end))
                    index = end + 2
                    continue
                }
            }

            if (text[index] == '`') {
                val end = text.indexOf('`', index + 1)
                if (end > index + 1) {
                    flushLiteral()
                    spans += MarkdownSpan.Code(text.substring(index + 1, end))
                    index = end + 1
                    continue
                }
            }

            if (text[index] == '[') {
                val close = text.indexOf(']', index + 1)
                if (close > index && text.getOrNull(close + 1) == '(') {
                    val urlEnd = text.indexOf(')', close + 2)
                    if (urlEnd > close + 1) {
                        flushLiteral()
                        spans += MarkdownSpan.Link(
                            text = text.substring(index + 1, close),
                            url = text.substring(close + 2, urlEnd),
                        )
                        index = urlEnd + 1
                        continue
                    }
                }
            }

            val emphasis = when (text[index]) {
                '*' -> "*"
                '_' -> "_"
                else -> null
            }
            if (emphasis != null &&
                text.getOrNull(index - 1) != emphasis.first() &&
                text.getOrNull(index + 1) != emphasis.first()
            ) {
                val end = text.indexOf(emphasis, index + 1)
                if (end > index + 1 && text.getOrNull(end + 1) != emphasis.first()) {
                    flushLiteral()
                    spans += MarkdownSpan.Italic(text.substring(index + 1, end))
                    index = end + 1
                    continue
                }
            }

            literal.append(text[index])
            index++
        }
        flushLiteral()
        return spans
    }

    private fun visibleText(text: String): String =
        inline(text).joinToString("") { span ->
            when (span) {
                is MarkdownSpan.Text -> span.text
                is MarkdownSpan.Bold -> span.text
                is MarkdownSpan.Italic -> span.text
                is MarkdownSpan.Strikethrough -> span.text
                is MarkdownSpan.Code -> span.text
                is MarkdownSpan.Link -> span.text
            }
        }
}
