package com.cohors.app.domain.model

import androidx.compose.runtime.Immutable

/**
 * Clean domain models consumed by ViewModel/UI layers.
 * All models are annotated @Immutable to help the Compose compiler
 * skip recomposition when the same instance is passed again.
 */

@Immutable
data class League(
    val id: Int,
    val name: String,
    val type: String,
    val logoUrl: String?,
    val countryName: String?,
    val countryFlagUrl: String?,
    val currentSeasonYear: Int?
)

@Immutable
data class Team(
    val id: Int,
    val name: String,
    val code: String?,
    val country: String?,
    val founded: Int?,
    val logoUrl: String?,
    val venueName: String?,
    val venueCity: String?,
    val venueImageUrl: String?
)

@Immutable
enum class PlayerPosition {
    GOALKEEPER, DEFENDER, MIDFIELDER, ATTACKER, UNKNOWN
}

@Immutable
data class SquadPlayer(
    val id: Int,
    val name: String,
    val age: Int?,
    val shirtNumber: Int?,
    val position: PlayerPosition,
    val photoUrl: String?
)

@Immutable
data class Fixture(
    val id: Int,
    val dateIso: String?,
    val timestamp: Long?,
    val statusShort: String?,
    val statusLong: String?,
    val homeTeamId: Int?,
    val homeTeamName: String?,
    val homeTeamLogoUrl: String?,
    val awayTeamId: Int?,
    val awayTeamName: String?,
    val awayTeamLogoUrl: String?,
    val leagueName: String?,
    val leagueSeason: Int?,
    val round: String?
) {
    val isUpcoming: Boolean
        get() = statusShort == "NS" || statusShort == "TBD"
}

@Immutable
data class LineupPlayer(
    val id: Int,
    val name: String,
    val shirtNumber: Int?,
    val positionCode: String?,
    val gridPosition: String?
)

@Immutable
data class TeamLineup(
    val teamId: Int?,
    val teamName: String?,
    val teamLogoUrl: String?,
    val formation: String?,
    val startingXI: List<LineupPlayer>,
    val substitutes: List<LineupPlayer>
)

@Immutable
data class Injury(
    val playerId: Int?,
    val playerName: String,
    val playerPhotoUrl: String?,
    val teamName: String?,
    val leagueName: String?,
    val type: String?,
    val reason: String?
)
