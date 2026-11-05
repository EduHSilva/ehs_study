package com.studyflow.app.navigation

object Routes {
    const val DASHBOARD = "dashboard"
    const val POMODORO = "pomodoro"
    const val NOTES = "notes"
    const val TASKS = "tasks"
    const val PROJECTS = "projects"
    const val PROFILE = "profile"
    const val REVIEW = "review"
    const val PROJECT_DETAIL = "project/{projectId}"

    fun projectDetail(id: String): String = "project/$id"
}
