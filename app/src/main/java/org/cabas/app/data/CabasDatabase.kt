package org.cabas.app.data

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow
import org.cabas.app.domain.Category
import org.cabas.app.domain.Kind
import org.cabas.app.domain.categoryName
import org.cabas.app.R

@Entity(tableName = "movements", indices = [Index("epochDay"), Index("categoryId")],
    foreignKeys = [ForeignKey(entity = ExpenseCategory::class, parentColumns = ["id"], childColumns = ["categoryId"], onDelete = ForeignKey.RESTRICT)])
data class Movement(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cents: Long,
    val kind: Kind,
    val category: Category? = null, // Legacy v1/v2 value retained for compatibility.
    val categoryId: Long? = null,
    val epochDay: Long,
    val note: String,
)

@Dao
interface MovementDao {
    @Query("SELECT * FROM movements WHERE epochDay >= :start AND epochDay < :end ORDER BY epochDay DESC, id DESC")
    fun observeMonth(start: Long, end: Long): Flow<List<Movement>>
    @Query("SELECT COUNT(*) FROM movements") fun observeCount(): Flow<Int>
    @Query("SELECT * FROM movements ORDER BY epochDay DESC, id DESC") fun observeAll(): Flow<List<Movement>>
    @Query("SELECT COUNT(*) FROM movements WHERE epochDay = :day") suspend fun countOn(day: Long): Int
    /** Concepts already used, most frequent first, with the kind and category of their latest use. */
    @Query("SELECT note, kind, categoryId, MAX(id) AS lastId FROM movements WHERE note != '' GROUP BY note ORDER BY COUNT(*) DESC, lastId DESC LIMIT 300")
    fun observeNoteSuggestions(): Flow<List<NoteSuggestion>>
    @Query("SELECT * FROM movements ORDER BY epochDay, id") suspend fun all(): List<Movement>
    @Query("SELECT * FROM movements WHERE id = :id") suspend fun get(id: Long): Movement?
    @Insert suspend fun insert(item: Movement): Long
    @Insert suspend fun insertAll(items: List<Movement>)
    @Update suspend fun update(item: Movement)
    @Query("DELETE FROM movements WHERE id = :id") suspend fun delete(id: Long)
    @Query("DELETE FROM movements") suspend fun clear()
}

data class NoteSuggestion(val note: String, val kind: Kind, val categoryId: Long?, val lastId: Long)

@Entity(tableName = "monthly_plans")
data class MonthlyPlan(
    @PrimaryKey val month: String,
    val budgetCents: Long?,
    val savingsGoalCents: Long?,
)

@Dao
interface MonthlyPlanDao {
    @Query("SELECT * FROM monthly_plans WHERE month = :month")
    fun observe(month: String): Flow<MonthlyPlan?>
    @Upsert suspend fun save(plan: MonthlyPlan)
    @Query("DELETE FROM monthly_plans WHERE month = :month") suspend fun clear(month: String)
    @Query("SELECT * FROM monthly_plans ORDER BY month") suspend fun all(): List<MonthlyPlan>
    @Insert suspend fun insertAll(items: List<MonthlyPlan>)
    @Query("DELETE FROM monthly_plans") suspend fun clearAll()
}

/** A template that adds one movement per month on [dayOfMonth] (clamped to the month length). */
@Entity(tableName = "recurring_movements")
data class RecurringMovement(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cents: Long,
    val kind: Kind,
    val categoryId: Long?,
    val dayOfMonth: Int,
    val note: String,
    /** First month (`YYYY-MM`) in which a movement may be generated. */
    val startMonth: String,
    /** Last month (`YYYY-MM`) already generated, or null if none yet. */
    val lastGeneratedMonth: String?,
    val active: Boolean = true,
)

@Dao
interface RecurringDao {
    @Query("SELECT * FROM recurring_movements ORDER BY active DESC, dayOfMonth, id")
    fun observeAll(): Flow<List<RecurringMovement>>
    @Query("SELECT * FROM recurring_movements ORDER BY id") suspend fun all(): List<RecurringMovement>
    @Query("SELECT * FROM recurring_movements WHERE id = :id") suspend fun get(id: Long): RecurringMovement?
    @Insert suspend fun insert(item: RecurringMovement): Long
    @Insert suspend fun insertAll(items: List<RecurringMovement>)
    @Update suspend fun update(item: RecurringMovement)
    @Query("UPDATE recurring_movements SET lastGeneratedMonth = :month WHERE id = :id") suspend fun markGenerated(id: Long, month: String)
    @Query("DELETE FROM recurring_movements WHERE id = :id") suspend fun delete(id: Long)
    @Query("DELETE FROM recurring_movements") suspend fun clear()
}

/** Kakebo end-of-month reflection: free text, one row per month (`YYYY-MM`). */
@Entity(tableName = "monthly_reflections")
data class MonthlyReflection(
    @PrimaryKey val month: String,
    val wentWell: String,
    val improve: String,
)

@Dao
interface ReflectionDao {
    @Query("SELECT * FROM monthly_reflections WHERE month = :month")
    fun observe(month: String): Flow<MonthlyReflection?>
    @Query("SELECT * FROM monthly_reflections ORDER BY month") suspend fun all(): List<MonthlyReflection>
    @Upsert suspend fun save(item: MonthlyReflection)
    @Query("DELETE FROM monthly_reflections WHERE month = :month") suspend fun clear(month: String)
    @Insert suspend fun insertAll(items: List<MonthlyReflection>)
    @Query("DELETE FROM monthly_reflections") suspend fun clearAll()
}

@Entity(tableName = "expense_categories", indices = [Index(value = ["nameKey"], unique = true)])
data class ExpenseCategory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val nameKey: String,
    val builtin: Boolean = false,
    val archived: Boolean = false,
    /** Optional spending limit for this category, applied to every month. */
    val monthlyBudgetCents: Long? = null,
)

@Dao
interface CategoryDao {
    @Query("SELECT * FROM expense_categories ORDER BY archived, name COLLATE NOCASE")
    fun observeAll(): Flow<List<ExpenseCategory>>
    @Query("SELECT * FROM expense_categories WHERE id = :id") suspend fun get(id: Long): ExpenseCategory?
    @Query("SELECT * FROM expense_categories WHERE nameKey = :key") suspend fun findName(key: String): ExpenseCategory?
    @Insert suspend fun insert(category: ExpenseCategory): Long
    @Query("UPDATE expense_categories SET name = :name, nameKey = :key WHERE id = :id")
    suspend fun rename(id: Long, name: String, key: String)
    @Query("UPDATE expense_categories SET monthlyBudgetCents = :cents WHERE id = :id")
    suspend fun setBudget(id: Long, cents: Long?)
    @Query("UPDATE expense_categories SET archived = :archived WHERE id = :id")
    suspend fun setArchived(id: Long, archived: Boolean)
    @Query("SELECT * FROM expense_categories ORDER BY id") suspend fun all(): List<ExpenseCategory>
    @Insert suspend fun insertAll(items: List<ExpenseCategory>)
    @Query("DELETE FROM expense_categories") suspend fun clear()
}

/** Immutable v3 seeds: keep these stable so upgrades and new installations agree. */
private fun seedV3Categories(db: SupportSQLiteDatabase) {
    db.execSQL("INSERT INTO expense_categories (id,name,nameKey,builtin,archived) VALUES (1,'Necessitats','necessitats',1,0),(2,'Desitjos','desitjos',1,0),(3,'Cultura','cultura',1,0),(4,'Imprevistos','imprevistos',1,0)")
}

@Database(
    entities = [Movement::class, MonthlyPlan::class, ExpenseCategory::class, RecurringMovement::class, MonthlyReflection::class],
    version = 5, exportSchema = true,
)
abstract class CabasDatabase : RoomDatabase() {
    abstract fun movements(): MovementDao
    abstract fun plans(): MonthlyPlanDao
    abstract fun categories(): CategoryDao
    abstract fun recurring(): RecurringDao
    abstract fun reflections(): ReflectionDao
    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `monthly_plans` (`month` TEXT NOT NULL, `budgetCents` INTEGER, `savingsGoalCents` INTEGER, PRIMARY KEY(`month`))")
            }
        }
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `expense_categories` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `nameKey` TEXT NOT NULL, `builtin` INTEGER NOT NULL, `archived` INTEGER NOT NULL)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_expense_categories_nameKey` ON `expense_categories` (`nameKey`)")
                seedV3Categories(db)
                db.execSQL("ALTER TABLE `movements` ADD COLUMN `categoryId` INTEGER REFERENCES `expense_categories`(`id`) ON UPDATE NO ACTION ON DELETE RESTRICT")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_movements_categoryId` ON `movements` (`categoryId`)")
                db.execSQL("UPDATE movements SET categoryId = CASE category WHEN 'NEEDS' THEN 1 WHEN 'WANTS' THEN 2 WHEN 'CULTURE' THEN 3 WHEN 'UNEXPECTED' THEN 4 END WHERE kind = 'EXPENSE'")
            }
        }
        /** v4 only adds two tables; existing movements, plans and categories are untouched. */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `recurring_movements` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `cents` INTEGER NOT NULL, `kind` TEXT NOT NULL, `categoryId` INTEGER, `dayOfMonth` INTEGER NOT NULL, `note` TEXT NOT NULL, `startMonth` TEXT NOT NULL, `lastGeneratedMonth` TEXT, `active` INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `monthly_reflections` (`month` TEXT NOT NULL, `wentWell` TEXT NOT NULL, `improve` TEXT NOT NULL, PRIMARY KEY(`month`))")
            }
        }
        /** v5 adds an optional monthly budget to each category. */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `expense_categories` ADD COLUMN `monthlyBudgetCents` INTEGER")
            }
        }
        /** New installations get the four Kakebo categories named in the app language. */
        private fun initialCategories(context: Context) = object : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                val names = listOf(R.string.needs, R.string.wants, R.string.culture, R.string.unexpected).map { context.getString(it) }
                val keys = names.map { categoryName(it)?.key ?: it.lowercase(java.util.Locale.ROOT) }
                if (keys.toSet().size != 4) { seedV3Categories(db); return }
                names.forEachIndexed { index, name ->
                    db.execSQL("INSERT INTO expense_categories (id,name,nameKey,builtin,archived) VALUES (?,?,?,1,0)", arrayOf<Any>(index + 1, name, keys[index]))
                }
            }
        }
        fun builder(context: Context, name: String) = Room.databaseBuilder(context.applicationContext, CabasDatabase::class.java, name)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5).addCallback(initialCategories(context.applicationContext))
        @Volatile private var instance: CabasDatabase? = null
        fun get(context: Context): CabasDatabase = instance ?: synchronized(this) {
            instance ?: builder(context, "cabas.db").build().also { instance = it }
        }
    }
}
