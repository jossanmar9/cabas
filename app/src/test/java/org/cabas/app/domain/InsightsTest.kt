package org.cabas.app.domain

import org.junit.Assert.*
import org.junit.Test
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

class InsightsTest {
    private fun day(y: Int, m: Int, d: Int) = LocalDate.of(y, m, d).toEpochDay()

    @Test fun totalsEveryMonthOfTheYear() {
        val months = yearTotals(2026, listOf(
            Triple(Kind.INCOME, day(2026, 1, 31), 100000L),
            Triple(Kind.EXPENSE, day(2026, 1, 1), 2500L),
            Triple(Kind.EXPENSE, day(2026, 3, 15), 700L),
            Triple(Kind.EXPENSE, day(2025, 12, 31), 99L),
        ))
        assertEquals(12, months.size)
        assertEquals(MonthTotals(YearMonth.of(2026, 1), 100000, 2500), months[0])
        assertEquals(97500L, months[0].balance)
        assertEquals(MonthTotals(YearMonth.of(2026, 2), 0, 0), months[1])
        assertEquals(700L, months[2].expense)
        assertEquals(0L, months.sumOf { it.expense } - 3200L)
    }

    @Test fun searchIgnoresCaseAccentsAndWordOrder() {
        assertEquals("cafe amb llet", searchKey("  Cafè   AMB llet "))
        assertTrue(matchesSearch("cafe", "Cafè del matí"))
        assertTrue(matchesSearch("mati cafe", "Cafè del matí"))
        assertTrue(matchesSearch("necessitats", "Pa", "Necessitats"))
        assertFalse(matchesSearch("llum", "Aigua", null))
        assertTrue(matchesSearch("   ", "qualsevol"))
    }

    @Test fun suggestsPreviousConceptsByWordStart() {
        val notes = listOf("Mercadona", "Mercat central", "Lloguer pis", "Pis", "Farmàcia")
        assertEquals(listOf("Mercadona", "Mercat central"), suggestionsFor("mer", notes))
        assertEquals(listOf("Lloguer pis"), suggestionsFor("pi", notes.filter { it != "Pis" }))
        assertEquals(listOf("Farmàcia"), suggestionsFor("farma", notes))
        assertTrue(suggestionsFor("m", notes).isEmpty())
        assertTrue(suggestionsFor("Mercadona", notes).isEmpty())
        assertEquals(1, suggestionsFor("me", notes, limit = 1).size)
    }

    @Test fun budgetStatusWarnsFromNinetyPercent() {
        assertEquals(BudgetStatus.OK, budgetStatus(8999, 10000))
        assertEquals(BudgetStatus.NEAR, budgetStatus(9000, 10000))
        assertEquals(BudgetStatus.NEAR, budgetStatus(10000, 10000))
        assertEquals(BudgetStatus.OVER, budgetStatus(10001, 10000))
        assertEquals(BudgetStatus.OK, budgetStatus(0, 0))
        assertEquals(BudgetStatus.OVER, budgetStatus(1, 0))
    }

    @Test fun reminderDelayPointsToTheNextOccurrence() {
        assertEquals(Duration.ofMinutes(90), delayUntilHour(LocalDateTime.of(2026, 10, 29, 19, 30), 21))
        assertEquals(Duration.ofHours(24), delayUntilHour(LocalDateTime.of(2026, 10, 29, 21, 0), 21))
        assertEquals(Duration.ofHours(23).plusMinutes(30), delayUntilHour(LocalDateTime.of(2026, 10, 29, 21, 30), 21))
    }

    @Test fun keepsOnlyTheNewestAutomaticBackups() {
        val names = listOf("cabas-auto-2026-10-01.json", "cabas-auto-2026-10-08.json", "cabas-auto-2026-10-15.json",
            "cabas-auto-2026-10-22.json", "cabas-auto-2026-10-29.json", "cabas-copia-2026-01-01.json", "notes.txt")
        assertEquals(listOf("cabas-auto-2026-10-01.json"), autoBackupsToDelete(names))
        assertEquals("cabas-auto-2026-10-29.json", autoBackupName(LocalDate.of(2026, 10, 29)))
        assertTrue(autoBackupsToDelete(names.take(4)).isEmpty())
    }
}
