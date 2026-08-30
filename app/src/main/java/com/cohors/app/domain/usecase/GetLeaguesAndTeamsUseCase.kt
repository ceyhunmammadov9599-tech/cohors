package com.cohors.app.domain.usecase

import com.cohors.app.core.util.Resource
import com.cohors.app.core.util.mapData
import com.cohors.app.domain.model.League
import com.cohors.app.domain.model.Team
import com.cohors.app.domain.repository.FootballRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Handles listing leagues and the teams within a league, including
 * client-side name search/filtering on top of the repository stream.
 */
class GetLeaguesAndTeamsUseCase @Inject constructor(
    private val repository: FootballRepository
) {

    fun leagues(
        searchQuery: String? = null,
        country: String? = null,
        season: Int? = null
    ): Flow<Resource<List<League>>> =
        repository.getLeagues(country = country, season = season).map { resource ->
            resource.mapData { list -> filterByQuery(list, searchQuery) { it.name } }
        }

    fun teams(
        leagueId: Int,
        season: Int,
        searchQuery: String? = null
    ): Flow<Resource<List<Team>>> =
        repository.getTeams(leagueId = leagueId, season = season).map { resource ->
            resource.mapData { list -> filterByQuery(list, searchQuery) { it.name } }
        }

    private inline fun <T> filterByQuery(
        list: List<T>,
        query: String?,
        nameOf: (T) -> String
    ): List<T> {
        if (query.isNullOrBlank()) return list
        return list.filter { nameOf(it).contains(query, ignoreCase = true) }
    }
}
