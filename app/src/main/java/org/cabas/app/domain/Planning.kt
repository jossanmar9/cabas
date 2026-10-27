package org.cabas.app.domain

import java.math.BigDecimal

/** Null means unset; zero is an explicit limit/goal. Values always refer to one month. */
data class Targets(val budgetCents: Long?, val savingsGoalCents: Long?)

fun parseTargets(budget: String, goal: String): Targets? {
    val budgetCents = if (budget.isBlank()) null else Money.parse(budget, allowZero = true) ?: return null
    val goalCents = if (goal.isBlank()) null else Money.parse(goal, allowZero = true) ?: return null
    return Targets(budgetCents, goalCents)
}

fun editableAmount(cents: Long?): String = cents?.let { BigDecimal.valueOf(it, 2).toPlainString().replace('.', ',') } ?: ""

data class PlanProgress(val totals: Totals, val targets: Targets) {
    val budgetRemaining: Long? get() = targets.budgetCents?.minus(totals.expense)
    val savingsRemaining: Long? get() = targets.savingsGoalCents?.let { (it - totals.balance).coerceAtLeast(0) }
    val goalReached: Boolean get() = savingsRemaining == 0L
    // Floating point is used only to draw indicators, never to compute monetary amounts.
    val budgetFraction: Float get() = targets.budgetCents?.let {
        if (it == 0L) { if (totals.expense > 0) 1f else 0f }
        else (totals.expense.toDouble() / it).coerceIn(0.0, 1.0).toFloat()
    } ?: 0f
    val savingsFraction: Float get() = targets.savingsGoalCents?.let {
        if (it == 0L) { if (totals.balance >= 0) 1f else 0f }
        else (totals.balance.toDouble() / it).coerceIn(0.0, 1.0).toFloat()
    } ?: 0f
}
