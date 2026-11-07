package com.wishat12.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class RecurrenceCalculatorTest {
    private val zone = ZoneId.of("Asia/Kolkata")

    @Test fun neverHasNoNextOccurrence() {
        val anchor = LocalDateTime.of(2026, 1, 1, 0, 0).atZone(zone).toInstant().toEpochMilli()
        assertNull(RecurrenceCalculator.nextOccurrence(anchor, RecurrenceCalculator.NEVER, zone, anchor + 1))
    }

    @Test fun yearlyBirthdayMovesToNextYear() {
        val anchor = LocalDateTime.of(2026, 5, 20, 0, 0).atZone(zone).toInstant().toEpochMilli()
        val now = LocalDateTime.of(2026, 5, 21, 0, 0).atZone(zone).toInstant().toEpochMilli()
        val next = RecurrenceCalculator.nextOccurrence(anchor, RecurrenceCalculator.YEARLY, zone, now)!!
        assertEquals(2027, java.time.Instant.ofEpochMilli(next).atZone(zone).year)
    }

    @Test fun leapDayBirthdayUsesFebruaryTwentyEightInNonLeapYear() {
        val anchor = LocalDateTime.of(2024, 2, 29, 9, 0).atZone(zone).toInstant().toEpochMilli()
        val now = LocalDateTime.of(2024, 3, 1, 0, 0).atZone(zone).toInstant().toEpochMilli()
        val next = RecurrenceCalculator.nextOccurrence(anchor, RecurrenceCalculator.YEARLY, zone, now)!!
        val local = java.time.Instant.ofEpochMilli(next).atZone(zone)
        assertEquals(2, local.monthValue)
        assertEquals(28, local.dayOfMonth)
    }

    @Test fun monthlyRecurrenceClampsAtMonthEnd() {
        val anchor = LocalDateTime.of(2026, 1, 31, 12, 0).atZone(zone).toInstant().toEpochMilli()
        val now = LocalDateTime.of(2026, 2, 1, 0, 0).atZone(zone).toInstant().toEpochMilli()
        val next = RecurrenceCalculator.nextOccurrence(anchor, RecurrenceCalculator.MONTHLY, zone, now)!!
        val local = java.time.Instant.ofEpochMilli(next).atZone(zone)
        assertEquals(28, local.dayOfMonth)
    }
    @Test fun monthlyMonthEndKeepsOriginalDayAfterShortMonth() {
        val anchor = LocalDateTime.of(2026, 1, 31, 12, 0).atZone(zone).toInstant().toEpochMilli()
        val afterFebruary = LocalDateTime.of(2026, 3, 1, 0, 0).atZone(zone).toInstant().toEpochMilli()
        val next = RecurrenceCalculator.nextOccurrence(anchor, RecurrenceCalculator.MONTHLY, zone, afterFebruary)!!
        val local = java.time.Instant.ofEpochMilli(next).atZone(zone)
        assertEquals(2026, local.year)
        assertEquals(3, local.monthValue)
        assertEquals(31, local.dayOfMonth)
    }

}
