package com.studyflow.app.feature.notes

/**
 * Ponte entre a navegação global (drawer, barra inferior e voltar) e o editor de notas.
 * A tela registra o handler enquanto está composta; `true` libera a saída.
 */
object NotesLeaveGuard {
    private var handler: (suspend () -> Boolean)? = null

    fun register(next: suspend () -> Boolean) {
        handler = next
    }

    fun clear() {
        handler = null
    }

    suspend fun canLeave(): Boolean = handler?.invoke() ?: true
}
