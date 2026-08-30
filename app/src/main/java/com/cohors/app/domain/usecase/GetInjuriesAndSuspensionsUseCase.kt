package com.cohors.app.domain.usecase

import com.cohors.app.core.util.Resource
import com.cohors.app.core.util.mapData
import com.cohors.app.domain.model.Injury
import com.cohors.app.domain.repository.FootballRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Fetches a team's injuries/suspensions and splits the single API-Football
 * "injuries" list into two UI-ready buckets: actual injuries vs.
 * card-based suspensions (detected from the type/reason text).
 */
class GetInjuriesAndSuspensionsUseCase @Inject constructor(
    private val repository: FootballRepository
) {

    @androidx.compose.runtime.Immutable
    data class InjurySuspensionResult(
        val injuries: List<Injury>,
        val suspensions: List<Injury>
    )

    operator fun invoke(teamId: Int, season: Int): Flow<Resource<InjurySuspensionResult>> =
        repository.getInjuries(teamId = teamId, season = season).map { resource ->
            resource.mapData { injuries ->
                val (suspensions, actualInjuries) = injuries.partition { it.isSuspension() }
                InjurySuspensionResult(injuries = actualInjuries, suspensions = suspensions)
            }
        }

    private fun Injury.isSuspension(): Boolean {
        val type = this.type?.lowercase().orEmpty()
        val reason = this.reason?.lowercase().orEmpty()
        return listOf("suspen", "card", "ceza").any { keyword ->
            type.contains(keyword) || reason.contains(keyword)
        }
    }
}
