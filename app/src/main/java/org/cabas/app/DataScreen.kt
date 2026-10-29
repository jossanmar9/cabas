package org.cabas.app

import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import android.net.Uri
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.documentfile.provider.DocumentFile
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.time.LocalDate

@Composable
internal fun DataContent(model: CabasViewModel, error: String?, modifier: Modifier) {
    val saving by model.saving.collectAsStateWithLifecycle()
    val lastBackup by model.lastBackup.collectAsStateWithLifecycle()
    val pending by model.pendingImport.collectAsStateWithLifecycle()
    val autoFolder by model.autoBackupFolder.collectAsStateWithLifecycle()
    val autoFailed by model.autoBackupFailed.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val chooseFolder = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri -> uri?.let(model::enableAutoBackup) }
    var noFilePicker by remember { mutableStateOf(false) }
    val exportJson = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri -> uri?.let(model::exportBackup) }
    val exportCsv = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri -> uri?.let(model::exportCsv) }
    val importJson = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> uri?.let(model::prepareImport) }
    fun launch(block: () -> Unit) {
        model.clearError()
        noFilePicker = try { block(); false } catch (_: ActivityNotFoundException) { true }
    }
    val today = LocalDate.now()

    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(stringResource(R.string.data_intro))
        Section(R.string.backup_title, R.string.backup_body) {
            Text(if (lastBackup == null) stringResource(R.string.backup_never) else stringResource(R.string.backup_last, shortDate(lastBackup!!)),
                style = MaterialTheme.typography.bodySmall)
            Button(enabled = !saving, onClick = { launch { exportJson.launch("cabas-copia-$today.json") } }) { Text(stringResource(R.string.export_backup)) }
        }
        Section(R.string.auto_backup_title, R.string.auto_backup_body) {
            if (autoFolder == null) {
                OutlinedButton(enabled = !saving, onClick = { launch { chooseFolder.launch(null) } }) { Text(stringResource(R.string.auto_backup_choose)) }
            } else {
                val folderName = remember(autoFolder) {
                    runCatching { DocumentFile.fromTreeUri(context, Uri.parse(autoFolder))?.name }.getOrNull() ?: "—"
                }
                Text(stringResource(R.string.auto_backup_folder, folderName))
                if (autoFailed) Text(stringResource(R.string.auto_backup_failed_body), color = MaterialTheme.colorScheme.error)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(enabled = !saving, onClick = { launch { chooseFolder.launch(null) } }) { Text(stringResource(R.string.auto_backup_change)) }
                    TextButton(enabled = !saving, onClick = model::disableAutoBackup) { Text(stringResource(R.string.auto_backup_disable)) }
                }
            }
        }
        Section(R.string.restore_title, R.string.restore_body) {
            OutlinedButton(enabled = !saving, onClick = { launch { importJson.launch(arrayOf("application/json", "application/octet-stream", "text/plain", "*/*")) } }) {
                Text(stringResource(R.string.restore_backup))
            }
        }
        Section(R.string.csv_title, R.string.csv_body) {
            OutlinedButton(enabled = !saving, onClick = { launch { exportCsv.launch("cabas-moviments-$today.csv") } }) { Text(stringResource(R.string.export_csv)) }
        }
        if (saving) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (noFilePicker) Text(stringResource(R.string.no_file_picker), color = MaterialTheme.colorScheme.error)
        if (error != null && pending == null) Text(error, color = MaterialTheme.colorScheme.error)
    }

    pending?.let { request ->
        val data = request.data
        AlertDialog(
            onDismissRequest = { if (!saving) model.cancelImport() },
            title = { Text(stringResource(R.string.restore_confirm_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.restore_confirm_counts, data.movements.size, data.categories.size, data.plans.size, data.recurring.size, data.reflections.size))
                    Text(stringResource(R.string.restore_confirm_body), color = MaterialTheme.colorScheme.error)
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                }
            },
            confirmButton = { TextButton(enabled = !saving, onClick = model::confirmImport) { Text(stringResource(R.string.restore_confirm)) } },
            dismissButton = { TextButton(enabled = !saving, onClick = model::cancelImport) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Composable
private fun Section(title: Int, body: Int, content: @Composable ColumnScope.() -> Unit) {
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(title), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(body))
            content()
        }
    }
}
