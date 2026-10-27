package org.cabas.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.core.view.WindowCompat
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import org.cabas.app.data.Movement
import org.cabas.app.data.MonthlyPlan
import org.cabas.app.data.ExpenseCategory
import org.cabas.app.domain.*
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val Valencian = Locale.forLanguageTag("ca-ES-valencia")
private fun Category.label() = when (this) {
    Category.NEEDS -> R.string.needs
    Category.WANTS -> R.string.wants
    Category.CULTURE -> R.string.culture
    Category.UNEXPECTED -> R.string.unexpected
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val model: CabasViewModel = viewModel()
            val theme by model.themeMode.collectAsStateWithLifecycle()
            val dark = when (theme) { ThemeMode.SYSTEM -> isSystemInDarkTheme(); ThemeMode.LIGHT -> false; ThemeMode.DARK -> true }
            SideEffect {
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !dark
                    isAppearanceLightNavigationBars = !dark
                }
            }
            MaterialTheme(colorScheme = if (dark) darkColorScheme(primary = Color(0xFF9AD5B4)) else lightColorScheme(
                primary = Color(0xFF246A52), onPrimary = Color.White,
                secondary = Color(0xFF865219), background = Color(0xFFFFFBF4),
                surface = Color(0xFFFFFBF4), surfaceVariant = Color(0xFFF0EBDD),
            )) { CabasScreen(model) }
        }
    }
}

@Composable
private fun CabasScreen(model: CabasViewModel = viewModel()) {
    val month by model.month.collectAsStateWithLifecycle()
    val state by model.state.collectAsStateWithLifecycle()
    val saving by model.saving.collectAsStateWithLifecycle()
    val error by model.error.collectAsStateWithLifecycle()
    var page by rememberSaveable { mutableStateOf("home") }
    var menu by remember { mutableStateOf(false) }
    val theme by model.themeMode.collectAsStateWithLifecycle()
    BackHandler(enabled = page != "home") { page = "home"; model.clearError() }
    var adding by rememberSaveable { mutableStateOf(false) }
    var planningMonth by rememberSaveable { mutableStateOf<String?>(null) }
    var planBudget by rememberSaveable { mutableStateOf("") }
    var planGoal by rememberSaveable { mutableStateOf("") }
    var deleting by remember { mutableStateOf<Movement?>(null) }
    val snack = remember { SnackbarHostState() }
    val message = error?.let { stringResource(it) }
    LaunchedEffect(message, adding, deleting, planningMonth, page) {
        if (message != null && !adding && deleting == null && planningMonth == null && page != "categories") { snack.showSnackbar(message); model.clearError() }
    }
    Scaffold(
        topBar = {
            Surface {
                Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (page != "home") TextButton(onClick = { page = "home"; model.clearError() }) { Text(stringResource(R.string.back)) }
                    Text(stringResource(pageTitle(page)), Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                    Box {
                        TextButton(onClick = { menu = true }) { Text(stringResource(R.string.menu)) }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            listOf("home", "categories", "appearance", "help", "privacy", "about").forEach { target ->
                                DropdownMenuItem(text = { Text(stringResource(pageTitle(target))) }, onClick = { menu = false; model.clearError(); page = target })
                            }
                        }
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snack) },
        floatingActionButton = {
            if (page == "home") ExtendedFloatingActionButton(onClick = { model.clearError(); adding = true }) { Text(stringResource(R.string.add)) }
        },
    ) { padding ->
        when (page) {
            "categories" -> CategoriesContent(state.categories, state.loading, state.failed, saving, message, Modifier.padding(padding), model::retry, model::clearError, model::saveCategory, model::archiveCategory)
            "appearance" -> AppearanceContent(theme, model::setTheme, Modifier.padding(padding))
            "help", "privacy", "about" -> InfoContent(page, Modifier.padding(padding))
            else -> LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Image(painterResource(R.drawable.cabas_logo), null, Modifier.size(64.dp))
                    Column { Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold); Text(stringResource(R.string.tagline)) }
                }
            }
            item {
                Column {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        TextButton(onClick = { model.moveMonth(-1) }) { Text(stringResource(R.string.previous)) }
                        TextButton(onClick = { model.moveMonth(1) }) { Text(stringResource(R.string.next)) }
                    }
                    Text(month.format(DateTimeFormatter.ofPattern("LLLL yyyy", Valencian)), style = MaterialTheme.typography.headlineSmall)
                }
            }
            when {
                (state.loading || (!state.failed && state.selectedMonth != month)) -> item { Text(stringResource(R.string.loading)) }
                state.failed -> item {
                    Text(stringResource(R.string.load_error))
                    Button(onClick = model::retry) { Text(stringResource(R.string.retry)) }
                }
                else -> {
                    val summary = totals(state.items.map { it.kind to it.cents })
                    item {
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(stringResource(R.string.balance))
                                Text(Money.format(summary.balance), style = MaterialTheme.typography.headlineLarge)
                                SummaryLine(R.string.income, summary.income)
                                SummaryLine(R.string.expense, summary.expense)
                            }
                        }
                    }
                    item {
                        PlanCard(state.plan, summary, !saving) {
                            model.clearError()
                            planBudget = editableAmount(state.plan?.budgetCents)
                            planGoal = editableAmount(state.plan?.savingsGoalCents)
                            planningMonth = month.toString()
                        }
                    }
                    item { Text(stringResource(R.string.movements), style = MaterialTheme.typography.titleLarge) }
                    if (state.items.isEmpty()) item {
                        Column(Modifier.padding(vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(stringResource(R.string.empty_title), style = MaterialTheme.typography.titleMedium)
                            Text(stringResource(R.string.empty_body))
                        }
                    }
                    items(state.items, key = { it.id }) { item ->
                        OutlinedCard(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                val categoryName = state.categories.firstOrNull { it.id == item.categoryId }?.name
                                    ?: item.category?.let { stringResource(it.label()) }
                                Text(item.note.ifBlank { categoryName ?: stringResource(R.string.income) }, style = MaterialTheme.typography.titleMedium)
                                Text((if (item.kind == Kind.EXPENSE) "−" else "+") + Money.format(item.cents), style = MaterialTheme.typography.titleLarge)
                                Text(LocalDate.ofEpochDay(item.epochDay).format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                                categoryName?.let { Text(it) }
                                TextButton(enabled = !saving, onClick = { model.clearError(); deleting = item }) { Text(stringResource(R.string.delete)) }
                            }
                        }
                    }
                }
            }
            item { Text(stringResource(R.string.local_only), style = MaterialTheme.typography.bodySmall) }
        }
        }
    }
    if (adding) MovementDialog(state.categories.filter { !it.archived }, saving, message, { if (!saving) { adding = false; model.clearError() } }) { amount, date, note, kind, category ->
        model.add(amount, date, note, kind, category) { adding = false }
    }
    planningMonth?.let { selected ->
        PlanDialog(selected, planBudget, planGoal, saving, message,
            { planBudget = it }, { planGoal = it },
            { if (!saving) { planningMonth = null; model.clearError() } },
            { model.savePlan(selected, planBudget, planGoal) { planningMonth = null } },
        )
    }
    deleting?.let { item ->
        AlertDialog(
            onDismissRequest = { if (!saving) { deleting = null; model.clearError() } },
            title = { Text(stringResource(R.string.delete_title)) },
            text = { Column { Text(stringResource(R.string.delete_body)); message?.let { Text(it, color = MaterialTheme.colorScheme.error) } } },
            confirmButton = { TextButton(enabled = !saving, onClick = { model.delete(item) { deleting = null } }) { Text(stringResource(R.string.delete)) } },
            dismissButton = { TextButton(enabled = !saving, onClick = { deleting = null; model.clearError() }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun SummaryLine(label: Int, cents: Long) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(stringResource(label), Modifier.weight(1f)); Text(Money.format(cents))
    }
}

@Composable
private fun MovementDialog(categories: List<ExpenseCategory>, saving: Boolean, error: String?, dismiss: () -> Unit, save: (String, String, String, Kind, Long?) -> Unit) {
    var amount by rememberSaveable { mutableStateOf("") }
    var date by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var note by rememberSaveable { mutableStateOf("") }
    var kind by rememberSaveable { mutableStateOf(Kind.EXPENSE) }
    var categoryId by rememberSaveable { mutableStateOf<Long?>(null) }
    var categoryMenu by remember { mutableStateOf(false) }
    val selectedCategory = categories.firstOrNull { it.id == categoryId } ?: categories.firstOrNull { it.id == 1L } ?: categories.firstOrNull()
    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text(stringResource(R.string.add)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = kind == Kind.EXPENSE, enabled = !saving, onClick = { kind = Kind.EXPENSE }, label = { Text(stringResource(R.string.expense)) })
                    FilterChip(selected = kind == Kind.INCOME, enabled = !saving, onClick = { kind = Kind.INCOME }, label = { Text(stringResource(R.string.income)) })
                }
                OutlinedTextField(amount, { amount = it }, enabled = !saving, label = { Text(stringResource(R.string.amount)) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                OutlinedTextField(date, { date = it }, enabled = !saving, label = { Text(stringResource(R.string.date)) }, singleLine = true)
                OutlinedTextField(note, { note = it }, enabled = !saving, label = { Text(stringResource(R.string.note)) }, singleLine = true)
                if (kind == Kind.EXPENSE) {
                    Text(stringResource(R.string.category))
                    if (categories.isEmpty()) Text(stringResource(R.string.no_active_categories))
                    else Box {
                        OutlinedButton(enabled = !saving, onClick = { categoryMenu = true }) { Text(selectedCategory?.name.orEmpty()) }
                        DropdownMenu(expanded = categoryMenu, onDismissRequest = { categoryMenu = false }) {
                            categories.forEach { option ->
                                DropdownMenuItem(text = { Text(option.name) }, onClick = { categoryId = option.id; categoryMenu = false })
                            }
                        }
                    }
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = { TextButton(enabled = !saving, onClick = { save(amount, date, note, kind, selectedCategory?.id) }) { Text(stringResource(if (saving) R.string.saving else R.string.save)) } },
        dismissButton = { TextButton(enabled = !saving, onClick = dismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun PlanCard(plan: MonthlyPlan?, totals: Totals, enabled: Boolean, edit: () -> Unit) {
    val progress = PlanProgress(totals, Targets(plan?.budgetCents, plan?.savingsGoalCents))
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.monthly_plan), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.budget), style = MaterialTheme.typography.titleMedium)
            if (plan?.budgetCents == null) Text(stringResource(R.string.not_set))
            else {
                Text(Money.format(plan.budgetCents))
                LinearProgressIndicator(progress = { progress.budgetFraction }, modifier = Modifier.fillMaxWidth())
                val remaining = progress.budgetRemaining!!
                Text(
                    stringResource(if (remaining < 0) R.string.budget_exceeded else R.string.budget_remaining, Money.format(if (remaining < 0) -remaining else remaining)),
                    color = if (remaining < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                )
            }
            HorizontalDivider()
            Text(stringResource(R.string.savings_goal), style = MaterialTheme.typography.titleMedium)
            if (plan?.savingsGoalCents == null) Text(stringResource(R.string.not_set))
            else {
                Text(Money.format(plan.savingsGoalCents))
                LinearProgressIndicator(progress = { progress.savingsFraction }, modifier = Modifier.fillMaxWidth())
                Text(if (progress.goalReached) stringResource(R.string.goal_reached)
                    else stringResource(R.string.savings_remaining, Money.format(progress.savingsRemaining!!)))
            }
            Text(stringResource(R.string.savings_explanation), style = MaterialTheme.typography.bodySmall)
            TextButton(enabled = enabled, onClick = edit) { Text(stringResource(if (plan == null) R.string.plan_month else R.string.edit_plan)) }
        }
    }
}

@Composable
private fun PlanDialog(month: String, budget: String, goal: String, saving: Boolean, error: String?,
    changeBudget: (String) -> Unit, changeGoal: (String) -> Unit, dismiss: () -> Unit, save: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = dismiss,
        title = { Column { Text(stringResource(R.string.monthly_plan)); Text(YearMonth.parse(month).format(DateTimeFormatter.ofPattern("LLLL yyyy", Valencian)), style = MaterialTheme.typography.titleMedium) } },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.plan_help))
                OutlinedTextField(budget, changeBudget, enabled = !saving, label = { Text(stringResource(R.string.budget_input)) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                OutlinedTextField(goal, changeGoal, enabled = !saving, label = { Text(stringResource(R.string.goal_input)) }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = { TextButton(enabled = !saving, onClick = save) { Text(stringResource(if (saving) R.string.saving else R.string.save)) } },
        dismissButton = { TextButton(enabled = !saving, onClick = dismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
