package com.example.gymlog

import com.example.gymlog.ui.sessionSetGridCells
import org.junit.Assert.assertEquals
import org.junit.Test

class SessionSetGridTest {
    @Test
    fun oneSetStaysInTheFirstCell() {
        assertEquals(listOf("80 kg×8"), sessionSetGridCells(listOf("80 kg×8")))
    }

    @Test
    fun twoSetsKeepLeftToRightOrder() {
        val lines = listOf("30 kg×12", "35 kg×12")
        assertEquals(lines, sessionSetGridCells(lines))
    }

    @Test
    fun fiveSetsAreAllVisible() {
        val lines = (1..5).map { "$it kg×8" }
        assertEquals(lines, sessionSetGridCells(lines))
    }

    @Test
    fun exactlySixSetsShowTheRealSixthSet() {
        val lines = (1..6).map { "$it kg×8" }
        assertEquals(lines, sessionSetGridCells(lines))
    }

    @Test
    fun sevenSetsUseTheSixthCellForEllipsis() {
        val lines = (1..7).map { "$it kg×8" }
        assertEquals(lines.take(5) + "…", sessionSetGridCells(lines))
        assertEquals(7, lines.size)
    }

    @Test
    fun manySetsKeepOnlyFiveRealCellsAndEllipsis() {
        val lines = (1..12).map { "$it kg×8" }
        assertEquals(lines.take(5) + "…", sessionSetGridCells(lines))
        assertEquals(12, lines.size)
    }
}
