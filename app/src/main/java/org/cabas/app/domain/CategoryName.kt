package org.cabas.app.domain

import java.text.Normalizer
import java.util.Locale

data class CategoryName(val display: String, val key: String)
fun categoryName(input: String): CategoryName? {
    val normalized = Normalizer.normalize(input, Normalizer.Form.NFC)
    // Reject control and invisible formatting characters before collapsing spaces.
    if (normalized.any { it.isISOControl() || Character.getType(it) == Character.FORMAT.toInt() }) return null
    val display = normalized.replace(Regex("[\\p{Z}\\s]+"), " ").trim()
    if (display.isBlank() || display.length > 40) return null
    return CategoryName(display, display.lowercase(Locale.ROOT))
}

enum class ThemeMode { SYSTEM, LIGHT, DARK }
