import type { Reminder } from '../reminders/types'

export interface NotePreview { id: string; week: string; title: string; subject: string }
export interface DashboardData {
  weeklyProgress: { completed: number; total: number }
  focus: { remainingSeconds: number; subject: string; running: boolean }
  reminders: Reminder[]
  notes: NotePreview[]
}
