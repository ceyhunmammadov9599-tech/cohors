package com.cohors.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cohors.app.domain.model.Fixture
import com.cohors.app.domain.model.LineupPlayer
import com.cohors.app.domain.model.PlayerPosition

/** Quick-access chips for the most popular leagues, on top of the search field. */
@Composable
fun PopularLeagueChipsRow(onChipClick: (String) -> Unit, modifier: Modifier = Modifier) {
    val popular = listOf("Premier League", "La Liga", "Serie A", "Bundesliga", "Ligue 1", "Süper Lig")
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp)
    ) {
        items(popular) { label ->
            androidx.compose.material3.AssistChip(
                onClick = { onChipClick(label) },
                label = { Text(label) }
            )
        }
    }
}

/** Section header for a squad position group ("Kaleci", "Defans", ...). */
@Composable
fun PositionHeader(position: PlayerPosition, modifier: Modifier = Modifier) {
    val (label, icon) = when (position) {
        PlayerPosition.GOALKEEPER -> "Kaleci" to Icons.Filled.Shield
        PlayerPosition.DEFENDER -> "Defans" to Icons.Filled.Shield
        PlayerPosition.MIDFIELDER -> "Orta Saha" to Icons.Filled.DirectionsRun
        PlayerPosition.ATTACKER -> "Forvet" to Icons.Filled.SportsSoccer
        PlayerPosition.UNKNOWN -> "Diğer" to Icons.Filled.DirectionsRun
    }
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Header card summarizing the resolved fixture (teams, date/status, competition). */
@Composable
fun FixtureHeader(fixture: Fixture, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth().padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            if (!fixture.leagueName.isNullOrBlank()) {
                Text(
                    text = listOfNotNull(fixture.leagueName, fixture.round).joinToString(" • "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TeamMini(name = fixture.homeTeamName, logoUrl = fixture.homeTeamLogoUrl)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = fixture.statusLong ?: "Planlandı",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TeamMini(name = fixture.awayTeamName, logoUrl = fixture.awayTeamLogoUrl)
            }
        }
    }
}

@Composable
private fun TeamMini(name: String?, logoUrl: String?, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        if (!logoUrl.isNullOrBlank()) {
            coil.compose.AsyncImage(
                model = logoUrl,
                contentDescription = null,
                modifier = Modifier.size(40.dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
            )
        }
        Text(
            text = name ?: "-",
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

/** Horizontal strip of substitute players below the pitch. */
@Composable
fun SubstitutesRow(substitutes: List<LineupPlayer>, modifier: Modifier = Modifier) {
    if (substitutes.isEmpty()) return
    Column(modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(Icons.Filled.SwapHoriz, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = "Yedekler",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
        }
        LazyRow(
            modifier = Modifier.padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(substitutes) { sub ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ShirtNumberBadge(number = sub.shirtNumber, modifier = Modifier.size(24.dp))
                        Text(text = sub.name, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

/** Dialog shown when a player pin on the tactical pitch is tapped. */
@Composable
fun PlayerInfoDialog(player: LineupPlayer, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(player.name) },
        text = {
            Column {
                Text("Forma No: ${player.shirtNumber ?: "-"}")
                Text("Mevki: ${player.positionCode ?: "-"}")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Kapat") }
        }
    )
}

/** Generic small section header used for list groupings other than positions. */
@Composable
fun ListSectionHeader(text: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
}
