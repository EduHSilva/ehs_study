package com.studyflow.app.di

import android.content.Context
import com.studyflow.app.BuildConfig
import com.studyflow.app.core.network.ApiClient
import com.studyflow.app.core.prefs.AppPreferences
import com.studyflow.app.core.session.SessionManager
import com.studyflow.app.core.settings.AppSettings
import com.studyflow.app.data.remote.AuthApi
import com.studyflow.app.data.remote.DashboardApi
import com.studyflow.app.data.remote.FocusApi
import com.studyflow.app.data.remote.NotesApi
import com.studyflow.app.data.remote.ProjectsApi
import com.studyflow.app.data.remote.ReviewApi
import com.studyflow.app.data.remote.TasksApi
import com.studyflow.app.data.repository.AuthRepository
import com.studyflow.app.data.repository.DashboardRepository
import com.studyflow.app.data.repository.FocusRepository
import com.studyflow.app.data.repository.NotesRepository
import com.studyflow.app.data.repository.ProjectsRepository
import com.studyflow.app.data.repository.ReviewRepository
import com.studyflow.app.data.repository.TasksRepository
import com.studyflow.app.feature.pomodoro.PomodoroTimer
import kotlinx.serialization.json.Json

/** Injeção manual (sem framework): a aplicação cria e expõe as dependências. */
class AppContainer(context: Context) {
    private val prefs = AppPreferences(context)

    val settings = AppSettings(prefs)
    val pomodoroTimer = PomodoroTimer(prefs)
    val sessionManager = SessionManager(
        prefs = prefs,
        json = Json { ignoreUnknownKeys = true },
    )

    private val apiClient = ApiClient(sessionManager)
    private val retrofit = apiClient.retrofit

    val authRepository = AuthRepository(retrofit.create(AuthApi::class.java), sessionManager)
    val dashboardRepository = DashboardRepository(retrofit.create(DashboardApi::class.java))
    val tasksRepository = TasksRepository(retrofit.create(TasksApi::class.java))
    val notesRepository = NotesRepository(retrofit.create(NotesApi::class.java))
    val focusRepository = FocusRepository(retrofit.create(FocusApi::class.java))
    val projectsRepository = ProjectsRepository(retrofit.create(ProjectsApi::class.java))
    val reviewRepository = ReviewRepository(retrofit.create(ReviewApi::class.java))

    val googleWebClientId: String = BuildConfig.GOOGLE_WEB_CLIENT_ID
    val googleLoginEnabled: Boolean get() = googleWebClientId.isNotBlank()
}
