package com.studyflow.app.domain

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Conversão das datas da API. O web usa `new Date(value)` (fuso local) para timestamps e trata
 * `yyyy-MM-dd` como dia puro — [dayKeyFrom] reproduz exatamente isso.
 */
object ApiDates {
    private val dateOnly = Regex("^\\d{4}-\\d{2}-\\d{2}$")

    fun instantOrNull(value: String?): Instant? {
        if (value.isNullOrBlank()) return null
        return runCatching { Instant.parse(value) }.getOrNull()
            ?: runCatching { OffsetDateTime.parse(value).toInstant() }.getOrNull()
            ?: runCatching {
                LocalDateTime.parse(value).atZone(ZoneId.systemDefault()).toInstant()
            }.getOrNull()
    }

    fun localDateOrNull(value: String?): LocalDate? {
        if (value.isNullOrBlank()) return null
        if (dateOnly.matches(value)) return runCatching { LocalDate.parse(value) }.getOrNull()
        return instantOrNull(value)?.atZone(ZoneId.systemDefault())?.toLocalDate()
    }

    fun localDateTimeOrNull(value: String?): LocalDateTime? {
        if (value.isNullOrBlank()) return null
        if (dateOnly.matches(value)) return runCatching { LocalDate.parse(value).atStartOfDay() }.getOrNull()
        return instantOrNull(value)?.atZone(ZoneId.systemDefault())?.toLocalDateTime()
    }

    /** Devolve a chave do dia local; strings `yyyy-MM-dd` são mantidas como estão. */
    fun dayKeyFrom(value: String?): String? {
        if (value.isNullOrBlank()) return null
        if (dateOnly.matches(value)) return value
        return instantOrNull(value)?.atZone(ZoneId.systemDefault())?.toLocalDate()?.toString()
    }

    fun toIso(value: LocalDateTime): String =
        value.atZone(ZoneId.systemDefault()).toInstant().toString()

    fun formatTime(value: String?, locale: java.util.Locale): String? {
        val instant = instantOrNull(value) ?: return null
        return DateTimeFormatter.ofPattern("HH:mm", locale)
            .format(instant.atZone(ZoneId.systemDefault()))
    }

    fun formatDateTime(value: String, locale: java.util.Locale): String {
        val dateTime = localDateTimeOrNull(value) ?: return value
        return DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", locale).format(dateTime)
    }
}
