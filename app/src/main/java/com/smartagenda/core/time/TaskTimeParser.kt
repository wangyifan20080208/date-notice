package com.smartagenda.core.time

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object TaskTimeParser {

    private val patterns = listOf(
        DateTimeFormatter.ISO_LOCAL_DATE_TIME,
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"),
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
        DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm"),
    )

    fun parse(raw: String?): Long? {
        val value = raw?.trim().orEmpty()
        if (value.isEmpty() || value.equals("null", ignoreCase = true)) return null

        val candidates = listOf(value, value.replace(' ', 'T'))
        val zone = ZoneId.systemDefault()

        for (formatter in patterns) {
            for (candidate in candidates) {
                val parsed = runCatching {
                    LocalDateTime.parse(candidate, formatter)
                }.getOrNull()
                if (parsed != null) {
                    return parsed.atZone(zone).toInstant().toEpochMilli()
                }
            }
        }

        val date = runCatching { LocalDate.parse(value) }.getOrNull()
        if (date != null) {
            return date.atStartOfDay(zone).toInstant().toEpochMilli()
        }

        return null
    }
}