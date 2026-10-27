package org.cabas.app.data

import android.content.Context
import androidx.room.*
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

@Database(entities = [Movement::class], version = 1, exportSchema = true)
abstract class CabasDatabase : RoomDatabase() {
    abstract fun movements(): MovementDao
    companion object {
        @Volatile private var instance: CabasDatabase? = null
        fun get(context: Context): CabasDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, CabasDatabase::class.java, "cabas.db")
                .build().also { instance = it }
        }
    }
}
