package org.cabas.app

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import org.cabas.app.data.ExpenseCategory
import org.cabas.app.data.Movement
import org.cabas.app.domain.AppLocale
import org.cabas.app.domain.Category
import org.cabas.app.domain.Kind
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val Valencian: Locale get() = AppLocale.current

private fun String.capitalized() = replaceFirstChar { if (it.isLowerCase()) it.titlecase(Valencian) else it.toString() }

internal fun monthLabel(month: YearMonth): String = month.format(DateTimeFormatter.ofPattern(AppLocale.monthPattern, Valencian)).capitalized()
internal fun dayLabel(date: LocalDate): String = date.format(DateTimeFormatter.ofPattern(AppLocale.dayPattern, Valencian)).capitalized()
internal fun shortDate(date: LocalDate): String = date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))

internal fun Category.label() = when (this) {
    Category.NEEDS -> R.string.needs
    Category.WANTS -> R.string.wants
    Category.CULTURE -> R.string.culture
    Category.UNEXPECTED -> R.string.unexpected
}

@Composable
internal fun UiMessage.text(): String = if (arg == null) stringResource(res) else stringResource(res, arg)

/** Category shown for a movement: the editable category, or the legacy one from v1/v2. */
@Composable
internal fun categoryLabel(movement: Movement, categories: List<ExpenseCategory>): String? =
    if (movement.kind == Kind.INCOME) null
    else categories.firstOrNull { it.id == movement.categoryId }?.name ?: movement.category?.let { stringResource(it.label()) }
