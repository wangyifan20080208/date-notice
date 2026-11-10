package com.smartagenda.core.time

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

enum class TaskSection(val label: String) {
    OVERDUE("已过期"),
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

    fun sectionFor(datetime: Long?, now: Long = System.currentTimeMillis()): TaskSection {
        if (datetime == null) return TaskSection.MISSING
        if (datetime < startOfToday(now)) return TaskSection.OVERDUE
        return when (dayOffset(datetime, now)) {
            0 -> TaskSection.TODAY
            1 -> TaskSection.TOMORROW
            else -> TaskSection.UPCOMING
        }
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

    fun formatCardDate(datetime: Long, now: Long = System.currentTimeMillis()): String {
        val local = toLocalDateTime(datetime)
        return when (dayOffset(datetime, now)) {
            0 -> "今天"
            1 -> "明天"
            2 -> "后天"
            in 3..6 -> dayWithWeek.format(local)
            else -> DateTimeFormatter.ofPattern("M月d日", Locale.CHINA).format(local)
        }
    }

    fun formatDoneStamp(doneAt: Long, now: Long = System.currentTimeMillis()): String {
        val days = -dayOffset(doneAt, now)
        val time = timeOnly.format(toLocalDateTime(doneAt))
        return when {
            days <= 0 -> "今天 $time"
            days == 1 -> "昨天 $time"
            days in 2..6 -> "$days 天前"
            else -> dayWithWeek.format(toLocalDateTime(doneAt))
        }
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