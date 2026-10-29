package org.cabas.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.cabas.app.domain.*
import java.time.LocalDate
import java.time.format.TextStyle

@Composable
internal fun StatsContent(model: CabasViewModel, modifier: Modifier) {
    val all by model.allMovements.collectAsStateWithLifecycle()
    val categories by model.allCategories.collectAsStateWithLifecycle()
    var year by rememberSaveable { mutableIntStateOf(LocalDate.now().year) }
    val inYear = all.filter { LocalDate.ofEpochDay(it.epochDay).year == year }
    val months = yearTotals(year, inYear.map { Triple(it.kind, it.epochDay, it.cents) })
    val income = months.sumOf { it.income }
    val expense = months.sumOf { it.expense }

    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { year-- }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, stringResource(R.string.previous_year)) }
            Text(year.toString(), Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            IconButton(onClick = { year++ }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, stringResource(R.string.next_year)) }
        }
        Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.year_balance))
                Text(Money.format(income - expense), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                Row { Text(stringResource(R.string.income), Modifier.weight(1f)); Text(Money.format(income)) }
                Row { Text(stringResource(R.string.expense), Modifier.weight(1f)); Text(Money.format(expense)) }
            }
        }
        if (inYear.isEmpty()) Text(stringResource(R.string.year_empty))
        else {
            OutlinedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(R.string.monthly_evolution), style = MaterialTheme.typography.titleLarge)
                    Legend()
                    BarChart(months)
                }
            }
            OutlinedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row {
                        Text(stringResource(R.string.month), Modifier.weight(1.2f), style = MaterialTheme.typography.labelLarge)
                        Text(stringResource(R.string.income), Modifier.weight(1f), style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.End)
                        Text(stringResource(R.string.expense), Modifier.weight(1f), style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.End)
                        Text(stringResource(R.string.balance_short), Modifier.weight(1f), style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.End)
                    }
                    HorizontalDivider()
                    months.filter { it.income > 0 || it.expense > 0 }.forEach { m ->
                        Row {
                            Text(m.month.month.getDisplayName(TextStyle.SHORT_STANDALONE, AppLocale.current), Modifier.weight(1.2f))
                            Text(Money.format(m.income), Modifier.weight(1f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodySmall)
                            Text(Money.format(m.expense), Modifier.weight(1f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodySmall)
                            Text(Money.format(m.balance), Modifier.weight(1f), textAlign = TextAlign.End, style = MaterialTheme.typography.bodySmall,
                                color = if (m.balance < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            }
            if (expense > 0) OutlinedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(R.string.year_by_category), style = MaterialTheme.typography.titleLarge)
                    expensesByCategory(inYear.map { Triple(it.kind, it.categoryId, it.cents) }).forEach { (id, cents) ->
                        val name = categories.firstOrNull { it.id == id }?.name
                            ?: inYear.firstOrNull { it.categoryId == id && it.category != null }?.category?.let { stringResource(it.label()) }
                            ?: stringResource(R.string.no_category)
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row {
                                Text(name, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(Money.format(cents) + " · " + stringResource(R.string.per_month, Money.format(cents / 12)))
                            }
                            LinearProgressIndicator(progress = { (cents.toDouble() / expense).toFloat() }, modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Legend() {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        LegendItem(MaterialTheme.colorScheme.primary, stringResource(R.string.income))
        LegendItem(MaterialTheme.colorScheme.tertiary, stringResource(R.string.expense))
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Canvas(Modifier.size(12.dp)) { drawRect(color) }
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}

/** Income and expense bars side by side for each month; the table below has the exact figures. */
@Composable
private fun BarChart(months: List<MonthTotals>) {
    val incomeColor = MaterialTheme.colorScheme.primary
    val expenseColor = MaterialTheme.colorScheme.tertiary
    val axis = MaterialTheme.colorScheme.outlineVariant
    val max = months.maxOf { maxOf(it.income, it.expense) }.coerceAtLeast(1)
    val description = stringResource(R.string.chart_description)
    Column {
        Canvas(Modifier.fillMaxWidth().height(160.dp).semantics { contentDescription = description }) {
            val slot = size.width / 12
            val bar = slot * 0.32f
            months.forEachIndexed { i, m ->
                val x = i * slot + slot * 0.14f
                val hIncome = size.height * (m.income.toFloat() / max)
                val hExpense = size.height * (m.expense.toFloat() / max)
                drawRect(incomeColor, Offset(x, size.height - hIncome), Size(bar, hIncome))
                drawRect(expenseColor, Offset(x + bar + slot * 0.04f, size.height - hExpense), Size(bar, hExpense))
            }
            drawLine(axis, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 2f)
        }
        Row(Modifier.fillMaxWidth().padding(top = 4.dp)) {
            months.forEach { m ->
                Text(m.month.month.getDisplayName(TextStyle.NARROW_STANDALONE, AppLocale.current), Modifier.weight(1f),
                    textAlign = TextAlign.Center, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}
