package org.cabas.app.domain

import org.junit.Assert.*
import org.junit.Test

class CategoryNameTest {
    @Test fun normalizesSpacesCaseAndCanonicalUnicode() {
        assertEquals(CategoryName("Menjar fora", "menjar fora"), categoryName("  Menjar   fora  "))
        assertEquals(categoryName("Cafè")!!.key, categoryName("CAFE\u0300")!!.key)
        assertEquals("Menjar fora", categoryName("Menjar\u00a0fora")!!.display)
        assertEquals(40, categoryName("x".repeat(40))!!.display.length)
    }
    @Test fun rejectsEmptyControlInvisibleAndExcessivelyLongNames() {
        listOf("", "  ", "\u00a0", "a\nb", "a\tb", "a\u200bb", "x".repeat(41)).forEach {
            assertNull(it, categoryName(it))
        }
    }
}
