<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import AppSidebar from '../components/AppSidebar.vue'; import ThemeToggle from '../components/ThemeToggle.vue'; import LanguageToggle from '../components/LanguageToggle.vue'; import { useI18n } from '../composables/useI18n'; import type { AppPage } from '../composables/useNavigation'
import { useStudentProfile } from '../features/auth/useAuth'
import { useNotes } from '../features/notes/useNotes'

defineEmits<{ navigate:[page:AppPage] }>()
const { t } = useI18n()
const { student } = useStudentProfile()
const { folders, folderNotes, loading, saving, error, selectedFolderId, selectedNoteId, load, selectFolder, selectNote, saveSelected, addFolder, addNote, removeSelectedNote, removeFolder, renameFolder } = useNotes()
onMounted(load)

const saved = ref(t('savedEarlier')); const title = ref(''); const content = ref('')
const editingFolderId = ref<string | null>(null); const folderName = ref('')
const creatingNote = ref(false)
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
  <main v-if="!loading" class="app-shell"><AppSidebar v-bind="student" active="notes" @navigate="$emit('navigate',$event)"/><section class="content"><header class="page-header"><div><h1>{{ t('notesTitle') }}</h1><p>{{ t('notesSubtitle') }}</p></div><div class="header-actions"><LanguageToggle/><ThemeToggle/></div></header>
    <p v-if="error" class="sync-error">{{ error }} <button @click="load">{{ t('retry') }}</button></p>
    <div class="notes-layout">
      <article class="card folder-card">
        <div class="card-title"><h2>{{ t('foldersTitle') }}</h2><button class="link" @click="addFolder">＋</button></div>
        <p v-if="!folders.length && !error" class="empty-note">{{ t('noFolders') }}</p>
        <div v-for="folder in folders" :key="folder.id" class="folder-row">
          <form v-if="editingFolderId===folder.id" class="folder-rename" @submit.prevent="saveFolderName(folder.id)"><input v-model="folderName" aria-label="Nome da pasta" maxlength="100"/><button class="link" type="submit">Salvar</button><button class="link" type="button" @click="editingFolderId=null">Cancelar</button></form>
          <template v-else><button class="folder" :class="{selected:folder.id===selectedFolderId}" @click="chooseFolder(folder.id)">▰ {{folder.name}}</button><button class="link rename-folder" aria-label="Renomear pasta" @click="beginRename(folder)">✎</button></template>
        </div>
        <button v-if="selectedFolderId" class="link danger folder-delete" @click="removeFolder(selectedFolderId)">{{ t('deleteFolderAction') }}</button>
      </article>
      <article class="card editor">
        <div class="card-title editor-heading"><h2>{{ t('folderNotesTitle') }}</h2><button v-if="selectedFolderId && !creatingNote" class="link" @click="beginCreateNote">＋ {{ t('newNote') }}</button></div>
        <nav v-if="folderNotes.length" class="tabs"><button v-for="note in folderNotes" :key="note.id" :class="{selected:note.id===selectedNoteId}" @click="chooseNote(note.id)">{{note.title}}</button></nav>
        <p v-if="selectedFolderId && !folderNotes.length && !creatingNote" class="empty-note folder-notes-empty">{{ t('noNotesInFolder') }}</p>
        <p v-else-if="!selectedFolderId && !creatingNote" class="empty-note folder-notes-empty">{{ t('selectFolderForNotes') }}</p>
        <template v-else>
          <div class="toolbar"><button>B</button><button><i>I</i></button><button><u>U</u></button><button>☷</button><span></span></div>
          <input v-model="title" :placeholder="t('selectNotePlaceholder')" :aria-label="t('noteTitleLabel')" />
          <textarea v-model="content" :placeholder="t('contentPlaceholder')" />
          <footer><small>{{ error ?? (creatingNote ? t('newNote') : saved) }}</small><div class="footer-actions"><button v-if="creatingNote" class="link" @click="cancelCreateNote">{{ t('cancelAction') }}</button><button v-else class="link danger" :disabled="!selectedNoteId" @click="removeSelectedNote">{{ t('deleteNoteAction') }}</button><button class="button" :disabled="saving || (!selectedNoteId && !creatingNote)" @click="save">{{ saving ? t('saving') : creatingNote ? t('createNote') : t('save') }}</button></div></footer>
        </template>
      </article>
    </div>
  </section></main>
  <main v-else class="loading">{{ t('notesLoading') }}</main>
</template>
<style scoped>
.folder-delete{margin-top:12px;padding:4px}
.folder-row{display:flex;align-items:center;gap:4px}
.folder-row>.folder{flex:1}
.rename-folder{padding:6px}
.folder-rename{display:flex;gap:4px;width:100%}
.folder-rename input{min-width:0;flex:1}
.editor-heading{margin-bottom:12px}
.folder-notes-empty{margin:auto}
.footer-actions{display:flex;align-items:center;gap:16px}
.danger{color:var(--color-danger)}
.danger:disabled{opacity:.5;cursor:not-allowed}
</style>
