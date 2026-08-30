package com.cohors.app.ui.components

import com.cohors.app.domain.model.LineupPlayer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TacticalPitchPlacementTest {

    private fun player(id: Int, grid: String? = null) =
        LineupPlayer(id = id, name = "Player $id", shirtNumber = id, positionCode = null, gridPosition = grid)

    @Test
    fun `empty players list returns empty pins`() {
        val pins = computePitchPositions("4-3-3", emptyList())
        assertTrue(pins.isEmpty())
    }

    @Test
    fun `formation 4-3-3 places 11 players in 4 lines with correct y-fractions`() {
        val players = (1..11).map { player(it) }
        val pins = computePitchPositions("4-3-3", players)

        assertEquals(11, pins.size)

        // 4 lines: GK (y=1.0), DEF (y~0.667), MID (y~0.333), ATT (y=0.0)
        assertEquals(1.0f, pins[0].yFraction, 0.01f) // GK at bottom
        assertEquals(0.0f, pins[10].yFraction, 0.01f) // last attacker at top
    }

    @Test
    fun `formation 4-2-3-1 places players in 5 lines`() {
        val players = (1..11).map { player(it) }
        val pins = computePitchPositions("4-2-3-1", players)

        assertEquals(11, pins.size)
        // 5 lines: GK, DEF(4), MID(2), ATT-MID(3), ATT(1)
        // y fractions: 1.0, 0.75, 0.5, 0.25, 0.0
        assertEquals(1.0f, pins[0].yFraction, 0.01f)   // GK
        assertEquals(0.0f, pins[10].yFraction, 0.01f)  // lone striker
    }

    @Test
    fun `unknown formation defaults to 4-3-3`() {
        val players = (1..11).map { player(it) }
        val pins = computePitchPositions(null, players)

        assertEquals(11, pins.size)
    }

    @Test
    fun `grid coordinates are used when majority of players have them`() {
        val players = listOf(
            player(1, "1:1"),   // GK
            player(2, "2:1"),   // DEF
            player(3, "2:2"),
            player(4, "2:3"),
            player(5, "2:4"),
            player(6, "3:1"),   // MID
            player(7, "3:2"),
            player(8, "3:3"),
            player(9, "4:1"),   // ATT
            player(10, "4:2"),
            player(11, "4:3")
        )
        val pins = computePitchPositions("4-3-3", players)

        // GK (row 1) should be at the bottom (y near 1.0)
        assertEquals(1.0f, pins[0].yFraction, 0.01f)
        // Attackers (row 4) should be at the top (y near 0.0)
        assertEquals(0.0f, pins[8].yFraction, 0.01f)
    }

    @Test
    fun `grid coordinates are ignored when fewer than 70 percent have them`() {
        val players = listOf(
            player(1, "1:1"),
            player(2, null),  // no grid
            player(3, null),
            player(4, null),
            player(5, null),
            player(6, null),
            player(7, null),
            player(8, null),
            player(9, null),
            player(10, null),
            player(11, null)
        )
        val pins = computePitchPositions("4-3-3", players)

        // Should fall back to formation-based layout
        assertEquals(11, pins.size)
        assertEquals(1.0f, pins[0].yFraction, 0.01f) // GK at bottom
    }

    @Test
    fun `extra players beyond formation capacity are placed at center`() {
        val players = (1..14).map { player(it) }
        val pins = computePitchPositions("4-3-3", players)

        // 11 in formation + 3 extras at center
        assertEquals(14, pins.size)
        // Extras (players 12, 13, 14) should be at x=0.5, y=0.5
        assertEquals(0.5f, pins[11].xFraction, 0.01f)
        assertEquals(0.5f, pins[11].yFraction, 0.01f)
    }

    @Test
    fun `players within a formation line are horizontally distributed`() {
        val players = (1..11).map { player(it) }
        val pins = computePitchPositions("4-4-2", players)

        // DEF line has 4 players (indices 1..4), x should be 0.2, 0.4, 0.6, 0.8
        assertEquals(0.2f, pins[1].xFraction, 0.01f)
        assertEquals(0.4f, pins[2].xFraction, 0.01f)
        assertEquals(0.6f, pins[3].xFraction, 0.01f)
        assertEquals(0.8f, pins[4].xFraction, 0.01f)
    }
}
