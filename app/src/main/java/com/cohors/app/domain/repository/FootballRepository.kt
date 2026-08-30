package com.cohors.app.domain.repository

import com.cohors.app.core.util.Resource
import com.cohors.app.domain.model.Fixture
import com.cohors.app.domain.model.Injury
import com.cohors.app.domain.model.League
import com.cohors.app.domain.model.SquadPlayer
import com.cohors.app.domain.model.Team
import com.cohors.app.domain.model.TeamLineup
import kotlinx.coroutines.flow.Flow

/**
 * Domain-facing contract for all football data operations.
 * ViewModels depend on this interface only — never on the Retrofit
 * service or DTOs directly (Dependency Inversion).
 */
interface FootballRepository {

    fun getLeagues(country: String? = null, season: Int? = null): Flow<Resource<List<League>>>

    fun getTeams(leagueId: Int, season: Int): Flow<Resource<List<Team>>>

    fun getTeamInfo(teamId: Int): Flow<Resource<Team?>>

    fun getSquad(teamId: Int): Flow<Resource<List<SquadPlayer>>>

    fun getUpcomingFixture(teamId: Int, season: Int? = null): Flow<Resource<Fixture?>>

    fun getLineup(fixtureId: Int): Flow<Resource<List<TeamLineup>>>

    fun getInjuries(teamId: Int, season: Int): Flow<Resource<List<Injury>>>
}
