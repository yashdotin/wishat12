package com.wishat12.app.domain

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId

object RecurrenceCalculator {
    const val NEVER = "NEVER"
    const val WEEKLY = "WEEKLY"
    const val MONTHLY = "MONTHLY"
    const val YEARLY = "YEARLY"

    fun nextOccurrence(anchorMillis: Long, recurrence: String, zoneId: ZoneId, nowMillis: Long): Long? {
        if (recurrence == NEVER) return null
        val anchor = LocalDateTime.ofInstant(Instant.ofEpochMilli(anchorMillis), zoneId)
        var candidate = anchor
        var guard = 0
        while (candidate.atZone(zoneId).toInstant().toEpochMilli() <= nowMillis && guard < 5000) {
            candidate = when (recurrence) {
                WEEKLY -> candidate.plusWeeks(1)
                MONTHLY -> {
                    val nextMonth = YearMonth.from(candidate).plusMonths(1)
                    candidate.withYear(nextMonth.year).withMonth(nextMonth.monthValue)
                        .withDayOfMonth(minOf(anchor.dayOfMonth, nextMonth.lengthOfMonth()))
                }
                YEARLY -> {
                    val nextYear = candidate.year + 1
                    val month = anchor.monthValue
                    val yearMonth = YearMonth.of(nextYear, month)
                    candidate.withYear(nextYear).withMonth(month)
                        .withDayOfMonth(minOf(anchor.dayOfMonth, yearMonth.lengthOfMonth()))
                }
                else -> return null
            }
            guard++
        }
        return if (guard >= 5000) null else candidate.atZone(zoneId).toInstant().toEpochMilli()
    }

    fun atLocalDateTime(date: LocalDate, hour: Int, minute: Int, zoneId: ZoneId): Long =
        date.atTime(hour, minute).atZone(zoneId).toInstant().toEpochMilli()

    fun nextDateTimeFromFields(date: String, time: String, zoneId: ZoneId): Long {
        val day = LocalDate.parse(date)
        val parts = time.split(":")
        return atLocalDateTime(day, parts[0].toInt(), parts[1].toInt(), zoneId)
    }
}
