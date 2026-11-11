package com.studyflow.app.domain.projects

import com.studyflow.app.domain.ApiDates
import java.time.Instant
import kotlin.math.abs
import kotlin.math.roundToLong

/** Prazos das versões, portados de `versionMeta`/`timelineVersions` do web. */
object VersionDeadline {
    private const val DAY_MS = 24L * 60 * 60 * 1000

    enum class Tone { NORMAL, SOON, OVERDUE }

    data class Meta(
        val tone: Tone,
        val days: Int?,
        val sortValue: Double,
    )

    fun meta(dueDate: String?, now: Instant = Instant.now()): Meta {
        val due = ApiDates.instantOrNull(dueDate)
            ?: return Meta(Tone.NORMAL, null, Double.POSITIVE_INFINITY)
        val days = ((due.toEpochMilli() - now.toEpochMilli()).toDouble() / DAY_MS).roundToLong().toInt()
        val tone = when {
            days < 0 -> Tone.OVERDUE
            days <= 3 -> Tone.SOON
            else -> Tone.NORMAL
        }
        val shownDays = if (days < 0) abs(days) else days
        return Meta(tone, shownDays, due.toEpochMilli().toDouble())
    }
}
