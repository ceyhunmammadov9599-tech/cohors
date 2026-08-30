package com.cohors.app.presentation.teamsquad

import app.cash.turbine.test
import com.cohors.app.core.util.Resource
import com.cohors.app.domain.model.League
import com.cohors.app.domain.model.PlayerPosition
import com.cohors.app.domain.model.SquadPlayer
import com.cohors.app.domain.model.Team
import com.cohors.app.domain.usecase.GetInjuriesAndSuspensionsUseCase
import com.cohors.app.domain.usecase.GetLeaguesAndTeamsUseCase
import com.cohors.app.domain.usecase.GetTeamSquadUseCase
import com.cohors.app.presentation.common.UiState
import com.cohors.app.util.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class TeamSquadViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val getLeaguesAndTeamsUseCase: GetLeaguesAndTeamsUseCase = mockk()
    private val getTeamSquadUseCase: GetTeamSquadUseCase = mockk()
    private val getInjuriesAndSuspensionsUseCase: GetInjuriesAndSuspensionsUseCase = mockk()

    private val premierLeague = League(39, "Premier League", "League", null, "England", null, 2024)
    private val manUtd = Team(33, "Manchester United", null, null, null, null, null, null, null)
    private val keeper = SquadPlayer(1, "Keeper", 30, 1, PlayerPosition.GOALKEEPER, null)

    private fun createViewModel(): TeamSquadViewModel =
        TeamSquadViewModel(getLeaguesAndTeamsUseCase, getTeamSquadUseCase, getInjuriesAndSuspensionsUseCase)

    @Before
    fun setUp() {
        // Default stub so every ViewModel instance can be constructed without an unmocked-call crash.
        every { getLeaguesAndTeamsUseCase.leagues(any(), any(), any()) } returns flowOf(Resource.Success(emptyList()))
    }

    @Test
    fun `initial load emits loading then success for leagues`() = runTest(mainDispatcherRule.testDispatcher) {
        every { getLeaguesAndTeamsUseCase.leagues(any(), any(), any()) } returns
            flowOf(Resource.Loading, Resource.Success(listOf(premierLeague)))

        val viewModel = createViewModel()

        viewModel.state.test {
            assertEquals(UiState.Loading, awaitItem().leagues) // default state before coroutine runs
            advanceUntilIdle()
            val loaded = awaitItem()
            assertEquals(UiState.Success(listOf(premierLeague)), loaded.leagues)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `leagues error updates state to Error and emits a snackbar side effect`() = runTest(mainDispatcherRule.testDispatcher) {
        every { getLeaguesAndTeamsUseCase.leagues(any(), any(), any()) } returns flowOf(Resource.Error("boom"))

        val viewModel = createViewModel()

        viewModel.sideEffect.test {
            advanceUntilIdle()
            val effect = awaitItem()
            assertEquals(TeamSquadSideEffect.ShowSnackbar("boom"), effect)
        }
        assertEquals(UiState.Error("boom"), viewModel.state.value.leagues)
    }

    @Test
    fun `selecting a league loads its teams and updates selectedLeague`() = runTest(mainDispatcherRule.testDispatcher) {
        every { getLeaguesAndTeamsUseCase.teams(any(), any(), any()) } returns flowOf(Resource.Success(listOf(manUtd)))

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(TeamSquadUiEvent.OnLeagueSelected(premierLeague))
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(premierLeague, state.selectedLeague)
        assertEquals(UiState.Success(listOf(manUtd)), state.teams)
    }

    @Test
    fun `selecting a team loads squad grouped by position and injuries`() = runTest(mainDispatcherRule.testDispatcher) {
        val grouped = mapOf(PlayerPosition.GOALKEEPER to listOf(keeper))
        every { getTeamSquadUseCase(33) } returns flowOf(Resource.Success(grouped))
        every { getInjuriesAndSuspensionsUseCase(33, any()) } returns
            flowOf(Resource.Success(GetInjuriesAndSuspensionsUseCase.InjurySuspensionResult(emptyList(), emptyList())))

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onEvent(TeamSquadUiEvent.OnTeamSelected(manUtd))
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(manUtd, state.selectedTeam)
        assertEquals(UiState.Success(grouped), state.squadByPosition)
        assertEquals(
            UiState.Empty,
            state.injuries
        )
    }

    @Test
    fun `clearing league selection returns to the league list and resets teams`() = runTest(mainDispatcherRule.testDispatcher) {
        every { getLeaguesAndTeamsUseCase.teams(any(), any(), any()) } returns flowOf(Resource.Success(listOf(manUtd)))

        val viewModel = createViewModel()
        advanceUntilIdle()
        viewModel.onEvent(TeamSquadUiEvent.OnLeagueSelected(premierLeague))
        advanceUntilIdle()

        viewModel.onEvent(TeamSquadUiEvent.OnClearLeagueSelection)
        advanceUntilIdle()

        val state = viewModel.state.value
        assertNull(state.selectedLeague)
        assertEquals(UiState.Empty, state.teams)
    }

    @Test
    fun `loadTeamById seeds a minimal team and loads squad plus injuries directly`() = runTest(mainDispatcherRule.testDispatcher) {
        val grouped = mapOf(PlayerPosition.GOALKEEPER to listOf(keeper))
        every { getTeamSquadUseCase(33) } returns flowOf(Resource.Success(grouped))
        every { getInjuriesAndSuspensionsUseCase(33, any()) } returns
            flowOf(Resource.Success(GetInjuriesAndSuspensionsUseCase.InjurySuspensionResult(emptyList(), emptyList())))

        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.loadTeamById(33, "Manchester United")
        advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(33, state.selectedTeam?.id)
        assertEquals("Manchester United", state.selectedTeam?.name)
        assertEquals(UiState.Success(grouped), state.squadByPosition)
    }
}
