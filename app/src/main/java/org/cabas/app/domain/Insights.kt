package org.cabas.app.domain

import java.text.Normalizer
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.util.Locale

/** Income and expense of one month. */
data class MonthTotals(val month: YearMonth, val income: Long, val expense: Long) {
    val balance: Long get() = income - expense
}

/** Twelve entries, January to December, for the movements given as (kind, epochDay, cents). */
fun yearTotals(year: Int, items: List<Triple<Kind, Long, Long>>): List<MonthTotals> {
    val byMonth = items.groupBy { YearMonth.from(LocalDate.ofEpochDay(it.second)) }
    return (1..12).map { m ->
        val month = YearMonth.of(year, m)
        val rows = byMonth[month].orEmpty()
        MonthTotals(month, rows.filter { it.first == Kind.INCOME }.sumOf { it.third }, rows.filter { it.first == Kind.EXPENSE }.sumOf { it.third })
    }
}

/** Lower case without accents or extra spaces, so "Cafè" matches "cafe". */
fun searchKey(text: String): String =
    Normalizer.normalize(text, Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")
        .lowercase(Locale.ROOT).replace(Regex("\\s+"), " ").trim()

/** True when every word of [query] appears in some of [fields]. */
fun matchesSearch(query: String, vararg fields: String?): Boolean {
    val words = searchKey(query).split(' ').filter { it.isNotEmpty() }
    if (words.isEmpty()) return true
    val haystack = fields.filterNotNull().joinToString(" ") { searchKey(it) }
    return words.all { it in haystack }
}

/** Up to [limit] previous concepts whose words start with what is being typed. */
fun suggestionsFor(typed: String, notes: List<String>, limit: Int = 4): List<String> {
    val key = searchKey(typed)
    if (key.length < 2) return emptyList()
    return notes.asSequence()
        .filter { note -> val k = searchKey(note); k != key && (k.startsWith(key) || k.contains(" $key")) }
        .take(limit).toList()
}

enum class BudgetStatus { OK, NEAR, OVER }

/** NEAR from 90 % of the budget; OVER once spending is above it. A zero budget is OVER with any spending. */
fun budgetStatus(spent: Long, budget: Long): BudgetStatus = when {
    spent > budget -> BudgetStatus.OVER
    budget > 0 && spent * 10 >= budget * 9 -> BudgetStatus.NEAR
    else -> BudgetStatus.OK
}

/** Time from [now] until the next [hour]:00 (today if still ahead, otherwise tomorrow). */
fun delayUntilHour(now: LocalDateTime, hour: Int): Duration {
    var next = now.toLocalDate().atTime(hour.coerceIn(0, 23), 0)
    if (!next.isAfter(now)) next = next.plusDays(1)
    return Duration.between(now, next)
}

private val AUTO_BACKUP_NAME = Regex("cabas-auto-(\\d{4}-\\d{2}-\\d{2})\\.json")

fun autoBackupName(date: LocalDate): String = "cabas-auto-$date.json"

/** Automatic backups beyond the newest [keep] ones; other files in the folder are never touched. */
fun autoBackupsToDelete(names: List<String>, keep: Int = 4): List<String> =
    names.filter { AUTO_BACKUP_NAME.matches(it) }.sortedDescending().drop(keep)
