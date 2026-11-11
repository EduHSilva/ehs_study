package com.studyflow.app.feature.notes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studyflow.app.core.i18n.LocalStrings
import com.studyflow.app.core.ui.components.CardTitle
import com.studyflow.app.core.ui.components.ContentCard
import com.studyflow.app.core.ui.components.EmptyNote
import com.studyflow.app.core.ui.components.ErrorBanner
import com.studyflow.app.core.ui.components.LoadingScreen
import com.studyflow.app.core.ui.components.Subtitle
import com.studyflow.app.core.ui.theme.StudyFlowTheme
import com.studyflow.app.domain.notes.NoteFormat
import com.studyflow.app.ui.appViewModel

@Composable
fun NotesScreen() {
    val vm: NotesViewModel = appViewModel { NotesViewModel(it.notesRepository, it.settings) }
    val state by vm.state.collectAsStateWithLifecycle()
    val s = LocalStrings.current

    var title by rememberSaveable { mutableStateOf("") }
    var content by rememberSaveable { mutableStateOf("") }
    var creatingNote by rememberSaveable { mutableStateOf(false) }

    var folderDialog by rememberSaveable { mutableStateOf(false) }
    var folderName by rememberSaveable { mutableStateOf("") }
    var renamingFolder by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmDeleteFolder by rememberSaveable { mutableStateOf(false) }
    var confirmDeleteNote by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(state.selectedNoteId) {
        val note = state.selectedNote
        title = note?.title.orEmpty()
        content = note?.content.orEmpty()
        creatingNote = false
    }

    if (state.loading) {
        LoadingScreen(s["notesLoading"])
        return
    }

    if (folderDialog || renamingFolder != null) {
        val renaming = renamingFolder != null
        AlertDialog(
            onDismissRequest = {
                folderDialog = false
                renamingFolder = null
            },
            title = { Text(if (renaming) s["renameFolder"] else s["newFolder"]) },
            text = {
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    label = { Text(s["folderNameLabel"]) },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (renaming) {
                            renamingFolder?.let { vm.renameFolder(it, folderName) }
                        } else {
                            vm.addFolder(folderName)
                        }
                        folderDialog = false
                        renamingFolder = null
                        folderName = ""
                    },
                ) { Text(s["confirmAction"]) }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        folderDialog = false
                        renamingFolder = null
                        folderName = ""
                    },
                ) { Text(s["cancelAction"]) }
            },
        )
    }

    if (confirmDeleteFolder) {
        AlertDialog(
            onDismissRequest = { confirmDeleteFolder = false },
            title = { Text(s["deleteFolderAction"]) },
            text = { Text(s["deleteFolderConfirm"]) },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.deleteSelectedFolder()
                        confirmDeleteFolder = false
                    },
                ) { Text(s["confirmAction"]) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteFolder = false }) { Text(s["cancelAction"]) }
            },
        )
    }

    if (confirmDeleteNote) {
        AlertDialog(
            onDismissRequest = { confirmDeleteNote = false },
            title = { Text(s["deleteNoteAction"]) },
            text = { Text(s["deleteNoteConfirm"]) },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.deleteSelectedNote()
                        confirmDeleteNote = false
                    },
                ) { Text(s["confirmAction"]) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteNote = false }) { Text(s["cancelAction"]) }
            },
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { Subtitle(s["notesSubtitle"]) }

        if (state.errorKey != null) {
            item {
                ErrorBanner(
                    message = s[state.errorKey!!],
                    onRetry = vm::load,
                    retryLabel = s["retry"],
                )
            }
        }

        item {
            ContentCard {
                CardTitle(
                    title = s["foldersTitle"],
                    action = {
                        IconButton(
                            onClick = {
                                folderName = ""
                                folderDialog = true
                            },
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = s["newFolder"], modifier = Modifier.size(20.dp))
                        }
                    },
                )
                Spacer(Modifier.height(10.dp))
                if (state.folders.isEmpty()) {
                    EmptyNote(s["noFolders"])
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        state.folders.forEach { folder ->
                            val selected = folder.id == state.selectedFolderId
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        if (selected) StudyFlowTheme.colors.primarySoft else androidx.compose.ui.graphics.Color.Transparent,
                                        RoundedCornerShape(8.dp),
                                    )
                                    .clickable {
                                        creatingNote = false
                                        vm.selectFolder(folder.id)
                                    }
                                    .padding(start = 10.dp, top = 6.dp, bottom = 6.dp, end = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(
                                    Icons.Filled.Folder,
                                    contentDescription = null,
                                    tint = if (selected) MaterialTheme.colorScheme.primary else StudyFlowTheme.colors.subtle,
                                    modifier = Modifier.size(17.dp),
                                )
                                Text(
                                    folder.name,
                                    modifier = Modifier.weight(1f),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                if (folder.notes.isNotEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .background(
                                                if (selected) MaterialTheme.colorScheme.surface
                                                else MaterialTheme.colorScheme.background,
                                                RoundedCornerShape(999.dp),
                                            )
                                            .padding(horizontal = 8.dp, vertical = 2.dp),
                                    ) {
                                        Text(
                                            folder.notes.size.toString(),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (selected) MaterialTheme.colorScheme.primary
                                            else StudyFlowTheme.colors.subtle,
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = {
                                        renamingFolder = folder.id
                                        folderName = folder.name
                                    },
                                    modifier = Modifier.size(32.dp),
                                ) {
                                    Icon(
                                        Icons.Filled.Edit,
                                        contentDescription = s["renameFolder"],
                                        modifier = Modifier.size(15.dp),
                                        tint = StudyFlowTheme.colors.subtle,
                                    )
                                }
                            }
                        }
                    }
                    if (state.selectedFolderId != null) {
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = { confirmDeleteFolder = true }) {
                            Icon(
                                Icons.Filled.DeleteOutline,
                                contentDescription = null,
                                tint = StudyFlowTheme.colors.danger,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.size(6.dp))
                            Text(s["deleteFolderAction"], color = StudyFlowTheme.colors.danger)
                        }
                    }
                }
            }
        }

        item {
            ContentCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            state.selectedFolder?.name ?: s["folderNotesTitle"],
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (state.selectedFolder != null) {
                            Text(
                                s.format("noteCount", "count" to state.folderNotes.size),
                                style = MaterialTheme.typography.bodySmall,
                                color = StudyFlowTheme.colors.subtle,
                            )
                        }
                    }
                    if (state.selectedFolderId != null && !creatingNote) {
                        TextButton(
                            onClick = {
                                creatingNote = true
                                title = ""
                                content = ""
                            },
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.size(4.dp))
                            Text(s["newNote"])
                        }
                    }
                }

                val notes = state.folderNotes
                if (notes.isNotEmpty()) {
                    Spacer(Modifier.height(10.dp))
                    NoteCards(
                        notes = notes,
                        selectedNoteId = if (creatingNote) null else state.selectedNoteId,
                        onSelect = { id ->
                            creatingNote = false
                            vm.selectNote(id)
                        },
                    )
                }

                Spacer(Modifier.height(12.dp))
                when {
                    creatingNote -> EditorBody(
                        title = title,
                        content = content,
                        onTitleChange = { title = it },
                        onContentChange = { content = it },
                        footer = s["newNote"],
                        creating = true,
                        canSave = !state.saving,
                        saving = state.saving,
                        onCancelCreate = {
                            creatingNote = false
                            title = state.selectedNote?.title.orEmpty()
                            content = state.selectedNote?.content.orEmpty()
                        },
                        onDelete = null,
                        onSave = { vm.createNote(title, content) },
                    )

                    state.selectedNote != null -> EditorBody(
                        title = title,
                        content = content,
                        onTitleChange = { title = it },
                        onContentChange = { content = it },
                        footer = state.errorKey?.let { s[it] } ?: state.savedMessageKey?.let { s[it] } ?: s["savedEarlier"],
                        creating = false,
                        canSave = !state.saving,
                        saving = state.saving,
                        onCancelCreate = null,
                        onDelete = { confirmDeleteNote = true },
                        onSave = { vm.saveSelected(title, content) },
                    )

                    state.selectedFolderId == null -> EmptyNote(s["selectFolderForNotes"])
                    notes.isNotEmpty() -> EmptyNote(s["selectNoteHint"])
                    else -> EmptyNote(s["noNotesInFolder"])
                }
            }
        }
    }
}

@Composable
private fun NoteCards(
    notes: List<com.studyflow.app.data.model.Note>,
    selectedNoteId: String?,
    onSelect: (String) -> Unit,
) {
    val s = LocalStrings.current
    val colors = StudyFlowTheme.colors

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        notes.forEach { note ->
            val selected = note.id == selectedNoteId
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (selected) colors.primarySoft else MaterialTheme.colorScheme.background,
                        RoundedCornerShape(12.dp),
                    )
                    .border(
                        1.dp,
                        if (selected) MaterialTheme.colorScheme.primary else colors.border,
                        RoundedCornerShape(12.dp),
                    )
                    .clickable { onSelect(note.id) }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    NoteFormat.title(note.title, s["untitledNote"]),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    NoteFormat.preview(note.content).ifEmpty { s["noNotePreview"] },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    s.format("noteWordCount", "count" to NoteFormat.wordCount(note.content)),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) MaterialTheme.colorScheme.primary else colors.subtle,
                )
            }
        }
    }
}

@Composable
private fun EditorBody(
    title: String,
    content: String,
    onTitleChange: (String) -> Unit,
    onContentChange: (String) -> Unit,
    footer: String,
    creating: Boolean,
    canSave: Boolean,
    saving: Boolean,
    onCancelCreate: (() -> Unit)?,
    onDelete: (() -> Unit)?,
    onSave: () -> Unit,
) {
    val s = LocalStrings.current

    androidx.compose.foundation.layout.Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
            value = title,
            onValueChange = onTitleChange,
            placeholder = { Text(s["selectNotePlaceholder"]) },
            label = { Text(s["noteTitleLabel"]) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = content,
            onValueChange = onContentChange,
            placeholder = { Text(s["contentPlaceholder"]) },
            modifier = Modifier.fillMaxWidth().heightIn(min = 220.dp),
            minLines = 8,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                footer,
                style = MaterialTheme.typography.bodySmall,
                color = if (footer == s["savedNow"]) StudyFlowTheme.colors.success else StudyFlowTheme.colors.subtle,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onCancelCreate != null) {
                    TextButton(onClick = onCancelCreate) { Text(s["cancelAction"]) }
                } else if (onDelete != null) {
                    TextButton(onClick = onDelete) {
                        Text(s["deleteNoteAction"], color = StudyFlowTheme.colors.danger)
                    }
                }
                Button(
                    onClick = onSave,
                    enabled = canSave && title.isNotBlank(),
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text(
                        when {
                            saving -> s["saving"]
                            creating -> s["createNote"]
                            else -> s["save"]
                        },
                    )
                }
            }
        }
    }
}
