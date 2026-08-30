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
 *
 * Search is filtered client-side against the last successfully fetched
 * list (allLeagues / allTeams) — typing does NOT trigger a new network
 * request per keystroke. The API's free-tier plan has a strict per-minute
 * rate limit, and re-fetching the full league/team list on every keystroke
 * exhausted it almost instantly, surfacing as "Sunucu hatası" (500/429).
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

    // Last successfully fetched, unfiltered lists — search filters these
    // in-memory instead of re-hitting the network on every keystroke.
    private var allLeagues: List<League> = emptyList()
    private var allTeams: List<Team> = emptyList()

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

    /**
     * Filters the already-fetched list in memory. No network call is made
     * here — [loadLeagues] / [loadTeams] are only invoked on initial load,
     * league selection, and retry.
     */
    private fun onSearchQueryChanged(query: String) {
        _state.update { it.copy(searchQuery = query) }
        val league = _state.value.selectedLeague
        if (league != null) {
            _state.update { it.copy(teams = filterToUiState(allTeams, query) { it.name }) }
        } else {
            _state.update { it.copy(leagues = filterToUiState(allLeagues, query) { it.name }) }
        }
    }

    private inline fun <T> filterToUiState(
        list: List<T>,
        query: String,
        nameOf: (T) -> String
    ): UiState<List<T>> {
        val filtered = if (query.isBlank()) list else list.filter { nameOf(it).contains(query, ignoreCase = true) }
        return if (filtered.isEmpty()) UiState.Empty else UiState.Success(filtered)
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
        loadTeams(league.id)
    }

    private fun onClearLeagueSelection() {
        teamsJob?.cancel()
        allTeams = emptyList()
        _state.update {
            it.copy(
                selectedLeague = null,
                searchQuery = "",
                teams = UiState.Empty,
                selectedTeam = null
            )
        }
        // Leagues were already fetched once; just re-show the full list.
        _state.update { it.copy(leagues = filterToUiState(allLeagues, "") { it.name }) }
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
            league != null -> loadTeams(league.id)
            else -> loadLeagues()
        }
    }

    /** Fetches the full league list once. Does not take a search query. */
    private fun loadLeagues() {
        leaguesJob?.cancel()
        leaguesJob = getLeaguesAndTeamsUseCase.leagues()
            .onEach { resource ->
                if (resource is Resource.Success) {
                    allLeagues = resource.data
                }
                _state.update { it.copy(leagues = resource.toUiState { list -> list.isEmpty() }) }
                if (resource is Resource.Error) {
                    _sideEffect.send(TeamSquadSideEffect.ShowSnackbar(resource.message))
                }
            }
            .launchIn(viewModelScope)
    }

    /** Fetches the full team list for a league once. Does not take a search query. */
    private fun loadTeams(leagueId: Int, season: Int = currentSeasonYear()) {
        teamsJob?.cancel()
        teamsJob = getLeaguesAndTeamsUseCase.teams(leagueId = leagueId, season = season)
            .onEach { resource ->
                if (resource is Resource.Success) {
                    allTeams = resource.data
                }
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
                _state.update {
                    it.copy(squadByPosition = resource.toUiState { map -> map.isEmpty() })
                }
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
