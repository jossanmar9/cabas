package org.cabas.app.data

import androidx.room.withTransaction
import org.cabas.app.domain.Category
import org.cabas.app.domain.Kind
import org.cabas.app.domain.Money
import org.cabas.app.domain.categoryName
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth

/** Everything Cabàs stores, as written to or read from a backup file. */
data class BackupData(
    val categories: List<ExpenseCategory>,
    val movements: List<Movement>,
    val plans: List<MonthlyPlan>,
    val recurring: List<RecurringMovement>,
    val reflections: List<MonthlyReflection>,
)

class InvalidBackupException(message: String) : Exception(message)

/**
 * Backup format 2 (format 1 is still accepted): a UTF-8 JSON document. Amounts are integer cents, dates are ISO `YYYY-MM-DD`
 * and months `YYYY-MM`, so the file is readable and has no rounding issues.
 */
object Backup {
    const val FORMAT = 2
    private val SUPPORTED_FORMATS = 1..FORMAT
    const val MAX_TEXT = 1000

    suspend fun read(db: CabasDatabase): BackupData = db.withTransaction {
        BackupData(db.categories().all(), db.movements().all(), db.plans().all(), db.recurring().all(), db.reflections().all())
    }

    /** Replaces all data atomically: if anything fails, the previous data stays untouched. */
    suspend fun restore(db: CabasDatabase, data: BackupData) = db.withTransaction {
        db.movements().clear()
        db.recurring().clear()
        db.categories().clear()
        db.plans().clearAll()
        db.reflections().clearAll()
        db.categories().insertAll(data.categories)
        db.movements().insertAll(data.movements)
        db.plans().insertAll(data.plans)
        db.recurring().insertAll(data.recurring)
        db.reflections().insertAll(data.reflections)
    }

    fun toJson(data: BackupData, exportedOn: LocalDate): String = JSONObject().apply {
        put("app", "cabas")
        put("format", FORMAT)
        put("exportedOn", exportedOn.toString())
        put("categories", JSONArray(data.categories.map {
            JSONObject().put("id", it.id).put("name", it.name).put("builtin", it.builtin).put("archived", it.archived)
                .put("monthlyBudgetCents", it.monthlyBudgetCents ?: JSONObject.NULL)
        }))
        put("movements", JSONArray(data.movements.map {
            JSONObject().put("id", it.id).put("cents", it.cents).put("kind", it.kind.name)
                .put("categoryId", it.categoryId ?: JSONObject.NULL)
                .put("legacyCategory", it.category?.name ?: JSONObject.NULL)
                .put("date", LocalDate.ofEpochDay(it.epochDay).toString()).put("note", it.note)
        }))
        put("plans", JSONArray(data.plans.map {
            JSONObject().put("month", it.month).put("budgetCents", it.budgetCents ?: JSONObject.NULL)
                .put("savingsGoalCents", it.savingsGoalCents ?: JSONObject.NULL)
        }))
        put("recurring", JSONArray(data.recurring.map {
            JSONObject().put("id", it.id).put("cents", it.cents).put("kind", it.kind.name)
                .put("categoryId", it.categoryId ?: JSONObject.NULL).put("dayOfMonth", it.dayOfMonth)
                .put("note", it.note).put("startMonth", it.startMonth)
                .put("lastGeneratedMonth", it.lastGeneratedMonth ?: JSONObject.NULL).put("active", it.active)
        }))
        put("reflections", JSONArray(data.reflections.map {
            JSONObject().put("month", it.month).put("wentWell", it.wentWell).put("improve", it.improve)
        }))
    }.toString(2)

    /** Parses and fully validates a backup. Throws [InvalidBackupException] with a short reason. */
    fun fromJson(text: String): BackupData {
        val root = runCatching { JSONObject(text) }.getOrNull() ?: fail("no és un fitxer JSON")
        if (root.optString("app") != "cabas") fail("no és una còpia de Cabàs")
        val format = root.optInt("format", -1)
        if (format !in SUPPORTED_FORMATS) fail("format $format no compatible")

        val categories = root.array("categories").objects().map {
            val name = categoryName(it.string("name")) ?: fail("nom de categoria no vàlid")
            // Format 1 had no category budgets.
            val budget = if (format >= 2) it.optionalCents("monthlyBudgetCents") else null
            ExpenseCategory(it.id("id"), name.display, name.key, it.bool("builtin"), it.bool("archived"), budget)
        }
        if (categories.map { it.id }.toSet().size != categories.size) fail("categories repetides")
        if (categories.map { it.nameKey }.toSet().size != categories.size) fail("noms de categoria repetits")
        val categoryIds = categories.map { it.id }.toSet()

        fun checkCategory(kind: Kind, id: Long?) {
            if (kind == Kind.EXPENSE && id == null) fail("despesa sense categoria")
            if (id != null && id !in categoryIds) fail("categoria inexistent")
        }

        val movements = root.array("movements").objects().map {
            val kind = it.kind()
            val categoryId = it.optionalLong("categoryId")
            checkCategory(kind, categoryId)
            val legacy = it.optionalString("legacyCategory")?.let { name ->
                Category.entries.firstOrNull { c -> c.name == name } ?: fail("categoria antiga no vàlida")
            }
            Movement(it.id("id"), it.cents("cents", allowZero = false), kind, legacy, categoryId,
                it.date("date").toEpochDay(), it.text("note", 120))
        }
        if (movements.map { it.id }.toSet().size != movements.size) fail("moviments repetits")

        val plans = root.array("plans").objects().map {
            MonthlyPlan(it.month("month").toString(), it.optionalCents("budgetCents"), it.optionalCents("savingsGoalCents"))
        }
        if (plans.map { it.month }.toSet().size != plans.size) fail("plans repetits")

        val recurring = root.array("recurring").objects().map {
            val kind = it.kind()
            val categoryId = it.optionalLong("categoryId")
            checkCategory(kind, categoryId)
            val day = it.optInt("dayOfMonth", -1).takeIf { d -> d in 1..31 } ?: fail("dia del mes no vàlid")
            RecurringMovement(it.id("id"), it.cents("cents", allowZero = false), kind, categoryId, day,
                it.text("note", 120), it.month("startMonth").toString(),
                it.optionalString("lastGeneratedMonth")?.let { m -> parseMonth(m).toString() }, it.bool("active"))
        }
        if (recurring.map { it.id }.toSet().size != recurring.size) fail("recurrents repetits")

        val reflections = root.array("reflections").objects().map {
            MonthlyReflection(it.month("month").toString(), it.text("wentWell", MAX_TEXT), it.text("improve", MAX_TEXT))
        }
        if (reflections.map { it.month }.toSet().size != reflections.size) fail("reflexions repetides")

        return BackupData(categories, movements, plans, recurring, reflections)
    }

    private fun fail(reason: String): Nothing = throw InvalidBackupException(reason)
    private fun parseMonth(text: String): YearMonth =
        runCatching { YearMonth.parse(text) }.getOrNull() ?: fail("mes no vàlid: $text")

    private fun JSONObject.array(name: String): JSONArray = optJSONArray(name) ?: fail("falta «$name»")
    private fun JSONArray.objects(): List<JSONObject> = (0 until length()).map { optJSONObject(it) ?: fail("element no vàlid") }
    private fun JSONObject.string(name: String): String = (opt(name) as? String) ?: fail("falta «$name»")
    private fun JSONObject.optionalString(name: String): String? = if (isNull(name)) null else string(name)
    private fun JSONObject.bool(name: String): Boolean = (opt(name) as? Boolean) ?: fail("falta «$name»")
    private fun JSONObject.text(name: String, max: Int): String {
        val value = string(name)
        if (value.length > max) fail("text massa llarg")
        return value
    }
    private fun JSONObject.exactLong(name: String): Long? = when (val v = opt(name)) {
        is Int -> v.toLong()
        is Long -> v
        is Number -> runCatching { BigDecimal(v.toString()).longValueExact() }.getOrNull()
        else -> null
    }
    private fun JSONObject.id(name: String): Long = exactLong(name)?.takeIf { it > 0 } ?: fail("identificador no vàlid")
    private fun JSONObject.optionalLong(name: String): Long? = if (isNull(name)) null else id(name)
    private fun JSONObject.cents(name: String, allowZero: Boolean): Long =
        exactLong(name)?.takeIf { it in (if (allowZero) 0L else 1L)..Money.MAX_CENTS } ?: fail("import no vàlid")
    private fun JSONObject.optionalCents(name: String): Long? = if (!has(name)) fail("falta «$name»") else if (isNull(name)) null else cents(name, allowZero = true)
    private fun JSONObject.kind(): Kind = Kind.entries.firstOrNull { it.name == optString("kind") } ?: fail("tipus no vàlid")
    private fun JSONObject.date(name: String): LocalDate =
        runCatching { LocalDate.parse(string(name)) }.getOrNull() ?: fail("data no vàlida")
    private fun JSONObject.month(name: String): YearMonth = parseMonth(string(name))
}

/** Movements as CSV for spreadsheets: semicolon separator, decimal comma and a BOM for Excel. */
fun movementsCsv(movements: List<Movement>, categoryName: (Movement) -> String): String = buildString {
    append('\uFEFF')
    append("data;tipus;import;categoria;concepte\r\n")
    movements.forEach { m ->
        val amount = BigDecimal.valueOf(if (m.kind == Kind.EXPENSE) -m.cents else m.cents, 2).toPlainString().replace('.', ',')
        val kind = if (m.kind == Kind.EXPENSE) "despesa" else "ingrés"
        append(listOf(LocalDate.ofEpochDay(m.epochDay).toString(), kind, amount, categoryName(m), m.note).joinToString(";") { csvField(it) })
        append("\r\n")
    }
}

private fun csvField(value: String): String {
    // Neutralise spreadsheet formulas and quote fields containing separators.
    val safe = if (value.isNotEmpty() && value[0] in "=+-@\t\r" && value.replace(',', '.').toBigDecimalOrNull() == null) "'$value" else value
    return if (safe.any { it == ';' || it == '"' || it == '\n' || it == '\r' }) "\"" + safe.replace("\"", "\"\"") + "\"" else safe
}
