package com.studyflow.app.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.studyflow.app.core.i18n.LocalStrings
import com.studyflow.app.core.ui.theme.StudyFlowTheme
import com.studyflow.app.domain.notes.MarkdownBlock
import com.studyflow.app.domain.notes.MarkdownSpan
import com.studyflow.app.domain.notes.NoteMarkdown

/** Prévia rolável, usada pelas notas. */
@Composable
fun MarkdownPreview(markdown: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        MarkdownBlocks(markdown)
    }
}

/**
 * Blocos de Markdown sem rolagem própria, para caber dentro de listas (detalhes da tarefa).
 * Com [onToggleCheck], os itens de checklist ficam clicáveis; o índice devolvido é o da
 * ocorrência no texto, o que permite alternar a linha sem reparsear o Markdown inteiro.
 */
@Composable
fun MarkdownBlocks(
    markdown: String,
    modifier: Modifier = Modifier,
    onToggleCheck: ((Int) -> Unit)? = null,
) {
    val s = LocalStrings.current
    val blocks = remember(markdown) { NoteMarkdown.parse(markdown) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (blocks.isEmpty()) {
            Text(
                text = s["noNotePreview"],
                style = MaterialTheme.typography.bodySmall,
                color = StudyFlowTheme.colors.subtle,
            )
        } else {
            blocks.forEach { block -> MarkdownBlockView(block, onToggleCheck) }
        }
    }
}

@Composable
private fun MarkdownBlockView(block: MarkdownBlock, onToggleCheck: ((Int) -> Unit)?) {
    val colors = StudyFlowTheme.colors
    when (block) {
        is MarkdownBlock.Heading -> Text(
            text = annotated(block.text),
            style = headingStyle(block.level),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 4.dp),
        )

        is MarkdownBlock.Paragraph -> Text(
            text = annotated(block.text),
            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
        )

        is MarkdownBlock.BulletItem -> ListRow(marker = "•", text = block.text)

        is MarkdownBlock.OrderedItem -> ListRow(marker = "${block.number}.", text = block.text)

        is MarkdownBlock.CheckItem -> CheckRow(block, onToggleCheck)

        is MarkdownBlock.Quote -> Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .background(
                    colors.primarySoft,
                    RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp),
                ),
        ) {
            Box(
                Modifier
                    .width(3.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.primary),
            )
            Text(
                text = annotated(block.text),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            )
        }

        is MarkdownBlock.Code -> Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))
                .border(1.dp, colors.border, RoundedCornerShape(10.dp))
                .padding(12.dp),
        ) {
            Text(
                text = block.code,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 18.sp,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        MarkdownBlock.Divider -> HorizontalDivider(
            color = colors.border,
            modifier = Modifier.padding(vertical = 4.dp),
        )
    }
}

@Composable
private fun ListRow(marker: String, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = marker,
            modifier = Modifier.width(20.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = annotated(text),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
        )
    }
}

@Composable
private fun CheckRow(item: MarkdownBlock.CheckItem, onToggleCheck: ((Int) -> Unit)?) {
    val clickable = if (onToggleCheck != null) {
        Modifier.toggleable(
            value = item.checked,
            role = Role.Checkbox,
            onValueChange = { onToggleCheck.invoke(item.index) },
        )
    } else {
        Modifier
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(clickable),
        verticalAlignment = Alignment.Top,
    ) {
        CheckMark(checked = item.checked)
        Spacer(Modifier.width(8.dp))
        Text(
            text = annotated(item.text),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
            color = if (item.checked) StudyFlowTheme.colors.subtle else MaterialTheme.colorScheme.onSurface,
            textDecoration = if (item.checked) TextDecoration.LineThrough else TextDecoration.None,
        )
    }
}

@Composable
private fun CheckMark(checked: Boolean) {
    val shape = RoundedCornerShape(5.dp)
    val accent = MaterialTheme.colorScheme.primary
    Box(
        modifier = Modifier
            .padding(top = 2.dp)
            .size(18.dp)
            .background(if (checked) accent else Color.Transparent, shape)
            .border(2.dp, if (checked) accent else StudyFlowTheme.colors.border, shape),
        contentAlignment = Alignment.Center,
    ) {
        if (checked) {
            Icon(
                Icons.Filled.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(12.dp),
            )
        }
    }
}

@Composable
private fun headingStyle(level: Int): TextStyle = when (level) {
    1 -> MaterialTheme.typography.headlineMedium
    2 -> MaterialTheme.typography.headlineSmall
    3 -> MaterialTheme.typography.titleLarge
    4 -> MaterialTheme.typography.titleMedium
    else -> MaterialTheme.typography.titleSmall
}

@Composable
private fun annotated(text: String): AnnotatedString {
    val spans = remember(text) { NoteMarkdown.inline(text) }
    val codeBackground = MaterialTheme.colorScheme.surfaceVariant
    val linkColor = MaterialTheme.colorScheme.primary
    return remember(spans, codeBackground, linkColor) {
        buildAnnotatedString {
            spans.forEach { span ->
                when (span) {
                    is MarkdownSpan.Text -> append(span.text)
                    is MarkdownSpan.Bold -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(span.text)
                    }

                    is MarkdownSpan.Italic -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(span.text)
                    }

                    is MarkdownSpan.Strikethrough -> withStyle(
                        SpanStyle(textDecoration = TextDecoration.LineThrough),
                    ) {
                        append(span.text)
                    }

                    is MarkdownSpan.Code -> withStyle(
                        SpanStyle(fontFamily = FontFamily.Monospace, background = codeBackground),
                    ) {
                        append(span.text)
                    }

                    is MarkdownSpan.Link -> withLink(LinkAnnotation.Url(span.url)) {
                        withStyle(
                            SpanStyle(
                                color = linkColor,
                                textDecoration = TextDecoration.Underline,
                            ),
                        ) {
                            append(span.text)
                        }
                    }
                }
            }
        }
    }
}
