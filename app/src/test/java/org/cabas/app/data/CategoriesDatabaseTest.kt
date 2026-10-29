package org.cabas.app.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.cabas.app.domain.*
import org.json.JSONObject
import org.json.JSONArray
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], qualifiers = "ca")
class CategoriesDatabaseTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private fun open(name: String) = CabasDatabase.builder(context, name).build()

    @Test fun freshInstallSeedsDefaultsAndPersistsRenameArchiveRestoreAndCustomCategories() = runBlocking {
        val name = "categories-fresh.db"
        context.deleteDatabase(name)
        var db = open(name)
        val day = LocalDate.of(2026, 10, 27).toEpochDay()
        try {
            assertEquals(4, db.categories().observeAll().first().size)
            val normalized = categoryName("  Menjar   fora  ")!!
            val id = db.categories().insert(ExpenseCategory(name = normalized.display, nameKey = normalized.key))
            db.movements().insert(Movement(cents = 2350, kind = Kind.EXPENSE, categoryId = id, epochDay = day, note = "Dinar"))
            db.categories().rename(id, "Restaurants", "restaurants")
            db.categories().setArchived(id, true)
            db.close()
            db = open(name)
            val stored = db.categories().get(id)!!
            assertEquals("Restaurants", stored.name)
            assertTrue(stored.archived)
            assertFalse(stored.builtin)
            assertEquals(id, db.movements().observeMonth(day, day + 1).first().single().categoryId)
            assertEquals(2350L, db.movements().observeMonth(day, day + 1).first().single().cents)
            db.categories().setArchived(id, false)
            assertFalse(db.categories().get(id)!!.archived)
            // Included categories are also editable without changing their identity.
            db.categories().rename(1, "Essencials", "essencials")
            assertTrue(db.categories().get(1)!!.builtin)
            assertEquals("Essencials", db.categories().get(1)!!.name)
            // Duplicate names are rejected at the storage boundary, including archived names.
            db.categories().setArchived(id, true)
            assertTrue(runCatching { db.categories().insert(ExpenseCategory(name = "RESTAURANTS", nameKey = "restaurants")) }.isFailure)
            // The foreign key prevents broken category references.
            assertTrue(runCatching { db.movements().insert(Movement(cents = 1, kind = Kind.EXPENSE, categoryId = 9999, epochDay = day, note = "")) }.isFailure)
            assertEquals(1, db.movements().observeMonth(day, day + 1).first().size)
        } finally { db.close(); context.deleteDatabase(name) }
    }

    @Test fun migratesV2CategoriesAndKeepsMonthlyPlansAndEveryMovementField() = runBlocking {
        val name = "categories-v2.db"
        context.deleteDatabase(name)
        val day = LocalDate.of(2026, 10, 27).toEpochDay()
        val schema = JSONObject(requireNotNull(javaClass.classLoader!!.getResourceAsStream(
            "org.cabas.app.data.CabasDatabase/2.json"
        )).bufferedReader().use { it.readText() }).getJSONObject("database")
        context.openOrCreateDatabase(name, Context.MODE_PRIVATE, null).use { old ->
            val entities = schema.getJSONArray("entities")
            for (i in 0 until entities.length()) {
                val entity = entities.getJSONObject(i)
                old.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", entity.getString("tableName")))
                val indices = entity.optJSONArray("indices") ?: JSONArray()
                for (j in 0 until indices.length()) old.execSQL(indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", entity.getString("tableName")))
            }
            val setup = schema.getJSONArray("setupQueries")
            for (i in 0 until setup.length()) old.execSQL(setup.getString(i))
            Category.entries.forEachIndexed { index, category ->
                old.execSQL("INSERT INTO movements (id,cents,kind,category,epochDay,note) VALUES (?,?, 'EXPENSE',?,?,?)", arrayOf<Any>(index + 1, 100 + index, category.name, day, category.name))
            }
            old.execSQL("INSERT INTO movements (id,cents,kind,category,epochDay,note) VALUES (5,10000,'INCOME',NULL,?,'Sou')", arrayOf(day))
            old.execSQL("INSERT INTO monthly_plans (month,budgetCents,savingsGoalCents) VALUES ('2026-10',50000,10000)")
            old.version = 2
        }
        val db = open(name)
        try {
            val items = db.movements().observeMonth(day, day + 1).first().sortedBy { it.id }
            assertEquals(5, items.size)
            items.take(4).forEachIndexed { index, item ->
                assertEquals(index + 1L, item.id)
                assertEquals(index + 1L, item.categoryId)
                assertEquals(100L + index, item.cents)
                assertEquals(Category.entries[index], item.category)
                assertEquals(Category.entries[index].name, item.note)
                assertEquals(day, item.epochDay)
                assertEquals(Kind.EXPENSE, item.kind)
            }
            assertNull(items.last().categoryId)
            assertEquals("Sou", items.last().note)
            assertEquals(10000L, items.last().cents)
            assertEquals(MonthlyPlan("2026-10", 50000, 10000), db.plans().observe("2026-10").first())
            assertEquals(4, db.categories().observeAll().first().size)
            assertEquals("Necessitats", db.categories().get(items.first().categoryId!!)!!.name)
        } finally { db.close(); context.deleteDatabase(name) }
    }
}
