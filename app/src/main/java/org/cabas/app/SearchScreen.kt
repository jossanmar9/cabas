package org.cabas.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.cabas.app.data.Movement
import org.cabas.app.domain.Kind
import org.cabas.app.domain.Money
import org.cabas.app.domain.matchesSearch
import org.cabas.app.domain.totals
import java.time.LocalDate

/** Results shown at most, newest first; the totals still cover every match. */
private const val MAX_RESULTS = 300

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SearchContent(model: CabasViewModel, error: String?, modifier: Modifier) {
    val all by model.allMovements.collectAsStateWithLifecycle()
    val categories by model.allCategories.collectAsStateWithLifecycle()
    val saving by model.saving.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    var kind by rememberSaveable { mutableStateOf<Kind?>(null) }
    var categoryId by rememberSaveable { mutableStateOf<Long?>(null) }
    var editingId by rememberSaveable { mutableStateOf<Long?>(null) }
    var deleting by remember { mutableStateOf<Movement?>(null) }
    val names = categories.associate { it.id to it.name }
    val incomeLabel = stringResource(R.string.income_single)
    val matches = all.filter { m ->
        (kind == null || m.kind == kind) && (categoryId == null || m.categoryId == categoryId) &&
            matchesSearch(query, m.note, names[m.categoryId], if (m.kind == Kind.INCOME) incomeLabel else null)
    }
    val filtering = query.isNotBlank() || kind != null || categoryId != null
    val editing = editingId?.let { id -> all.firstOrNull { it.id == id } }

    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            OutlinedTextField(query, { query = it.take(100) }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.search_hint)) })
        }
        item {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = kind == Kind.EXPENSE, onClick = { kind = if (kind == Kind.EXPENSE) null else Kind.EXPENSE }, label = { Text(stringResource(R.string.expense)) })
                FilterChip(selected = kind == Kind.INCOME, onClick = { kind = if (kind == Kind.INCOME) null else Kind.INCOME; categoryId = null }, label = { Text(stringResource(R.string.income)) })
                if (kind != Kind.INCOME) categories.filter { !it.archived || it.id == categoryId }.forEach { c ->
                    FilterChip(selected = categoryId == c.id, onClick = { categoryId = if (categoryId == c.id) null else c.id }, label = { Text(c.name) })
                }
            }
        }
        if (!filtering) item { Text(stringResource(R.string.search_help)) }
        else {
            item {
                val sum = totals(matches.map { it.kind to it.cents })
                Text(pluralStringResource(R.plurals.search_count, matches.size, matches.size) + " · " +
                    stringResource(R.string.search_totals, Money.format(sum.income), Money.format(sum.expense)),
                    style = MaterialTheme.typography.labelLarge)
            }
            if (matches.isEmpty()) item { Text(stringResource(R.string.search_empty)) }
            items(matches.take(MAX_RESULTS), key = { it.id }) { item ->
                OutlinedCard(Modifier.fillMaxWidth()) {
                    Column {
                        MovementRow(item, categories, enabled = !saving) { model.clearError(); editingId = item.id }
                        Text(shortDate(LocalDate.ofEpochDay(item.epochDay)), style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(start = 16.dp, bottom = 10.dp))
                    }
                }
            }
            if (matches.size > MAX_RESULTS) item { Text(stringResource(R.string.search_more, MAX_RESULTS)) }
        }
    }

    editing?.let { item ->
        MovementDialog(editing = item, categories = categories, saving = saving, error = error,
            dismiss = { if (!saving) { editingId = null; model.clearError() } },
            delete = { model.clearError(); deleting = item },
        ) { amount, date, note, k, category ->
            model.saveMovement(item.id, amount, date, note, k, category) { editingId = null }
        }
    }
    deleting?.let { item ->
        AlertDialog(
            onDismissRequest = { if (!saving) deleting = null },
            title = { Text(stringResource(R.string.delete_title)) },
            text = { Text(stringResource(R.string.delete_body)) },
            confirmButton = { TextButton(enabled = !saving, onClick = { model.delete(item) { deleting = null; editingId = null } }) { Text(stringResource(R.string.delete)) } },
            dismissButton = { TextButton(enabled = !saving, onClick = { deleting = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}
