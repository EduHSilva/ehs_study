package com.studyflow.app.domain.projects

import com.studyflow.app.data.model.Project
import com.studyflow.app.data.model.ProjectStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class ProjectSortingTest {

    private fun project(title: String, status: ProjectStatus) = Project(
        id = title,
        title = title,
        progress = 0,
        status = status,
        advisor = "",
    )

    @Test
    fun `pendentes, em andamento e concluidos ficam no fim`() {
        val sorted = ProjectSorting.sort(
            listOf(
                project("Zeta", ProjectStatus.DONE),
                project("Beta", ProjectStatus.WORKING),
                project("Alfa", ProjectStatus.WORKING),
                project("Omega", ProjectStatus.DONE),
                project("Delta", ProjectStatus.WAITING),
            ),
        )

        assertEquals(
            listOf("Delta", "Alfa", "Beta", "Omega", "Zeta"),
            sorted.map { it.title },
        )
    }

    @Test
    fun `ordem alfabetica ignora caixa`() {
        val sorted = ProjectSorting.sort(
            listOf(
                project("beta", ProjectStatus.WORKING),
                project("Alfa", ProjectStatus.WORKING),
            ),
        )

        assertEquals(listOf("Alfa", "beta"), sorted.map { it.title })
    }
}
