import { httpClient } from '../../composables/httpClient'
import type { Reminder, ReminderInput } from './types'

export async function getReminders(): Promise<Reminder[]> {
  return httpClient<Reminder[]>('/reminders')
}

export async function createReminder(input: ReminderInput): Promise<Reminder> {
  return httpClient<Reminder>('/reminders', { method: 'POST', body: JSON.stringify({ priority: 'Média', ...input }) })
}

export async function updateReminder(id: string, completed: boolean): Promise<Reminder> {
  return httpClient<Reminder>(`/reminders/${id}`, { method: 'PATCH', body: JSON.stringify({ completed }) })
}
