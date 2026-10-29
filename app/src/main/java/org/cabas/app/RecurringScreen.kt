package org.cabas.app

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.cabas.app.data.ExpenseCategory
import org.cabas.app.data.RecurringMovement
import org.cabas.app.domain.Kind
import org.cabas.app.domain.Money
import org.cabas.app.domain.editableAmount

@Composable
internal fun RecurringContent(model: CabasViewModel, error: String?, modifier: Modifier) {
    val items by model.recurring.collectAsStateWithLifecycle()
    val state by model.state.collectAsStateWithLifecycle()
    val saving by model.saving.collectAsStateWithLifecycle()
    var creating by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableStateOf<Long?>(null) }
    var deleting by remember { mutableStateOf<RecurringMovement?>(null) }
    val editing = editingId?.let { id -> items.firstOrNull { it.id == id } }

    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text(stringResource(R.string.recurring_help)) }
        item {
            Button(enabled = !saving && !state.loading, onClick = { model.clearError(); creating = true }) { Text(stringResource(R.string.new_recurring)) }
            if (error != null && !creating && editing == null && deleting == null) Text(error, color = MaterialTheme.colorScheme.error)
        }
        if (items.isEmpty()) item { Text(stringResource(R.string.no_recurring), style = MaterialTheme.typography.bodyMedium) }
        items(items, key = { it.id }) { item -> RecurringRow(item, state.categories, !saving) { model.clearError(); editingId = item.id } }
    }

    if (creating || editing != null) RecurringDialog(
        editing, state.categories, saving, error,
        dismiss = { if (!saving) { creating = false; editingId = null; model.clearError() } },
        delete = editing?.let { item -> { model.clearError(); deleting = item } },
    ) { amount, kind, category, day, note, active ->
        model.saveRecurring(editing?.id, amount, kind, category, day, note, active) { creating = false; editingId = null }
    }
    deleting?.let { item ->
        AlertDialog(
            onDismissRequest = { if (!saving) deleting = null },
            title = { Text(stringResource(R.string.delete_recurring_title)) },
            text = { Text(stringResource(R.string.delete_recurring_body)) },
            confirmButton = { TextButton(enabled = !saving, onClick = { model.deleteRecurring(item) { deleting = null; editingId = null } }) { Text(stringResource(R.string.delete)) } },
            dismissButton = { TextButton(enabled = !saving, onClick = { deleting = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun RecurringRow(item: RecurringMovement, categories: List<ExpenseCategory>, enabled: Boolean, edit: () -> Unit) {
    val category = if (item.kind == Kind.EXPENSE) categories.firstOrNull { it.id == item.categoryId }?.name else stringResource(R.string.income_single)
    OutlinedCard(Modifier.fillMaxWidth()) {
        ListItem(
            modifier = Modifier.clickable(enabled = enabled, onClickLabel = stringResource(R.string.edit), onClick = edit),
            colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surface),
            headlineContent = { Text(item.note.ifBlank { category.orEmpty() }, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            supportingContent = {
                val schedule = stringResource(R.string.every_month_day, item.dayOfMonth)
                Text(listOfNotNull(schedule, category.takeIf { item.note.isNotBlank() }, if (!item.active) stringResource(R.string.paused) else null).joinToString(" · "))
            },
            trailingContent = {
                Text((if (item.kind == Kind.EXPENSE) "−" else "+") + Money.format(item.cents), style = MaterialTheme.typography.titleMedium,
                    color = if (item.kind == Kind.INCOME) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
            },
        )
    }
}

@Composable
private fun RecurringDialog(
    editing: RecurringMovement?, categories: List<ExpenseCategory>, saving: Boolean, error: String?,
    dismiss: () -> Unit, delete: (() -> Unit)?, save: (String, Kind, Long?, String, String, Boolean) -> Unit,
) {
    val key = editing?.id
    var amount by rememberSaveable(key) { mutableStateOf(editing?.let { editableAmount(it.cents) } ?: "") }
    var day by rememberSaveable(key) { mutableStateOf(editing?.dayOfMonth?.toString() ?: "1") }
    var note by rememberSaveable(key) { mutableStateOf(editing?.note ?: "") }
    var kind by rememberSaveable(key) { mutableStateOf(editing?.kind ?: Kind.EXPENSE) }
    var categoryId by rememberSaveable(key) { mutableStateOf(editing?.categoryId) }
    var active by rememberSaveable(key) { mutableStateOf(editing?.active ?: true) }
    val options = selectableCategories(categories, editing?.categoryId)
    val selected = options.firstOrNull { it.id == categoryId } ?: options.firstOrNull { it.id == 1L } ?: options.firstOrNull()
    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text(stringResource(if (editing == null) R.string.new_recurring else R.string.edit_recurring)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                KindChips(kind, !saving) { kind = it }
                OutlinedTextField(amount, { amount = it }, enabled = !saving, label = { Text(stringResource(R.string.amount)) }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(day, { day = it.filter(Char::isDigit).take(2) }, enabled = !saving, label = { Text(stringResource(R.string.day_of_month)) },
                    singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth(),
                    supportingText = { Text(stringResource(R.string.day_of_month_help)) })
                OutlinedTextField(note, { note = it }, enabled = !saving, label = { Text(stringResource(R.string.note)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                if (kind == Kind.EXPENSE) CategoryPicker(options, selected, !saving) { categoryId = it }
                if (editing != null) Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.recurring_active), Modifier.weight(1f))
                    Switch(checked = active, enabled = !saving, onCheckedChange = { active = it })
                }
                Text(stringResource(if (editing == null) R.string.recurring_new_note else R.string.recurring_edit_note), style = MaterialTheme.typography.bodySmall)
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (delete != null) TextButton(enabled = !saving, onClick = delete,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text(stringResource(R.string.delete)) }
            }
        },
        confirmButton = { TextButton(enabled = !saving, onClick = { save(amount, kind, selected?.id, day, note, active) }) { Text(stringResource(if (saving) R.string.saving else R.string.save)) } },
        dismissButton = { TextButton(enabled = !saving, onClick = dismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
