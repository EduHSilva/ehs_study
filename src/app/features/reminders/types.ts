export type Priority = 'Alta' | 'Média' | 'Baixa'

export interface Reminder { id: string; title: string; schedule: string; subject: string; priority: Priority; completed: boolean }
export interface ReminderInput { title: string; schedule: string; subject: string; priority?: Priority }
