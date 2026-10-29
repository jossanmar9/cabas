package org.cabas.app.domain

import java.time.LocalDate
import java.time.YearMonth

/** Upper bound of months generated at once, so a very old template cannot flood the database. */
const val MAX_RECURRING_CATCH_UP = 120

/** Day of [month] for a template set on [dayOfMonth]: 31 becomes 30, 29 or 28 when needed. */
fun recurringDate(month: YearMonth, dayOfMonth: Int): LocalDate =
    month.atDay(dayOfMonth.coerceIn(1, month.lengthOfMonth()))

/**
 * Months that still need a movement: from [start] (or the month after [lastGenerated]) up to the
 * month whose date has already arrived on [today]. Future dates are never generated.
 */
fun pendingRecurringMonths(start: YearMonth, lastGenerated: YearMonth?, dayOfMonth: Int, today: LocalDate): List<YearMonth> {
    var month = if (lastGenerated == null || lastGenerated < start) start else lastGenerated.plusMonths(1)
    val result = mutableListOf<YearMonth>()
    while (result.size < MAX_RECURRING_CATCH_UP && !recurringDate(month, dayOfMonth).isAfter(today)) {
        result += month
        month = month.plusMonths(1)
    }
    return result
}

fun parseDayOfMonth(text: String): Int? = text.trim().toIntOrNull()?.takeIf { it in 1..31 }

/** Expense total per category id (null for movements without category), largest first. */
fun expensesByCategory(items: List<Triple<Kind, Long?, Long>>): List<Pair<Long?, Long>> =
    items.filter { it.first == Kind.EXPENSE }
        .groupBy({ it.second }, { it.third })
        .map { (category, amounts) -> category to amounts.sum() }
        .sortedByDescending { it.second }
