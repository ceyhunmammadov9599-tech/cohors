package com.cohors.app.data.remote.api

import com.cohors.app.data.remote.model.ApiResponse
import com.cohors.app.data.remote.model.LeagueDto
import com.cohors.app.data.remote.model.TeamDto
import com.cohors.app.data.remote.model.SquadDto
import com.cohors.app.data.remote.model.FixtureDto
import com.cohors.app.data.remote.model.LineupDto
import com.cohors.app.data.remote.model.InjuryDto
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * API-Football (via RapidAPI) endpoint definitions.
 * Docs: https://www.api-football.com/documentation-v3
 *
 * Free tier: 100 requests/day.
 * Base URL: https://api-football-v1.p.rapidapi.com/v3/
 *
 * Note: The v3 endpoints are used here. If your RapidAPI subscription
 * routes to v1, adjust the base URL in NetworkModule accordingly.
 */
interface ApiFootballService {

    // --- Leagues ---

    @GET("leagues")
    suspend fun getLeagues(
        @Query("country") country: String? = null,
        @Query("season") season: Int? = null,
        @Query("type") type: String? = null  // "league" or "cup"
    ): ApiResponse<List<LeagueDto>>

    // --- Teams ---

    @GET("teams")
    suspend fun getTeams(
        @Query("league") leagueId: Int,
        @Query("season") season: Int
    ): ApiResponse<List<TeamDto>>

    @GET("teams")
    suspend fun getTeamInfo(
        @Query("id") teamId: Int
    ): ApiResponse<List<TeamDto>>

    // --- Squad (current season roster) ---

    @GET("players/squads")
    suspend fun getSquad(
        @Query("team") teamId: Int
    ): ApiResponse<List<SquadDto>>

    // --- Fixtures (upcoming matches) ---

    @GET("fixtures")
    suspend fun getUpcomingFixtures(
        @Query("team") teamId: Int,
        @Query("next") next: Int = 1,
        @Query("season") season: Int? = null
    ): ApiResponse<List<FixtureDto>>

    // --- Lineups (requires fixture ID) ---

    @GET("fixtures/lineups")
    suspend fun getLineup(
        @Query("fixture") fixtureId: Int
    ): ApiResponse<List<LineupDto>>

    // --- Injuries ---

    @GET("injuries")
    suspend fun getInjuries(
        @Query("team") teamId: Int,
        @Query("season") season: Int
    ): ApiResponse<List<InjuryDto>>
}
