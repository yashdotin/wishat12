package com.wishat12.app.domain

object ReminderDeliveryPolicy {
    const val CATCH_UP_WINDOW_MILLIS = 24L * 60L * 60L * 1000L
    enum class MissedSingleShot { SHOW_CATCH_UP, EXPIRE }

    fun shouldProcess(isActive: Boolean, isCompleted: Boolean, lastNotifiedAt: Long?, scheduledAt: Long): Boolean =
        isActive && !isCompleted && lastNotifiedAt != scheduledAt

    fun missedSingleShot(ageMillis: Long): MissedSingleShot =
        if (ageMillis in 0..CATCH_UP_WINDOW_MILLIS) MissedSingleShot.SHOW_CATCH_UP else MissedSingleShot.EXPIRE
}
