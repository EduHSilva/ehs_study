import { computed, ref } from 'vue'
import { useI18n } from '../../composables/useI18n'
import { onSessionCacheReset } from '../../composables/useSessionCache'
import { createFolder, createNote as createNoteRequest, deleteFolder, deleteNote, getFolders, renameFolder as renameFolderRequest, saveNote } from './notes.api'
import type { NoteFolder } from './types'

// Nota que outra tela (o dashboard) pediu para abrir. O pedido é consumido no próximo `load`,
// quando as pastas chegam e dá para localizar a nota.
const requestedNoteId = ref<string | null>(null)

export const requestOpenNote = (id: string) => {
  requestedNoteId.value = id
}

// Cache de módulo: pastas e seleção sobrevivem à troca de tela; ao voltar, a tela aparece na
// hora e a lista é revalidada em segundo plano.
const folders = ref<NoteFolder[]>([])
const loading = ref(true)
const error = ref<string | null>(null)
const selectedFolderId = ref<string | null>(null)
const selectedNoteId = ref<string | null>(null)
let loaded = false
let inFlight: Promise<void> | null = null
let cacheEpoch = 0

onSessionCacheReset(() => {
  cacheEpoch += 1
  loaded = false
  inFlight = null
  requestedNoteId.value = null
  folders.value = []
  selectedFolderId.value = null
  selectedNoteId.value = null
  loading.value = true
  error.value = null
})

export function useNotes() {
  const { t } = useI18n()
  const saving = ref(false)

  const selectedFolder = computed(() => folders.value.find(folder => folder.id === selectedFolderId.value) ?? null)
  const folderNotes = computed(() => selectedFolder.value?.notes ?? [])
  const selectedNote = computed(() => folderNotes.value.find(note => note.id === selectedNoteId.value) ?? null)

  const load = (): Promise<void> => {
    // Prefetch disparado pelo hover pode estar em andamento quando a tela pede uma nota
    // específica; nesse caso espera a busca atual e roda de novo já com o pedido.
    if (inFlight) return inFlight.then(() => (requestedNoteId.value ? load() : undefined))
    const epoch = cacheEpoch
    const requested = requestedNoteId.value
    requestedNoteId.value = null
    loading.value = !loaded
    error.value = null
    inFlight = (async () => {
      try {
        const nextFolders = await getFolders()
        if (epoch !== cacheEpoch) return
        folders.value = nextFolders
        const requestedFolder = requested
          ? nextFolders.find(folder => folder.notes.some(note => note.id === requested)) ?? null
          : null
        if (requestedFolder) {
          selectedFolderId.value = requestedFolder.id
          selectedNoteId.value = requested
        } else if (!nextFolders.some(folder => folder.id === selectedFolderId.value)) {
          // Sem seleção válida (primeira visita ou pasta removida), abre a primeira pasta.
          const firstFolder = nextFolders[0] ?? null
          selectedFolderId.value = firstFolder?.id ?? null
          selectedNoteId.value = firstFolder?.notes[0]?.id ?? null
        } else if (!folderNotes.value.some(note => note.id === selectedNoteId.value)) {
          selectedNoteId.value = folderNotes.value[0]?.id ?? null
        }
        loaded = true
      } catch {
        if (epoch !== cacheEpoch) return
        error.value = t('notesLoadFailed')
      } finally {
        if (epoch === cacheEpoch) {
          loading.value = false
          inFlight = null
        }
      }
    })()
    return inFlight
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
