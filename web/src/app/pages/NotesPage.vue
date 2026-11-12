<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import PageSkeleton from '../components/PageSkeleton.vue'
import ThemeToggle from '../components/ThemeToggle.vue'; import LanguageToggle from '../components/LanguageToggle.vue'; import { useI18n } from '../composables/useI18n'; import type { AppPage } from '../composables/useNavigation'
import { useNotes } from '../features/notes/useNotes'

defineEmits<{ navigate:[page:AppPage] }>()
const { t } = useI18n()
const { folders, folderNotes, selectedFolder, loading, saving, error, selectedFolderId, selectedNoteId, load, selectFolder, selectNote, saveSelected, addFolder, addNote, removeSelectedNote, removeFolder, renameFolder } = useNotes()
onMounted(load)

const saved = ref(t('savedEarlier')); const title = ref(''); const content = ref('')
const editingFolderId = ref<string | null>(null); const folderName = ref('')
const creatingNote = ref(false)

const notePreview = (value: string) => {
  const text = value.replace(/\s+/g, ' ').trim()
  return text.length > 110 ? `${text.slice(0, 110)}…` : text
}
const noteWordCount = (value: string) => value.trim().split(/\s+/).filter(Boolean).length

watch(selectedNoteId, () => {
  const note = folderNotes.value.find(item => item.id === selectedNoteId.value)
  title.value = note?.title ?? ''
  content.value = note?.content ?? ''
  creatingNote.value = false
}, { immediate: true })

const save = async () => {
  const success = creatingNote.value
    ? await addNote(title.value, content.value)
    : await saveSelected(title.value, content.value)
  if (success) {
    creatingNote.value = false
    saved.value = t('savedNow')
  }
}
const beginCreateNote = () => { creatingNote.value = true; title.value = ''; content.value = '' }
const cancelCreateNote = () => {
  creatingNote.value = false
  const note = folderNotes.value.find(item => item.id === selectedNoteId.value)
  title.value = note?.title ?? ''
  content.value = note?.content ?? ''
}
const chooseFolder = (id: string) => {
  creatingNote.value = false
  selectFolder(id)
  const note = folderNotes.value.find(item => item.id === selectedNoteId.value)
  title.value = note?.title ?? ''
  content.value = note?.content ?? ''
}
const chooseNote = (id: string) => {
  creatingNote.value = false
  selectNote(id)
  const note = folderNotes.value.find(item => item.id === id)
  title.value = note?.title ?? ''
  content.value = note?.content ?? ''
}
const beginRename = (folder: { id: string; name: string }) => { editingFolderId.value = folder.id; folderName.value = folder.name }
const saveFolderName = async (id: string) => { if (await renameFolder(id, folderName.value)) editingFolderId.value = null }
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
          <template v-else><button class="folder" :class="{selected:folder.id===selectedFolderId}" @click="chooseFolder(folder.id)">▰ <span class="folder-name">{{folder.name}}</span><small v-if="folder.notes.length" class="folder-count">{{ folder.notes.length }}</small></button><button class="link rename-folder" aria-label="Renomear pasta" @click="beginRename(folder)">✎</button></template>
        </div>
        <button v-if="selectedFolderId" class="link danger folder-delete" @click="removeFolder(selectedFolderId)">{{ t('deleteFolderAction') }}</button>
      </article>
      <article class="card editor">
        <div class="card-title editor-heading"><div class="editor-title"><h2>{{ selectedFolder?.name ?? t('folderNotesTitle') }}</h2><small v-if="selectedFolder">{{ t('noteCount', { count: folderNotes.length }) }}</small></div><button v-if="selectedFolderId && !creatingNote" class="link" @click="beginCreateNote">＋ {{ t('newNote') }}</button></div>
        <nav v-if="selectedFolder && folderNotes.length" class="note-cards" :aria-label="t('folderNotesTitle')">
          <button v-for="note in folderNotes" :key="note.id" class="note-card" :class="{selected:note.id===selectedNoteId && !creatingNote}" @click="chooseNote(note.id)">
            <strong>{{ note.title || t('untitledNote') }}</strong>
            <small>{{ notePreview(note.content) || t('noNotePreview') }}</small>
            <span>{{ t('noteWordCount', { count: noteWordCount(note.content) }) }}</span>
          </button>
        </nav>
        <template v-if="selectedFolderId && (selectedNoteId || creatingNote)">
          <div class="toolbar"><button>B</button><button><i>I</i></button><button><u>U</u></button><button>☷</button><span></span></div>
          <input v-model="title" :placeholder="t('selectNotePlaceholder')" :aria-label="t('noteTitleLabel')" />
          <textarea v-model="content" :placeholder="t('contentPlaceholder')" />
          <footer><small>{{ error ?? (creatingNote ? t('newNote') : saved) }}</small><div class="footer-actions"><button v-if="creatingNote" class="link" @click="cancelCreateNote">{{ t('cancelAction') }}</button><button v-else class="link danger" :disabled="!selectedNoteId" @click="removeSelectedNote">{{ t('deleteNoteAction') }}</button><button class="button" :disabled="saving || (!selectedNoteId && !creatingNote)" @click="save">{{ saving ? t('saving') : creatingNote ? t('createNote') : t('save') }}</button></div></footer>
        </template>
        <p v-else-if="selectedFolderId && !folderNotes.length" class="empty-note folder-notes-empty">{{ t('noNotesInFolder') }}</p>
        <p v-else-if="!selectedFolderId" class="empty-note folder-notes-empty">{{ t('selectFolderForNotes') }}</p>
        <p v-else class="empty-note folder-notes-empty">{{ t('selectNoteHint') }}</p>
      </article>
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
.folder-notes-empty{margin:auto}
.footer-actions{display:flex;align-items:center;gap:16px}
.danger{color:var(--color-danger)}
.danger:disabled{opacity:.5;cursor:not-allowed}
@media(max-width:600px){.note-cards{grid-template-columns:1fr;max-height:none;padding:0 16px 16px}}
</style>
