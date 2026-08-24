import { httpClient } from '../../composables/httpClient'
import type { Note, NoteFolder, NotesIndex } from './types'

export async function getNotes(): Promise<NotesIndex> {
  return httpClient<NotesIndex>('/notes')
}

export async function saveNote(id: string, patch: { title: string; content: string }): Promise<Note> {
  return httpClient<Note>(`/notes/${id}`, { method: 'PUT', body: JSON.stringify(patch) })
}

export async function createFolder(name: string): Promise<NoteFolder> {
  return httpClient<NoteFolder>('/notes/folders', { method: 'POST', body: JSON.stringify({ name }) })
}
