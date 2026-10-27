package org.cabas.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.cabas.app.data.*
import org.cabas.app.domain.*
import java.time.YearMonth

data class MonthState(val items: List<Movement> = emptyList(), val plan: MonthlyPlan? = null, val selectedMonth: YearMonth? = null, val loading: Boolean = true, val failed: Boolean = false)

@OptIn(ExperimentalCoroutinesApi::class)
class CabasViewModel(application: Application) : AndroidViewModel(application) {
    private val database = CabasDatabase.get(application)
    private val dao = database.movements()
    private val plans = database.plans()
    val month = MutableStateFlow(YearMonth.now())
    private val reload = MutableStateFlow(0)
    val state = combine(month, reload) { month, _ -> month }.flatMapLatest { selected ->
        combine(
            dao.observeMonth(selected.atDay(1).toEpochDay(), selected.plusMonths(1).atDay(1).toEpochDay()),
            plans.observe(selected.toString()),
        ) { items, plan -> MonthState(items = items, plan = plan, selectedMonth = selected, loading = false) }
            .onStart { emit(MonthState()) }
            .catch { emit(MonthState(loading = false, failed = true)) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), MonthState())
    private val _saving = MutableStateFlow(false)
    val saving = _saving.asStateFlow()
    private val _error = MutableStateFlow<Int?>(null)
    val error = _error.asStateFlow()
    fun clearError() { _error.value = null }
    fun retry() { reload.value += 1 }
    fun moveMonth(delta: Long) { month.value = month.value.plusMonths(delta) }
    fun add(amount: String, date: String, note: String, kind: Kind, category: Category, done: () -> Unit) {
        if (_saving.value) return
        val cents = Money.parse(amount) ?: return fail(R.string.invalid_amount)
        val day = parseDate(date) ?: return fail(R.string.invalid_date)
        if (note.trim().length > 120) return fail(R.string.note_too_long)
        write {
            dao.insert(Movement(cents = cents, kind = kind, category = category.takeIf { kind == Kind.EXPENSE }, epochDay = day.toEpochDay(), note = note.trim()))
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
    private fun fail(message: Int) { _error.value = message }
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
}
