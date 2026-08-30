package com.cohors.app.domain.usecase

import com.cohors.app.core.util.Resource
import com.cohors.app.core.util.mapData
import com.cohors.app.domain.model.PlayerPosition
import com.cohors.app.domain.model.SquadPlayer
import com.cohors.app.domain.repository.FootballRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Fetches a team's squad and organizes it the way the UI needs it:
 * grouped by position (Kaleci -> Defans -> Orta Saha -> Forvet order)
 * and sorted by shirt number within each group (players with no
 * assigned number sink to the bottom).
 */
class GetTeamSquadUseCase @Inject constructor(
    private val repository: FootballRepository
) {

    private val positionOrder = listOf(
        PlayerPosition.GOALKEEPER,
        PlayerPosition.DEFENDER,
        PlayerPosition.MIDFIELDER,
        PlayerPosition.ATTACKER,
        PlayerPosition.UNKNOWN
    )

    operator fun invoke(teamId: Int): Flow<Resource<Map<PlayerPosition, List<SquadPlayer>>>> =
        repository.getSquad(teamId).map { resource ->
            resource.mapData { players -> groupAndSort(players) }
        }

    private fun groupAndSort(players: List<SquadPlayer>): Map<PlayerPosition, List<SquadPlayer>> =
        players
            .groupBy { it.position }
            .toSortedMap(compareBy { positionOrder.indexOf(it) })
            .mapValues { (_, list) ->
                list.sortedWith(compareBy(nullsLast<Int>()) { it.shirtNumber })
            }
}
