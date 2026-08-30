package com.cohors.app.domain.model

/**
 * Clean domain models consumed by ViewModel/UI layers.
 * These are fully decoupled from the API-Football DTO shape —
 * no nullable-everything, no raw JSON field names.
 */

data class League(
    val id: Int,
    val name: String,
    val type: String,
    val logoUrl: String?,
    val countryName: String?,
    val countryFlagUrl: String?,
    val currentSeasonYear: Int?
)

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

enum class PlayerPosition {
    GOALKEEPER, DEFENDER, MIDFIELDER, ATTACKER, UNKNOWN
}

data class SquadPlayer(
    val id: Int,
    val name: String,
    val age: Int?,
    val shirtNumber: Int?,
    val position: PlayerPosition,
    val photoUrl: String?
)

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

data class LineupPlayer(
    val id: Int,
    val name: String,
    val shirtNumber: Int?,
    val positionCode: String?, // G, D, M, F
    val gridPosition: String?  // e.g. "3:4:3" grid coordinate
)

data class TeamLineup(
    val teamId: Int?,
    val teamName: String?,
    val teamLogoUrl: String?,
    val formation: String?,
    val startingXI: List<LineupPlayer>,
    val substitutes: List<LineupPlayer>
)

data class Injury(
    val playerId: Int?,
    val playerName: String,
    val playerPhotoUrl: String?,
    val teamName: String?,
    val leagueName: String?,
    val type: String?,
    val reason: String?
)
