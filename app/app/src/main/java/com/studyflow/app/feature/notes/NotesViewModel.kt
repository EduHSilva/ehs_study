package com.studyflow.app.feature.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studyflow.app.core.settings.AppSettings
import com.studyflow.app.data.model.Note
import com.studyflow.app.data.model.NoteFolder
import com.studyflow.app.data.repository.NotesRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class NotesUiState(
    val folders: List<NoteFolder> = emptyList(),
    val selectedFolderId: String? = null,
    val selectedNoteId: String? = null,
    val loading: Boolean = true,
    val saving: Boolean = false,
    val errorKey: String? = null,
    val savedMessageKey: String? = null,
) {
    val selectedFolder: NoteFolder?
        get() = folders.firstOrNull { it.id == selectedFolderId }

    val folderNotes: List<Note>
        get() = selectedFolder?.notes.orEmpty()

    val selectedNote: Note?
        get() = folderNotes.firstOrNull { it.id == selectedNoteId }
}

class NotesViewModel(
    private val notesRepository: NotesRepository,
    private val settings: AppSettings,
) : ViewModel() {

    private val _state = MutableStateFlow(NotesUiState())
    val state: StateFlow<NotesUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, errorKey = null) }
            try {
                val folders = notesRepository.getFolders()
                val first = folders.firstOrNull()
                _state.update {
                    it.copy(
                        folders = folders,
                        selectedFolderId = first?.id,
                        selectedNoteId = first?.notes?.firstOrNull()?.id,
                        loading = false,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _state.update { it.copy(loading = false, errorKey = "notesLoadFailed") }
            }
        }
    }

    fun selectFolder(id: String) {
        _state.update { current ->
            val folder = current.folders.firstOrNull { it.id == id }
            current.copy(
                selectedFolderId = id,
                selectedNoteId = folder?.notes?.firstOrNull()?.id,
                savedMessageKey = null,
            )
        }
    }

    fun selectNote(id: String) {
        _state.update { it.copy(selectedNoteId = id, savedMessageKey = null) }
    }

    fun saveSelected(title: String, content: String) {
        val note = _state.value.selectedNote ?: return
        if (_state.value.saving) return
        viewModelScope.launch {
            _state.update { it.copy(saving = true, errorKey = null, savedMessageKey = null) }
            try {
                val saved = notesRepository.saveNote(note.id, title, content)
                replaceNote(saved.copy(folderId = note.folderId))
                _state.update { it.copy(savedMessageKey = "savedNow") }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _state.update { it.copy(errorKey = "noteSaveFailed") }
            } finally {
                _state.update { it.copy(saving = false) }
            }
        }
    }

    fun createNote(title: String, content: String) {
        val folder = _state.value.selectedFolder ?: return
        val trimmed = title.trim()
        if (trimmed.isEmpty()) {
            _state.update { it.copy(errorKey = "noteTitleRequired") }
            return
        }
        if (_state.value.saving) return
        viewModelScope.launch {
            _state.update { it.copy(saving = true, errorKey = null, savedMessageKey = null) }
            try {
                val note = notesRepository.createNote(folder.id, trimmed, content)
                _state.update { current ->
                    current.copy(
                        folders = current.folders.map {
                            if (it.id == folder.id) it.copy(notes = it.notes + note) else it
                        },
                        selectedNoteId = note.id,
                        savedMessageKey = "savedNow",
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _state.update { it.copy(errorKey = "noteCreateFailed") }
            } finally {
                _state.update { it.copy(saving = false) }
            }
        }
    }

    fun deleteSelectedNote() {
        val current = _state.value
        val note = current.selectedNote ?: return
        val folder = current.selectedFolder ?: return
        viewModelScope.launch {
            _state.update { it.copy(errorKey = null) }
            try {
                notesRepository.deleteNote(note.id)
                _state.update { state ->
                    val updatedFolder = folder.copy(notes = folder.notes.filterNot { it.id == note.id })
                    state.copy(
                        folders = state.folders.map { if (it.id == folder.id) updatedFolder else it },
                        selectedNoteId = updatedFolder.notes.firstOrNull()?.id,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _state.update { it.copy(errorKey = "noteDeleteFailed") }
            }
        }
    }

    fun addFolder(name: String? = null) {
        val trimmed = name?.trim().orEmpty()
        val folderName = trimmed.ifEmpty {
            "${settings.strings()["newFolder"]} ${_state.value.folders.size + 1}"
        }
        viewModelScope.launch {
            _state.update { it.copy(errorKey = null) }
            try {
                val folder = notesRepository.createFolder(folderName)
                _state.update { current ->
                    val folders = current.folders + folder
                    current.copy(
                        folders = folders,
                        selectedFolderId = folder.id,
                        selectedNoteId = folder.notes.firstOrNull()?.id,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _state.update { it.copy(errorKey = "folderCreateFailed") }
            }
        }
    }

    fun renameFolder(id: String, name: String): Boolean {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return false
        val folder = _state.value.folders.firstOrNull { it.id == id } ?: return false
        viewModelScope.launch {
            _state.update { it.copy(errorKey = null) }
            try {
                val renamed = notesRepository.renameFolder(id, trimmed)
                _state.update { current ->
                    current.copy(
                        folders = current.folders.map {
                            if (it.id == id) {
                                it.copy(
                                    name = renamed.name,
                                    notes = if (renamed.notes.isEmpty()) folder.notes else renamed.notes,
                                )
                            } else {
                                it
                            }
                        },
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _state.update { it.copy(errorKey = "folderRenameFailed") }
            }
        }
        return true
    }

    fun deleteSelectedFolder() {
        val folder = _state.value.selectedFolder ?: return
        viewModelScope.launch {
            _state.update { it.copy(errorKey = null) }
            try {
                notesRepository.deleteFolder(folder.id)
                _state.update { current ->
                    val folders = current.folders.filterNot { it.id == folder.id }
                    val first = folders.firstOrNull()
                    current.copy(
                        folders = folders,
                        selectedFolderId = first?.id,
                        selectedNoteId = first?.notes?.firstOrNull()?.id,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                _state.update { it.copy(errorKey = "folderDeleteFailed") }
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(errorKey = null) }
    }

    private fun replaceNote(note: Note) {
        _state.update { current ->
            current.copy(
                folders = current.folders.map { folder ->
                    if (folder.id == note.folderId) {
                        folder.copy(notes = folder.notes.map { if (it.id == note.id) note else it })
                    } else {
                        folder
                    }
                },
            )
        }
    }
}
