package org.cabas.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.cabas.app.data.ExpenseCategory
import org.cabas.app.data.Movement
import org.cabas.app.data.MonthlyPlan
import org.cabas.app.domain.*
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

@Composable
internal fun HomeContent(model: CabasViewModel, error: String?, modifier: Modifier, onDialogChange: (Boolean) -> Unit, open: (String) -> Unit) {
    val month by model.month.collectAsStateWithLifecycle()
    val state by model.state.collectAsStateWithLifecycle()
    val saving by model.saving.collectAsStateWithLifecycle()
    val movementCount by model.movementCount.collectAsStateWithLifecycle()
    val lastBackup by model.lastBackup.collectAsStateWithLifecycle()

    var adding by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableStateOf<Long?>(null) }
    var planningMonth by rememberSaveable { mutableStateOf<String?>(null) }
    var planBudget by rememberSaveable { mutableStateOf("") }
    var planGoal by rememberSaveable { mutableStateOf("") }
    var deleting by remember { mutableStateOf<Movement?>(null) }
    val editing = editingId?.let { id -> state.items.firstOrNull { it.id == id } }
    val dialogOpen = adding || editing != null || planningMonth != null || deleting != null
    LaunchedEffect(dialogOpen) { onDialogChange(dialogOpen) }

    val ready = !state.loading && !state.failed && state.selectedMonth == month
    Box(modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { MonthHeader(month, model::moveMonth, model::goToCurrentMonth) }
            when {
                state.failed -> item {
                    Text(stringResource(R.string.load_error))
                    Button(onClick = model::retry) { Text(stringResource(R.string.retry)) }
                }
                !ready -> item { Text(stringResource(R.string.loading)) }
                else -> {
                    val summary = totals(state.items.map { it.kind to it.cents })
                    item { BalanceCard(summary) }
                    if (needsBackupReminder(movementCount, lastBackup)) item { BackupReminder(lastBackup) { open("data") } }
                    item {
                        PlanCard(state.plan, summary, !saving) {
                            model.clearError()
                            planBudget = editableAmount(state.plan?.budgetCents)
                            planGoal = editableAmount(state.plan?.savingsGoalCents)
                            planningMonth = month.toString()
                        }
                    }
                    if (summary.expense > 0) item { CategoryBreakdown(state.items, state.categories, summary.expense) }
                    if (!month.isAfter(YearMonth.now())) item {
                        ReflectionCard(state.reflection != null, month == YearMonth.now()) { open("reflection") }
                    }
                    item { Text(stringResource(R.string.movements), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp)) }
                    if (state.items.isEmpty()) item {
                        Column(Modifier.padding(vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(stringResource(R.string.empty_title), style = MaterialTheme.typography.titleMedium)
                            Text(stringResource(R.string.empty_body))
                        }
                    }
                    state.items.groupBy { it.epochDay }.forEach { (day, movements) ->
                        item(key = "day-$day") {
                            Text(dayLabel(LocalDate.ofEpochDay(day)), style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                        }
                        item(key = "list-$day") {
                            OutlinedCard(Modifier.fillMaxWidth()) {
                                movements.forEachIndexed { index, item ->
                                    if (index > 0) HorizontalDivider()
                                    MovementRow(item, state.categories, enabled = !saving) { model.clearError(); editingId = item.id }
                                }
                            }
                        }
                    }
                }
            }
            item { Text(stringResource(R.string.local_only), style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp)) }
        }
        ExtendedFloatingActionButton(
            onClick = { model.clearError(); adding = true },
            icon = { Icon(Icons.Default.Add, null) },
            text = { Text(stringResource(R.string.add)) },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        )
    }

    if (adding || editing != null) MovementDialog(
        editing = editing, categories = state.categories, saving = saving, error = error,
        dismiss = { if (!saving) { adding = false; editingId = null; model.clearError() } },
        delete = editing?.let { item -> { model.clearError(); deleting = item } },
    ) { amount, date, note, kind, category ->
        model.saveMovement(editing?.id, amount, date, note, kind, category) { adding = false; editingId = null }
    }
    planningMonth?.let { selected ->
        PlanDialog(selected, planBudget, planGoal, state.previousPlan, saving, error,
            { planBudget = it }, { planGoal = it },
            { if (!saving) { planningMonth = null; model.clearError() } },
            { model.savePlan(selected, planBudget, planGoal) { planningMonth = null } },
        )
    }
    deleting?.let { item ->
        AlertDialog(
            onDismissRequest = { if (!saving) { deleting = null; model.clearError() } },
            title = { Text(stringResource(R.string.delete_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text((if (item.kind == Kind.EXPENSE) "−" else "+") + Money.format(item.cents) + " · " + shortDate(LocalDate.ofEpochDay(item.epochDay)))
                    Text(stringResource(R.string.delete_body))
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = { TextButton(enabled = !saving, onClick = { model.delete(item) { deleting = null; editingId = null } }) { Text(stringResource(R.string.delete)) } },
            dismissButton = { TextButton(enabled = !saving, onClick = { deleting = null; model.clearError() }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

internal fun needsBackupReminder(movements: Int, lastBackup: LocalDate?, today: LocalDate = LocalDate.now()): Boolean =
    movements > 0 && (lastBackup == null || ChronoUnit.DAYS.between(lastBackup, today) >= BACKUP_REMINDER_DAYS)

@Composable
private fun MonthHeader(month: YearMonth, move: (Long) -> Unit, today: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { move(-1) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, stringResource(R.string.previous)) }
            Text(monthLabel(month), Modifier.weight(1f), style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            IconButton(onClick = { move(1) }) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, stringResource(R.string.next)) }
        }
        if (month != YearMonth.now()) TextButton(onClick = today) { Text(stringResource(R.string.current_month)) }
    }
}

@Composable
private fun BalanceCard(summary: Totals) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.balance))
            Text(Money.format(summary.balance), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
            SummaryLine(R.string.income, summary.income)
            SummaryLine(R.string.expense, summary.expense)
        }
    }
}

@Composable
private fun SummaryLine(label: Int, cents: Long) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(stringResource(label), Modifier.weight(1f)); Text(Money.format(cents))
    }
}

@Composable
private fun BackupReminder(lastBackup: LocalDate?, open: () -> Unit) {
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.backup_reminder_title), style = MaterialTheme.typography.titleMedium)
            Text(if (lastBackup == null) stringResource(R.string.backup_never) else stringResource(R.string.backup_last, shortDate(lastBackup)))
            FilledTonalButton(onClick = open) { Text(stringResource(R.string.backup_now)) }
        }
    }
}

@Composable
private fun CategoryBreakdown(items: List<Movement>, categories: List<ExpenseCategory>, total: Long) {
    val rows = expensesByCategory(items.map { Triple(it.kind, it.categoryId, it.cents) })
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.by_category), style = MaterialTheme.typography.titleLarge)
            rows.forEach { (id, cents) ->
                val name = categories.firstOrNull { it.id == id }?.name
                    ?: items.firstOrNull { it.categoryId == id && it.category != null }?.category?.let { stringResource(it.label()) }
                    ?: stringResource(R.string.no_category)
                val percent = (cents * 100.0 / total).let { if (it < 1 && cents > 0) "<1" else Math.round(it).toString() }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(Modifier.fillMaxWidth()) {
                        Text(name, Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(Money.format(cents) + " · " + percent + " %")
                    }
                    LinearProgressIndicator(progress = { (cents.toDouble() / total).toFloat() }, modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.secondary)
                }
            }
        }
    }
}

@Composable
private fun ReflectionCard(written: Boolean, currentMonth: Boolean, open: () -> Unit) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.reflection), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(if (written) R.string.reflection_written else if (currentMonth) R.string.reflection_pending_current else R.string.reflection_pending))
            TextButton(onClick = open) { Text(stringResource(if (written) R.string.reflection_edit else R.string.reflection_write)) }
        }
    }
}

@Composable
private fun MovementRow(item: Movement, categories: List<ExpenseCategory>, enabled: Boolean, edit: () -> Unit) {
    val category = categoryLabel(item, categories)
    val title = item.note.ifBlank { category ?: stringResource(R.string.income_single) }
    val subtitle = if (item.kind == Kind.INCOME) stringResource(R.string.income_single) else category.takeIf { item.note.isNotBlank() }
    ListItem(
        modifier = Modifier.clickable(enabled = enabled, onClickLabel = stringResource(R.string.edit_movement), onClick = edit),
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
        headlineContent = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = if (subtitle != null) { { Text(subtitle, maxLines = 1, overflow = TextOverflow.Ellipsis) } } else null,
        trailingContent = {
            Text((if (item.kind == Kind.EXPENSE) "−" else "+") + Money.format(item.cents),
                style = MaterialTheme.typography.titleMedium,
                color = if (item.kind == Kind.INCOME) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
        },
    )
}

@Composable
private fun PlanCard(plan: MonthlyPlan?, totals: Totals, enabled: Boolean, edit: () -> Unit) {
    val progress = PlanProgress(totals, Targets(plan?.budgetCents, plan?.savingsGoalCents))
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.monthly_plan), style = MaterialTheme.typography.titleLarge)
            if (plan == null) Text(stringResource(R.string.plan_empty))
            if (plan?.budgetCents != null) {
                Row(Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.budget), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                    Text(Money.format(plan.budgetCents))
                }
                val remaining = progress.budgetRemaining!!
                LinearProgressIndicator(progress = { progress.budgetFraction }, modifier = Modifier.fillMaxWidth(),
                    color = if (remaining < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                Text(
                    stringResource(if (remaining < 0) R.string.budget_exceeded else R.string.budget_remaining, Money.format(if (remaining < 0) -remaining else remaining)),
                    color = if (remaining < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                )
            }
            if (plan?.budgetCents != null && plan.savingsGoalCents != null) HorizontalDivider()
            if (plan?.savingsGoalCents != null) {
                Row(Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.savings_goal), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                    Text(Money.format(plan.savingsGoalCents))
                }
                LinearProgressIndicator(progress = { progress.savingsFraction }, modifier = Modifier.fillMaxWidth())
                Text(if (progress.goalReached) stringResource(R.string.goal_reached)
                    else stringResource(R.string.savings_remaining, Money.format(progress.savingsRemaining!!)))
                Text(stringResource(R.string.savings_explanation), style = MaterialTheme.typography.bodySmall)
            }
            TextButton(enabled = enabled, onClick = edit) { Text(stringResource(if (plan == null) R.string.plan_month else R.string.edit_plan)) }
        }
    }
}
