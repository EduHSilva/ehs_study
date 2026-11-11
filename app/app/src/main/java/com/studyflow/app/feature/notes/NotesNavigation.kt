package com.studyflow.app.feature.notes

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// Nota que outra tela (o dashboard) pediu para abrir. A tela de notas consome o pedido quando as
// pastas chegam, porque só aí dá para localizar a nota.
object NotesNavigation {
    private val _requestedNoteId = MutableStateFlow<String?>(null)
    val requestedNoteId: StateFlow<String?> = _requestedNoteId.asStateFlow()

    fun request(id: String) {
        _requestedNoteId.value = id
    }

    fun clear() {
        _requestedNoteId.value = null
    }
}
