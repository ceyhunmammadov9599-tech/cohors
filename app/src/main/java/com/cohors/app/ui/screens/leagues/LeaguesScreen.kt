package com.cohors.app.ui.screens.leagues

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items as columnItems
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.navigation.compose.hiltViewModel
import com.cohors.app.presentation.teamsquad.TeamSquadSideEffect
import com.cohors.app.presentation.teamsquad.TeamSquadUiEvent
import com.cohors.app.presentation.teamsquad.TeamSquadViewModel
import com.cohors.app.ui.components.CohorsSearchField
import com.cohors.app.ui.components.LeagueRow
import com.cohors.app.ui.components.PopularLeagueChipsRow
import com.cohors.app.ui.components.TeamCard
import com.cohors.app.ui.components.UiStateContent

/**
 * League & Team search/selection screen — the app's home screen.
 * Shows leagues (with popular-league quick chips) first; once a league is
 * selected, shows its teams in a grid. Search filters whichever list is
 * currently active.
 */
@Composable
fun LeaguesScreen(
    onTeamSelected: (teamId: Int, teamName: String) -> Unit,
    viewModel: TeamSquadViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.sideEffect.collect { effect ->
            if (effect is TeamSquadSideEffect.ShowSnackbar) {
                snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .statusBarsPadding()
        ) {
            Text(
                text = "Cohors",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )

            CohorsSearchField(
                query = state.searchQuery,
                onQueryChange = { viewModel.onEvent(TeamSquadUiEvent.OnSearchQueryChanged(it)) },
                placeholder = if (state.selectedLeague == null) "Lig ara..." else "Takım ara...",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            if (state.selectedLeague != null) {
                AssistChip(
                    onClick = { viewModel.onEvent(TeamSquadUiEvent.OnClearLeagueSelection) },
                    label = { Text(state.selectedLeague!!.name) },
                    trailingIcon = { androidx.compose.material3.Icon(Icons.Filled.Close, contentDescription = "Kapat") },
                    modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                )
            } else {
                PopularLeagueChipsRow(
                    onChipClick = { label -> viewModel.onEvent(TeamSquadUiEvent.OnSearchQueryChanged(label)) },
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            Box(modifier = Modifier.fillMaxSize()) {
                if (state.selectedLeague == null) {
                    UiStateContent(
                        state = state.leagues,
                        emptyMessage = "Aramanla eşleşen lig bulunamadı.",
                        onRetry = { viewModel.onEvent(TeamSquadUiEvent.OnRetry) }
                    ) { leagues ->
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            columnItems(leagues, key = { it.id }) { league ->
                                LeagueRow(
                                    league = league,
                                    onClick = { viewModel.onEvent(TeamSquadUiEvent.OnLeagueSelected(league)) }
                                )
                            }
                        }
                    }
                } else {
                    UiStateContent(
                        state = state.teams,
                        emptyMessage = "Bu ligde eşleşen takım bulunamadı.",
                        onRetry = { viewModel.onEvent(TeamSquadUiEvent.OnRetry) }
                    ) { teams ->
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(teams, key = { it.id }) { team ->
                                TeamCard(
                                    team = team,
                                    onClick = { onTeamSelected(team.id, team.name) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
