package com.cohors.app.data.remote.model

import com.squareup.moshi.JsonClass

/**
 * Generic API-Football response wrapper.
 * API-Football wraps all responses in: { "get": "...", "results": N, "response": [...] }
 */
@JsonClass(generateAdapter = true)
data class ApiResponse<T>(
    val get: String? = null,
    val results: Int? = null,
    val response: T? = null
)

// ============================================================
// League
// ============================================================

@JsonClass(generateAdapter = true)
data class LeagueDto(
    val league: LeagueInfo? = null,
    val country: CountryInfo? = null,
    val seasons: List<SeasonInfo>? = null
)

@JsonClass(generateAdapter = true)
data class LeagueInfo(
    val id: Int? = null,
    val name: String? = null,
    val type: String? = null,
    val logo: String? = null
)

@JsonClass(generateAdapter = true)
data class CountryInfo(
    val name: String? = null,
    val code: String? = null,
    val flag: String? = null
)

@JsonClass(generateAdapter = true)
data class SeasonInfo(
    val year: Int? = null,
    val current: Boolean? = null
)

// ============================================================
// Team
// ============================================================

@JsonClass(generateAdapter = true)
data class TeamDto(
    val team: TeamInfo? = null,
    val venue: VenueInfo? = null
)

@JsonClass(generateAdapter = true)
data class TeamInfo(
    val id: Int? = null,
    val name: String? = null,
    val code: String? = null,
    val country: String? = null,
    val founded: Int? = null,
    val logo: String? = null
)

@JsonClass(generateAdapter = true)
data class VenueInfo(
    val id: Int? = null,
    val name: String? = null,
    val city: String? = null,
    val image: String? = null
)

// ============================================================
// Squad (players grouped by team)
// ============================================================

@JsonClass(generateAdapter = true)
data class SquadDto(
    val team: TeamInfo? = null,
    val players: List<SquadPlayerDto>? = null
)

@JsonClass(generateAdapter = true)
data class SquadPlayerDto(
    val id: Int? = null,
    val name: String? = null,
    val age: Int? = null,
    val number: Int? = null,
    val position: String? = null,  // "Goalkeeper", "Defender", "Midfielder", "Attacker"
    val photo: String? = null
)

// ============================================================
// Fixture
// ============================================================

@JsonClass(generateAdapter = true)
data class FixtureDto(
    val fixture: FixtureInfo? = null,
    val teams: FixtureTeams? = null,
    val league: FixtureLeague? = null
)

@JsonClass(generateAdapter = true)
data class FixtureInfo(
    val id: Int? = null,
    val date: String? = null,
    val timestamp: Long? = null,
    val status: FixtureStatus? = null
)

@JsonClass(generateAdapter = true)
data class FixtureStatus(
    val short: String? = null,
    val long: String? = null
)

@JsonClass(generateAdapter = true)
data class FixtureTeams(
    val home: TeamInfo? = null,
    val away: TeamInfo? = null
)

@JsonClass(generateAdapter = true)
data class FixtureLeague(
    val id: Int? = null,
    val name: String? = null,
    val season: Int? = null,
    val round: String? = null
)

// ============================================================
// Lineup
// ============================================================

@JsonClass(generateAdapter = true)
data class LineupDto(
    val team: TeamInfo? = null,
    val formation: String? = null,
    val startXI: List<LineupPlayerDto>? = null,
    val substitutes: List<LineupPlayerDto>? = null
)

@JsonClass(generateAdapter = true)
data class LineupPlayerDto(
    val id: Int? = null,
    val name: String? = null,
    val number: Int? = null,
    val pos: String? = null,       // position code
    val grid: String? = null       // e.g. "3:4:3" layout position
)

// ============================================================
// Injuries
// ============================================================

@JsonClass(generateAdapter = true)
data class InjuryDto(
    val player: InjuryPlayer? = null,
    val team: TeamInfo? = null,
    val league: FixtureLeague? = null,
    val fixture: FixtureInfo? = null,
    val type: String? = null,
    val reason: String? = null
)

@JsonClass(generateAdapter = true)
data class InjuryPlayer(
    val id: Int? = null,
    val name: String? = null,
    val photo: String? = null
)
