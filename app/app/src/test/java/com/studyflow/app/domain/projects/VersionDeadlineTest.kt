package com.studyflow.app.domain.projects

import com.studyflow.app.domain.projects.VersionDeadline.Tone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class VersionDeadlineTest {

    private val now = Instant.parse("2026-01-10T12:00:00Z")

    @Test
    fun `data invalida nao tem prazo`() {
        val meta = VersionDeadline.meta("data-quebrada", now)

        assertEquals(Tone.NORMAL, meta.tone)
        assertNull(meta.days)
        assertEquals(Double.POSITIVE_INFINITY, meta.sortValue, 0.0)
    }

    @Test
    fun `hoje e logo`() {
        val meta = VersionDeadline.meta("2026-01-10T12:00:00Z", now)

        assertEquals(Tone.SOON, meta.tone)
        assertEquals(0, meta.days)
    }

    @Test
    fun `tres dias ainda e logo`() {
        val meta = VersionDeadline.meta("2026-01-13T12:00:00Z", now)

        assertEquals(Tone.SOON, meta.tone)
        assertEquals(3, meta.days)
    }

    @Test
    fun `mais de tres dias e normal`() {
        val meta = VersionDeadline.meta("2026-01-14T12:00:00Z", now)

        assertEquals(Tone.NORMAL, meta.tone)
        assertEquals(4, meta.days)
    }

    @Test
    fun `atrasada mostra dias em modulo`() {
        val meta = VersionDeadline.meta("2026-01-08T12:00:00Z", now)

        assertEquals(Tone.OVERDUE, meta.tone)
        assertEquals(2, meta.days)
        assertEquals(Instant.parse("2026-01-08T12:00:00Z").toEpochMilli().toDouble(), meta.sortValue, 0.0)
    }
}
