import type { Task } from '../tasks/types'

export interface NotePreview { id: string; title: string; content: string }
export interface DashboardData {
  weeklyProgress: { completed: number; total: number }
  tasks: Task[]
  notes: NotePreview[]
}
