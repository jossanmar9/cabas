package org.cabas.app.data

import android.content.Context
import androidx.room.Room
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
class MonthlyPlanDatabaseTest {
    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private fun open(name: String) = Room.databaseBuilder(context, CabasDatabase::class.java, name)
        .addMigrations(CabasDatabase.MIGRATION_1_2).build()

    @Test fun upgradesV1RetainsMovementsAndPersistsIndependentMonthlyPlans() = runBlocking {
        val name = "migration-plan-test.db"
        context.deleteDatabase(name)
        val start = LocalDate.of(2026, 10, 1).toEpochDay()
        // Recreate the actual released v1 schema, including its Room identity hash.
        val schema = JSONObject(requireNotNull(javaClass.classLoader!!.getResourceAsStream(
            "org.cabas.app.data.CabasDatabase/1.json"
        )).bufferedReader().use { it.readText() }).getJSONObject("database")
        context.openOrCreateDatabase(name, Context.MODE_PRIVATE, null).use { old ->
            val entities = schema.getJSONArray("entities")
            for (i in 0 until entities.length()) {
                val entity = entities.getJSONObject(i)
                old.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}", entity.getString("tableName")))
                val indices = entity.getJSONArray("indices")
                for (j in 0 until indices.length()) old.execSQL(indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}", entity.getString("tableName")))
            }
            val setup = schema.getJSONArray("setupQueries")
            for (i in 0 until setup.length()) old.execSQL(setup.getString(i))
            old.execSQL("INSERT INTO movements (id,cents,kind,category,epochDay,note) VALUES (7,1234,'EXPENSE','NEEDS',?,'Mercat')", arrayOf(start))
            old.version = 1
        }
        var db = open(name)
        try {
            // Opening with Room runs the migration and validates the entire v2 schema.
            val original = db.movements().observeMonth(start, start + 31).first().single()
            assertEquals(7L, original.id)
            assertEquals(1234L, original.cents)
            assertEquals(Kind.EXPENSE, original.kind)
            assertEquals(Category.NEEDS, original.category)
            assertEquals("Mercat", original.note)
            assertNull(db.plans().observe("2026-10").first())
            db.plans().save(MonthlyPlan("2026-10", 80000, 20000))
            db.plans().save(MonthlyPlan("2026-11", 0, null))
            db.close()
            db = open(name)
            assertEquals(MonthlyPlan("2026-10", 80000, 20000), db.plans().observe("2026-10").first())
            assertEquals(MonthlyPlan("2026-11", 0, null), db.plans().observe("2026-11").first())
            db.plans().save(MonthlyPlan("2026-10", null, 50000))
            assertEquals(MonthlyPlan("2026-10", null, 50000), db.plans().observe("2026-10").first())
            assertEquals(0L, db.plans().observe("2026-11").first()!!.budgetCents)
            db.plans().clear("2026-10")
            assertNull(db.plans().observe("2026-10").first())
            assertEquals(original, db.movements().observeMonth(start, start + 31).first().single())
            // Existing movements remain writable after migration.
            db.movements().insert(original.copy(id = 0, note = "Nou"))
            assertEquals(2, db.movements().observeMonth(start, start + 31).first().size)
        } finally { db.close(); context.deleteDatabase(name) }
    }

    @Test fun createsFreshDatabaseWithSavingsOnlyAndExplicitZeroGoal() = runBlocking {
        val name = "fresh-plan-test.db"
        context.deleteDatabase(name)
        var db = open(name)
        try {
            db.plans().save(MonthlyPlan("2026-12", null, 0))
            db.close()
            db = open(name)
            assertEquals(MonthlyPlan("2026-12", null, 0), db.plans().observe("2026-12").first())
            assertNull(db.plans().observe("2027-01").first())
        } finally { db.close(); context.deleteDatabase(name) }
    }
}
