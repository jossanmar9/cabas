package org.cabas.app.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
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
class MigrationV5Test {
    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Test fun upgradesV4KeepsEverythingAndAddsCategoryBudgets() = runBlocking {
        val name = "migration-v5.db"
        context.deleteDatabase(name)
        val day = LocalDate.of(2026, 10, 29).toEpochDay()
        val schema = JSONObject(requireNotNull(javaClass.classLoader!!.getResourceAsStream(
            "org.cabas.app.data.CabasDatabase/4.json"
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
            old.execSQL("INSERT INTO expense_categories (id,name,nameKey,builtin,archived) VALUES (1,'Necessitats','necessitats',1,0),(7,'Gats','gats',0,1)")
            old.execSQL("INSERT INTO movements (id,cents,kind,category,categoryId,epochDay,note) VALUES (3,4200,'EXPENSE','NEEDS',1,?,'Mercat'),(9,100000,'INCOME',NULL,NULL,?,'Sou')", arrayOf<Any>(day, day))
            old.execSQL("INSERT INTO monthly_plans (month,budgetCents,savingsGoalCents) VALUES ('2026-10',50000,NULL)")
            old.execSQL("INSERT INTO recurring_movements (id,cents,kind,categoryId,dayOfMonth,note,startMonth,lastGeneratedMonth,active) VALUES (2,60000,'EXPENSE',1,1,'Lloguer','2026-10','2026-10',1)")
            old.execSQL("INSERT INTO monthly_reflections (month,wentWell,improve) VALUES ('2026-10','bé','millor')")
            old.version = 4
        }
        val db = CabasDatabase.builder(context, name).build()
        try {
            val data = Backup.read(db)
            assertEquals(listOf(3L, 9L), data.movements.map { it.id })
            assertEquals(listOf(1L, 7L), data.categories.map { it.id })
            assertTrue(data.categories.all { it.monthlyBudgetCents == null })
            assertTrue(data.categories.single { it.id == 7L }.archived)
            assertEquals(MonthlyPlan("2026-10", 50000, null), data.plans.single())
            assertEquals("2026-10", data.recurring.single().lastGeneratedMonth)
            assertEquals("millor", data.reflections.single().improve)
            db.categories().setBudget(1, 15000)
            assertEquals(15000L, db.categories().get(1)!!.monthlyBudgetCents)
            db.categories().setBudget(1, null)
            assertNull(db.categories().get(1)!!.monthlyBudgetCents)
        } finally { db.close(); context.deleteDatabase(name) }
    }
}
