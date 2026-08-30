package com.cohors.app.data.mapper

import kotlin.jvm.JvmName

import com.cohors.app.data.remote.model.FixtureDto
import com.cohors.app.data.remote.model.InjuryDto
import com.cohors.app.data.remote.model.LeagueDto
import com.cohors.app.data.remote.model.LineupDto
import com.cohors.app.data.remote.model.LineupPlayerDto
import com.cohors.app.data.remote.model.SquadPlayerDto
import com.cohors.app.data.remote.model.TeamDto
import com.cohors.app.domain.model.Fixture
import com.cohors.app.domain.model.Injury
import com.cohors.app.domain.model.League
import com.cohors.app.domain.model.LineupPlayer
import com.cohors.app.domain.model.PlayerPosition
import com.cohors.app.domain.model.SquadPlayer
import com.cohors.app.domain.model.Team
import com.cohors.app.domain.model.TeamLineup

/**
 * Mapper extension functions: DTO (data layer, API shape) -> Domain model
 * (clean, UI-safe shape). Every mapper is defensive against nulls —
 * missing/unexpected API fields never crash the app, they degrade gracefully.
 */

fun LeagueDto.toDomain(): League = League(
    id = league?.id ?: -1,
    name = league?.name ?: "Bilinmeyen Lig",
    type = league?.type ?: "league",
    logoUrl = league?.logo,
    countryName = country?.name,
    countryFlagUrl = country?.flag,
    currentSeasonYear = seasons?.firstOrNull { it.current == true }?.year
        ?: seasons?.lastOrNull()?.year
)

fun TeamDto.toDomain(): Team = Team(
    id = team?.id ?: -1,
    name = team?.name ?: "Bilinmeyen Takım",
    code = team?.code,
    country = team?.country,
    founded = team?.founded,
    logoUrl = team?.logo,
    venueName = venue?.name,
    venueCity = venue?.city,
    venueImageUrl = venue?.image
)

private fun mapPosition(raw: String?): PlayerPosition = when (raw?.trim()?.lowercase()) {
    "goalkeeper" -> PlayerPosition.GOALKEEPER
    "defender" -> PlayerPosition.DEFENDER
    "midfielder" -> PlayerPosition.MIDFIELDER
    "attacker" -> PlayerPosition.ATTACKER
    else -> PlayerPosition.UNKNOWN
}

fun SquadPlayerDto.toDomain(): SquadPlayer = SquadPlayer(
    id = id ?: -1,
    name = name ?: "Bilinmeyen Oyuncu",
    age = age,
    shirtNumber = number,
    position = mapPosition(position),
    photoUrl = photo
)

fun FixtureDto.toDomain(): Fixture = Fixture(
    id = fixture?.id ?: -1,
    dateIso = fixture?.date,
    timestamp = fixture?.timestamp,
    statusShort = fixture?.status?.short,
    statusLong = fixture?.status?.long,
    homeTeamId = teams?.home?.id,
    homeTeamName = teams?.home?.name,
    homeTeamLogoUrl = teams?.home?.logo,
    awayTeamId = teams?.away?.id,
    awayTeamName = teams?.away?.name,
    awayTeamLogoUrl = teams?.away?.logo,
    leagueName = league?.name,
    leagueSeason = league?.season,
    round = league?.round
)

fun LineupPlayerDto.toDomain(): LineupPlayer = LineupPlayer(
    id = id ?: -1,
    name = name ?: "?",
    shirtNumber = number,
    positionCode = pos,
    gridPosition = grid
)

fun LineupDto.toDomain(): TeamLineup = TeamLineup(
    teamId = team?.id,
    teamName = team?.name,
    teamLogoUrl = team?.logo,
    formation = formation,
    startingXI = startXI?.map { it.toDomain() } ?: emptyList(),
    substitutes = substitutes?.map { it.toDomain() } ?: emptyList()
)

fun InjuryDto.toDomain(): Injury = Injury(
    playerId = player?.id,
    playerName = player?.name ?: "Bilinmeyen Oyuncu",
    playerPhotoUrl = player?.photo,
    teamName = team?.name,
    leagueName = league?.name,
    type = type,
    reason = reason
)

// List mapper convenience helpers

@JvmName("leagueDtoListToDomain")
fun List<LeagueDto>.toDomainList(): List<League> = map { it.toDomain() }
@JvmName("teamDtoListToDomain")
fun List<TeamDto>.toDomainList(): List<Team> = map { it.toDomain() }
@JvmName("squadPlayerDtoListToDomain")
fun List<SquadPlayerDto>.toDomainList(): List<SquadPlayer> = map { it.toDomain() }
@JvmName("fixtureDtoListToDomain")
fun List<FixtureDto>.toDomainList(): List<Fixture> = map { it.toDomain() }
@JvmName("lineupDtoListToDomain")
fun List<LineupDto>.toDomainList(): List<TeamLineup> = map { it.toDomain() }
@JvmName("injuryDtoListToDomain")
fun List<InjuryDto>.toDomainList(): List<Injury> = map { it.toDomain() }
