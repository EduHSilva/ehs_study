package com.studyflow.app.data.repository

import com.studyflow.app.core.network.unwrap
import com.studyflow.app.data.mappers.Mappers
import com.studyflow.app.data.model.DashboardData
import com.studyflow.app.data.model.FocusSession
import com.studyflow.app.data.model.Note
import com.studyflow.app.data.model.NoteFolder
import com.studyflow.app.data.model.ReviewReport
import com.studyflow.app.data.remote.CreateFocusRequest
import com.studyflow.app.data.remote.DashboardApi
import com.studyflow.app.data.remote.FocusApi
import com.studyflow.app.data.remote.FolderRequest
import com.studyflow.app.data.remote.NotesApi
import com.studyflow.app.data.remote.ReviewApi
import com.studyflow.app.data.remote.SaveNoteRequest
import java.time.LocalDate

class DashboardRepository(private val api: DashboardApi) {
    suspend fun load(): DashboardData = Mappers.dashboard(api.get().unwrap())
}

class NotesRepository(private val api: NotesApi) {
    suspend fun getFolders(): List<NoteFolder> =
        api.folders(page = 0, size = LIST_PAGE_SIZE).unwrap().items.orEmpty().map(Mappers::folder)

    suspend fun saveNote(id: String, title: String, content: String): Note =
        Mappers.note(api.saveNote(id, SaveNoteRequest(title, content)).unwrap(), null)

    suspend fun createFolder(name: String): NoteFolder =
        Mappers.folder(api.createFolder(FolderRequest(name)).unwrap())

    suspend fun createNote(folderId: String, title: String, content: String): Note =
        Mappers.note(api.createNote(folderId, SaveNoteRequest(title, content)).unwrap(), folderId)

    suspend fun renameFolder(id: String, name: String): NoteFolder =
        Mappers.folder(api.renameFolder(id, FolderRequest(name)).unwrap())

    suspend fun deleteNote(id: String) {
        api.deleteNote(id)
    }

    suspend fun deleteFolder(id: String) {
        api.deleteFolder(id)
    }

    private companion object {
        const val LIST_PAGE_SIZE = 100
    }
}

class FocusRepository(private val api: FocusApi) {
    /** O back interpreta `date` no fuso local do servidor, então mandamos o dia do aparelho. */
    suspend fun getTodaySessions(): List<FocusSession> =
        api.sessions(
            page = 0,
            size = LIST_PAGE_SIZE,
            date = LocalDate.now().toString(),
        ).unwrap().items.orEmpty().map(Mappers::focus)

    suspend fun createSession(title: String, minutes: Int): FocusSession =
        Mappers.focus(api.create(CreateFocusRequest(title, minutes)).unwrap())

    private companion object {
        const val LIST_PAGE_SIZE = 100
    }
}

class ReviewRepository(private val api: ReviewApi) {
    suspend fun getReport(): ReviewReport = Mappers.review(api.report().unwrap())
}
