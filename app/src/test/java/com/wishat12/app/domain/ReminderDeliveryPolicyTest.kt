package com.wishat12.app.domain

import org.junit.Assert.*
import org.junit.Test

class ReminderDeliveryPolicyTest {
    @Test fun previouslyProcessedAlarmIsIgnored() {
        assertFalse(ReminderDeliveryPolicy.shouldProcess(true, false, 123L, 123L))
        assertTrue(ReminderDeliveryPolicy.shouldProcess(true, false, null, 123L))
    }

    @Test fun inactiveOrCompletedReminderIsIgnored() {
        assertFalse(ReminderDeliveryPolicy.shouldProcess(false, false, null, 123L))
        assertFalse(ReminderDeliveryPolicy.shouldProcess(true, true, null, 123L))
    }

    @Test fun oneTimeReminderMissedWithinTwentyFourHoursGetsCatchUp() {
        assertEquals(ReminderDeliveryPolicy.MissedSingleShot.SHOW_CATCH_UP, ReminderDeliveryPolicy.missedSingleShot(3_600_000L))
    }

    @Test fun oldOneTimeReminderExpires() {
        assertEquals(ReminderDeliveryPolicy.MissedSingleShot.EXPIRE, ReminderDeliveryPolicy.missedSingleShot(25L * 60L * 60L * 1000L))
    }
}
