package org.cabas.app.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.cabas.app.domain.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

@RunWith(AndroidJUnit4::class)
class MovementDaoTest {
    @Test fun persistsAcrossReopenFiltersMonthAndDeletes() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "movement-test.db"
        context.deleteDatabase(name)
        val start = LocalDate.of(2026, 10, 1).toEpochDay()
        val end = LocalDate.of(2026, 11, 1).toEpochDay()
        fun open() = CabasDatabase.builder(context, name).build()
        var db = open()
        try {
            db.movements().insert(Movement(cents = 1234, kind = Kind.EXPENSE, category = Category.NEEDS, epochDay = start, note = "Mercat"))
            db.movements().insert(Movement(cents = 5000, kind = Kind.INCOME, category = null, epochDay = end, note = "Novembre"))
            db.close()
            db = open()
            val october = db.movements().observeMonth(start, end).first()
            assertEquals(1, october.size)
            assertEquals(1234L, october.single().cents)
            assertEquals(Category.NEEDS, october.single().category)
            assertEquals("Mercat", october.single().note)
            db.movements().delete(october.single().id)
            assertTrue(db.movements().observeMonth(start, end).first().isEmpty())
            assertEquals(1, db.movements().observeMonth(end, end + 30).first().size)
        } finally { db.close(); context.deleteDatabase(name) }
    }
}
