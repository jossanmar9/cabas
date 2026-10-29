package org.cabas.app

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.cabas.app.data.ExpenseCategory
import org.cabas.app.domain.Money
import org.cabas.app.domain.ThemeMode
import org.cabas.app.domain.editableAmount
import org.cabas.app.work.Background

internal fun pageTitle(page: String) = when (page) {
    "categories" -> R.string.categories
    "recurring" -> R.string.recurring
    "reflection" -> R.string.reflection
    "data" -> R.string.data
    "settings" -> R.string.settings
    "search" -> R.string.search
    "stats" -> R.string.stats
    "help" -> R.string.help
    "privacy" -> R.string.privacy
    "about" -> R.string.about
    else -> R.string.home
}

@Composable
internal fun CategoriesContent(model: CabasViewModel, error: String?, modifier: Modifier) {
    val state by model.state.collectAsStateWithLifecycle()
    val saving by model.saving.collectAsStateWithLifecycle()
    CategoriesContent(state.categories, state.loading, state.failed, saving, error, modifier, model::retry, model::clearError, model::saveCategory, model::archiveCategory, model::saveCategoryBudget)
}

@Composable
private fun CategoriesContent(
    categories: List<ExpenseCategory>, loading: Boolean, failed: Boolean, saving: Boolean,
    error: String?, modifier: Modifier, retry: () -> Unit, clearError: () -> Unit,
    save: (Long?, String, () -> Unit) -> Unit,
    archive: (ExpenseCategory, () -> Unit) -> Unit,
    saveBudget: (Long, String, () -> Unit) -> Unit,
) {
    var budgetId by rememberSaveable { mutableStateOf<Long?>(null) }
    var budget by rememberSaveable { mutableStateOf("") }
    var showArchived by rememberSaveable { mutableStateOf(false) }
    var editing by rememberSaveable { mutableStateOf(false) }
    var editedId by rememberSaveable { mutableStateOf<Long?>(null) }
    var name by rememberSaveable { mutableStateOf("") }
    var archiveId by rememberSaveable { mutableStateOf<Long?>(null) }
    val visible = categories.filter { showArchived || !it.archived }
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text(stringResource(R.string.category_help)) }
        item {
            Button(enabled = !loading && !failed && !saving, onClick = { clearError(); editedId = null; name = ""; editing = true }) { Text(stringResource(R.string.new_category)) }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.show_archived), Modifier.weight(1f))
                Switch(checked = showArchived, onCheckedChange = { showArchived = it })
            }
            if (error != null && !editing && archiveId == null && budgetId == null) Text(error, color = MaterialTheme.colorScheme.error)
        }
        when {
            loading -> item { Text(stringResource(R.string.loading)) }
            failed -> item { Text(stringResource(R.string.load_error)); Button(onClick = retry) { Text(stringResource(R.string.retry)) } }
            visible.isEmpty() -> item { Text(stringResource(R.string.no_categories)) }
            else -> items(visible, key = { it.id }) { category ->
                OutlinedCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(category.name, style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(if (category.archived) R.string.archived else if (category.builtin) R.string.included else R.string.custom), style = MaterialTheme.typography.bodySmall)
                        Text(category.monthlyBudgetCents?.let { stringResource(R.string.category_budget_value, Money.format(it)) } ?: stringResource(R.string.category_budget_none))
                        Row {
                            TextButton(enabled = !saving, onClick = { clearError(); editedId = category.id; name = category.name; editing = true }) { Text(stringResource(R.string.edit)) }
                            TextButton(enabled = !saving, onClick = {
                                clearError()
                                if (category.archived) archive(category) {} else archiveId = category.id
                            }) { Text(stringResource(if (category.archived) R.string.restore else R.string.archive)) }
                            if (!category.archived) TextButton(enabled = !saving, onClick = {
                                clearError(); budget = editableAmount(category.monthlyBudgetCents); budgetId = category.id
                            }) { Text(stringResource(R.string.category_budget)) }
                        }
                    }
                }
            }
        }
    }
    if (editing) AlertDialog(
        onDismissRequest = { if (!saving) { editing = false; clearError() } },
        title = { Text(stringResource(if (editedId == null) R.string.new_category else R.string.edit_category)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, enabled = !saving, singleLine = true, label = { Text(stringResource(R.string.category_name)) })
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = { TextButton(enabled = !saving, onClick = { save(editedId, name) { editing = false } }) { Text(stringResource(if (saving) R.string.saving else R.string.save)) } },
        dismissButton = { TextButton(enabled = !saving, onClick = { editing = false; clearError() }) { Text(stringResource(R.string.cancel)) } },
    )
    categories.firstOrNull { it.id == budgetId }?.let { category ->
        AlertDialog(
            onDismissRequest = { if (!saving) { budgetId = null; clearError() } },
            title = { Text(stringResource(R.string.category_budget_title, category.name)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.category_budget_help))
                    OutlinedTextField(budget, { budget = it }, enabled = !saving, singleLine = true,
                        label = { Text(stringResource(R.string.budget_input)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = { TextButton(enabled = !saving, onClick = { saveBudget(category.id, budget) { budgetId = null } }) { Text(stringResource(if (saving) R.string.saving else R.string.save)) } },
            dismissButton = { TextButton(enabled = !saving, onClick = { budgetId = null; clearError() }) { Text(stringResource(R.string.cancel)) } },
        )
    }
    categories.firstOrNull { it.id == archiveId }?.let { category ->
        AlertDialog(
            onDismissRequest = { if (!saving) { archiveId = null; clearError() } },
            title = { Text(stringResource(R.string.archive_title, category.name)) },
            text = { Column { Text(stringResource(R.string.archive_help)); error?.let { Text(it, color = MaterialTheme.colorScheme.error) } } },
            confirmButton = { TextButton(enabled = !saving, onClick = { archive(category) { archiveId = null } }) { Text(stringResource(R.string.archive)) } },
            dismissButton = { TextButton(enabled = !saving, onClick = { archiveId = null; clearError() }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
internal fun SettingsContent(model: CabasViewModel, lock: LockSupport, modifier: Modifier) {
    val mode by model.themeMode.collectAsStateWithLifecycle()
    val lockEnabled by model.lockEnabled.collectAsStateWithLifecycle()
    val reminderHour by model.reminderHour.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var notificationsDenied by remember { mutableStateOf(false) }
    var pendingHour by rememberSaveable { mutableStateOf(21) }
    val askNotifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        notificationsDenied = !granted
        if (granted) model.setReminder(pendingHour)
    }
    fun enableReminder(hour: Int) {
        pendingHour = hour
        if (Build.VERSION.SDK_INT >= 33 && !Background.canNotify(context)) askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        else { notificationsDenied = !Background.canNotify(context); model.setReminder(hour) }
    }

    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SettingsSection(R.string.appearance) {
            Text(stringResource(R.string.theme_help))
            ThemeMode.entries.forEach { option ->
                Row(Modifier.fillMaxWidth().selectable(selected = mode == option, role = Role.RadioButton, onClick = { model.setTheme(option) }).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    RadioButton(selected = mode == option, onClick = null)
                    Text(stringResource(when (option) { ThemeMode.SYSTEM -> R.string.theme_system; ThemeMode.LIGHT -> R.string.theme_light; ThemeMode.DARK -> R.string.theme_dark }))
                }
            }
        }
        SettingsSection(R.string.lock) {
            Text(stringResource(R.string.lock_help))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.lock_switch), Modifier.weight(1f))
                Switch(checked = lockEnabled, enabled = lock.available || lockEnabled, onCheckedChange = { on ->
                    // Confirm with the phone's lock both to turn it on and off.
                    lock.authenticate { model.setLock(on) }
                })
            }
            if (!lock.available) Text(stringResource(R.string.lock_unavailable), style = MaterialTheme.typography.bodySmall)
        }
        SettingsSection(R.string.reminder) {
            Text(stringResource(R.string.reminder_help))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.reminder_switch), Modifier.weight(1f))
                Switch(checked = reminderHour != null, onCheckedChange = { on -> if (on) enableReminder(pendingHour) else model.setReminder(null) })
            }
            if (reminderHour != null) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(20, 21, 22).forEach { hour ->
                    FilterChip(selected = reminderHour == hour, onClick = { enableReminder(hour) }, label = { Text("%02d:00".format(hour)) })
                }
            }
            if (notificationsDenied) Text(stringResource(R.string.reminder_denied), color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun SettingsSection(title: Int, content: @Composable ColumnScope.() -> Unit) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(title), style = MaterialTheme.typography.titleLarge)
            content()
        }
    }
}

@Composable
internal fun InfoContent(page: String, modifier: Modifier) {
    val uri = LocalUriHandler.current
    var linkFailed by remember { mutableStateOf(false) }
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (page == "about") {
            Image(painterResource(R.drawable.cabas_logo), null, Modifier.size(100.dp))
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineLarge)
            Text(stringResource(R.string.tagline), style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.version, BuildConfig.VERSION_NAME, BuildConfig.VERSION_CODE))
            Text(stringResource(R.string.about_body))
            Text(stringResource(R.string.license))
            OutlinedButton(onClick = { linkFailed = runCatching { uri.openUri("https://github.com/jossanmar9/cabas") }.isFailure }) { Text(stringResource(R.string.source_code)) }
            OutlinedButton(onClick = { linkFailed = runCatching { uri.openUri("https://github.com/jossanmar9/cabas/blob/main/LICENSE") }.isFailure }) { Text(stringResource(R.string.view_license)) }
            if (linkFailed) Text(stringResource(R.string.link_error), color = MaterialTheme.colorScheme.error)
        } else Text(stringResource(if (page == "privacy") R.string.privacy_body else R.string.help_body))
    }
}
