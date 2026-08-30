package com.cohors.app.data.repository

import com.cohors.app.core.util.Resource
import com.cohors.app.core.util.resourceFlow
import com.cohors.app.data.mapper.toDomain
import com.cohors.app.data.mapper.toDomainList
import com.cohors.app.data.remote.api.ApiFootballService
import com.cohors.app.domain.model.Fixture
import com.cohors.app.domain.model.Injury
import com.cohors.app.domain.model.League
import com.cohors.app.domain.model.SquadPlayer
import com.cohors.app.domain.model.Team
import com.cohors.app.domain.model.TeamLineup
import com.cohors.app.domain.repository.FootballRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Concrete implementation of [FootballRepository] backed by the
 * API-Football Retrofit service. Every call is wrapped in [resourceFlow],
 * which emits Loading -> Success/Error and maps exceptions to
 * user-friendly messages.
 */
@Singleton
class FootballRepositoryImpl @Inject constructor(
    private val apiService: ApiFootballService
) : FootballRepository {

    override fun getLeagues(country: String?, season: Int?): Flow<Resource<List<League>>> =
        resourceFlow {
            val response = apiService.getLeagues(country = country, season = season)
            response.response?.toDomainList() ?: emptyList()
        }

    override fun getTeams(leagueId: Int, season: Int): Flow<Resource<List<Team>>> =
        resourceFlow {
            val response = apiService.getTeams(leagueId = leagueId, season = season)
            response.response?.toDomainList() ?: emptyList()
        }

    override fun getTeamInfo(teamId: Int): Flow<Resource<Team?>> =
        resourceFlow {
            val response = apiService.getTeamInfo(teamId = teamId)
            response.response?.firstOrNull()?.toDomain()
        }

    override fun getSquad(teamId: Int): Flow<Resource<List<SquadPlayer>>> =
        resourceFlow {
            val response = apiService.getSquad(teamId = teamId)
            response.response?.firstOrNull()?.players?.toDomainList() ?: emptyList()
        }

    override fun getUpcomingFixture(teamId: Int, season: Int?): Flow<Resource<Fixture?>> =
        resourceFlow {
            val response = apiService.getUpcomingFixtures(
                teamId = teamId,
                next = 1,
                season = season
            )
            response.response?.firstOrNull()?.toDomain()
        }

    override fun getLineup(fixtureId: Int): Flow<Resource<List<TeamLineup>>> =
        resourceFlow {
            val response = apiService.getLineup(fixtureId = fixtureId)
            response.response?.toDomainList() ?: emptyList()
        }

    override fun getInjuries(teamId: Int, season: Int): Flow<Resource<List<Injury>>> =
        resourceFlow {
            val response = apiService.getInjuries(teamId = teamId, season = season)
            response.response?.toDomainList() ?: emptyList()
        }
}
