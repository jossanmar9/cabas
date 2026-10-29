package org.cabas.app.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.cabas.app.domain.Category
import org.cabas.app.domain.Kind
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class BackupTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private fun open(name: String) = CabasDatabase.builder(context, name).build()
    private val day = LocalDate.of(2026, 10, 29).toEpochDay()

    private suspend fun fill(db: CabasDatabase) {
        val custom = db.categories().insert(ExpenseCategory(name = "Casa \"nova\"; 1", nameKey = "casa \"nova\"; 1"))
        db.categories().setArchived(2, true)
        db.movements().insert(Movement(cents = 1234, kind = Kind.EXPENSE, category = Category.NEEDS, categoryId = 1, epochDay = day, note = "Mercat"))
        db.movements().insert(Movement(cents = 99, kind = Kind.EXPENSE, categoryId = custom, epochDay = day - 40, note = "=SUMA(A1)"))
        db.movements().insert(Movement(cents = 150000, kind = Kind.INCOME, categoryId = null, epochDay = day, note = "Nòmina ✓"))
        db.plans().save(MonthlyPlan("2026-10", 80000, null))
        db.plans().save(MonthlyPlan("2026-11", 0, 20000))
        db.recurring().insert(RecurringMovement(cents = 60000, kind = Kind.EXPENSE, categoryId = 1, dayOfMonth = 31, note = "Lloguer", startMonth = "2026-10", lastGeneratedMonth = null, active = false))
        db.reflections().save(MonthlyReflection("2026-10", "He cuinat més", "Menys cafés fora"))
    }

    @Test fun roundTripRestoresEveryTableExactly() = runBlocking {
        listOf("backup-source.db", "backup-target.db").forEach(context::deleteDatabase)
        val sourceDb = open("backup-source.db")
        val targetDb = open("backup-target.db")
        try {
            fill(sourceDb)
            val original = Backup.read(sourceDb)
            val json = Backup.toJson(original, LocalDate.of(2026, 10, 29))
            // Data that the restore must replace, including an extra category.
            targetDb.categories().insert(ExpenseCategory(name = "Vella", nameKey = "vella"))
            targetDb.movements().insert(Movement(cents = 5, kind = Kind.EXPENSE, categoryId = 3, epochDay = day, note = "esborrar"))
            Backup.restore(targetDb, Backup.fromJson(json))
            assertEquals(original, Backup.read(targetDb))
            assertEquals(3, targetDb.movements().observeCount().first())
            // The restored database keeps working normally.
            targetDb.movements().insert(Movement(cents = 1, kind = Kind.EXPENSE, categoryId = 1, epochDay = day, note = ""))
            assertEquals(4, targetDb.movements().observeCount().first())
        } finally {
            sourceDb.close(); targetDb.close()
            listOf("backup-source.db", "backup-target.db").forEach(context::deleteDatabase)
        }
    }

    @Test fun rejectsInvalidBackupsWithoutTouchingData() = runBlocking {
        val name = "backup-invalid.db"
        context.deleteDatabase(name)
        val db = open(name)
        try {
            fill(db)
            val before = Backup.read(db)
            val valid = JSONObject(Backup.toJson(before, LocalDate.of(2026, 10, 29)))
            fun mutated(block: JSONObject.() -> Unit) = JSONObject(valid.toString()).apply(block).toString()
            val invalid = listOf(
                "no és json",
                "{}",
                mutated { put("app", "altra") },
                mutated { put("format", 2) },
                mutated { getJSONArray("movements").getJSONObject(0).put("cents", 0) },
                mutated { getJSONArray("movements").getJSONObject(0).put("cents", 12.5) },
                mutated { getJSONArray("movements").getJSONObject(0).put("date", "2026-02-30") },
                mutated { getJSONArray("movements").getJSONObject(0).put("categoryId", 999) },
                mutated { getJSONArray("movements").getJSONObject(0).put("kind", "ALTRE") },
                mutated { getJSONArray("movements").let { it.getJSONObject(1).put("id", it.getJSONObject(0).getLong("id")) } },
                mutated { getJSONArray("categories").getJSONObject(1).put("name", "necessitats") },
                mutated { getJSONArray("recurring").getJSONObject(0).put("dayOfMonth", 32) },
                mutated { getJSONArray("plans").getJSONObject(0).put("month", "2026-13") },
                mutated { getJSONArray("reflections").getJSONObject(0).put("improve", "x".repeat(Backup.MAX_TEXT + 1)) },
                mutated { remove("plans") },
            )
            invalid.forEach { text ->
                assertThrows(InvalidBackupException::class.java) { Backup.fromJson(text) }
            }
            assertEquals(before, Backup.read(db))
        } finally { db.close(); context.deleteDatabase(name) }
    }

    @Test fun csvUsesSemicolonsDecimalCommaAndEscapesFields() {
        val csv = movementsCsv(listOf(
            Movement(id = 1, cents = 1234, kind = Kind.EXPENSE, categoryId = 1, epochDay = day, note = "Pa; llet \"bo\""),
            Movement(id = 2, cents = 150000, kind = Kind.INCOME, categoryId = null, epochDay = day, note = "=HYPERLINK(1)"),
        )) { if (it.kind == Kind.EXPENSE) "Necessitats" else "" }
        val lines = csv.removePrefix("﻿").split("\r\n")
        assertEquals("data;tipus;import;categoria;concepte", lines[0])
        assertEquals("2026-10-29;despesa;-12,34;Necessitats;\"Pa; llet \"\"bo\"\"\"", lines[1])
        assertEquals("2026-10-29;ingrés;1500,00;;'=HYPERLINK(1)", lines[2])
    }
}
