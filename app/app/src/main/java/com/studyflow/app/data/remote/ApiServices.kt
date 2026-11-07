package com.studyflow.app.data.remote

import com.studyflow.app.core.network.EmptyEnvelope
import com.studyflow.app.core.network.Envelope
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface AuthApi {
    @POST("users/auth/login")
    suspend fun login(@Body body: LoginRequest): Envelope<LoginDto>

    @GET("users/auth/me")
    suspend fun me(): Envelope<LoginDto>

    @POST("users/auth/register")
    suspend fun register(@Body body: RegisterRequest): Envelope<RegisterDto>

    @PUT("users/{id}")
    suspend fun updateUser(@Path("id") id: String, @Body body: UpdateUserRequest): Envelope<RegisterDto>
}

interface TasksApi {
    @GET("tasks")
    suspend fun list(
        @Query("page") page: Int,
        @Query("size") size: Int,
        @Query("start") start: String? = null,
        @Query("end") end: String? = null,
        @Query("projectId") projectId: String? = null,
        @Query("stageId") stageId: String? = null,
    ): Envelope<TaskPageDto>

    @POST("tasks")
    suspend fun create(@Body body: CreateTaskRequest): Envelope<TaskDto>

    @PUT("tasks/{id}")
    suspend fun update(@Path("id") id: String, @Body body: UpdateTaskRequest): Envelope<TaskDto>

    @DELETE("tasks/{id}")
    suspend fun delete(@Path("id") id: String): EmptyEnvelope
}

interface DashboardApi {
    @GET("study/dashboard")
    suspend fun get(): Envelope<DashboardDto>
}

interface NotesApi {
    @GET("study/notes/folders")
    suspend fun folders(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 100,
    ): Envelope<FolderPageDto>

    @POST("study/notes/folders")
    suspend fun createFolder(@Body body: FolderRequest): Envelope<FolderDto>

    @PUT("study/notes/folders/{id}")
    suspend fun renameFolder(@Path("id") id: String, @Body body: FolderRequest): Envelope<FolderDto>

    @DELETE("study/notes/folders/{id}")
    suspend fun deleteFolder(@Path("id") id: String): EmptyEnvelope

    @POST("study/notes/{folderId}")
    suspend fun createNote(@Path("folderId") folderId: String, @Body body: SaveNoteRequest): Envelope<NoteDto>

    @PUT("study/notes/{id}")
    suspend fun saveNote(@Path("id") id: String, @Body body: SaveNoteRequest): Envelope<NoteDto>

    @DELETE("study/notes/{id}")
    suspend fun deleteNote(@Path("id") id: String): EmptyEnvelope
}

interface FocusApi {
    @GET("study/focus")
    suspend fun sessions(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 100,
        @Query("date") date: String,
    ): Envelope<FocusPageDto>

    @POST("study/focus")
    suspend fun create(@Body body: CreateFocusRequest): Envelope<FocusSessionDto>
}

interface ProjectsApi {
    @GET("study/projects")
    suspend fun list(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 100,
    ): Envelope<ProjectPageDto>

    @GET("study/projects/{id}")
    suspend fun details(@Path("id") id: String): Envelope<ProjectDto>

    @POST("study/projects")
    suspend fun create(@Body body: CreateProjectRequest): Envelope<ProjectDto>

    @POST("study/project/stage")
    suspend fun createStage(@Body body: StageRequest): Envelope<StageDto>

    @PUT("study/project/stage/{id}")
    suspend fun updateStage(@Path("id") id: String, @Body body: StageRequest): Envelope<StageDto>

    @POST("study/project/version")
    suspend fun createVersion(@Body body: VersionRequest): Envelope<VersionDto>

    @PUT("study/project/version/{id}")
    suspend fun updateVersion(@Path("id") id: String, @Body body: VersionRequest): Envelope<VersionDto>
}

interface ReviewApi {
    @GET("study/review/report")
    suspend fun report(): Envelope<ReviewReportDto>
}
