import { httpClient, unwrap } from '../../composables/httpClient'
import type { DefaultResponse } from '../../composables/httpClient'
import type { Note, NoteFolder, NotesIndex } from './types'

export async function getNotes(): Promise<NotesIndex> {
  return unwrap(await httpClient<DefaultResponse<NotesIndex>>('/notes'))
}

export async function saveNote(id: string, patch: { title: string; content: string }): Promise<Note> {
  return unwrap(await httpClient<DefaultResponse<Note>>(`/notes/${id}`, { method: 'PUT', body: JSON.stringify(patch) }))
}

export async function createFolder(name: string): Promise<NoteFolder> {
  return unwrap(
    await httpClient<DefaultResponse<NoteFolder>>('/notes/folders', { method: 'POST', body: JSON.stringify({ name }) })
  )
}
