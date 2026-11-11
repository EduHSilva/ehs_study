package com.studyflow.app.domain.projects

import com.studyflow.app.data.model.Project
import com.studyflow.app.data.model.ProjectStatus

/** Porte de `sortProjects` do web: pendentes, em andamento e concluídos no fim. */
object ProjectSorting {
    private fun rank(status: ProjectStatus): Int = when (status) {
        ProjectStatus.WAITING -> 0
        ProjectStatus.WORKING -> 1
        ProjectStatus.DONE -> 2
    }

    fun sort(projects: List<Project>): List<Project> =
        projects.sortedWith(
            compareBy<Project> { rank(it.status) }
                .thenBy(String.CASE_INSENSITIVE_ORDER) { it.title },
        )
}
