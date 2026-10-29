package org.cabas.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.cabas.app.data.Backup
import org.cabas.app.domain.Money
import org.cabas.app.domain.totals

/**
 * The four Kakebo questions for the selected month: the first three are answered from the
 * registered data, the last one ("how can I improve?") is written by the user.
 */
@Composable
internal fun ReflectionContent(model: CabasViewModel, error: String?, modifier: Modifier) {
    val state by model.state.collectAsStateWithLifecycle()
    val month by model.month.collectAsStateWithLifecycle()
    val saving by model.saving.collectAsStateWithLifecycle()
    val ready = !state.loading && !state.failed && state.selectedMonth == month
    var wentWell by rememberSaveable(month, ready) { mutableStateOf(state.reflection?.wentWell ?: "") }
    var improve by rememberSaveable(month, ready) { mutableStateOf(state.reflection?.improve ?: "") }

    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(monthLabel(month), style = MaterialTheme.typography.headlineSmall)
        if (!ready) Text(stringResource(if (state.failed) R.string.load_error else R.string.loading))
        else {
            val summary = totals(state.items.map { it.kind to it.cents })
            val goal = state.plan?.savingsGoalCents
            OutlinedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Answer(R.string.kakebo_q1, Money.format(summary.income))
                    Answer(R.string.kakebo_q2, goal?.let(Money::format) ?: stringResource(R.string.not_set))
                    Answer(R.string.kakebo_q3, Money.format(summary.expense))
                    Answer(R.string.kakebo_q4, Money.format(summary.balance))
                    if (goal != null) Text(
                        stringResource(if (summary.balance >= goal) R.string.kakebo_goal_met else R.string.kakebo_goal_missed),
                        color = if (summary.balance >= goal) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    )
                }
            }
            Text(stringResource(R.string.reflection_help))
            OutlinedTextField(wentWell, { wentWell = it.take(Backup.MAX_TEXT) }, enabled = !saving, minLines = 3,
                label = { Text(stringResource(R.string.reflection_went_well)) }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(improve, { improve = it.take(Backup.MAX_TEXT) }, enabled = !saving, minLines = 3,
                label = { Text(stringResource(R.string.reflection_improve)) }, modifier = Modifier.fillMaxWidth())
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(enabled = !saving, onClick = { model.saveReflection(month, wentWell, improve) {} }) {
                Text(stringResource(if (saving) R.string.saving else R.string.save))
            }
        }
    }
}

@Composable
private fun Answer(question: Int, answer: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(stringResource(question), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(answer, style = MaterialTheme.typography.titleLarge)
    }
}
