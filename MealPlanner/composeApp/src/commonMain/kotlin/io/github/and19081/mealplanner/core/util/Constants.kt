package io.github.and19081.mealplanner.core.util

import kotlinx.datetime.LocalDate

/**
 * Named constants for date range queries to avoid magic strings.
 * Use [EARLIEST_DATE] and [LATEST_DATE] for DAO queries that need to match all rows.
 */
object DateConstants {
    /**
     * Earliest date string for SQL date range queries.
     * Matches any date in the database since SQLite dates are stored as ISO 8601 strings.
     */
    const val EARLIEST_DATE: String = "0000-00-00"

    /**
     * Latest date string for SQL date range queries.
     * Matches any date in the database since SQLite dates are stored as ISO 8601 strings.
     */
    const val LATEST_DATE: String = "9999-99-99"

    /**
     * Parse result for invalid dates - represents the epoch date (Jan 1, 1 AD)
     */
    val INVALID_DATE_REPRESENTATION: LocalDate = LocalDate.parse("0001-01-01")

    /**
     * Default time for parse failures
     */
    const val DEFAULT_TIME_STRING: String = "12:00:00"
}
