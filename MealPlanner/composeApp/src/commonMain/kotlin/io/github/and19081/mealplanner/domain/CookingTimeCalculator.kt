package io.github.and19081.mealplanner.domain

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.time.Duration.Companion.minutes

object CookingTimeCalculator {
    /**
     * Calculates the ideal exact timestamp (in epoch milliseconds) to begin cooking.
     */
    fun calculateStartCookingTimestampMillis(
        scheduledDate: LocalDate,
        scheduledTime: LocalTime,
        prepTimeMinutes: Int,
        cookTimeMinutes: Int,
        timeZone: TimeZone = TimeZone.currentSystemDefault()
    ): Long {
        val scheduledDateTime = LocalDateTime(scheduledDate, scheduledTime)
        val scheduledInstant = scheduledDateTime.toInstant(timeZone)
        
        // Subtract total preparation and cook time from the scheduled meal time
        val totalDuration = (prepTimeMinutes + cookTimeMinutes).minutes
        val startInstant = scheduledInstant.minus(totalDuration)
        
        return startInstant.toEpochMilliseconds()
    }
}
