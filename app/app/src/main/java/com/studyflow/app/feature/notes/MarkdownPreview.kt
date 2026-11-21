package com.studyflow.app.feature.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

@Composable
fun MarkdownPreview(markdown: String, modifier: Modifier = Modifier) {
    val s = LocalStrings.current
    val blocks = remember(markdown) { NoteMarkdown.parse(markdown) }
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (blocks.isEmpty()) {
            Text(
                text = s["noNotePreview"],
                style = MaterialTheme.typography.bodySmall,
                color = StudyFlowTheme.colors.subtle,
            )
        } else {
            blocks.forEach { block -> MarkdownBlockView(block) }
        }
    }
}

@Composable
private fun MarkdownBlockView(block: MarkdownBlock) {
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
