<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import AppSidebar from '../components/AppSidebar.vue'; import ThemeToggle from '../components/ThemeToggle.vue'; import LanguageToggle from '../components/LanguageToggle.vue'; import { useI18n } from '../composables/useI18n'; import type { AppPage } from '../composables/useNavigation'
import { useStudentProfile } from '../features/auth/useAuth'
import { useNotes } from '../features/notes/useNotes'

defineEmits<{ navigate:[page:AppPage] }>()
const { t } = useI18n()
const { student } = useStudentProfile()
const { folders, folderNotes, loading, saving, error, selectedFolderId, selectedNoteId, load, selectFolder, selectNote, saveSelected, addFolder } = useNotes()
onMounted(load)

const saved = ref(t('savedEarlier')); const title = ref(''); const content = ref('')
watch(selectedNoteId, () => {
  const note = folderNotes.value.find(item => item.id === selectedNoteId.value)
  title.value = note?.title ?? ''
  content.value = note?.content ?? ''
}, { immediate: true })

const save = async () => { if (await saveSelected(title.value, content.value)) saved.value = t('savedNow') }
</script>
<template>
  <main v-if="!loading" class="app-shell"><AppSidebar v-bind="student" active="notes" @navigate="$emit('navigate',$event)"/><section class="content"><header class="page-header"><div><h1>{{ t('notesTitle') }}</h1><p>{{ t('notesSubtitle') }}</p></div><div class="header-actions"><LanguageToggle/><ThemeToggle/></div></header>
    <p v-if="error" class="sync-error">{{ error }} <button @click="load">{{ t('retry') }}</button></p>
    <div class="notes-layout">
      <article class="card folder-card">
        <div class="card-title"><h2>{{ t('foldersTitle') }}</h2><button class="link" @click="addFolder">＋</button></div>
        <p v-if="!folders.length && !error" class="empty-note">{{ t('noFolders') }}</p>
        <button v-for="folder in folders" :key="folder.id" class="folder" :class="{selected:folder.id===selectedFolderId}" @click="selectFolder(folder.id)">▰ {{folder.name}}</button>
      </article>
      <article class="card editor">
        <nav v-if="folderNotes.length" class="tabs"><button v-for="note in folderNotes" :key="note.id" :class="{selected:note.id===selectedNoteId}" @click="selectNote(note.id)">{{note.title}}</button></nav>
        <div class="toolbar"><button>B</button><button><i>I</i></button><button><u>U</u></button><button>☷</button><span></span></div>
        <input v-model="title" :placeholder="selectedNoteId ? '' : t('selectNotePlaceholder')" />
        <textarea v-model="content" :placeholder="t('contentPlaceholder')" />
        <footer><small>{{ error ?? saved }}</small><button class="button" :disabled="saving || !selectedNoteId" @click="save">{{ saving ? t('saving') : t('save') }}</button></footer>
      </article>
    </div>
  </section></main>
  <main v-else class="loading">{{ t('notesLoading') }}</main>
</template>
