package org.cabas.app.domain

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import java.util.Currency
import java.util.Locale
import java.text.NumberFormat

/** Locale of the language the interface is shown in; set by the activity from its resources. */
object AppLocale {
    val VALENCIAN: Locale = Locale.forLanguageTag("ca-ES-valencia")
    @Volatile var current: Locale = VALENCIAN
    @Volatile var monthPattern: String = "LLLL yyyy"
    @Volatile var dayPattern: String = "EEEE d MMMM"
}

/** EUR only in this first version. Never use Float/Double for money. */
object Money {
    const val MAX_CENTS = 99_999_999_999L
    fun parse(text: String, allowZero: Boolean = false): Long? {
        val value = text.trim()
        if (!Regex("[0-9]{1,9}([.,][0-9]{1,2})?").matches(value)) return null
        return runCatching {
            BigDecimal(value.replace(',', '.')).movePointRight(2)
                .setScale(0, RoundingMode.UNNECESSARY).longValueExact()
        }.getOrNull()?.takeIf { it in (if (allowZero) 0L else 1L)..MAX_CENTS }
    }
    fun format(cents: Long): String = NumberFormat.getCurrencyInstance(AppLocale.current).apply {
        currency = Currency.getInstance("EUR")
    }.format(BigDecimal.valueOf(cents, 2))
}

enum class Kind { INCOME, EXPENSE }
enum class Category { NEEDS, WANTS, CULTURE, UNEXPECTED }

data class Totals(val income: Long, val expense: Long) {
    val balance: Long get() = income - expense
}
fun totals(items: List<Pair<Kind, Long>>): Totals = Totals(
    items.filter { it.first == Kind.INCOME }.sumOf { it.second },
    items.filter { it.first == Kind.EXPENSE }.sumOf { it.second },
)
fun parseDate(text: String): LocalDate? = if (Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}").matches(text)) {
    runCatching { LocalDate.parse(text) }.getOrNull()
} else null
