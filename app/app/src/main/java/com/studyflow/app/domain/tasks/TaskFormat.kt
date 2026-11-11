package com.studyflow.app.domain.tasks

import com.studyflow.app.domain.ApiDates
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Formatação de prazos, portada de `taskFormat.ts`. */
object TaskFormat {
    fun formatEndDate(endDate: String?, locale: Locale): String {
        if (endDate.isNullOrBlank()) return "—"
        val dateTime = ApiDates.localDateTimeOrNull(endDate) ?: return endDate
        return DateTimeFormatter.ofPattern("dd/MM, HH:mm", locale).format(dateTime)
    }

    private fun dayStamp(value: LocalDateTime, locale: Locale): String =
        DateTimeFormatter.ofPattern("dd/MM", locale).format(value)

    fun formatTaskPeriod(startDate: String?, endDate: String?, locale: Locale): String {
        val end = formatEndDate(endDate, locale)
        if (startDate.isNullOrBlank()) return end
        val start = ApiDates.localDateTimeOrNull(startDate) ?: return end
        val endDateValue = ApiDates.localDateTimeOrNull(endDate) ?: return formatEndDate(startDate, locale)
        if (start.toLocalDate() == endDateValue.toLocalDate()) return end
        return "${dayStamp(start, locale)} → $end"
    }

    /** Lista pronta para colar fora do app, no formato de checklist (`- [] título`). */
    fun formatTaskList(titles: List<String>): String = titles.joinToString("\n") { "- [] $it" }

    fun formatCompactDate(value: String?, locale: Locale): String {
        val dateTime = ApiDates.localDateTimeOrNull(value) ?: return value ?: "—"
        return DateTimeFormatter.ofPattern("dd MMM", locale).format(dateTime)
    }

    fun formatVersionDue(value: String?, locale: Locale): String {
        val dateTime = ApiDates.localDateTimeOrNull(value) ?: return value ?: "—"
        val date = DateTimeFormatter.ofPattern("dd MMM yyyy", locale).format(dateTime)
        val time = DateTimeFormatter.ofPattern("HH:mm", locale).format(dateTime)
        return "$date · $time"
    }

    fun formatDayChip(key: String, locale: Locale): String {
        val date = runCatching { StudyCalendar.parseDayKey(key) }.getOrNull() ?: return key
        return DateTimeFormatter.ofPattern("EEE, dd/MM", locale).format(date)
    }

    fun formatDateTime(value: LocalDateTime, locale: Locale): String =
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", locale).format(value)
}
