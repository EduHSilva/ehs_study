import { computed, ref } from 'vue'
import { useI18n } from '../../composables/useI18n'
import { createFolder, getNotes, saveNote } from './notes.api'
import type { Note, NoteFolder } from './types'

export function useNotes() {
  const { t } = useI18n()
  const folders = ref<NoteFolder[]>([])
  const noteItems = ref<Note[]>([])
  const loading = ref(true)
  const saving = ref(false)
  const error = ref<string | null>(null)
  const selectedFolderId = ref<string | null>(null)
  const selectedNoteId = ref<string | null>(null)

  const folderNotes = computed(() => noteItems.value.filter(note => note.folderId === selectedFolderId.value))
  const selectedNote = computed(() => noteItems.value.find(note => note.id === selectedNoteId.value) ?? null)

  const load = async () => {
    try {
      loading.value = true
      error.value = null
      const index = await getNotes()
      folders.value = index.folders
      noteItems.value = index.notes
      selectedFolderId.value = index.folders[0]?.id ?? null
      selectedNoteId.value = index.notes.find(note => note.folderId === selectedFolderId.value)?.id ?? null
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

  return { folders, noteItems, folderNotes, selectedNote, loading, saving, error, selectedFolderId, selectedNoteId, load, selectFolder, selectNote, saveSelected, addFolder }
}
