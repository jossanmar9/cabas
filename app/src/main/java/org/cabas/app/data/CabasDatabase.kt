package org.cabas.app.data

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow
import org.cabas.app.domain.Category
import org.cabas.app.domain.Kind

@Entity(tableName = "movements", indices = [Index("epochDay")])
data class Movement(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cents: Long,
    val kind: Kind,
    val category: Category?,
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

@Database(entities = [Movement::class, MonthlyPlan::class], version = 2, exportSchema = true)
abstract class CabasDatabase : RoomDatabase() {
    abstract fun movements(): MovementDao
    abstract fun plans(): MonthlyPlanDao
    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `monthly_plans` (`month` TEXT NOT NULL, `budgetCents` INTEGER, `savingsGoalCents` INTEGER, PRIMARY KEY(`month`))")
            }
        }
        @Volatile private var instance: CabasDatabase? = null
        fun get(context: Context): CabasDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, CabasDatabase::class.java, "cabas.db")
                .addMigrations(MIGRATION_1_2).build().also { instance = it }
        }
    }
}
