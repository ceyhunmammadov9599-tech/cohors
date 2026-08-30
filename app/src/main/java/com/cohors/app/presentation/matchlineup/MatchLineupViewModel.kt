package com.cohors.app.presentation.matchlineup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cohors.app.core.util.Resource
import com.cohors.app.domain.usecase.GetMatchLineupUseCase
import com.cohors.app.presentation.common.toUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * MVI ViewModel for the Match Lineup / Tactical Pitch screen: resolves the
 * team's upcoming fixture and renders the starting XI + formation reactively.
 */
@HiltViewModel
class MatchLineupViewModel @Inject constructor(
    private val getMatchLineupUseCase: GetMatchLineupUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(MatchLineupScreenState())
    val state: StateFlow<MatchLineupScreenState> = _state.asStateFlow()

    private val _sideEffect = Channel<MatchLineupSideEffect>(Channel.BUFFERED)
    val sideEffect: Flow<MatchLineupSideEffect> = _sideEffect.receiveAsFlow()

    private var currentTeamId: Int? = null
    private var currentSeason: Int? = null
    private var loadJob: Job? = null

    fun onEvent(event: MatchLineupUiEvent) {
        when (event) {
            is MatchLineupUiEvent.LoadForTeam -> {
                currentTeamId = event.teamId
                currentSeason = event.season
                load(event.teamId, event.season)
            }
            MatchLineupUiEvent.OnRetry, MatchLineupUiEvent.OnRefresh -> {
                currentTeamId?.let { load(it, currentSeason) }
            }
        }
    }

    private fun load(teamId: Int, season: Int?) {
        loadJob?.cancel()
        loadJob = getMatchLineupUseCase(teamId, season)
            .onEach { resource ->
                _state.update {
                    it.copy(
                        matchLineup = resource.toUiState { result -> result.lineups.isEmpty() }
                    )
                }
                if (resource is Resource.Error) {
                    _sideEffect.send(MatchLineupSideEffect.ShowSnackbar(resource.message))
                }
            }
            .launchIn(viewModelScope)
    }
}
