package com.cohors.app.presentation.teamsquad

import androidx.compose.runtime.Stable
import com.cohors.app.domain.model.League
import com.cohors.app.domain.model.PlayerPosition
import com.cohors.app.domain.model.SquadPlayer
import com.cohors.app.domain.model.Team
import com.cohors.app.domain.usecase.GetInjuriesAndSuspensionsUseCase.InjurySuspensionResult
import com.cohors.app.presentation.common.UiState

enum class SquadTab {
    SQUAD, INJURIES
}

/**
 * Aggregate screen state for the Team & Squad screen (league/team
 * selection + squad list + injuries/suspensions tab), MVI-style.
 * Annotated @Stable so the Compose compiler can determine its
 * stability even though UiState is a sealed interface.
 */
@Stable
data class TeamSquadScreenState(
    val searchQuery: String = "",
    val leagues: UiState<List<League>> = UiState.Loading,
    val selectedLeague: League? = null,
    val teams: UiState<List<Team>> = UiState.Empty,
    val selectedTeam: Team? = null,
    val squadByPosition: UiState<Map<PlayerPosition, List<SquadPlayer>>> = UiState.Empty,
    val injuries: UiState<InjurySuspensionResult> = UiState.Empty,
    val selectedTab: SquadTab = SquadTab.SQUAD
)

sealed interface TeamSquadUiEvent {
    data class OnSearchQueryChanged(val query: String) : TeamSquadUiEvent
    data class OnLeagueSelected(val league: League) : TeamSquadUiEvent
    data object OnClearLeagueSelection : TeamSquadUiEvent
    data class OnTeamSelected(val team: Team) : TeamSquadUiEvent
    data class OnTabSelected(val tab: SquadTab) : TeamSquadUiEvent
    data object OnRetry : TeamSquadUiEvent
    data object OnRefresh : TeamSquadUiEvent
}

sealed interface TeamSquadSideEffect {
    data class ShowSnackbar(val message: String) : TeamSquadSideEffect
    data class NavigateToLineup(val teamId: Int) : TeamSquadSideEffect
}
