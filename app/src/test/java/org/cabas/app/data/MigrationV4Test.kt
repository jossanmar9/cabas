package org.cabas.app.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.cabas.app.domain.Kind
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], qualifiers = "ca")
class MigrationV4Test {
    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Test fun upgradesV3ToLatestKeepsDataAndAddsRecurringAndReflections() = runBlocking {
        val name = "migration-v4.db"
        context.deleteDatabase(name)
        val day = LocalDate.of(2026, 10, 29).toEpochDay()
        val schema = JSONObject(requireNotNull(javaClass.classLoader!!.getResourceAsStream(
            "org.cabas.app.data.CabasDatabase/3.json"
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
            old.execSQL("INSERT INTO expense_categories (id,name,nameKey,builtin,archived) VALUES (1,'Necessitats','necessitats',1,0),(5,'Gats','gats',0,1)")
            old.execSQL("INSERT INTO movements (id,cents,kind,category,categoryId,epochDay,note) VALUES (3,4200,'EXPENSE','NEEDS',1,?,'Mercat'),(4,900,'EXPENSE',NULL,5,?,'Pinso'),(9,100000,'INCOME',NULL,NULL,?,'Sou')", arrayOf<Any>(day, day, day))
            old.execSQL("INSERT INTO monthly_plans (month,budgetCents,savingsGoalCents) VALUES ('2026-10',50000,NULL)")
            old.version = 3
        }
        val db = CabasDatabase.builder(context, name).build()
        try {
            // Opening runs the migration and validates the whole v4 schema.
            val items = db.movements().all()
            assertEquals(listOf(3L, 4L, 9L), items.map { it.id })
            assertEquals(listOf(4200L, 900L, 100000L), items.map { it.cents })
            assertEquals(listOf(1L, 5L, null), items.map { it.categoryId })
            assertEquals(MonthlyPlan("2026-10", 50000, null), db.plans().observe("2026-10").first())
            assertTrue(db.categories().get(5)!!.archived)
            assertTrue(db.recurring().all().isEmpty())
            val id = db.recurring().insert(RecurringMovement(cents = 60000, kind = Kind.EXPENSE, categoryId = 1, dayOfMonth = 1, note = "Lloguer", startMonth = "2026-10", lastGeneratedMonth = null))
            db.recurring().markGenerated(id, "2026-10")
            assertEquals("2026-10", db.recurring().get(id)!!.lastGeneratedMonth)
            db.reflections().save(MonthlyReflection("2026-10", "bé", "millor"))
            assertEquals("millor", db.reflections().observe("2026-10").first()!!.improve)
        } finally { db.close(); context.deleteDatabase(name) }
    }
}
