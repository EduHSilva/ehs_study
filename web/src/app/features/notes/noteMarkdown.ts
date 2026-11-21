import DOMPurify from 'dompurify'
import { marked } from 'marked'

marked.setOptions({ gfm: true, breaks: true })

export const renderNoteMarkdown = (markdown: string): string =>
  DOMPurify.sanitize(marked.parse(markdown, { async: false }))

// Prévia e contagem de palavras ignoram a sintaxe do Markdown (marcadores de título,
// listas, ênfase e links), para o cartão não exibir `**` nem contar símbolos como palavras.
export const plainNoteText = (markdown: string): string =>
  markdown
    .replace(/```[\s\S]*?```/g, ' ')
    .replace(/`([^`]*)`/g, '$1')
    .replace(/^#{1,6}\s+/gm, '')
    .replace(/^\s*([-*+]|\d+\.)\s+/gm, '')
    .replace(/^>\s?/gm, '')
    .replace(/!\[([^\]]*)\]\([^)]*\)/g, '$1')
    .replace(/\[([^\]]*)\]\([^)]*\)/g, '$1')
    .replace(/(\*\*|__|~~|\*|_)/g, '')
    .replace(/\s+/g, ' ')
    .trim()
