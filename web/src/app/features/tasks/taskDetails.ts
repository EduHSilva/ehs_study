export const MAX_DETAILS_LENGTH = 2000

const URL_PATTERN = /https?:\/\/[^\s<>"']+/gi
const TRAILING_PUNCTUATION = /[.,;:!?]+$/

export interface DetailsSegment {
  text: string
  href?: string
}

// Quebra os detalhes em texto e URLs para renderizar links de verdade, sem `v-html`.
export function detailsSegments(text: string): DetailsSegment[] {
  const segments: DetailsSegment[] = []
  let cursor = 0
  for (const match of text.matchAll(URL_PATTERN)) {
    const start = match.index ?? 0
    const raw = match[0]
    const url = raw.replace(TRAILING_PUNCTUATION, '')
    if (!url) continue
    if (start > cursor) segments.push({ text: text.slice(cursor, start) })
    segments.push({ text: url, href: url })
    const trailing = raw.slice(url.length)
    if (trailing) segments.push({ text: trailing })
    cursor = start + raw.length
  }
  if (cursor < text.length) segments.push({ text: text.slice(cursor) })
  return segments
}
