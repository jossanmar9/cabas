package org.cabas.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.cabas.app.data.ExpenseCategory
import org.cabas.app.data.Movement
import org.cabas.app.data.MonthlyPlan
import org.cabas.app.data.NoteSuggestion
import org.cabas.app.domain.suggestionsFor
import org.cabas.app.domain.Kind
import org.cabas.app.domain.Money
import org.cabas.app.domain.editableAmount
import org.cabas.app.domain.parseDate
import java.time.LocalDate
import java.time.YearMonth

private const val DAY_MILLIS = 86_400_000L

/** Categories offered in a form: the active ones, plus the current one if it has been archived. */
internal fun selectableCategories(all: List<ExpenseCategory>, currentId: Long?): List<ExpenseCategory> =
    all.filter { !it.archived || it.id == currentId }

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun MovementDialog(
    editing: Movement?, categories: List<ExpenseCategory>, saving: Boolean, error: String?,
    dismiss: () -> Unit, delete: (() -> Unit)?, suggestions: List<NoteSuggestion> = emptyList(),
    save: (String, String, String, Kind, Long?) -> Unit,
) {
    val key = editing?.id
    var amount by rememberSaveable(key) { mutableStateOf(editing?.let { editableAmount(it.cents) } ?: "") }
    var date by rememberSaveable(key) { mutableStateOf(editing?.let { LocalDate.ofEpochDay(it.epochDay).toString() } ?: LocalDate.now().toString()) }
    var note by rememberSaveable(key) { mutableStateOf(editing?.note ?: "") }
    var kind by rememberSaveable(key) { mutableStateOf(editing?.kind ?: Kind.EXPENSE) }
    var categoryId by rememberSaveable(key) { mutableStateOf(editing?.categoryId) }
    val options = selectableCategories(categories, editing?.categoryId)
    val selectedCategory = options.firstOrNull { it.id == categoryId } ?: options.firstOrNull { it.id == 1L } ?: options.firstOrNull()
    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text(stringResource(if (editing == null) R.string.add else R.string.edit_movement)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                KindChips(kind, !saving) { kind = it }
                OutlinedTextField(amount, { amount = it }, enabled = !saving, label = { Text(stringResource(R.string.amount)) },
                    singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                DateField(date, !saving) { date = it }
                OutlinedTextField(note, { note = it }, enabled = !saving, label = { Text(stringResource(R.string.note)) },
                    singleLine = true, modifier = Modifier.fillMaxWidth())
                val matches = remember(note, suggestions) { suggestionsFor(note, suggestions.map { it.note }) }
                if (matches.isNotEmpty()) FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    matches.forEach { text ->
                        SuggestionChip(enabled = !saving, label = { Text(text) }, onClick = {
                            note = text
                            // A new movement also takes the kind and category used last time with this concept.
                            if (editing == null) suggestions.firstOrNull { it.note == text }?.let { previous ->
                                kind = previous.kind
                                if (previous.categoryId != null && options.any { it.id == previous.categoryId }) categoryId = previous.categoryId
                            }
                        })
                    }
                }
                if (kind == Kind.EXPENSE) CategoryPicker(options, selectedCategory, !saving) { categoryId = it }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (delete != null) TextButton(enabled = !saving, onClick = delete,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text(stringResource(R.string.delete)) }
            }
        },
        confirmButton = { TextButton(enabled = !saving, onClick = { save(amount, date, note, kind, selectedCategory?.id) }) { Text(stringResource(if (saving) R.string.saving else R.string.save)) } },
        dismissButton = { TextButton(enabled = !saving, onClick = dismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
internal fun KindChips(kind: Kind, enabled: Boolean, change: (Kind) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(selected = kind == Kind.EXPENSE, enabled = enabled, onClick = { change(Kind.EXPENSE) }, label = { Text(stringResource(R.string.expense_single)) })
        FilterChip(selected = kind == Kind.INCOME, enabled = enabled, onClick = { change(Kind.INCOME) }, label = { Text(stringResource(R.string.income_single)) })
    }
}

/** Shows the date as dd/MM/yyyy and opens a calendar; the value is kept as ISO `YYYY-MM-DD`. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DateField(isoDate: String, enabled: Boolean, change: (String) -> Unit) {
    var open by rememberSaveable { mutableStateOf(false) }
    val current = parseDate(isoDate) ?: LocalDate.now()
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(R.string.date), style = MaterialTheme.typography.labelLarge)
        OutlinedButton(enabled = enabled, onClick = { open = true }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.DateRange, null)
            Spacer(Modifier.width(8.dp))
            Text(shortDate(current), Modifier.weight(1f))
            Text(stringResource(R.string.change))
        }
    }
    if (open) {
        val picker = rememberDatePickerState(initialSelectedDateMillis = current.toEpochDay() * DAY_MILLIS)
        DatePickerDialog(
            onDismissRequest = { open = false },
            confirmButton = {
                TextButton(onClick = {
                    picker.selectedDateMillis?.let { change(LocalDate.ofEpochDay(Math.floorDiv(it, DAY_MILLIS)).toString()) }
                    open = false
                }) { Text(stringResource(R.string.accept)) }
            },
            dismissButton = { TextButton(onClick = { open = false }) { Text(stringResource(R.string.cancel)) } },
        ) { DatePicker(state = picker, showModeToggle = true) }
    }
}

@Composable
internal fun CategoryPicker(options: List<ExpenseCategory>, selected: ExpenseCategory?, enabled: Boolean, change: (Long) -> Unit) {
    var menu by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(R.string.category), style = MaterialTheme.typography.labelLarge)
        if (options.isEmpty()) Text(stringResource(R.string.no_active_categories))
        else Box {
            OutlinedButton(enabled = enabled, onClick = { menu = true }, modifier = Modifier.fillMaxWidth()) {
                Text(selected?.name.orEmpty(), Modifier.weight(1f))
                Icon(Icons.Default.ArrowDropDown, null)
            }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                options.forEach { option ->
                    DropdownMenuItem(text = { Text(option.name) }, onClick = { change(option.id); menu = false })
                }
            }
        }
    }
}

@Composable
internal fun PlanDialog(month: String, budget: String, goal: String, previous: MonthlyPlan?, saving: Boolean, error: String?,
    changeBudget: (String) -> Unit, changeGoal: (String) -> Unit, dismiss: () -> Unit, save: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = dismiss,
        title = { Column { Text(stringResource(R.string.monthly_plan)); Text(monthLabel(YearMonth.parse(month)), style = MaterialTheme.typography.titleMedium) } },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.plan_help))
                if (previous != null) OutlinedButton(enabled = !saving, onClick = {
                    changeBudget(editableAmount(previous.budgetCents)); changeGoal(editableAmount(previous.savingsGoalCents))
                }) { Text(stringResource(R.string.copy_previous_plan)) }
                previous?.let { plan ->
                    Text(stringResource(R.string.previous_plan_summary,
                        plan.budgetCents?.let(Money::format) ?: "—", plan.savingsGoalCents?.let(Money::format) ?: "—"),
                        style = MaterialTheme.typography.bodySmall)
                }
                OutlinedTextField(budget, changeBudget, enabled = !saving, label = { Text(stringResource(R.string.budget_input)) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                OutlinedTextField(goal, changeGoal, enabled = !saving, label = { Text(stringResource(R.string.goal_input)) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = { TextButton(enabled = !saving, onClick = save) { Text(stringResource(if (saving) R.string.saving else R.string.save)) } },
        dismissButton = { TextButton(enabled = !saving, onClick = dismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
