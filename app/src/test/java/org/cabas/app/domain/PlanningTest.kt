package org.cabas.app.domain

import org.junit.Assert.*
import org.junit.Test

class PlanningTest {
    @Test fun acceptsIndependentTargetsAndDistinguishesBlankFromZero() {
        assertEquals(Targets(null, null), parseTargets("", "  "))
        assertEquals(Targets(0, 20000), parseTargets("0", "200,00"))
        assertEquals(Targets(12345, null), parseTargets("123.45", ""))
        assertEquals(Targets(null, 0), parseTargets("", "0,00"))
        assertNull(Money.parse("0")) // Movement amounts must still be strictly positive.
        listOf("-1", "1.001", "1.000,00", "1000000000", "NaN").forEach {
            assertNull(parseTargets(it, ""))
            assertNull(parseTargets("", it))
        }
    }
    @Test fun showsRemainingBudgetAndSavingsFromRecordedBalance() {
        val progress = PlanProgress(Totals(100000, 25000), Targets(80000, 80000))
        assertEquals(55000L, progress.budgetRemaining)
        assertEquals(5000L, progress.savingsRemaining)
        assertFalse(progress.goalReached)
        assertEquals(0.3125f, progress.budgetFraction)
        assertEquals(0.9375f, progress.savingsFraction)
    }
    @Test fun handlesOverspendDeficitAndClampsIndicators() {
        val progress = PlanProgress(Totals(10000, 30000), Targets(20000, 5000))
        assertEquals(-10000L, progress.budgetRemaining)
        assertEquals(25000L, progress.savingsRemaining)
        assertEquals(1f, progress.budgetFraction)
        assertEquals(0f, progress.savingsFraction)
        val achieved = PlanProgress(Totals(100000, 10000), Targets(null, 5000))
        assertTrue(achieved.goalReached)
        assertEquals(0L, achieved.savingsRemaining)
        assertEquals(1f, achieved.savingsFraction)
    }
    @Test fun handlesZeroAndUnsetTargetsWithoutDivisionByZero() {
        val empty = PlanProgress(Totals(0, 0), Targets(0, 0))
        assertEquals(0f, empty.budgetFraction)
        assertEquals(1f, empty.savingsFraction)
        assertTrue(empty.goalReached)
        val deficit = PlanProgress(Totals(0, 1), Targets(0, 0))
        assertEquals(1f, deficit.budgetFraction)
        assertEquals(0f, deficit.savingsFraction)
        assertEquals(1L, deficit.savingsRemaining)
        val unset = PlanProgress(Totals(0, 0), Targets(null, null))
        assertNull(unset.budgetRemaining)
        assertNull(unset.savingsRemaining)
        assertFalse(unset.goalReached)
    }
    @Test fun preservesCentsWhenEditingMaximumAndZeroAmounts() {
        listOf(0L, 1L, 12345L, Money.MAX_CENTS).forEach {
            assertEquals(it, Money.parse(editableAmount(it), allowZero = true))
        }
        assertEquals("", editableAmount(null))
    }
}
