package com.cohors.app.presentation.matchlineup

import com.cohors.app.domain.usecase.GetMatchLineupUseCase.MatchLineupResult
import com.cohors.app.presentation.common.UiState

/** Aggregate screen state for the Match Lineup / Tactical Pitch screen. */
data class MatchLineupScreenState(
    val matchLineup: UiState<MatchLineupResult> = UiState.Loading
)

/** User-triggered interactions on the Match Lineup screen. */
sealed interface MatchLineupUiEvent {
    data class LoadForTeam(val teamId: Int, val season: Int? = null) : MatchLineupUiEvent
    data object OnRetry : MatchLineupUiEvent
    data object OnRefresh : MatchLineupUiEvent
}

/** One-shot events the screen should react to exactly once. */
sealed interface MatchLineupSideEffect {
    data class ShowSnackbar(val message: String) : MatchLineupSideEffect
}
