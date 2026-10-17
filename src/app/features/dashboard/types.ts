import type { Task } from '../tasks/types'

export interface NotePreview { id: string; week: string; title: string; subject: string }
export interface DashboardData {
  weeklyProgress: { completed: number; total: number }
  focus: { remainingSeconds: number; subject: string; running: boolean }
  tasks: Task[]
  notes: NotePreview[]
}
