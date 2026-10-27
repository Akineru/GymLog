package com.example.gymlog

import com.example.gymlog.data.formatHexColor
import com.example.gymlog.data.parseHexColor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HexColorTest {
    @Test
    fun acceptsSixDigitsWithOrWithoutHash() {
        assertEquals(0xFFFF8A80.toInt(), parseHexColor("#FF8A80"))
        assertEquals(0xFFFF8A80.toInt(), parseHexColor("ff8a80"))
    }

    @Test
    fun rejectsMalformedInput() {
        assertNull(parseHexColor(""))
        assertNull(parseHexColor("#FFF"))
        assertNull(parseHexColor("#GG0000"))
        assertNull(parseHexColor("#FF8A80FF"))
        assertNull(parseHexColor("red"))
    }

    @Test
    fun formatsOpaqueRgb() {
        assertEquals("#FF8A80", formatHexColor(0xFFFF8A80.toInt()))
    }
}
