package com.cohors.app.presentation.matchlineup

import app.cash.turbine.test
import com.cohors.app.core.util.Resource
import com.cohors.app.domain.model.Fixture
import com.cohors.app.domain.model.TeamLineup
import com.cohors.app.domain.usecase.GetMatchLineupUseCase
import com.cohors.app.presentation.common.UiState
import com.cohors.app.util.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class MatchLineupViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getMatchLineupUseCase: GetMatchLineupUseCase = mockk()

    private val fixture = Fixture(
        id = 555, dateIso = "2026-09-05T18:00:00+00:00", timestamp = null,
        statusShort = "NS", statusLong = "Not Started",
        homeTeamId = 33, homeTeamName = "Man United", homeTeamLogoUrl = null,
        awayTeamId = 40, awayTeamName = "Liverpool", awayTeamLogoUrl = null,
        leagueName = "Premier League", leagueSeason = 2026, round = "Round 5"
    )
    private val lineup = TeamLineup(
        teamId = 33, teamName = "Man United", teamLogoUrl = null,
        formation = "4-3-3", startingXI = emptyList(), substitutes = emptyList()
    )
    private val result = GetMatchLineupUseCase.MatchLineupResult(fixture, listOf(lineup))

    @Before
    fun setUp() {
        every { getMatchLineupUseCase(any(), any()) } returns flowOf(Resource.Success(result))
    }

    @Test
    fun `LoadForTeam updates state to success with lineup data`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = MatchLineupViewModel(getMatchLineupUseCase)

        viewModel.onEvent(MatchLineupUiEvent.LoadForTeam(33))
        advanceUntilIdle()

        assertEquals(UiState.Success(result), viewModel.state.value.matchLineup)
    }

    @Test
    fun `LoadForTeam with error updates state to Error and emits snackbar`() = runTest(mainDispatcherRule.testDispatcher) {
        every { getMatchLineupUseCase(33, null) } returns flowOf(Resource.Error("lineup unavailable"))
        val viewModel = MatchLineupViewModel(getMatchLineupUseCase)

        viewModel.sideEffect.test {
            viewModel.onEvent(MatchLineupUiEvent.LoadForTeam(33))
            advanceUntilIdle()
            assertEquals(MatchLineupSideEffect.ShowSnackbar("lineup unavailable"), awaitItem())
        }
        assertEquals(UiState.Error("lineup unavailable"), viewModel.state.value.matchLineup)
    }

    @Test
    fun `OnRetry reloads with the previously stored teamId`() = runTest(mainDispatcherRule.testDispatcher) {
        val viewModel = MatchLineupViewModel(getMatchLineupUseCase)
        viewModel.onEvent(MatchLineupUiEvent.LoadForTeam(42))
        advanceUntilIdle()

        every { getMatchLineupUseCase(42, null) } returns flowOf(Resource.Success(result))
        viewModel.onEvent(MatchLineupUiEvent.OnRetry)
        advanceUntilIdle()

        assertEquals(UiState.Success(result), viewModel.state.value.matchLineup)
    }

    @Test
    fun `LoadForTeam stores teamId and season for later retry`() = runTest(mainDispatcherRule.testDispatcher) {
        every { getMatchLineupUseCase(50, 2025) } returns flowOf(Resource.Success(result))
        val viewModel = MatchLineupViewModel(getMatchLineupUseCase)

        viewModel.onEvent(MatchLineupUiEvent.LoadForTeam(50, 2025))
        advanceUntilIdle()

        assertEquals(UiState.Success(result), viewModel.state.value.matchLineup)
    }
}
