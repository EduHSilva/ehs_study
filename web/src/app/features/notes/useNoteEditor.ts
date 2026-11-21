import { computed, nextTick, onBeforeUnmount, onMounted, ref } from 'vue'
import type { Ref } from 'vue'
import { renderNoteMarkdown } from './noteMarkdown'

const LINE_MARKER = /^(#{1,6}\s+|[-*+]\s+|\d+\.\s+|>\s?)/

export function useNoteEditor(content: Ref<string>) {
  const textarea = ref<HTMLTextAreaElement | null>(null)
  const previewing = ref(false)
  const expanded = ref(false)
  const previewHtml = computed(() => renderNoteMarkdown(content.value))

  const focusSelection = (start: number, end = start) => {
    nextTick(() => {
      const el = textarea.value
      if (!el) return
      el.focus()
      el.setSelectionRange(start, end)
    })
  }

  const wrapSelection = (marker: string) => {
    const el = textarea.value
    if (!el) return
    const start = el.selectionStart
    const end = el.selectionEnd
    const selected = content.value.slice(start, end)
    content.value = `${content.value.slice(0, start)}${marker}${selected}${marker}${content.value.slice(end)}`
    focusSelection(start + marker.length, start + marker.length + selected.length)
  }

  // Aplica a transformação nas linhas tocadas pela seleção, para títulos e listas
  // funcionarem mesmo sem selecionar o texto todo.
  const mapSelectedLines = (transform: (line: string) => string) => {
    const el = textarea.value
    if (!el) return
    const start = el.selectionStart
    const end = el.selectionEnd
    const lineStart = content.value.slice(0, start).lastIndexOf('\n') + 1
    const breakAfter = content.value.indexOf('\n', end)
    const lineEnd = breakAfter === -1 ? content.value.length : breakAfter
    const next = content.value.slice(lineStart, lineEnd).split('\n').map(transform).join('\n')
    content.value = content.value.slice(0, lineStart) + next + content.value.slice(lineEnd)
    focusSelection(lineStart, lineStart + next.length)
  }

  const prefixLines = (prefix: string) => mapSelectedLines(line =>
    line.startsWith(prefix) ? line.slice(prefix.length) : prefix + line.replace(LINE_MARKER, '')
  )

  const heading = (level: number) => {
    const marker = `${'#'.repeat(level)} `
    mapSelectedLines(line =>
      line.startsWith(marker) ? line.slice(marker.length) : marker + line.replace(LINE_MARKER, '')
    )
  }

  const onContentKeydown = (event: KeyboardEvent) => {
    if (!(event.ctrlKey || event.metaKey) || event.altKey) return
    const key = event.key.toLowerCase()
    if (key === 'b') {
      event.preventDefault()
      wrapSelection('**')
    } else if (key === 'i') {
      event.preventDefault()
      wrapSelection('*')
    }
  }

  const togglePreview = () => { previewing.value = !previewing.value }
  const toggleExpanded = () => { expanded.value = !expanded.value }

  const onWindowKeydown = (event: KeyboardEvent) => {
    if (event.key === 'Escape' && expanded.value) expanded.value = false
  }
  onMounted(() => window.addEventListener('keydown', onWindowKeydown))
  onBeforeUnmount(() => window.removeEventListener('keydown', onWindowKeydown))

  return { textarea, previewing, expanded, previewHtml, wrapSelection, prefixLines, heading, onContentKeydown, togglePreview, toggleExpanded }
}
