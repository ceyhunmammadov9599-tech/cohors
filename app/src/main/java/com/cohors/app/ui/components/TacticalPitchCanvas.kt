package com.cohors.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.Canvas
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cohors.app.domain.model.LineupPlayer
import com.cohors.app.domain.model.TeamLineup
import kotlin.math.min

/** A player's computed position on the pitch, as fractions of width/height (0..1). */
data class PitchPlayerPin(
    val player: LineupPlayer,
    val xFraction: Float,
    val yFraction: Float
)

private data class GridCoord(val row: Int, val col: Int)

private fun parseGrid(raw: String?): GridCoord? {
    val parts = raw?.split(":") ?: return null
    if (parts.size != 2) return null
    val row = parts[0].trim().toIntOrNull() ?: return null
    val col = parts[1].trim().toIntOrNull() ?: return null
    return GridCoord(row, col)
}

/**
 * Dynamic placement algorithm: prefers the API-provided "grid" coordinate
 * per player (row:col, as returned by API-Football) when most players
 * have it; otherwise falls back to auto-layout derived from the formation
 * string (e.g. "4-3-3", "4-2-3-1", "3-5-2").
 */
fun computePitchPositions(formation: String?, players: List<LineupPlayer>): List<PitchPlayerPin> {
    if (players.isEmpty()) return emptyList()

    val grids = players.map { it to parseGrid(it.gridPosition) }
    val validCount = grids.count { it.second != null }

    return if (validCount >= (players.size * 0.7f).toInt().coerceAtLeast(1)) {
        layoutFromGrid(grids)
    } else {
        layoutFromFormation(formation, players)
    }
}

private fun layoutFromGrid(grids: List<Pair<LineupPlayer, GridCoord?>>): List<PitchPlayerPin> {
    val validRows = grids.mapNotNull { it.second?.row }.distinct().sorted()
    val maxRowIndex = (validRows.size - 1).coerceAtLeast(1)

    return grids.map { (player, grid) ->
        if (grid != null) {
            val rowRank = validRows.indexOf(grid.row).coerceAtLeast(0)
            // Row 1 (goalkeeper, in API-Football convention) sits near the bottom (own goal);
            // higher rows advance up the pitch toward the opponent's goal.
            val yFraction = 1f - (rowRank.toFloat() / maxRowIndex)
            val colsInRow = grids.mapNotNull { g -> if (g.second?.row == grid.row) g.second?.col else null }
                .distinct().sorted()
            val colIndex = colsInRow.indexOf(grid.col).coerceAtLeast(0)
            val n = colsInRow.size.coerceAtLeast(1)
            val xFraction = (colIndex + 1f) / (n + 1f)
            PitchPlayerPin(player, xFraction, yFraction)
        } else {
            PitchPlayerPin(player, 0.5f, 0.5f)
        }
    }
}

private fun layoutFromFormation(formation: String?, players: List<LineupPlayer>): List<PitchPlayerPin> {
    val outfieldLines = formation?.split("-")?.mapNotNull { it.trim().toIntOrNull() } ?: emptyList()
    val lines = listOf(1) + outfieldLines.ifEmpty { listOf(4, 3, 3) } // default 4-3-3 if formation unknown
    val lineCount = lines.size

    val pins = mutableListOf<PitchPlayerPin>()
    var playerIndex = 0

    lines.forEachIndexed { lineIdx, count ->
        val yFraction = if (lineCount <= 1) 0.5f else 1f - (lineIdx.toFloat() / (lineCount - 1))
        for (posInLine in 0 until count) {
            if (playerIndex >= players.size) return@forEachIndexed
            val xFraction = (posInLine + 1f) / (count + 1f)
            pins.add(PitchPlayerPin(players[playerIndex], xFraction, yFraction))
            playerIndex++
        }
    }
    while (playerIndex < players.size) {
        pins.add(PitchPlayerPin(players[playerIndex], 0.5f, 0.5f))
        playerIndex++
    }
    return pins
}

private val PitchGreenLight = Color(0xFF2E8B4F)
private val PitchGreenDark = Color(0xFF267A44)
private val LineWhite = Color(0xFFF5F5F5)

/**
 * Draws a realistic vertical football pitch (touchlines, penalty areas,
 * six-yard boxes, penalty arcs, center circle, corner arcs) using Compose
 * Canvas, and overlays clickable player pins positioned via
 * [computePitchPositions].
 */
@Composable
fun TacticalPitchCanvas(
    lineup: TeamLineup,
    onPlayerClick: (LineupPlayer) -> Unit,
    modifier: Modifier = Modifier
) {
    val pins = remember(lineup) { computePitchPositions(lineup.formation, lineup.startingXI) }

    BoxWithConstraints(
        modifier = modifier
            .aspectRatio(0.68f)
            .clip(RoundedCornerShape(12.dp))
    ) {
        val pitchWidth = maxWidth
        val pitchHeight = maxHeight

        Canvas(modifier = Modifier.fillMaxSize()) {
            drawPitch(size)
        }

        pins.forEach { pin ->
            val centerX = pitchWidth * pin.xFraction
            val centerY = pitchHeight * pin.yFraction
            PlayerPin(
                pin = pin,
                modifier = Modifier
                    .offset(x = centerX - PIN_WIDTH / 2, y = centerY - PIN_HEIGHT / 2)
                    .clickable { onPlayerClick(pin.player) }
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPitch(canvasSize: Size) {
    val margin = min(canvasSize.width, canvasSize.height) * 0.03f
    val w = canvasSize.width
    val h = canvasSize.height
    val strokeWidth = min(w, h) * 0.006f

    // Alternating mowed-grass stripes for a realistic pitch background.
    val stripeCount = 8
    val stripeHeight = h / stripeCount
    for (i in 0 until stripeCount) {
        drawRect(
            color = if (i % 2 == 0) PitchGreenLight else PitchGreenDark,
            topLeft = Offset(0f, i * stripeHeight),
            size = Size(w, stripeHeight)
        )
    }

    val fieldLeft = margin
    val fieldTop = margin
    val fieldRight = w - margin
    val fieldBottom = h - margin
    val fieldWidth = fieldRight - fieldLeft
    val fieldHeight = fieldBottom - fieldTop

    val lineStroke = Stroke(width = strokeWidth)

    // Outer boundary (touchlines + goal lines).
    drawRect(
        color = LineWhite,
        topLeft = Offset(fieldLeft, fieldTop),
        size = Size(fieldWidth, fieldHeight),
        style = lineStroke
    )

    // Halfway line.
    drawLine(
        color = LineWhite,
        start = Offset(fieldLeft, fieldTop + fieldHeight / 2f),
        end = Offset(fieldRight, fieldTop + fieldHeight / 2f),
        strokeWidth = strokeWidth
    )

    // Center circle + center spot.
    val centerRadius = fieldWidth * 0.16f
    drawCircle(
        color = LineWhite,
        radius = centerRadius,
        center = Offset(fieldLeft + fieldWidth / 2f, fieldTop + fieldHeight / 2f),
        style = lineStroke
    )
    drawCircle(
        color = LineWhite,
        radius = strokeWidth * 1.4f,
        center = Offset(fieldLeft + fieldWidth / 2f, fieldTop + fieldHeight / 2f)
    )

    val penaltyAreaWidth = fieldWidth * 0.62f
    val penaltyAreaDepth = fieldHeight * 0.16f
    val goalAreaWidth = fieldWidth * 0.30f
    val goalAreaDepth = fieldHeight * 0.06f
    val penaltySpotOffset = fieldHeight * 0.12f
    val arcRadius = fieldWidth * 0.16f

    // --- Top penalty area (opponent goal) ---
    val topPenaltyLeft = fieldLeft + (fieldWidth - penaltyAreaWidth) / 2f
    drawRect(
        color = LineWhite,
        topLeft = Offset(topPenaltyLeft, fieldTop),
        size = Size(penaltyAreaWidth, penaltyAreaDepth),
        style = lineStroke
    )
    val topGoalAreaLeft = fieldLeft + (fieldWidth - goalAreaWidth) / 2f
    drawRect(
        color = LineWhite,
        topLeft = Offset(topGoalAreaLeft, fieldTop),
        size = Size(goalAreaWidth, goalAreaDepth),
        style = lineStroke
    )
    val topPenaltySpot = Offset(fieldLeft + fieldWidth / 2f, fieldTop + penaltySpotOffset)
    drawCircle(color = LineWhite, radius = strokeWidth * 1.2f, center = topPenaltySpot)
    drawArc(
        color = LineWhite,
        startAngle = 25f,
        sweepAngle = 130f,
        useCenter = false,
        topLeft = Offset(topPenaltySpot.x - arcRadius, topPenaltySpot.y - arcRadius),
        size = Size(arcRadius * 2, arcRadius * 2),
        style = lineStroke
    )

    // --- Bottom penalty area (own goal) ---
    val bottomPenaltyTop = fieldBottom - penaltyAreaDepth
    drawRect(
        color = LineWhite,
        topLeft = Offset(topPenaltyLeft, bottomPenaltyTop),
        size = Size(penaltyAreaWidth, penaltyAreaDepth),
        style = lineStroke
    )
    val bottomGoalAreaTop = fieldBottom - goalAreaDepth
    drawRect(
        color = LineWhite,
        topLeft = Offset(topGoalAreaLeft, bottomGoalAreaTop),
        size = Size(goalAreaWidth, goalAreaDepth),
        style = lineStroke
    )
    val bottomPenaltySpot = Offset(fieldLeft + fieldWidth / 2f, fieldBottom - penaltySpotOffset)
    drawCircle(color = LineWhite, radius = strokeWidth * 1.2f, center = bottomPenaltySpot)
    drawArc(
        color = LineWhite,
        startAngle = 205f,
        sweepAngle = 130f,
        useCenter = false,
        topLeft = Offset(bottomPenaltySpot.x - arcRadius, bottomPenaltySpot.y - arcRadius),
        size = Size(arcRadius * 2, arcRadius * 2),
        style = lineStroke
    )

    // Corner arcs.
    val cornerRadius = fieldWidth * 0.035f
    val corners = listOf(
        Triple(Offset(fieldLeft, fieldTop), 0f, 90f),
        Triple(Offset(fieldRight, fieldTop), 90f, 90f),
        Triple(Offset(fieldLeft, fieldBottom), 270f, 90f),
        Triple(Offset(fieldRight, fieldBottom), 180f, 90f)
    )
    corners.forEach { (corner, startAngle, sweep) ->
        drawArc(
            color = LineWhite,
            startAngle = startAngle,
            sweepAngle = sweep,
            useCenter = false,
            topLeft = Offset(corner.x - cornerRadius, corner.y - cornerRadius),
            size = Size(cornerRadius * 2, cornerRadius * 2),
            style = lineStroke
        )
    }
}

private val PIN_WIDTH = 56.dp
private val PIN_HEIGHT = 64.dp

@Composable
private fun PlayerPin(pin: PitchPlayerPin, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.size(width = PIN_WIDTH, height = PIN_HEIGHT),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(Color.White)
                .padding(2.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = pin.player.shirtNumber?.toString() ?: "-",
                color = MaterialTheme.colorScheme.onPrimary,
                style = MaterialTheme.typography.labelLarge
            )
        }
        Text(
            text = pin.player.name.substringBefore(" ").ifBlank { pin.player.name },
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .padding(top = 2.dp)
                .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(4.dp))
                .padding(horizontal = 4.dp, vertical = 1.dp)
        )
    }
}
