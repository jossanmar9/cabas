package org.cabas.app.domain

import org.junit.Assert.*
import org.junit.Test

class MoneyTest {
    @Test fun parsesExactCentsWithBothDecimalSeparators() {
        assertEquals(10L, Money.parse("0,10"))
        assertEquals(12345L, Money.parse("123.45"))
        assertEquals(100L, Money.parse(" 1 "))
        assertEquals(Money.MAX_CENTS, Money.parse("999999999,99"))
    }
    @Test fun rejectsAmbiguousNegativeZeroAndExcessPrecision() {
        listOf("", "0", "-1", "1,001", "1.000,00", "NaN", "1e3", "1000000000", "1.").forEach {
            assertNull(it, Money.parse(it))
        }
    }
    @Test fun computesBalanceWithoutFloatingPointRounding() {
        val result = totals(listOf(Kind.INCOME to 30L, Kind.EXPENSE to 10L, Kind.EXPENSE to 20L))
        assertEquals(0L, result.balance)
        assertEquals(Totals(0, 0), totals(emptyList()))
        assertEquals(-5L, totals(listOf(Kind.EXPENSE to 5L)).balance)
    }
    @Test fun validatesRealCalendarDates() {
        assertNotNull(parseDate("2024-02-29"))
        listOf("2025-02-29", "2026-13-01", "27/10/2026", "2026-2-01").forEach { assertNull(parseDate(it)) }
    }
}
