package org.cabas.app.data

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow
import org.cabas.app.domain.Category
import org.cabas.app.domain.Kind

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
    @Insert suspend fun insert(item: Movement)
    @Query("DELETE FROM movements WHERE id = :id") suspend fun delete(id: Long)
}

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
}

@Entity(tableName = "expense_categories", indices = [Index(value = ["nameKey"], unique = true)])
data class ExpenseCategory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val nameKey: String,
    val builtin: Boolean = false,
    val archived: Boolean = false,
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
    @Query("UPDATE expense_categories SET archived = :archived WHERE id = :id")
    suspend fun setArchived(id: Long, archived: Boolean)
}

/** Immutable v3 seeds: keep these stable so upgrades and new installations agree. */
private fun seedV3Categories(db: SupportSQLiteDatabase) {
    db.execSQL("INSERT INTO expense_categories (id,name,nameKey,builtin,archived) VALUES (1,'Necessitats','necessitats',1,0),(2,'Desitjos','desitjos',1,0),(3,'Cultura','cultura',1,0),(4,'Imprevistos','imprevistos',1,0)")
}

@Database(entities = [Movement::class, MonthlyPlan::class, ExpenseCategory::class], version = 3, exportSchema = true)
abstract class CabasDatabase : RoomDatabase() {
    abstract fun movements(): MovementDao
    abstract fun plans(): MonthlyPlanDao
    abstract fun categories(): CategoryDao
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
        private val INITIAL_CATEGORIES = object : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) { seedV3Categories(db) }
        }
        fun builder(context: Context, name: String) = Room.databaseBuilder(context.applicationContext, CabasDatabase::class.java, name)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3).addCallback(INITIAL_CATEGORIES)
        @Volatile private var instance: CabasDatabase? = null
        fun get(context: Context): CabasDatabase = instance ?: synchronized(this) {
            instance ?: builder(context, "cabas.db").build().also { instance = it }
        }
    }
}
