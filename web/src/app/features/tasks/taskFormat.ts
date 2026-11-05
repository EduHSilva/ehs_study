export function formatEndDate(endDate: string | null): string {
  if (!endDate) return '—'
  const date = new Date(endDate)
  if (Number.isNaN(date.getTime())) return endDate
  const day = String(date.getDate()).padStart(2, '0')
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const hour = String(date.getHours()).padStart(2, '0')
  const minute = String(date.getMinutes()).padStart(2, '0')
  return `${day}/${month}, ${hour}:${minute}`
}

export function localInputToIso(value: string): string | null {
  if (!value) return null
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? null : date.toISOString()
}

const dayStamp = (date: Date) => {
  const day = String(date.getDate()).padStart(2, '0')
  const month = String(date.getMonth() + 1).padStart(2, '0')
  return `${day}/${month}`
}

const dayKeyOf = (value: string) => {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return null
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${date.getFullYear()}-${month}-${day}`
}

// Rótulo do período ocupado pela task. Uma UNIQUE que ocupa um intervalo mostra
// "05/10, 09:00 → 09/10, 18:00"; quando o início e o fim caem no mesmo dia, mostra só o dia.
export function formatTaskPeriod(startDate: string | null, endDate: string | null): string {
  const end = formatEndDate(endDate)
  if (!startDate) return end
  const start = new Date(startDate)
  if (Number.isNaN(start.getTime())) return end
  if (!endDate) return formatEndDate(startDate)
  if (dayKeyOf(startDate) === dayKeyOf(endDate)) return end
  return `${dayStamp(start)} → ${end}`
}