package org.cabas.app.domain

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class RecurringTest {
    private val october = YearMonth.of(2026, 10)

    @Test fun clampsDayToMonthLength() {
        assertEquals(LocalDate.of(2026, 2, 28), recurringDate(YearMonth.of(2026, 2), 31))
        assertEquals(LocalDate.of(2028, 2, 29), recurringDate(YearMonth.of(2028, 2), 30))
        assertEquals(LocalDate.of(2026, 4, 30), recurringDate(YearMonth.of(2026, 4), 31))
        assertEquals(LocalDate.of(2026, 10, 1), recurringDate(october, 1))
    }

    @Test fun generatesOnlyMonthsWhoseDateHasArrived() {
        assertEquals(listOf(october), pendingRecurringMonths(october, null, 5, LocalDate.of(2026, 10, 5)))
        assertTrue(pendingRecurringMonths(october, null, 6, LocalDate.of(2026, 10, 5)).isEmpty())
        assertTrue(pendingRecurringMonths(october, october, 1, LocalDate.of(2026, 10, 31)).isEmpty())
    }

    @Test fun catchesUpMissedMonthsWithoutRepeating() {
        val months = pendingRecurringMonths(october, YearMonth.of(2026, 10), 31, LocalDate.of(2027, 2, 28))
        assertEquals(listOf(YearMonth.of(2026, 11), YearMonth.of(2026, 12), YearMonth.of(2027, 1), YearMonth.of(2027, 2)), months)
        assertEquals(MAX_RECURRING_CATCH_UP, pendingRecurringMonths(YearMonth.of(1990, 1), null, 1, LocalDate.of(2026, 10, 29)).size)
        // A last-generated month before the start is ignored.
        assertEquals(listOf(october), pendingRecurringMonths(october, YearMonth.of(2026, 1), 1, LocalDate.of(2026, 10, 2)))
    }

    @Test fun parsesDayOfMonth() {
        assertEquals(1, parseDayOfMonth("1"))
        assertEquals(31, parseDayOfMonth(" 31 "))
        assertNull(parseDayOfMonth("0"))
        assertNull(parseDayOfMonth("32"))
        assertNull(parseDayOfMonth("x"))
    }

    @Test fun groupsExpensesByCategoryLargestFirst() {
        val rows = expensesByCategory(listOf(
            Triple(Kind.EXPENSE, 1L, 500L), Triple(Kind.EXPENSE, 2L, 2000L),
            Triple(Kind.EXPENSE, 1L, 700L), Triple(Kind.INCOME, null, 100000L),
        ))
        assertEquals(listOf(2L to 2000L, 1L to 1200L), rows)
    }
}
