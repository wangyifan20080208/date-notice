package com.smartagenda.core.time

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

enum class TaskSection(val label: String) {
    TODAY("今天"),
    TOMORROW("明天"),
    UPCOMING("未来"),
    MISSING("待补充时间"),
}

object TimeFormatter {

    private val zone: ZoneId get() = ZoneId.systemDefault()

    private val timeOnly = DateTimeFormatter.ofPattern("HH:mm", Locale.CHINA)
    private val dayWithWeek = DateTimeFormatter.ofPattern("M月d日 EEE", Locale.CHINA)
    private val fullPattern = DateTimeFormatter.ofPattern("M月d日 EEE HH:mm", Locale.CHINA)

    fun startOfToday(now: Long = System.currentTimeMillis()): Long =
        toLocalDate(now).atStartOfDay(zone).toInstant().toEpochMilli()

    fun startOfTomorrow(now: Long = System.currentTimeMillis()): Long =
        toLocalDate(now).plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

    fun endOfDay(datetime: Long): Long =
        toLocalDate(datetime).plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1

    fun dayOffset(datetime: Long, now: Long = System.currentTimeMillis()): Int =
        ChronoUnit.DAYS.between(toLocalDate(now), toLocalDate(datetime)).toInt()

    fun sectionFor(datetime: Long?, now: Long = System.currentTimeMillis()): TaskSection = when {
        datetime == null -> TaskSection.MISSING
        dayOffset(datetime, now) <= 0 -> TaskSection.TODAY
        dayOffset(datetime, now) == 1 -> TaskSection.TOMORROW
        else -> TaskSection.UPCOMING
    }

    fun isOverdue(datetime: Long?, now: Long = System.currentTimeMillis()): Boolean =
        datetime != null && datetime < now

    fun formatTime(datetime: Long): String =
        timeOnly.format(toLocalDateTime(datetime))

    fun formatDate(datetime: Long): String =
        dayWithWeek.format(toLocalDateTime(datetime))

    fun formatFull(datetime: Long): String =
        fullPattern.format(toLocalDateTime(datetime))

    fun formatSectionDate(section: TaskSection, datetime: Long): String = when (section) {
        TaskSection.TODAY -> "今天 · ${formatDate(datetime)}"
        TaskSection.TOMORROW -> "明天 · ${formatDate(datetime)}"
        else -> formatDate(datetime)
    }

    fun todayHeader(now: Long = System.currentTimeMillis()): String =
        dayWithWeek.format(toLocalDate(now))

    fun toLocalDateTime(datetime: Long): LocalDateTime =
        Instant.ofEpochMilli(datetime).atZone(zone).toLocalDateTime()

    fun toLocalDate(datetime: Long): LocalDate =
        Instant.ofEpochMilli(datetime).atZone(zone).toLocalDate()

    fun toEpochMillis(date: LocalDate, hour: Int, minute: Int): Long =
        date.atTime(hour, minute).atZone(zone).toInstant().toEpochMilli()

    fun isoLocal(datetime: Long): String =
        DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(toLocalDateTime(datetime))
}