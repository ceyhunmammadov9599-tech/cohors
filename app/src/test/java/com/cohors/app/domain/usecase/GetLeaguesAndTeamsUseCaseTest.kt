package com.cohors.app.domain.usecase

import com.cohors.app.core.util.Resource
import com.cohors.app.domain.model.League
import com.cohors.app.domain.model.Team
import com.cohors.app.domain.repository.FootballRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class GetLeaguesAndTeamsUseCaseTest {

    private val repository: FootballRepository = mockk()
    private lateinit var useCase: GetLeaguesAndTeamsUseCase

    private val premierLeague = League(39, "Premier League", "League", null, "England", null, 2024)
    private val laLiga = League(140, "La Liga", "League", null, "Spain", null, 2024)

    private val manUtd = Team(33, "Manchester United", null, null, null, null, null, null, null)
    private val manCity = Team(50, "Manchester City", null, null, null, null, null, null, null)
    private val chelsea = Team(49, "Chelsea", null, null, null, null, null, null, null)

    @Before
    fun setUp() {
        useCase = GetLeaguesAndTeamsUseCase(repository)
    }

    @Test
    fun `leagues with no search query returns the full unfiltered list`() = runTest {
        every { repository.getLeagues(any(), any()) } returns flowOf(Resource.Success(listOf(premierLeague, laLiga)))

        val result = (useCase.leagues().toList().last() as Resource.Success).data

        assertEquals(2, result.size)
    }

    @Test
    fun `leagues with a search query filters case-insensitively by name`() = runTest {
        every { repository.getLeagues(any(), any()) } returns flowOf(Resource.Success(listOf(premierLeague, laLiga)))

        val result = (useCase.leagues(searchQuery = "premier").toList().last() as Resource.Success).data

        assertEquals(1, result.size)
        assertEquals("Premier League", result.first().name)
    }

    @Test
    fun `leagues with a blank search query is treated as no filter`() = runTest {
        every { repository.getLeagues(any(), any()) } returns flowOf(Resource.Success(listOf(premierLeague, laLiga)))

        val result = (useCase.leagues(searchQuery = "   ").toList().last() as Resource.Success).data

        assertEquals(2, result.size)
    }

    @Test
    fun `leagues with a non-matching query returns an empty list`() = runTest {
        every { repository.getLeagues(any(), any()) } returns flowOf(Resource.Success(listOf(premierLeague, laLiga)))

        val result = (useCase.leagues(searchQuery = "Bundesliga").toList().last() as Resource.Success).data

        assertEquals(0, result.size)
    }

    @Test
    fun `leagues propagates repository errors unchanged`() = runTest {
        every { repository.getLeagues(any(), any()) } returns flowOf(Resource.Error("network down"))

        val error = useCase.leagues().toList().last() as Resource.Error

        assertEquals("network down", error.message)
    }

    @Test
    fun `teams filters by name for the given league`() = runTest {
        every { repository.getTeams(leagueId = 39, season = 2024) } returns
            flowOf(Resource.Success(listOf(manUtd, manCity, chelsea)))

        val result = (useCase.teams(leagueId = 39, season = 2024, searchQuery = "man").toList().last() as Resource.Success).data

        assertEquals(2, result.size)
        assertEquals(setOf("Manchester United", "Manchester City"), result.map { it.name }.toSet())
    }
}
