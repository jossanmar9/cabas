package org.cabas.app

import android.os.Bundle
import androidx.activity.ComponentActivity
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
import org.cabas.app.domain.*
import java.time.LocalDate
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
            MaterialTheme(colorScheme = lightColorScheme(
                primary = Color(0xFF246A52), onPrimary = Color.White,
                secondary = Color(0xFF865219), background = Color(0xFFFFFBF4),
                surface = Color(0xFFFFFBF4), surfaceVariant = Color(0xFFF0EBDD),
            )) { CabasScreen() }
        }
    }
}

@Composable
private fun CabasScreen(model: CabasViewModel = viewModel()) {
    val month by model.month.collectAsStateWithLifecycle()
    val state by model.state.collectAsStateWithLifecycle()
    val saving by model.saving.collectAsStateWithLifecycle()
    val error by model.error.collectAsStateWithLifecycle()
    var adding by rememberSaveable { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<Movement?>(null) }
    val snack = remember { SnackbarHostState() }
    val message = error?.let { stringResource(it) }
    LaunchedEffect(message, adding, deleting) {
        if (message != null && !adding && deleting == null) { snack.showSnackbar(message); model.clearError() }
    }
    Scaffold(
        snackbarHost = { SnackbarHost(snack) },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { model.clearError(); adding = true }) { Text(stringResource(R.string.add)) }
        },
    ) { padding ->
        LazyColumn(
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
                state.loading -> item { Text(stringResource(R.string.loading)) }
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
                                Text(item.note.ifBlank { stringResource(if (item.kind == Kind.INCOME) R.string.income else item.category!!.label()) }, style = MaterialTheme.typography.titleMedium)
                                Text((if (item.kind == Kind.EXPENSE) "−" else "+") + Money.format(item.cents), style = MaterialTheme.typography.titleLarge)
                                Text(LocalDate.ofEpochDay(item.epochDay).format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                                item.category?.let { Text(stringResource(it.label())) }
                                TextButton(enabled = !saving, onClick = { model.clearError(); deleting = item }) { Text(stringResource(R.string.delete)) }
                            }
                        }
                    }
                }
            }
            item { Text(stringResource(R.string.local_only), style = MaterialTheme.typography.bodySmall) }
        }
    }
    if (adding) MovementDialog(saving, message, { if (!saving) { adding = false; model.clearError() } }) { amount, date, note, kind, category ->
        model.add(amount, date, note, kind, category) { adding = false }
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
private fun MovementDialog(saving: Boolean, error: String?, dismiss: () -> Unit, save: (String, String, String, Kind, Category) -> Unit) {
    var amount by rememberSaveable { mutableStateOf("") }
    var date by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var note by rememberSaveable { mutableStateOf("") }
    var kind by rememberSaveable { mutableStateOf(Kind.EXPENSE) }
    var category by rememberSaveable { mutableStateOf(Category.NEEDS) }
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
                    Category.entries.forEach { option ->
                        FilterChip(selected = category == option, enabled = !saving, onClick = { category = option }, label = { Text(stringResource(option.label())) })
                    }
                }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = { TextButton(enabled = !saving, onClick = { save(amount, date, note, kind, category) }) { Text(stringResource(if (saving) R.string.saving else R.string.save)) } },
        dismissButton = { TextButton(enabled = !saving, onClick = dismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
