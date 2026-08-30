package com.cohors.app.presentation.teamsquad

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cohors.app.core.util.Resource
import com.cohors.app.domain.model.League
import com.cohors.app.domain.model.Team
import com.cohors.app.domain.usecase.GetInjuriesAndSuspensionsUseCase
import com.cohors.app.domain.usecase.GetLeaguesAndTeamsUseCase
import com.cohors.app.domain.usecase.GetTeamSquadUseCase
import com.cohors.app.presentation.common.UiState
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
import java.util.Calendar
import javax.inject.Inject

/**
 * MVI ViewModel for the Team & Squad screen: league/team search & selection,
 * squad grouped by position, and injuries/suspensions — all reactive via
 * StateFlow, with one-shot side effects delivered through a Channel.
 */
@HiltViewModel
class TeamSquadViewModel @Inject constructor(
    private val getLeaguesAndTeamsUseCase: GetLeaguesAndTeamsUseCase,
    private val getTeamSquadUseCase: GetTeamSquadUseCase,
    private val getInjuriesAndSuspensionsUseCase: GetInjuriesAndSuspensionsUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(TeamSquadScreenState())
    val state: StateFlow<TeamSquadScreenState> = _state.asStateFlow()

    private val _sideEffect = Channel<TeamSquadSideEffect>(Channel.BUFFERED)
    val sideEffect: Flow<TeamSquadSideEffect> = _sideEffect.receiveAsFlow()

    private var leaguesJob: Job? = null
    private var teamsJob: Job? = null
    private var squadJob: Job? = null
    private var injuriesJob: Job? = null

    private var loadedTeamId: Int? = null

    init {
        loadLeagues()
    }

    fun onEvent(event: TeamSquadUiEvent) {
        when (event) {
            is TeamSquadUiEvent.OnSearchQueryChanged -> onSearchQueryChanged(event.query)
            is TeamSquadUiEvent.OnLeagueSelected -> onLeagueSelected(event.league)
            TeamSquadUiEvent.OnClearLeagueSelection -> onClearLeagueSelection()
            is TeamSquadUiEvent.OnTeamSelected -> onTeamSelected(event.team)
            is TeamSquadUiEvent.OnTabSelected -> _state.update { it.copy(selectedTab = event.tab) }
            TeamSquadUiEvent.OnRetry, TeamSquadUiEvent.OnRefresh -> onRetry()
        }
    }

    /**
     * Entry point used by the Squad screen when it's opened directly via
     * navigation (teamId + teamName from nav args) rather than through the
     * league/team search flow. Seeds a minimal [Team] and loads its data.
     */
    fun loadTeamById(teamId: Int, teamName: String) {
        if (loadedTeamId == teamId && _state.value.selectedTeam != null) return
        loadedTeamId = teamId
        val minimalTeam = _state.value.selectedTeam?.takeIf { it.id == teamId }
            ?: Team(
                id = teamId,
                name = teamName,
                code = null,
                country = null,
                founded = null,
                logoUrl = null,
                venueName = null,
                venueCity = null,
                venueImageUrl = null
            )
        _state.update { it.copy(selectedTeam = minimalTeam) }
        loadSquad(teamId)
        loadInjuries(teamId)
    }

    private fun onSearchQueryChanged(query: String) {
        _state.update { it.copy(searchQuery = query) }
        val league = _state.value.selectedLeague
        if (league != null) {
            loadTeams(league.id, query)
        } else {
            loadLeagues(query)
        }
    }

    private fun onLeagueSelected(league: League) {
        _state.update {
            it.copy(
                selectedLeague = league,
                searchQuery = "",
                selectedTeam = null,
                teams = UiState.Loading,
                squadByPosition = UiState.Empty,
                injuries = UiState.Empty
            )
        }
        loadTeams(league.id, null)
    }

    private fun onClearLeagueSelection() {
        teamsJob?.cancel()
        _state.update {
            it.copy(
                selectedLeague = null,
                searchQuery = "",
                teams = UiState.Empty,
                selectedTeam = null
            )
        }
        loadLeagues(null)
    }

    private fun onTeamSelected(team: Team) {
        loadedTeamId = team.id
        _state.update { it.copy(selectedTeam = team) }
        loadSquad(team.id)
        loadInjuries(team.id)
    }

    private fun onRetry() {
        val team = _state.value.selectedTeam
        val league = _state.value.selectedLeague
        when {
            team != null -> {
                loadSquad(team.id)
                loadInjuries(team.id)
            }
            league != null -> loadTeams(league.id, _state.value.searchQuery)
            else -> loadLeagues(_state.value.searchQuery)
        }
    }

    private fun loadLeagues(query: String? = null) {
        leaguesJob?.cancel()
        leaguesJob = getLeaguesAndTeamsUseCase.leagues(searchQuery = query)
            .onEach { resource ->
                _state.update { it.copy(leagues = resource.toUiState { list -> list.isEmpty() }) }
                if (resource is Resource.Error) {
                    _sideEffect.send(TeamSquadSideEffect.ShowSnackbar(resource.message))
                }
            }
            .launchIn(viewModelScope)
    }

    private fun loadTeams(leagueId: Int, query: String? = null, season: Int = currentSeasonYear()) {
        teamsJob?.cancel()
        teamsJob = getLeaguesAndTeamsUseCase.teams(leagueId = leagueId, season = season, searchQuery = query)
            .onEach { resource ->
                _state.update { it.copy(teams = resource.toUiState { list -> list.isEmpty() }) }
                if (resource is Resource.Error) {
                    _sideEffect.send(TeamSquadSideEffect.ShowSnackbar(resource.message))
                }
            }
            .launchIn(viewModelScope)
    }

    private fun loadSquad(teamId: Int) {
        squadJob?.cancel()
        squadJob = getTeamSquadUseCase(teamId)
            .onEach { resource ->
                _state.update { it.copy(squadByPosition = resource.toUiState { map -> map.isEmpty() }) }
                if (resource is Resource.Error) {
                    _sideEffect.send(TeamSquadSideEffect.ShowSnackbar(resource.message))
                }
            }
            .launchIn(viewModelScope)
    }

    private fun loadInjuries(teamId: Int, season: Int = currentSeasonYear()) {
        injuriesJob?.cancel()
        injuriesJob = getInjuriesAndSuspensionsUseCase(teamId, season)
            .onEach { resource ->
                _state.update {
                    it.copy(
                        injuries = resource.toUiState { result ->
                            result.injuries.isEmpty() && result.suspensions.isEmpty()
                        }
                    )
                }
                if (resource is Resource.Error) {
                    _sideEffect.send(TeamSquadSideEffect.ShowSnackbar(resource.message))
                }
            }
            .launchIn(viewModelScope)
    }

    private fun currentSeasonYear(): Int = Calendar.getInstance().get(Calendar.YEAR)
}
