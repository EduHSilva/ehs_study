package com.studyflow.app.feature.notes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.FullscreenExit
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studyflow.app.core.i18n.LocalStrings
import com.studyflow.app.core.ui.components.CardTitle
import com.studyflow.app.core.ui.components.ContentCard
import com.studyflow.app.core.ui.components.EmptyNote
import com.studyflow.app.core.ui.components.ErrorBanner
import com.studyflow.app.core.ui.components.LoadingScreen
import com.studyflow.app.core.ui.components.RefreshableLazyColumn
import com.studyflow.app.core.ui.components.Subtitle
import com.studyflow.app.core.ui.theme.StudyFlowTheme
import com.studyflow.app.domain.notes.MarkdownEdit
import com.studyflow.app.domain.notes.NoteFormat
import com.studyflow.app.domain.notes.NoteMarkdownEditor
import com.studyflow.app.ui.appViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch

@Composable
fun NotesScreen() {
    val vm: NotesViewModel = appViewModel { NotesViewModel(it.notesRepository, it.settings) }
    val state by vm.state.collectAsStateWithLifecycle()
    val requestedNoteId by NotesNavigation.requestedNoteId.collectAsStateWithLifecycle()
    val s = LocalStrings.current
    val scope = rememberCoroutineScope()
    val inlineFocusRequester = remember { FocusRequester() }
    val fullscreenFocusRequester = remember { FocusRequester() }

    LaunchedEffect(requestedNoteId, state.folders) {
        val id = requestedNoteId ?: return@LaunchedEffect
        if (state.folders.isEmpty()) return@LaunchedEffect
        vm.openNote(id)
        NotesNavigation.clear()
    }

    var title by rememberSaveable { mutableStateOf("") }
    var content by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) }
    var creatingNote by rememberSaveable { mutableStateOf(false) }
    var previewing by rememberSaveable { mutableStateOf(false) }
    var expanded by rememberSaveable { mutableStateOf(false) }

    var folderDialog by rememberSaveable { mutableStateOf(false) }
    var folderName by rememberSaveable { mutableStateOf("") }
    var renamingFolder by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmDeleteFolder by rememberSaveable { mutableStateOf(false) }
    var confirmDeleteNote by rememberSaveable { mutableStateOf(false) }
    var movingNote by rememberSaveable { mutableStateOf(false) }

    var unsavedDialogOpen by remember { mutableStateOf(false) }
    var pendingUnsaved by remember { mutableStateOf<CompletableDeferred<Boolean>?>(null) }

    LaunchedEffect(state.selectedNoteId) {
        val note = state.selectedNote
        title = note?.title.orEmpty()
        content = TextFieldValue(note?.content.orEmpty())
        creatingNote = false
        movingNote = false
        if (note == null) expanded = false
    }

    val isDirty = when {
        creatingNote -> title.trim().isNotEmpty() || content.text.trim().isNotEmpty()
        state.selectedNote != null ->
            title != state.selectedNote?.title || content.text != state.selectedNote?.content
        else -> false
    }

    fun closeUnsaved(allow: Boolean) {
        unsavedDialogOpen = false
        pendingUnsaved?.complete(allow)
        pendingUnsaved = null
    }

    suspend fun askUnsaved(): Boolean {
        if (!isDirty) return true
        pendingUnsaved?.let { return it.await() }
        val deferred = CompletableDeferred<Boolean>()
        pendingUnsaved = deferred
        unsavedDialogOpen = true
        return deferred.await()
    }

    val leaveHandler: suspend () -> Boolean = {
        when {
            unsavedDialogOpen -> {
                closeUnsaved(false)
                false
            }

            expanded -> {
                expanded = false
                false
            }

            else -> askUnsaved()
        }
    }
    val currentLeaveHandler by rememberUpdatedState(leaveHandler)
    DisposableEffect(Unit) {
        NotesLeaveGuard.register { currentLeaveHandler() }
        onDispose {
            NotesLeaveGuard.clear()
            pendingUnsaved?.complete(false)
            pendingUnsaved = null
        }
    }

    fun applyEdit(edit: MarkdownEdit) {
        content = TextFieldValue(edit.text, TextRange(edit.selectionStart, edit.selectionEnd))
        val target = if (expanded) fullscreenFocusRequester else inlineFocusRequester
        target.requestFocus()
    }

    fun wrapSelection(marker: String) {
        applyEdit(
            NoteMarkdownEditor.wrap(
                text = content.text,
                selectionStart = content.selection.start,
                selectionEnd = content.selection.end,
                marker = marker,
            ),
        )
    }

    fun applyHeading(level: Int) {
        applyEdit(
            NoteMarkdownEditor.heading(
                text = content.text,
                selectionStart = content.selection.start,
                selectionEnd = content.selection.end,
                level = level,
            ),
        )
    }

    fun applyPrefix(prefix: String) {
        applyEdit(
            NoteMarkdownEditor.prefixLines(
                text = content.text,
                selectionStart = content.selection.start,
                selectionEnd = content.selection.end,
                prefix = prefix,
            ),
        )
    }

    suspend fun persistEditor(): Boolean {
        val success = if (creatingNote) {
            vm.createNote(title, content.text)
        } else {
            vm.saveSelected(title, content.text)
        }
        if (success) {
            creatingNote = false
            vm.state.value.selectedNote?.let { note ->
                title = note.title
                content = TextFieldValue(note.content)
            }
        }
        return success
    }

    val folderTitle = state.selectedFolder?.name ?: s["folderNotesTitle"]
    val footer = state.errorKey?.let { s[it] }
        ?: state.savedMessageKey?.let { s[it] }
        ?: s["savedEarlier"]
    val canSave = !state.saving && title.isNotBlank()

    if (state.loading) {
        LoadingScreen(s["notesLoading"])
        return
    }

    if (unsavedDialogOpen) {
        AlertDialog(
            onDismissRequest = { if (!state.saving) closeUnsaved(false) },
            title = { Text(s["unsavedChangesTitle"]) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(s["unsavedChangesText"])
                    state.errorKey?.let {
                        Text(s[it], color = StudyFlowTheme.colors.danger, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { scope.launch { if (persistEditor()) closeUnsaved(true) } },
                    enabled = !state.saving,
                ) {
                    Text(if (state.saving) s["saving"] else s["save"])
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = { closeUnsaved(true) },
                        enabled = !state.saving,
                    ) { Text(s["discardChanges"]) }
                    TextButton(
                        onClick = { closeUnsaved(false) },
                        enabled = !state.saving,
                    ) { Text(s["cancelAction"]) }
                }
            },
        )
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

    if (movingNote) {
        val note = state.selectedNote
        val targets = state.folders.filter { it.id != state.selectedFolderId }
        AlertDialog(
            onDismissRequest = { movingNote = false },
            title = { Text(s["moveNoteAction"]) },
            text = {
                if (targets.isEmpty()) {
                    Text(s["noOtherFolders"])
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        targets.forEach { folder ->
                            TextButton(
                                onClick = {
                                    note?.let { vm.moveNote(it.id, folder.id) }
                                    movingNote = false
                                },
                            ) { Text(folder.name) }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { movingNote = false }) { Text(s["cancelAction"]) }
            },
        )
    }

    if (expanded && (creatingNote || state.selectedNote != null)) {
        Dialog(
            onDismissRequest = { expanded = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
            ) {
                NoteEditor(
                    title = title,
                    content = content,
                    onTitleChange = { title = it },
                    onContentChange = { content = it },
                    previewing = previewing,
                    expanded = true,
                    focusRequester = fullscreenFocusRequester,
                    onWrap = ::wrapSelection,
                    onHeading = ::applyHeading,
                    onPrefix = ::applyPrefix,
                    onTogglePreview = { previewing = !previewing },
                    onToggleExpanded = { expanded = false },
                    footer = footer,
                    creating = creatingNote,
                    canSave = canSave,
                    saving = state.saving,
                    onCancelCreate = if (creatingNote) {
                        {
                            creatingNote = false
                            expanded = false
                            title = state.selectedNote?.title.orEmpty()
                            content = TextFieldValue(state.selectedNote?.content.orEmpty())
                        }
                    } else {
                        null
                    },
                    onMove = if (!creatingNote) {
                        {
                            scope.launch {
                                if (!askUnsaved()) return@launch
                                val note = vm.state.value.selectedNote ?: return@launch
                                title = note.title
                                content = TextFieldValue(note.content)
                                movingNote = true
                            }
                        }
                    } else {
                        null
                    },
                    onDelete = if (!creatingNote) {
                        { confirmDeleteNote = true }
                    } else {
                        null
                    },
                    onSave = { scope.launch { persistEditor() } },
                    modifier = Modifier
                        .fillMaxSize()
                        .safeDrawingPadding()
                        .imePadding()
                        .padding(16.dp),
                )
            }
        }
    }

    RefreshableLazyColumn(
        isRefreshing = state.refreshing,
        onRefresh = { vm.load(silent = true) },
        modifier = Modifier.fillMaxSize().imePadding(),
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
                                        scope.launch {
                                            if (folder.id == state.selectedFolderId) return@launch
                                            if (!askUnsaved()) return@launch
                                            creatingNote = false
                                            val target = state.folders.firstOrNull { it.id == folder.id }
                                            val note = target?.notes?.firstOrNull()
                                            title = note?.title.orEmpty()
                                            content = TextFieldValue(note?.content.orEmpty())
                                            vm.selectFolder(folder.id)
                                        }
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
                            folderTitle,
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
                                scope.launch {
                                    if (!askUnsaved()) return@launch
                                    creatingNote = true
                                    previewing = false
                                    title = ""
                                    content = TextFieldValue("")
                                }
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
                            scope.launch {
                                if (id == state.selectedNoteId && !creatingNote) return@launch
                                if (!askUnsaved()) return@launch
                                creatingNote = false
                                val note = state.folderNotes.firstOrNull { it.id == id }
                                title = note?.title.orEmpty()
                                content = TextFieldValue(note?.content.orEmpty())
                                vm.selectNote(id)
                            }
                        },
                    )
                }

                Spacer(Modifier.height(12.dp))
                when {
                    creatingNote || state.selectedNote != null -> NoteEditor(
                        title = title,
                        content = content,
                        onTitleChange = { title = it },
                        onContentChange = { content = it },
                        previewing = previewing,
                        expanded = false,
                        focusRequester = inlineFocusRequester,
                        onWrap = ::wrapSelection,
                        onHeading = ::applyHeading,
                        onPrefix = ::applyPrefix,
                        onTogglePreview = { previewing = !previewing },
                        onToggleExpanded = { expanded = true },
                        footer = footer,
                        creating = creatingNote,
                        canSave = canSave,
                        saving = state.saving,
                        onCancelCreate = if (creatingNote) {
                            {
                                creatingNote = false
                                title = state.selectedNote?.title.orEmpty()
                                content = TextFieldValue(state.selectedNote?.content.orEmpty())
                            }
                        } else {
                            null
                        },
                        onMove = if (!creatingNote) {
                            {
                                scope.launch {
                                    if (!askUnsaved()) return@launch
                                    val note = vm.state.value.selectedNote ?: return@launch
                                    title = note.title
                                    content = TextFieldValue(note.content)
                                    movingNote = true
                                }
                            }
                        } else {
                            null
                        },
                        onDelete = if (!creatingNote) {
                            { confirmDeleteNote = true }
                        } else {
                            null
                        },
                        onSave = { scope.launch { persistEditor() } },
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
private fun NoteEditor(
    title: String,
    content: TextFieldValue,
    onTitleChange: (String) -> Unit,
    onContentChange: (TextFieldValue) -> Unit,
    previewing: Boolean,
    expanded: Boolean,
    focusRequester: FocusRequester,
    onWrap: (String) -> Unit,
    onHeading: (Int) -> Unit,
    onPrefix: (String) -> Unit,
    onTogglePreview: () -> Unit,
    onToggleExpanded: () -> Unit,
    footer: String,
    creating: Boolean,
    canSave: Boolean,
    saving: Boolean,
    onCancelCreate: (() -> Unit)?,
    onMove: (() -> Unit)?,
    onDelete: (() -> Unit)?,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val s = LocalStrings.current
    val colors = StudyFlowTheme.colors

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        EditorToolbar(
            previewing = previewing,
            expanded = expanded,
            formattingEnabled = !previewing && !saving,
            onWrap = onWrap,
            onHeading = onHeading,
            onPrefix = onPrefix,
            onTogglePreview = onTogglePreview,
            onToggleExpanded = onToggleExpanded,
        )
        OutlinedTextField(
            value = title,
            onValueChange = onTitleChange,
            placeholder = { Text(s["selectNotePlaceholder"]) },
            label = { Text(s["noteTitleLabel"]) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        if (previewing) {
            val previewModifier = if (expanded) {
                Modifier.weight(1f).fillMaxWidth()
            } else {
                Modifier.fillMaxWidth().heightIn(min = 220.dp, max = 420.dp)
            }
            Surface(
                modifier = previewModifier,
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.background,
                border = BorderStroke(1.dp, colors.border),
            ) {
                MarkdownPreview(
                    markdown = content.text,
                    modifier = Modifier.fillMaxSize().padding(14.dp),
                )
            }
        } else {
            val fieldModifier = if (expanded) {
                Modifier.weight(1f).fillMaxWidth()
            } else {
                Modifier.fillMaxWidth().heightIn(min = 220.dp)
            }
            OutlinedTextField(
                value = content,
                onValueChange = onContentChange,
                placeholder = { Text(s["contentPlaceholder"]) },
                modifier = fieldModifier.focusRequester(focusRequester),
                minLines = if (expanded) 1 else 8,
            )
        }
        EditorFooter(
            footer = footer,
            creating = creating,
            canSave = canSave,
            saving = saving,
            onCancelCreate = onCancelCreate,
            onMove = onMove,
            onDelete = onDelete,
            onSave = onSave,
        )
    }
}

@Composable
private fun EditorToolbar(
    previewing: Boolean,
    expanded: Boolean,
    formattingEnabled: Boolean,
    onWrap: (String) -> Unit,
    onHeading: (Int) -> Unit,
    onPrefix: (String) -> Unit,
    onTogglePreview: () -> Unit,
    onToggleExpanded: () -> Unit,
) {
    val s = LocalStrings.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            ToolButton(Icons.Filled.FormatBold, s["formatBold"], formattingEnabled) { onWrap("**") }
            ToolButton(Icons.Filled.FormatItalic, s["formatItalic"], formattingEnabled) { onWrap("*") }
            ToolButton(Icons.Filled.Title, s["formatHeading1"], formattingEnabled) { onHeading(1) }
            ToolButton(Icons.Filled.FormatSize, s["formatHeading2"], formattingEnabled) { onHeading(2) }
            ToolButton(Icons.AutoMirrored.Filled.FormatListBulleted, s["formatBulletList"], formattingEnabled) { onPrefix("- ") }
            ToolButton(Icons.Filled.FormatListNumbered, s["formatNumberedList"], formattingEnabled) { onPrefix("1. ") }
            ToolButton(Icons.Filled.FormatQuote, s["formatQuote"], formattingEnabled) { onPrefix("> ") }
            ToolButton(Icons.Filled.Code, s["formatCode"], formattingEnabled) { onWrap("`") }
        }
        ToolButton(
            icon = if (previewing) Icons.Filled.Edit else Icons.Filled.Visibility,
            label = if (previewing) s["editAction"] else s["previewAction"],
            enabled = true,
            onClick = onTogglePreview,
        )
        ToolButton(
            icon = if (expanded) Icons.Filled.FullscreenExit else Icons.Filled.Fullscreen,
            label = if (expanded) s["collapseNote"] else s["expandNote"],
            enabled = true,
            onClick = onToggleExpanded,
        )
    }
}

@Composable
private fun ToolButton(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(36.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(18.dp),
            tint = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else StudyFlowTheme.colors.subtle,
        )
    }
}

@Composable
private fun EditorFooter(
    footer: String,
    creating: Boolean,
    canSave: Boolean,
    saving: Boolean,
    onCancelCreate: (() -> Unit)?,
    onMove: (() -> Unit)?,
    onDelete: (() -> Unit)?,
    onSave: () -> Unit,
) {
    val s = LocalStrings.current
    val colors = StudyFlowTheme.colors

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = footer,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            color = if (footer == s["savedNow"]) colors.success else colors.subtle,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.width(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (onCancelCreate != null) {
                TextButton(onClick = onCancelCreate) { Text(s["cancelAction"]) }
            } else if (onDelete != null) {
                if (onMove != null) {
                    TextButton(onClick = onMove) { Text(s["moveNoteAction"]) }
                }
                TextButton(onClick = onDelete) {
                    Text(s["deleteNoteAction"], color = colors.danger)
                }
            }
            Button(
                onClick = onSave,
                enabled = canSave,
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
