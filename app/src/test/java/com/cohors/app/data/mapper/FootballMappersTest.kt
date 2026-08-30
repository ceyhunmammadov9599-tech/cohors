package com.cohors.app.data.mapper

import com.cohors.app.data.remote.model.CountryInfo
import com.cohors.app.data.remote.model.FixtureDto
import com.cohors.app.data.remote.model.FixtureInfo
import com.cohors.app.data.remote.model.FixtureLeague
import com.cohors.app.data.remote.model.FixtureStatus
import com.cohors.app.data.remote.model.FixtureTeams
import com.cohors.app.data.remote.model.InjuryDto
import com.cohors.app.data.remote.model.InjuryPlayer
import com.cohors.app.data.remote.model.LeagueDto
import com.cohors.app.data.remote.model.LeagueInfo
import com.cohors.app.data.remote.model.LineupDto
import com.cohors.app.data.remote.model.LineupPlayerDto
import com.cohors.app.data.remote.model.SeasonInfo
import com.cohors.app.data.remote.model.SquadPlayerDto
import com.cohors.app.data.remote.model.TeamDto
import com.cohors.app.data.remote.model.TeamInfo
import com.cohors.app.domain.model.PlayerPosition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Boundary/null-case tests for every DTO -> Domain mapper. API-Football
 * fields are all nullable, so every mapper must degrade gracefully instead
 * of crashing when the API returns partial or unexpected data.
 */
class FootballMappersTest {

    // ---------------- League ----------------

    @Test
    fun `LeagueDto with all-null fields maps to safe fallback values`() {
        val domain = LeagueDto().toDomain()

        assertEquals(-1, domain.id)
        assertEquals("Bilinmeyen Lig", domain.name)
        assertEquals("league", domain.type)
        assertNull(domain.logoUrl)
        assertNull(domain.countryName)
        assertNull(domain.currentSeasonYear)
    }

    @Test
    fun `LeagueDto picks the season marked current over the last season`() {
        val dto = LeagueDto(
            league = LeagueInfo(id = 1, name = "Test League"),
            seasons = listOf(
                SeasonInfo(year = 2022, current = false),
                SeasonInfo(year = 2023, current = true),
                SeasonInfo(year = 2024, current = false)
            )
        )
        assertEquals(2023, dto.toDomain().currentSeasonYear)
    }

    @Test
    fun `LeagueDto falls back to last season when none is marked current`() {
        val dto = LeagueDto(
            league = LeagueInfo(id = 1, name = "Test League"),
            seasons = listOf(SeasonInfo(year = 2022, current = false), SeasonInfo(year = 2023, current = false))
        )
        assertEquals(2023, dto.toDomain().currentSeasonYear)
    }

    @Test
    fun `LeagueDto with empty seasons list has null current season`() {
        val dto = LeagueDto(league = LeagueInfo(id = 1, name = "Test"), seasons = emptyList())
        assertNull(dto.toDomain().currentSeasonYear)
    }

    // ---------------- Team ----------------

    @Test
    fun `TeamDto with all-null fields maps to safe fallback values`() {
        val domain = TeamDto().toDomain()
        assertEquals(-1, domain.id)
        assertEquals("Bilinmeyen Takım", domain.name)
        assertNull(domain.venueName)
    }

    @Test
    fun `TeamDto with populated fields maps 1-to-1`() {
        val dto = TeamDto(
            team = TeamInfo(id = 33, name = "Manchester United", code = "MUN", country = "England", founded = 1878, logo = "logo.png"),
            venue = com.cohors.app.data.remote.model.VenueInfo(id = 1, name = "Old Trafford", city = "Manchester", image = "venue.png")
        )
        val domain = dto.toDomain()
        assertEquals(33, domain.id)
        assertEquals("Manchester United", domain.name)
        assertEquals("Old Trafford", domain.venueName)
        assertEquals("Manchester", domain.venueCity)
    }

    // ---------------- Squad player / position mapping ----------------

    @Test
    fun `SquadPlayerDto maps known position strings case-insensitively`() {
        assertEquals(PlayerPosition.GOALKEEPER, SquadPlayerDto(position = "Goalkeeper").toDomain().position)
        assertEquals(PlayerPosition.DEFENDER, SquadPlayerDto(position = "defender").toDomain().position)
        assertEquals(PlayerPosition.MIDFIELDER, SquadPlayerDto(position = "MIDFIELDER").toDomain().position)
        assertEquals(PlayerPosition.ATTACKER, SquadPlayerDto(position = " Attacker ").toDomain().position)
    }

    @Test
    fun `SquadPlayerDto maps unknown or null position to UNKNOWN`() {
        assertEquals(PlayerPosition.UNKNOWN, SquadPlayerDto(position = "Winger").toDomain().position)
        assertEquals(PlayerPosition.UNKNOWN, SquadPlayerDto(position = null).toDomain().position)
    }

    @Test
    fun `SquadPlayerDto with null id and name falls back safely`() {
        val domain = SquadPlayerDto(id = null, name = null).toDomain()
        assertEquals(-1, domain.id)
        assertEquals("Bilinmeyen Oyuncu", domain.name)
    }

    // ---------------- Fixture ----------------

    @Test
    fun `FixtureDto with all-null nested objects maps to safe defaults`() {
        val domain = FixtureDto().toDomain()
        assertEquals(-1, domain.id)
        assertNull(domain.homeTeamName)
        assertNull(domain.statusShort)
        assertTrue(!domain.isUpcoming) // status null -> not "NS"/"TBD"
    }

    @Test
    fun `Fixture isUpcoming is true only for NS or TBD status codes`() {
        val ns = FixtureDto(fixture = FixtureInfo(status = FixtureStatus(short = "NS"))).toDomain()
        val tbd = FixtureDto(fixture = FixtureInfo(status = FixtureStatus(short = "TBD"))).toDomain()
        val live = FixtureDto(fixture = FixtureInfo(status = FixtureStatus(short = "1H"))).toDomain()

        assertTrue(ns.isUpcoming)
        assertTrue(tbd.isUpcoming)
        assertTrue(!live.isUpcoming)
    }

    @Test
    fun `FixtureDto maps team and league info correctly`() {
        val dto = FixtureDto(
            fixture = FixtureInfo(id = 100, date = "2026-09-01", status = FixtureStatus(short = "NS", long = "Not Started")),
            teams = FixtureTeams(
                home = TeamInfo(id = 1, name = "Home FC", logo = "home.png"),
                away = TeamInfo(id = 2, name = "Away FC", logo = "away.png")
            ),
            league = FixtureLeague(id = 39, name = "Premier League", season = 2026, round = "Round 3")
        )
        val domain = dto.toDomain()
        assertEquals(100, domain.id)
        assertEquals("Home FC", domain.homeTeamName)
        assertEquals("Away FC", domain.awayTeamName)
        assertEquals("Premier League", domain.leagueName)
        assertEquals("Round 3", domain.round)
    }

    // ---------------- Lineup ----------------

    @Test
    fun `LineupPlayerDto with null name falls back to a placeholder`() {
        val domain = LineupPlayerDto(id = 7, name = null, number = 9, pos = "F", grid = "1:1").toDomain()
        assertEquals("?", domain.name)
        assertEquals(9, domain.shirtNumber)
        assertEquals("1:1", domain.gridPosition)
    }

    @Test
    fun `LineupDto maps starting XI and substitutes, defaulting nulls to empty lists`() {
        val dtoWithNulls = LineupDto(formation = "4-3-3", startXI = null, substitutes = null)
        val domain = dtoWithNulls.toDomain()
        assertTrue(domain.startingXI.isEmpty())
        assertTrue(domain.substitutes.isEmpty())
        assertEquals("4-3-3", domain.formation)
    }

    @Test
    fun `LineupDto maps a populated starting XI correctly`() {
        val dto = LineupDto(
            team = TeamInfo(id = 33, name = "Man United"),
            formation = "4-2-3-1",
            startXI = listOf(LineupPlayerDto(id = 1, name = "Keeper", number = 1, pos = "G", grid = "1:1")),
            substitutes = listOf(LineupPlayerDto(id = 12, name = "Sub One", number = 12, pos = "M", grid = null))
        )
        val domain = dto.toDomain()
        assertEquals(1, domain.startingXI.size)
        assertEquals("Keeper", domain.startingXI.first().name)
        assertEquals(1, domain.substitutes.size)
    }

    // ---------------- Injuries ----------------

    @Test
    fun `InjuryDto with null player falls back to a placeholder name`() {
        val domain = InjuryDto(player = null, type = "Muscle Injury").toDomain()
        assertEquals("Bilinmeyen Oyuncu", domain.playerName)
        assertNull(domain.playerId)
    }

    @Test
    fun `InjuryDto maps populated fields correctly`() {
        val dto = InjuryDto(
            player = InjuryPlayer(id = 10, name = "Star Player", photo = "photo.png"),
            team = TeamInfo(name = "Team A"),
            type = "Suspended",
            reason = "Red Card"
        )
        val domain = dto.toDomain()
        assertEquals(10, domain.playerId)
        assertEquals("Star Player", domain.playerName)
        assertEquals("Suspended", domain.type)
        assertEquals("Red Card", domain.reason)
    }

    // ---------------- List mapper helpers ----------------

    @Test
    fun `list mapper extensions map empty lists to empty lists`() {
        assertTrue(emptyList<LeagueDto>().toDomainList().isEmpty())
        assertTrue(emptyList<TeamDto>().toDomainList().isEmpty())
        assertTrue(emptyList<SquadPlayerDto>().toDomainList().isEmpty())
    }

    @Test
    fun `list mapper extensions preserve element order and count`() {
        val dtos = listOf(
            SquadPlayerDto(id = 1, name = "A"),
            SquadPlayerDto(id = 2, name = "B"),
            SquadPlayerDto(id = 3, name = "C")
        )
        val domain = dtos.toDomainList()
        assertEquals(3, domain.size)
        assertEquals(listOf("A", "B", "C"), domain.map { it.name })
    }
}
