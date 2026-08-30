package com.cohors.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.cohors.app.domain.model.Injury
import com.cohors.app.domain.model.SquadPlayer

/** Small circular badge showing a player's shirt number (or "-" if unknown). */
@Composable
fun ShirtNumberBadge(number: Int?, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = number?.toString() ?: "-",
            color = MaterialTheme.colorScheme.onPrimary,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
    }
}

enum class PlayerStatus { INJURED, SUSPENDED }

/** Small status pill shown next to a player's name when injured or suspended. */
@Composable
fun StatusBadge(status: PlayerStatus, modifier: Modifier = Modifier) {
    val (color, icon, label) = when (status) {
        PlayerStatus.INJURED -> Triple(MaterialTheme.colorScheme.error, Icons.Filled.LocalHospital, "Sakat")
        PlayerStatus.SUSPENDED -> Triple(Color(0xFFB8860B), Icons.Filled.Warning, "Cezalı")
    }
    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.15f),
        contentColor = color,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(14.dp))
            Text(text = label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium)
        }
    }
}

/** A single squad member row: photo, shirt number, name/age, and optional status badge. */
@Composable
fun PlayerRow(
    player: SquadPlayer,
    status: PlayerStatus? = null,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.background(Color.Transparent) else Modifier)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ShirtNumberBadge(number = player.shirtNumber)
        PlayerAvatar(url = player.photoUrl, size = 44.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = player.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = player.age?.let { "Yaş: $it" } ?: "Yaş bilinmiyor",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        status?.let { StatusBadge(status = it) }
    }
}

/** A single injury/suspension list row. */
@Composable
fun InjuryRow(injury: Injury, status: PlayerStatus, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        PlayerAvatar(url = injury.playerPhotoUrl, size = 44.dp)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = injury.playerName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val detail = listOfNotNull(injury.type, injury.reason).distinct().joinToString(" • ")
            if (detail.isNotBlank()) {
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        StatusBadge(status = status)
    }
}

@Composable
private fun PlayerAvatar(url: String?, size: androidx.compose.ui.unit.Dp) {
    if (!url.isNullOrBlank()) {
        AsyncImage(
            model = url,
            contentDescription = null,
            modifier = Modifier.size(size).clip(CircleShape),
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
