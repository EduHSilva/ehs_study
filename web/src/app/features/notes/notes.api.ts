import { httpClient, unwrap } from '../../composables/httpClient'
import type { DefaultResponse } from '../../composables/httpClient'
import type { Note, NoteFolder } from './types'

const LIST_PAGE_SIZE = 100

interface NotePayload {
  id: string
  folderId?: string | null
  title?: string | null
  content?: string | null
}

interface FolderPayload {
  id: string
  name?: string | null
  notes?: NotePayload[] | null
}

interface FolderPagePayload {
  items?: FolderPayload[]
}

function toNote(payload: NotePayload, fallbackFolderId: string | null): Note {
  return {
    id: payload.id,
    folderId: payload.folderId ?? fallbackFolderId,
    title: payload.title ?? '',
    content: payload.content ?? ''
  }
}

function toFolder(payload: FolderPayload): NoteFolder {
  return {
    id: payload.id,
    name: payload.name ?? '',
    notes: (payload.notes ?? []).map(note => toNote(note, payload.id))
  }
}

export async function getFolders(): Promise<NoteFolder[]> {
  const res = await httpClient<DefaultResponse<FolderPagePayload>>(`/study/notes/folders?page=0&size=${LIST_PAGE_SIZE}`)
  return (unwrap(res).items ?? []).map(toFolder)
}

export async function saveNote(id: string, patch: { title: string; content: string }): Promise<Note> {
  const payload = unwrap(
    await httpClient<DefaultResponse<NotePayload>>(`/study/notes/${id}`, { method: 'PUT', body: JSON.stringify(patch) })
  )
  return toNote(payload, null)
}

export async function createFolder(name: string): Promise<NoteFolder> {
  const payload = unwrap(
    await httpClient<DefaultResponse<FolderPayload>>('/study/notes/folders', {
      method: 'POST',
      body: JSON.stringify({ name })
    })
  )
  return toFolder(payload)
}

export async function createNote(folderId: string, title: string, content: string): Promise<Note> {
  const payload = unwrap(
    await httpClient<DefaultResponse<NotePayload>>(`/study/notes/${folderId}`, {
      method: 'POST',
      body: JSON.stringify({ title, content })
    })
  )
  return toNote(payload, folderId)
}

export async function renameFolder(id: string, name: string): Promise<NoteFolder> {
  const payload = unwrap(
    await httpClient<DefaultResponse<FolderPayload>>(`/study/notes/folders/${id}`, {
      method: 'PUT',
      body: JSON.stringify({ name })
    })
  )
  return toFolder(payload)
}

export async function deleteNote(id: string): Promise<void> {
  await httpClient<DefaultResponse<null>>(`/study/notes/${id}`, { method: 'DELETE' })
}

export async function deleteFolder(id: string): Promise<void> {
  await httpClient<DefaultResponse<null>>(`/study/notes/folders/${id}`, { method: 'DELETE' })
}
