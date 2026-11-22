import DOMPurify from 'dompurify'
import { marked } from 'marked'

export const MAX_DETAILS_LENGTH = 2000

marked.setOptions({ gfm: true, breaks: true })

// Mesma sintaxe de checklist aceita pelo `marked` (GFM); o índice da ocorrência é o que liga o
// checkbox renderizado de volta à linha do texto original.
const CHECK_ITEM = /^([ \t]*(?:[-*+]|\d+[.)])[ \t]+\[)([ xX])(\])/gm

// Renderiza os detalhes com o mesmo Markdown das notas e deixa os checkboxes clicáveis: cada um
// carrega o índice da ocorrência no texto original, usado por `toggleDetailsCheck`.
export function renderTaskDetails(markdown: string): string {
  const template = document.createElement('template')
  template.innerHTML = DOMPurify.sanitize(marked.parse(markdown, { async: false }))
  template.content.querySelectorAll<HTMLInputElement>('input[type="checkbox"]').forEach((input, index) => {
    input.removeAttribute('disabled')
    input.dataset.checkIndex = String(index)
  })
  template.content.querySelectorAll('a').forEach(link => {
    link.target = '_blank'
    link.rel = 'noopener noreferrer'
  })
  return template.innerHTML
}

export function toggleDetailsCheck(markdown: string, index: number): string {
  let current = 0
  return markdown.replace(CHECK_ITEM, (item, prefix: string, mark: string, close: string) => {
    if (current++ !== index) return item
    return `${prefix}${mark === ' ' ? 'x' : ' '}${close}`
  })
}
