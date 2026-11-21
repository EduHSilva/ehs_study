<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import PageSkeleton from '../components/PageSkeleton.vue'
import ThemeToggle from '../components/ThemeToggle.vue'; import LanguageToggle from '../components/LanguageToggle.vue'; import { useI18n } from '../composables/useI18n'; import type { AppPage } from '../composables/useNavigation'
import { useNotes } from '../features/notes/useNotes'
import { plainNoteText } from '../features/notes/noteMarkdown'
import { useNoteEditor } from '../features/notes/useNoteEditor'
import { setNavigationGuard } from '../composables/useNavigation'

defineEmits<{ navigate:[page:AppPage] }>()
const { t } = useI18n()
const { folders, folderNotes, selectedFolder, loading, saving, error, selectedFolderId, selectedNoteId, load, selectFolder, selectNote, saveSelected, addFolder, addNote, removeSelectedNote, removeFolder, moveNote, renameFolder } = useNotes()

const saved = ref(t('savedEarlier')); const title = ref(''); const content = ref('')
const editingFolderId = ref<string | null>(null); const folderName = ref('')
const creatingNote = ref(false)
const { textarea, previewing, expanded, previewHtml, wrapSelection, prefixLines, heading, onContentKeydown, togglePreview, toggleExpanded } = useNoteEditor(content)

const notePreview = (value: string) => {
  const text = plainNoteText(value).replace(/\s+/g, ' ')
  return text.length > 110 ? `${text.slice(0, 110)}…` : text
}
const noteWordCount = (value: string) => plainNoteText(value).split(/\s+/).filter(Boolean).length

const storedNote = computed(() => folderNotes.value.find(note => note.id === selectedNoteId.value) ?? null)
const isDirty = computed(() => {
  if (creatingNote.value) return title.value.trim() !== '' || content.value.trim() !== ''
  const note = storedNote.value
  return !!note && (title.value !== note.title || content.value !== note.content)
})

const unsavedDialogOpen = ref(false)
const unsavedDialog = ref<HTMLElement | null>(null)
let resolveUnsaved: ((allow: boolean) => void) | null = null
let unsavedPending: Promise<boolean> | null = null

const closeUnsaved = (allow: boolean) => {
  unsavedDialogOpen.value = false
  resolveUnsaved?.(allow)
  resolveUnsaved = null
  unsavedPending = null
}

// Um único modal para todas as saídas: navegação entre telas, troca de nota/pasta e
// "nova nota". Enquanto ele está aberto, novas tentativas reaproveitam a mesma resposta.
const askUnsaved = (): Promise<boolean> => {
  if (!isDirty.value) return Promise.resolve(true)
  if (unsavedPending) return unsavedPending
  unsavedDialogOpen.value = true
  unsavedPending = new Promise<boolean>(resolve => { resolveUnsaved = resolve })
  nextTick(() => unsavedDialog.value?.querySelector<HTMLButtonElement>('button')?.focus())
  return unsavedPending
}

const handleUnsavedChoice = async (choice: 'save' | 'discard' | 'cancel') => {
  if (choice === 'cancel') closeUnsaved(false)
  else if (choice === 'discard') closeUnsaved(true)
  else if (await save()) closeUnsaved(true)
}

const onBeforeUnload = (event: BeforeUnloadEvent) => {
  if (!isDirty.value) return
  event.preventDefault()
  event.returnValue = ''
}

onMounted(() => {
  void load()
  setNavigationGuard(() => askUnsaved())
  window.addEventListener('beforeunload', onBeforeUnload)
})
onBeforeUnmount(() => {
  setNavigationGuard(null)
  window.removeEventListener('beforeunload', onBeforeUnload)
  if (unsavedPending) closeUnsaved(false)
})

watch(selectedNoteId, () => {
  const note = folderNotes.value.find(item => item.id === selectedNoteId.value)
  title.value = note?.title ?? ''
  content.value = note?.content ?? ''
  creatingNote.value = false
}, { immediate: true })

const save = async (): Promise<boolean> => {
  const success = creatingNote.value
    ? await addNote(title.value, content.value)
    : await saveSelected(title.value, content.value)
  if (success) {
    creatingNote.value = false
    const note = storedNote.value
    if (note) {
      title.value = note.title
      content.value = note.content
    }
    saved.value = t('savedNow')
  }
  return success
}
const beginCreateNote = async () => {
  if (!(await askUnsaved())) return
  creatingNote.value = true
  previewing.value = false
  title.value = ''
  content.value = ''
}
const cancelCreateNote = () => {
  creatingNote.value = false
  const note = folderNotes.value.find(item => item.id === selectedNoteId.value)
  title.value = note?.title ?? ''
  content.value = note?.content ?? ''
}
const chooseFolder = async (id: string) => {
  if (id === selectedFolderId.value) return
  if (!(await askUnsaved())) return
  creatingNote.value = false
  selectFolder(id)
  const note = folderNotes.value.find(item => item.id === selectedNoteId.value)
  title.value = note?.title ?? ''
  content.value = note?.content ?? ''
}
const chooseNote = async (id: string) => {
  if (id === selectedNoteId.value && !creatingNote.value) return
  if (!(await askUnsaved())) return
  creatingNote.value = false
  selectNote(id)
  const note = folderNotes.value.find(item => item.id === id)
  title.value = note?.title ?? ''
  content.value = note?.content ?? ''
}
const beginRename = (folder: { id: string; name: string }) => { editingFolderId.value = folder.id; folderName.value = folder.name }
const saveFolderName = async (id: string) => { if (await renameFolder(id, folderName.value)) editingFolderId.value = null }
const draggingNoteId = ref<string | null>(null)
const startNoteDrag = (id: string, event: DragEvent) => {
  draggingNoteId.value = id
  event.dataTransfer?.setData('text/plain', id)
  if (event.dataTransfer) event.dataTransfer.effectAllowed = 'move'
}
const dropNoteOnFolder = async (folderId: string) => {
  const noteId = draggingNoteId.value
  draggingNoteId.value = null
  if (!noteId) return
  if (noteId === selectedNoteId.value && !(await askUnsaved())) return
  if (!(await moveNote(noteId, folderId))) return
  const note = folderNotes.value.find(item => item.id === noteId)
  title.value = note?.title ?? ''
  content.value = note?.content ?? ''
  saved.value = t('savedNow')
}
</script>
<template>
  <section class="content"><header class="page-header"><div><h1>{{ t('notesTitle') }}</h1><p>{{ t('notesSubtitle') }}</p></div><div class="header-actions"><LanguageToggle/><ThemeToggle/></div></header>
    <p v-if="error" class="sync-error">{{ error }} <button @click="load">{{ t('retry') }}</button></p>
    <PageSkeleton v-if="loading" layout="split" :label="t('notesLoading')" />
    <div v-else class="notes-layout">
      <article class="card folder-card">
        <div class="card-title"><h2>{{ t('foldersTitle') }}</h2><button class="link" @click="addFolder">＋</button></div>
        <p v-if="!folders.length && !error" class="empty-note">{{ t('noFolders') }}</p>
        <div v-for="folder in folders" :key="folder.id" class="folder-row">
          <form v-if="editingFolderId===folder.id" class="folder-rename" @submit.prevent="saveFolderName(folder.id)"><input v-model="folderName" aria-label="Nome da pasta" maxlength="100"/><button class="link" type="submit">Salvar</button><button class="link" type="button" @click="editingFolderId=null">Cancelar</button></form>
          <template v-else><button class="folder" :class="{selected:folder.id===selectedFolderId, 'drop-target': draggingNoteId !== null && folder.id !== selectedFolderId}" @click="chooseFolder(folder.id)" @dragover.prevent @drop.prevent="dropNoteOnFolder(folder.id)">▰ <span class="folder-name">{{folder.name}}</span><small v-if="folder.notes.length" class="folder-count">{{ folder.notes.length }}</small></button><button class="link rename-folder" aria-label="Renomear pasta" @click="beginRename(folder)">✎</button></template>
        </div>
        <button v-if="selectedFolderId" class="link danger folder-delete" @click="removeFolder(selectedFolderId)">{{ t('deleteFolderAction') }}</button>
      </article>
      <article class="card editor" :class="{ expanded }">
        <div class="card-title editor-heading"><div class="editor-title"><h2>{{ selectedFolder?.name ?? t('folderNotesTitle') }}</h2><small v-if="selectedFolder">{{ t('noteCount', { count: folderNotes.length }) }}</small></div><button v-if="selectedFolderId && !creatingNote" class="link" @click="beginCreateNote">＋ {{ t('newNote') }}</button></div>
        <nav v-if="selectedFolder && folderNotes.length" class="note-cards" :aria-label="t('folderNotesTitle')">
          <button v-for="note in folderNotes" :key="note.id" class="note-card" :class="{selected:note.id===selectedNoteId && !creatingNote, dragging: note.id === draggingNoteId}" draggable="true" :title="t('noteDragHint')" @click="chooseNote(note.id)" @dragstart="startNoteDrag(note.id, $event)" @dragend="draggingNoteId = null">
            <strong>{{ note.title || t('untitledNote') }}</strong>
            <small>{{ notePreview(note.content) || t('noNotePreview') }}</small>
            <span>{{ t('noteWordCount', { count: noteWordCount(note.content) }) }}</span>
          </button>
        </nav>
        <template v-if="selectedFolderId && (selectedNoteId || creatingNote)">
          <div class="toolbar">
            <div class="toolbar-group">
              <button type="button" class="tool" :disabled="previewing" :aria-label="t('formatBold')" :title="t('formatBold')" @mousedown.prevent @click="wrapSelection('**')"><strong>B</strong></button>
              <button type="button" class="tool" :disabled="previewing" :aria-label="t('formatItalic')" :title="t('formatItalic')" @mousedown.prevent @click="wrapSelection('*')"><em class="italic-icon">I</em></button>
              <button type="button" class="tool" :disabled="previewing" :aria-label="t('formatHeading1')" :title="t('formatHeading1')" @mousedown.prevent @click="heading(1)">H1</button>
              <button type="button" class="tool" :disabled="previewing" :aria-label="t('formatHeading2')" :title="t('formatHeading2')" @mousedown.prevent @click="heading(2)">H2</button>
              <button type="button" class="tool" :disabled="previewing" :aria-label="t('formatBulletList')" :title="t('formatBulletList')" @mousedown.prevent @click="prefixLines('- ')">•</button>
              <button type="button" class="tool" :disabled="previewing" :aria-label="t('formatNumberedList')" :title="t('formatNumberedList')" @mousedown.prevent @click="prefixLines('1. ')">1.</button>
              <button type="button" class="tool" :disabled="previewing" :aria-label="t('formatQuote')" :title="t('formatQuote')" @mousedown.prevent @click="prefixLines('> ')">❝</button>
              <button type="button" class="tool" :disabled="previewing" :aria-label="t('formatCode')" :title="t('formatCode')" @mousedown.prevent @click="wrapSelection('`')">&lt;/&gt;</button>
            </div>
            <div class="toolbar-actions">
              <button type="button" class="tool textual" :aria-label="previewing ? t('editAction') : t('previewAction')" @click="togglePreview">{{ previewing ? t('editAction') : t('previewAction') }}</button>
              <button type="button" class="tool" :aria-label="expanded ? t('collapseNote') : t('expandNote')" :title="expanded ? t('collapseNote') : t('expandNote')" @click="toggleExpanded">{{ expanded ? '⤡' : '⤢' }}</button>
            </div>
          </div>
          <input v-model="title" :placeholder="t('selectNotePlaceholder')" :aria-label="t('noteTitleLabel')" />
          <div v-if="previewing" class="note-preview" v-html="previewHtml"></div>
          <textarea v-else ref="textarea" v-model="content" :placeholder="t('contentPlaceholder')" @keydown="onContentKeydown" />
          <footer><small>{{ error ?? (creatingNote ? t('newNote') : saved) }}</small><div class="footer-actions"><button v-if="creatingNote" class="link" @click="cancelCreateNote">{{ t('cancelAction') }}</button><button v-else class="link danger" :disabled="!selectedNoteId" @click="removeSelectedNote">{{ t('deleteNoteAction') }}</button><button class="button" :disabled="saving || (!selectedNoteId && !creatingNote)" @click="save">{{ saving ? t('saving') : creatingNote ? t('createNote') : t('save') }}</button></div></footer>
        </template>
        <p v-else-if="selectedFolderId && !folderNotes.length" class="empty-note folder-notes-empty">{{ t('noNotesInFolder') }}</p>
        <p v-else-if="!selectedFolderId" class="empty-note folder-notes-empty">{{ t('selectFolderForNotes') }}</p>
        <p v-else class="empty-note folder-notes-empty">{{ t('selectNoteHint') }}</p>
      </article>
    </div>
    <div v-if="unsavedDialogOpen" class="modal-backdrop" @click.self="handleUnsavedChoice('cancel')" @keydown.esc.stop="handleUnsavedChoice('cancel')">
      <div ref="unsavedDialog" class="modal" role="dialog" aria-modal="true" aria-labelledby="unsaved-title" aria-describedby="unsaved-text">
        <h2 id="unsaved-title">{{ t('unsavedChangesTitle') }}</h2>
        <p id="unsaved-text">{{ t('unsavedChangesText') }}</p>
        <p v-if="error" class="modal-error">{{ error }}</p>
        <div class="modal-actions">
          <button type="button" class="link" :disabled="saving" @click="handleUnsavedChoice('cancel')">{{ t('cancelAction') }}</button>
          <button type="button" class="button secondary" :disabled="saving" @click="handleUnsavedChoice('discard')">{{ t('discardChanges') }}</button>
          <button type="button" class="button" :disabled="saving" @click="handleUnsavedChoice('save')">{{ saving ? t('saving') : t('save') }}</button>
        </div>
      </div>
    </div>
  </section>
</template>
<style scoped>
.folder-delete{margin-top:12px;padding:4px}
.folder-row{display:flex;align-items:center;gap:4px}
.folder-row>.folder{flex:1;display:flex;align-items:center;gap:8px}
.folder-name{flex:1;min-width:0;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
.folder-count{flex:none;padding:2px 8px;border-radius:999px;background:var(--color-bg);color:var(--color-muted);font-size:11px;font-weight:600}
.folder.selected .folder-count{background:var(--color-surface);color:var(--color-primary)}
.rename-folder{padding:6px}
.folder-rename{display:flex;gap:4px;width:100%}
.folder-rename input{min-width:0;flex:1}
.editor-heading{margin-bottom:12px}
.editor-title{min-width:0}
.editor-title small{display:block;margin-top:2px;color:var(--color-muted);font-size:12px}
.note-cards{display:grid;grid-template-columns:repeat(auto-fill,minmax(176px,1fr));gap:10px;max-height:264px;padding:0 24px 20px;overflow:auto}
.note-card{display:grid;gap:6px;align-content:start;padding:12px 14px;border:1px solid var(--color-border);border-radius:12px;background:var(--color-bg);color:var(--color-text);text-align:left;cursor:pointer}
.note-card:hover{border-color:var(--color-border-strong)}
.note-card.selected{border-color:var(--color-primary);background:var(--color-primary-soft)}
.note-card strong{font-size:14px}
.note-card small{display:-webkit-box;overflow:hidden;color:var(--color-muted);font-size:12px;line-height:1.45;-webkit-box-orient:vertical;-webkit-line-clamp:2}
.note-card span{color:var(--color-subtle);font-size:11px}
.note-card.selected span{color:var(--color-primary)}
.note-card.dragging{opacity:.5}
.folder.drop-target{border-color:var(--color-primary);background:var(--color-primary-soft)}
.folder-notes-empty{margin:auto}
.toolbar{flex-wrap:wrap}
.toolbar-group{display:flex;flex-wrap:wrap;gap:2px}
.toolbar-actions{display:flex;gap:4px;margin-left:auto}
.toolbar .tool{min-width:30px;padding:6px 5px;border:1px solid transparent;border-radius:8px;font:600 13px var(--font-body);color:var(--color-muted)}
.toolbar .italic-icon{font-family:Georgia,'Times New Roman',serif;font-size:15px;font-style:italic;line-height:1}
.toolbar .tool:hover:not(:disabled){background:var(--color-bg);color:var(--color-text)}
.toolbar .tool:disabled{opacity:.4;cursor:default}
.toolbar .tool.textual{padding:6px 10px}
.editor.expanded{position:fixed;inset:0;z-index:60;border:0;border-radius:0;display:flex;flex-direction:column}
.editor.expanded .note-cards{display:none}
.editor.expanded input{flex:none}
.editor.expanded textarea,.editor.expanded .note-preview{flex:1;min-height:0;height:auto;resize:none}
.editor.expanded footer{flex:none}
.note-preview{padding:16px 24px;min-height:260px;overflow:auto;font-size:14px;line-height:1.65}
.note-preview :deep(h1),.note-preview :deep(h2),.note-preview :deep(h3),.note-preview :deep(h4){margin:20px 0 8px;font-family:var(--font-heading);line-height:1.3}
.note-preview :deep(h1:first-child),.note-preview :deep(h2:first-child),.note-preview :deep(h3:first-child){margin-top:0}
.note-preview :deep(h1){font-size:26px}
.note-preview :deep(h2){font-size:21px}
.note-preview :deep(h3){font-size:17px}
.note-preview :deep(p){margin:0 0 12px}
.note-preview :deep(ul),.note-preview :deep(ol){margin:0 0 12px;padding-left:24px}
.note-preview :deep(li){margin:4px 0}
.note-preview :deep(li>p){margin:0}
.note-preview :deep(blockquote){margin:0 0 12px;padding:8px 14px;border-left:3px solid var(--color-primary);border-radius:0 8px 8px 0;background:var(--color-primary-soft);color:var(--color-muted)}
.note-preview :deep(blockquote p:last-child){margin:0}
.note-preview :deep(code){padding:2px 6px;border:1px solid var(--color-border);border-radius:6px;background:var(--color-bg);font-size:13px}
.note-preview :deep(pre){margin:0 0 12px;padding:12px 14px;border:1px solid var(--color-border);border-radius:10px;background:var(--color-bg);overflow:auto}
.note-preview :deep(pre code){padding:0;border:0;background:transparent}
.note-preview :deep(a){color:var(--color-primary)}
.note-preview :deep(hr){margin:16px 0;border:0;border-top:1px solid var(--color-border)}
.note-preview :deep(table){width:100%;margin:0 0 12px;border-collapse:collapse}
.note-preview :deep(th),.note-preview :deep(td){padding:6px 10px;border:1px solid var(--color-border);text-align:left}
.note-preview :deep(th){background:var(--color-bg)}
.note-preview :deep(input[type=checkbox]){margin-right:6px}
.modal-backdrop{position:fixed;inset:0;z-index:80;display:grid;place-items:center;padding:20px;background:rgba(9,9,11,.55)}
.modal{width:min(420px,100%);padding:24px;border:1px solid var(--color-border);border-radius:16px;background:var(--color-surface);color:var(--color-text);box-shadow:0 24px 60px rgba(0,0,0,.28)}
.modal h2{margin:0 0 8px;font:600 18px var(--font-heading)}
.modal p{margin:0 0 16px;color:var(--color-muted);font-size:14px;line-height:1.5}
.modal .modal-error{margin:0 0 12px;padding:8px 12px;border-radius:8px;background:var(--color-danger-soft);color:var(--color-danger)}
.modal-actions{display:flex;align-items:center;justify-content:flex-end;gap:12px;flex-wrap:wrap}
.footer-actions{display:flex;align-items:center;gap:16px}
.danger{color:var(--color-danger)}
.danger:disabled{opacity:.5;cursor:not-allowed}
@media(max-width:600px){.note-cards{grid-template-columns:1fr;max-height:none;padding:0 16px 16px}}
</style>
