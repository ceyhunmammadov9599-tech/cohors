package com.cohors.app.presentation.matchlineup

import androidx.compose.runtime.Stable
import com.cohors.app.domain.usecase.GetMatchLineupUseCase.MatchLineupResult
import com.cohors.app.presentation.common.UiState

@Stable
data class MatchLineupScreenState(
    val matchLineup: UiState<MatchLineupResult> = UiState.Loading
)

sealed interface MatchLineupUiEvent {
    data class LoadForTeam(val teamId: Int, val season: Int? = null) : MatchLineupUiEvent
    data object OnRetry : MatchLineupUiEvent
    data object OnRefresh : MatchLineupUiEvent
}

sealed interface MatchLineupSideEffect {
    data class ShowSnackbar(val message: String) : MatchLineupSideEffect
}
