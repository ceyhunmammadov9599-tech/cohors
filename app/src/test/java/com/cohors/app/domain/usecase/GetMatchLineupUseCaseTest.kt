package com.cohors.app.domain.usecase

import com.cohors.app.core.util.Resource
import com.cohors.app.domain.model.Fixture
import com.cohors.app.domain.model.TeamLineup
import com.cohors.app.domain.repository.FootballRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GetMatchLineupUseCaseTest {

    private val repository: FootballRepository = mockk()
    private lateinit var useCase: GetMatchLineupUseCase

    private val fixture = Fixture(
        id = 555,
        dateIso = "2026-09-05T18:00:00+00:00",
        timestamp = null,
        statusShort = "NS",
        statusLong = "Not Started",
        homeTeamId = 33,
        homeTeamName = "Manchester United",
        homeTeamLogoUrl = null,
        awayTeamId = 40,
        awayTeamName = "Liverpool",
        awayTeamLogoUrl = null,
        leagueName = "Premier League",
        leagueSeason = 2026,
        round = "Round 5"
    )

    private val lineup = TeamLineup(
        teamId = 33,
        teamName = "Manchester United",
        teamLogoUrl = null,
        formation = "4-2-3-1",
        startingXI = emptyList(),
        substitutes = emptyList()
    )

    @Before
    fun setUp() {
        useCase = GetMatchLineupUseCase(repository)
    }

    @Test
    fun `emits success with fixture and lineups when both calls succeed`() = runTest {
        every { repository.getUpcomingFixture(33, null) } returns flowOf(Resource.Success(fixture))
        every { repository.getLineup(555) } returns flowOf(Resource.Success(listOf(lineup)))

        val emissions = useCase(33).toList()

        assertEquals(Resource.Loading, emissions.first())
        val success = emissions.last() as Resource.Success
        assertEquals(555, success.data.fixture.id)
        assertEquals(1, success.data.lineups.size)
        assertEquals("4-2-3-1", success.data.lineups.first().formation)
    }

    @Test
    fun `emits an error when no upcoming fixture is found`() = runTest {
        every { repository.getUpcomingFixture(33, null) } returns flowOf(Resource.Success(null))

        val error = useCase(33).toList().last() as Resource.Error

        assertEquals("Yaklaşan veya devam eden bir maç bulunamadı.", error.message)
    }

    @Test
    fun `propagates the fixture resolution error without calling getLineup`() = runTest {
        every { repository.getUpcomingFixture(33, null) } returns flowOf(Resource.Error("fixture fetch failed"))

        val error = useCase(33).toList().last() as Resource.Error

        assertEquals("fixture fetch failed", error.message)
        io.mockk.verify(exactly = 0) { repository.getLineup(any()) }
    }

    @Test
    fun `propagates the lineup resolution error after a successful fixture lookup`() = runTest {
        every { repository.getUpcomingFixture(33, null) } returns flowOf(Resource.Success(fixture))
        every { repository.getLineup(555) } returns flowOf(Resource.Error("lineup fetch failed"))

        val error = useCase(33).toList().last() as Resource.Error

        assertEquals("lineup fetch failed", error.message)
    }

    @Test
    fun `passes the given season through to getUpcomingFixture`() = runTest {
        every { repository.getUpcomingFixture(33, 2025) } returns flowOf(Resource.Success(fixture))
        every { repository.getLineup(555) } returns flowOf(Resource.Success(listOf(lineup)))

        val success = useCase(33, season = 2025).toList().last() as Resource.Success

        assertTrue(success.data.fixture.id == 555)
    }
}
