import { computed, ref } from 'vue'
import { useI18n } from '../../composables/useI18n'
import { createFolder, createNote as createNoteRequest, deleteFolder, deleteNote, getFolders, renameFolder as renameFolderRequest, saveNote } from './notes.api'
import type { NoteFolder } from './types'

export function useNotes() {
  const { t } = useI18n()
  const folders = ref<NoteFolder[]>([])
  const loading = ref(true)
  const saving = ref(false)
  const error = ref<string | null>(null)
  const selectedFolderId = ref<string | null>(null)
  const selectedNoteId = ref<string | null>(null)

  const selectedFolder = computed(() => folders.value.find(folder => folder.id === selectedFolderId.value) ?? null)
  const folderNotes = computed(() => selectedFolder.value?.notes ?? [])
  const selectedNote = computed(() => folderNotes.value.find(note => note.id === selectedNoteId.value) ?? null)

  const load = async () => {
    try {
      loading.value = true
      error.value = null
      folders.value = await getFolders()
      const firstFolder = folders.value[0] ?? null
      selectedFolderId.value = firstFolder?.id ?? null
      selectedNoteId.value = firstFolder?.notes[0]?.id ?? null
    } catch {
      error.value = t('notesLoadFailed')
    } finally {
      loading.value = false
    }
  }

  const selectFolder = (id: string) => {
    selectedFolderId.value = id
    selectedNoteId.value = folderNotes.value[0]?.id ?? null
  }

  const selectNote = (id: string) => {
    selectedNoteId.value = id
  }

  const saveSelected = async (title: string, content: string): Promise<boolean> => {
    if (!selectedNote.value) return false
    saving.value = true
    error.value = null
    try {
      Object.assign(selectedNote.value, await saveNote(selectedNote.value.id, { title, content }))
      return true
    } catch {
      error.value = t('noteSaveFailed')
      return false
    } finally {
      saving.value = false
    }
  }

  const addNote = async (title: string, content: string): Promise<boolean> => {
    const folder = selectedFolder.value
    if (!folder) return false
    if (!title.trim()) {
      error.value = t('noteTitleRequired')
      return false
    }
    saving.value = true
    error.value = null
    try {
      const note = await createNoteRequest(folder.id, title.trim(), content)
      folder.notes.push(note)
      selectedNoteId.value = note.id
      return true
    } catch {
      error.value = t('noteCreateFailed')
      return false
    } finally {
      saving.value = false
    }
  }

  const addFolder = async () => {
    error.value = null
    try {
      const folder = await createFolder(`Nova pasta ${folders.value.length + 1}`)
      folders.value.push(folder)
      selectFolder(folder.id)
    } catch {
      error.value = t('folderCreateFailed')
    }
  }

  const removeSelectedNote = async () => {
    const note = selectedNote.value
    if (!note) return
    error.value = null
    try {
      await deleteNote(note.id)
      const folder = selectedFolder.value
      if (folder) folder.notes = folder.notes.filter(item => item.id !== note.id)
      selectedNoteId.value = folderNotes.value[0]?.id ?? null
    } catch {
      error.value = t('noteDeleteFailed')
    }
  }

  const removeFolder = async (id: string) => {
    error.value = null
    try {
      await deleteFolder(id)
      folders.value = folders.value.filter(folder => folder.id !== id)
      if (selectedFolderId.value === id) {
        const firstFolder = folders.value[0] ?? null
        selectedFolderId.value = firstFolder?.id ?? null
        selectedNoteId.value = firstFolder?.notes[0]?.id ?? null
      }
    } catch {
      error.value = t('folderDeleteFailed')
    }
  }

  const renameFolder = async (id: string, name: string) => {
    const folder = folders.value.find(item => item.id === id)
    if (!folder || !name.trim()) return false
    error.value = null
    try {
      Object.assign(folder, await renameFolderRequest(id, name.trim()))
      return true
    } catch {
      error.value = t('folderRenameFailed')
      return false
    }
  }

  return { folders, folderNotes, selectedFolder, selectedNote, loading, saving, error, selectedFolderId, selectedNoteId, load, selectFolder, selectNote, saveSelected, addFolder, addNote, removeSelectedNote, removeFolder, renameFolder }
}
