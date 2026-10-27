package com.example.gymlog.data

private val HexColorPattern = Regex("^#?[0-9a-fA-F]{6}$")

/**
 * Accepts `#RRGGBB` and `RRGGBB`; returns opaque ARGB, or null when invalid.
 * Three-digit shorthands and named colors are intentionally not accepted.
 */
fun parseHexColor(input: String): Int? {
    val raw = input.trim()
    if (!HexColorPattern.matches(raw)) return null
    val rgb = raw.removePrefix("#").toLong(16).toInt()
    return 0xFF000000.toInt() or rgb
}

fun formatHexColor(argb: Int): String = String.format("#%06X", argb and 0xFFFFFF)
