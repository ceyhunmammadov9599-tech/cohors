package com.cohors.app.domain.usecase

import com.cohors.app.core.util.Resource
import com.cohors.app.domain.model.PlayerPosition
import com.cohors.app.domain.model.SquadPlayer
import com.cohors.app.domain.repository.FootballRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class GetTeamSquadUseCaseTest {

    private val repository: FootballRepository = mockk()
    private lateinit var useCase: GetTeamSquadUseCase

    private fun player(id: Int, name: String, number: Int?, position: PlayerPosition) =
        SquadPlayer(id = id, name = name, age = 25, shirtNumber = number, position = position, photoUrl = null)

    @Before
    fun setUp() {
        useCase = GetTeamSquadUseCase(repository)
    }

    @Test
    fun `squad is grouped by position in GK - DEF - MID - ATT order`() = runTest {
        val players = listOf(
            player(1, "Striker", 9, PlayerPosition.ATTACKER),
            player(2, "Keeper", 1, PlayerPosition.GOALKEEPER),
            player(3, "Midfielder", 8, PlayerPosition.MIDFIELDER),
            player(4, "Defender", 4, PlayerPosition.DEFENDER)
        )
        every { repository.getSquad(33) } returns flowOf(Resource.Success(players))

        val grouped = (useCase(33).toList().last() as Resource.Success).data

        assertEquals(
            listOf(
                PlayerPosition.GOALKEEPER,
                PlayerPosition.DEFENDER,
                PlayerPosition.MIDFIELDER,
                PlayerPosition.ATTACKER
            ),
            grouped.keys.toList()
        )
    }

    @Test
    fun `players within a position group are sorted by shirt number ascending`() = runTest {
        val players = listOf(
            player(1, "Def C", 22, PlayerPosition.DEFENDER),
            player(2, "Def A", 2, PlayerPosition.DEFENDER),
            player(3, "Def B", 5, PlayerPosition.DEFENDER)
        )
        every { repository.getSquad(33) } returns flowOf(Resource.Success(players))

        val grouped = (useCase(33).toList().last() as Resource.Success).data

        assertEquals(listOf("Def A", "Def B", "Def C"), grouped[PlayerPosition.DEFENDER]?.map { it.name })
    }

    @Test
    fun `players with no shirt number sink to the bottom of their group`() = runTest {
        val players = listOf(
            player(1, "No Number", null, PlayerPosition.MIDFIELDER),
            player(2, "Number Ten", 10, PlayerPosition.MIDFIELDER)
        )
        every { repository.getSquad(33) } returns flowOf(Resource.Success(players))

        val grouped = (useCase(33).toList().last() as Resource.Success).data

        assertEquals(listOf("Number Ten", "No Number"), grouped[PlayerPosition.MIDFIELDER]?.map { it.name })
    }

    @Test
    fun `empty squad list maps to an empty grouped map`() = runTest {
        every { repository.getSquad(33) } returns flowOf(Resource.Success(emptyList()))

        val grouped = (useCase(33).toList().last() as Resource.Success).data

        assertEquals(true, grouped.isEmpty())
    }

    @Test
    fun `repository errors are propagated unchanged`() = runTest {
        every { repository.getSquad(33) } returns flowOf(Resource.Error("squad fetch failed"))

        val error = useCase(33).toList().last() as Resource.Error

        assertEquals("squad fetch failed", error.message)
    }
}
