package com.cohors.app.domain.usecase

import com.cohors.app.core.util.Resource
import com.cohors.app.domain.model.Fixture
import com.cohors.app.domain.model.TeamLineup
import com.cohors.app.domain.repository.FootballRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import javax.inject.Inject

/**
 * Resolves a team's upcoming/live fixture and then fetches the official
 * (or probable) starting XI + formation for that fixture in a single
 * reactive stream: Loading -> Success(fixture + lineups) / Error.
 */
class GetMatchLineupUseCase @Inject constructor(
    private val repository: FootballRepository
) {

    @androidx.compose.runtime.Immutable
    data class MatchLineupResult(
        val fixture: Fixture,
        val lineups: List<TeamLineup>
    )

    operator fun invoke(teamId: Int, season: Int? = null): Flow<Resource<MatchLineupResult>> = flow {
        emit(Resource.Loading)

        val fixtureResource = repository.getUpcomingFixture(teamId, season)
            .first { it !is Resource.Loading }

        when (fixtureResource) {
            is Resource.Error -> emit(Resource.Error(fixtureResource.message, fixtureResource.throwable))
            is Resource.Success -> {
                val fixture = fixtureResource.data
                if (fixture == null) {
                    emit(Resource.Error("Yaklaşan veya devam eden bir maç bulunamadı."))
                } else {
                    val lineupResource = repository.getLineup(fixture.id)
                        .first { it !is Resource.Loading }

                    when (lineupResource) {
                        is Resource.Error -> emit(Resource.Error(lineupResource.message, lineupResource.throwable))
                        is Resource.Success -> emit(
                            Resource.Success(MatchLineupResult(fixture = fixture, lineups = lineupResource.data))
                        )
                        Resource.Loading -> Unit // unreachable, filtered above
                    }
                }
            }
            Resource.Loading -> Unit // unreachable, filtered above
        }
    }.flowOn(Dispatchers.IO)
}
