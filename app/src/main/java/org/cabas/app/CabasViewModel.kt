package org.cabas.app

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.cabas.app.data.*
import org.cabas.app.domain.*
import org.cabas.app.work.AppPrefs
import org.cabas.app.work.Background
import java.time.LocalDate
import java.time.YearMonth

data class MonthState(
    val items: List<Movement> = emptyList(),
    val plan: MonthlyPlan? = null,
    val previousPlan: MonthlyPlan? = null,
    val reflection: MonthlyReflection? = null,
    val categories: List<ExpenseCategory> = emptyList(),
    val selectedMonth: YearMonth? = null,
    val loading: Boolean = true,
    val failed: Boolean = false,
)

/** A message shown to the user: a string resource with an optional argument. */
data class UiMessage(val res: Int, val arg: String? = null)

/** A validated backup waiting for the user to confirm that it replaces the current data. */
data class PendingImport(val data: BackupData)

/** Days without a backup after which the summary shows a reminder. */
const val BACKUP_REMINDER_DAYS = 30L

@OptIn(ExperimentalCoroutinesApi::class)
class CabasViewModel(application: Application) : AndroidViewModel(application) {
    private val database = CabasDatabase.get(application)
    private val dao = database.movements()
    private val plans = database.plans()
    private val categoryDao = database.categories()
    private val recurringDao = database.recurring()
    private val reflections = database.reflections()
    private val preferences = application.getSharedPreferences("preferences", 0)
    private val prefs = AppPrefs(application)

    private val _themeMode = MutableStateFlow(ThemeMode.entries.firstOrNull { it.name == preferences.getString("theme", null) } ?: ThemeMode.SYSTEM)
    val themeMode = _themeMode.asStateFlow()
    fun setTheme(mode: ThemeMode) {
        preferences.edit().putString("theme", mode.name).apply()
        _themeMode.value = mode
    }

    private val _lastBackup = MutableStateFlow(prefs.lastBackupDay)
    val lastBackup = _lastBackup.asStateFlow()
    private val _autoBackupFolder = MutableStateFlow(prefs.autoBackupFolder)
    val autoBackupFolder = _autoBackupFolder.asStateFlow()
    private val _autoBackupFailed = MutableStateFlow(prefs.lastAutoBackupFailed)
    val autoBackupFailed = _autoBackupFailed.asStateFlow()
    private val _reminderHour = MutableStateFlow(prefs.reminderHour)
    val reminderHour = _reminderHour.asStateFlow()
    private val _lockEnabled = MutableStateFlow(prefs.lockEnabled)
    val lockEnabled = _lockEnabled.asStateFlow()
    /** Set by the launcher shortcut or the reminder; the summary opens the add form and clears it. */
    private val _addRequested = MutableStateFlow(false)
    val addRequested = _addRequested.asStateFlow()

    val allMovements = dao.observeAll().catch { emit(emptyList()) }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val noteSuggestions = dao.observeNoteSuggestions().catch { emit(emptyList()) }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val allCategories = categoryDao.observeAll().catch { emit(emptyList()) }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val movementCount = dao.observeCount().catch { emit(0) }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
    val recurring = recurringDao.observeAll().catch { emit(emptyList()) }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val month = MutableStateFlow(YearMonth.now())
    private val reload = MutableStateFlow(0)
    val state = combine(month, reload) { month, _ -> month }.flatMapLatest { selected ->
        combine(
            dao.observeMonth(selected.atDay(1).toEpochDay(), selected.plusMonths(1).atDay(1).toEpochDay()),
            plans.observe(selected.toString()),
            plans.observe(selected.minusMonths(1).toString()),
            reflections.observe(selected.toString()),
            categoryDao.observeAll(),
        ) { items, plan, previous, reflection, categories ->
            MonthState(items, plan, previous, reflection, categories, selected, loading = false)
        }
            .onStart { emit(MonthState()) }
            .catch { emit(MonthState(loading = false, failed = true)) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MonthState())

    private val _saving = MutableStateFlow(false)
    val saving = _saving.asStateFlow()
    private val _error = MutableStateFlow<UiMessage?>(null)
    val error = _error.asStateFlow()
    private val _notice = MutableStateFlow<UiMessage?>(null)
    val notice = _notice.asStateFlow()
    private val _pendingImport = MutableStateFlow<PendingImport?>(null)
    val pendingImport = _pendingImport.asStateFlow()

    init { generateRecurring() }

    fun clearError() { _error.value = null }

    fun handleIntent(intent: Intent?) { if (intent?.action == org.cabas.app.work.ACTION_ADD) _addRequested.value = true }
    fun consumeAddRequest() { _addRequested.value = false }

    /** Background work may have changed preferences while the app was closed. */
    fun refreshPreferences() {
        _lastBackup.value = prefs.lastBackupDay
        _autoBackupFailed.value = prefs.lastAutoBackupFailed
        _autoBackupFolder.value = prefs.autoBackupFolder
    }

    fun enableAutoBackup(folder: Uri) {
        val app = getApplication<Application>()
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        try { app.contentResolver.takePersistableUriPermission(folder, flags) }
        catch (_: SecurityException) { return fail(R.string.auto_backup_error) }
        prefs.autoBackupFolder?.let { old -> if (old != folder.toString()) runCatching { app.contentResolver.releasePersistableUriPermission(Uri.parse(old), flags) } }
        prefs.autoBackupFolder = folder.toString()
        prefs.lastAutoBackupFailed = false
        _autoBackupFolder.value = folder.toString()
        _autoBackupFailed.value = false
        Background.scheduleAutoBackup(app)
        _notice.value = UiMessage(R.string.auto_backup_enabled)
    }

    fun disableAutoBackup() {
        val app = getApplication<Application>()
        Background.cancelAutoBackup(app)
        prefs.autoBackupFolder?.let { old ->
            runCatching { app.contentResolver.releasePersistableUriPermission(Uri.parse(old), Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION) }
        }
        prefs.autoBackupFolder = null
        prefs.lastAutoBackupFailed = false
        _autoBackupFolder.value = null
        _autoBackupFailed.value = false
    }

    fun setReminder(hour: Int?) {
        val app = getApplication<Application>()
        prefs.reminderHour = hour
        _reminderHour.value = hour
        if (hour == null) Background.cancelReminder(app) else Background.scheduleReminder(app, hour)
    }

    fun setLock(enabled: Boolean) {
        prefs.lockEnabled = enabled
        _lockEnabled.value = enabled
    }

    fun saveCategoryBudget(id: Long, input: String, done: () -> Unit) {
        if (_saving.value) return
        val cents = if (input.isBlank()) null else Money.parse(input, allowZero = true) ?: return fail(R.string.invalid_plan_amount)
        write { categoryDao.setBudget(id, cents); done() }
    }
    fun clearNotice() { _notice.value = null }
    fun retry() { reload.value += 1 }
    fun moveMonth(delta: Long) { month.value = month.value.plusMonths(delta) }
    fun goToCurrentMonth() { month.value = YearMonth.now() }

    /** Adds a movement, or updates it when [id] is not null. */
    fun saveMovement(id: Long?, amount: String, date: String, note: String, kind: Kind, categoryId: Long?, done: () -> Unit) {
        if (_saving.value) return
        val cents = Money.parse(amount) ?: return fail(R.string.invalid_amount)
        val day = parseDate(date) ?: return fail(R.string.invalid_date)
        if (note.trim().length > 120) return fail(R.string.note_too_long)
        write {
            val existing = id?.let { dao.get(it) }
            if (id != null && existing == null) { fail(R.string.storage_error); return@write }
            val selectedCategory = if (kind == Kind.EXPENSE) categoryId?.let { categoryDao.get(it) } else null
            // An archived category is accepted only if the movement being edited already had it.
            val keepsOwnCategory = existing != null && selectedCategory?.id == existing.categoryId
            if (kind == Kind.EXPENSE && (selectedCategory == null || (selectedCategory.archived && !keepsOwnCategory))) {
                fail(R.string.choose_active_category)
                return@write
            }
            val legacy = if (existing != null && kind == Kind.EXPENSE && selectedCategory?.id == existing.categoryId) existing.category else null
            val item = Movement(id = id ?: 0, cents = cents, kind = kind, category = legacy, categoryId = selectedCategory?.id, epochDay = day.toEpochDay(), note = note.trim())
            if (existing == null) dao.insert(item) else dao.update(item)
            month.value = YearMonth.from(day)
            done()
        }
    }

    fun delete(item: Movement, done: () -> Unit) {
        if (_saving.value) return
        write { dao.delete(item.id); done() }
    }

    fun savePlan(selectedMonth: String, budget: String, goal: String, done: () -> Unit) {
        if (_saving.value) return
        val targets = parseTargets(budget, goal) ?: return fail(R.string.invalid_plan_amount)
        val key = runCatching { YearMonth.parse(selectedMonth).toString() }.getOrNull() ?: return fail(R.string.invalid_date)
        write {
            if (targets.budgetCents == null && targets.savingsGoalCents == null) plans.clear(key)
            else plans.save(MonthlyPlan(key, targets.budgetCents, targets.savingsGoalCents))
            done()
        }
    }

    fun saveReflection(selectedMonth: YearMonth, wentWell: String, improve: String, done: () -> Unit) {
        if (_saving.value) return
        if (wentWell.length > Backup.MAX_TEXT || improve.length > Backup.MAX_TEXT) return fail(R.string.reflection_too_long)
        write {
            val key = selectedMonth.toString()
            if (wentWell.isBlank() && improve.isBlank()) reflections.clear(key)
            else reflections.save(MonthlyReflection(key, wentWell.trim(), improve.trim()))
            _notice.value = UiMessage(R.string.reflection_saved)
            done()
        }
    }

    fun saveCategory(id: Long?, input: String, done: () -> Unit) {
        if (_saving.value) return
        val name = categoryName(input) ?: return fail(R.string.invalid_category_name)
        write {
            val duplicate = categoryDao.findName(name.key)
            if (duplicate != null && duplicate.id != id) {
                fail(R.string.duplicate_category)
                return@write
            }
            if (id == null) categoryDao.insert(ExpenseCategory(name = name.display, nameKey = name.key))
            else categoryDao.rename(id, name.display, name.key)
            done()
        }
    }

    fun archiveCategory(category: ExpenseCategory, done: () -> Unit) {
        if (_saving.value) return
        write { categoryDao.setArchived(category.id, !category.archived); done() }
    }

    /** Adds or edits a recurring template, then fills in any month whose date has already arrived. */
    fun saveRecurring(id: Long?, amount: String, kind: Kind, categoryId: Long?, dayText: String, note: String, active: Boolean, done: () -> Unit) {
        if (_saving.value) return
        val cents = Money.parse(amount) ?: return fail(R.string.invalid_amount)
        val day = parseDayOfMonth(dayText) ?: return fail(R.string.invalid_day)
        if (note.trim().length > 120) return fail(R.string.note_too_long)
        write {
            val existing = id?.let { recurringDao.get(it) }
            val selectedCategory = if (kind == Kind.EXPENSE) categoryId?.let { categoryDao.get(it) } else null
            val keepsOwnCategory = existing != null && selectedCategory?.id == existing.categoryId
            if (kind == Kind.EXPENSE && (selectedCategory == null || (selectedCategory.archived && !keepsOwnCategory))) {
                fail(R.string.choose_active_category)
                return@write
            }
            if (existing == null) recurringDao.insert(RecurringMovement(cents = cents, kind = kind, categoryId = selectedCategory?.id,
                dayOfMonth = day, note = note.trim(), startMonth = YearMonth.now().toString(), lastGeneratedMonth = null, active = active))
            else recurringDao.update(existing.copy(cents = cents, kind = kind, categoryId = selectedCategory?.id, dayOfMonth = day, note = note.trim(),
                active = active,
                // Re-activating does not back-fill the paused months.
                lastGeneratedMonth = if (active && !existing.active) YearMonth.now().minusMonths(1).toString().coerceAtLeastMonth(existing.lastGeneratedMonth) else existing.lastGeneratedMonth))
            generateRecurringNow()
            done()
        }
    }

    fun deleteRecurring(item: RecurringMovement, done: () -> Unit) {
        if (_saving.value) return
        write { recurringDao.delete(item.id); done() }
    }

    /** Creates the movements of active recurring templates whose date has arrived. Safe to repeat. */
    fun generateRecurring() {
        viewModelScope.launch {
            try { generateRecurringNow() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { fail(R.string.recurring_error) }
        }
    }

    private suspend fun generateRecurringNow() {
        val today = LocalDate.now()
        database.withTransaction {
            recurringDao.all().filter { it.active }.forEach { template ->
                val start = runCatching { YearMonth.parse(template.startMonth) }.getOrNull() ?: return@forEach
                val last = template.lastGeneratedMonth?.let { runCatching { YearMonth.parse(it) }.getOrNull() }
                val months = pendingRecurringMonths(start, last, template.dayOfMonth, today)
                if (months.isEmpty()) return@forEach
                val category = template.categoryId?.let { categoryDao.get(it) }
                if (template.kind == Kind.EXPENSE && category == null) return@forEach
                months.forEach { m ->
                    dao.insert(Movement(cents = template.cents, kind = template.kind, categoryId = category?.id,
                        epochDay = recurringDate(m, template.dayOfMonth).toEpochDay(), note = template.note))
                }
                recurringDao.markGenerated(template.id, months.last().toString())
            }
        }
    }

    fun exportBackup(uri: Uri) = io(R.string.export_error) {
        val data = Backup.read(database)
        val json = Backup.toJson(data, LocalDate.now())
        writeText(uri, json)
        markBackup()
        _notice.value = UiMessage(R.string.export_done, data.movements.size.toString())
    }

    fun exportCsv(uri: Uri) = io(R.string.export_error) {
        val data = Backup.read(database)
        val names = data.categories.associate { it.id to it.name }
        writeText(uri, movementsCsv(data.movements) { m ->
            if (m.kind == Kind.INCOME) "" else names[m.categoryId] ?: ""
        })
        _notice.value = UiMessage(R.string.csv_done, data.movements.size.toString())
    }

    /** Reads and validates a backup; nothing changes until [confirmImport]. */
    fun prepareImport(uri: Uri) = io(R.string.import_read_error) {
        val text = getApplication<Application>().contentResolver.openInputStream(uri)?.use { stream ->
            val bytes = stream.readNBytesCompat(MAX_BACKUP_BYTES + 1)
            if (bytes.size > MAX_BACKUP_BYTES) throw InvalidBackupException("fitxer massa gran")
            bytes.toString(Charsets.UTF_8)
        } ?: throw InvalidBackupException("no s’ha pogut obrir")
        _pendingImport.value = PendingImport(Backup.fromJson(text))
    }

    fun cancelImport() { _pendingImport.value = null }

    fun confirmImport() {
        val pending = _pendingImport.value ?: return
        if (_saving.value) return
        write {
            Backup.restore(database, pending.data)
            _pendingImport.value = null
            month.value = YearMonth.now()
            generateRecurringNow()
            _notice.value = UiMessage(R.string.import_done, pending.data.movements.size.toString())
        }
    }

    private fun markBackup() {
        val today = LocalDate.now()
        prefs.lastBackupDay = today
        _lastBackup.value = today
    }

    private suspend fun writeText(uri: Uri, text: String) = withContext(Dispatchers.IO) {
        getApplication<Application>().contentResolver.openOutputStream(uri, "wt")?.use { it.write(text.toByteArray(Charsets.UTF_8)) }
            ?: throw IllegalStateException("No output stream")
    }

    private fun io(errorRes: Int, block: suspend () -> Unit) {
        if (_saving.value) return
        _saving.value = true
        clearError()
        viewModelScope.launch {
            try { withContext(Dispatchers.IO) { block() } }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (invalid: InvalidBackupException) { _error.value = UiMessage(R.string.import_invalid, invalid.message) }
            catch (_: Exception) { fail(errorRes) }
            finally { _saving.value = false }
        }
    }

    private fun fail(message: Int) { _error.value = UiMessage(message) }
    private fun write(block: suspend () -> Unit) {
        _saving.value = true
        clearError()
        viewModelScope.launch {
            try { block() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { fail(R.string.storage_error) }
            finally { _saving.value = false }
        }
    }

    companion object {
        const val MAX_BACKUP_BYTES = 20 * 1024 * 1024
    }
}

private fun String.coerceAtLeastMonth(other: String?): String = if (other != null && other > this) other else this

private fun java.io.InputStream.readNBytesCompat(limit: Int): ByteArray {
    val out = java.io.ByteArrayOutputStream()
    val buffer = ByteArray(8192)
    while (out.size() < limit) {
        val read = read(buffer, 0, minOf(buffer.size, limit - out.size()))
        if (read < 0) break
        out.write(buffer, 0, read)
    }
    return out.toByteArray()
}
