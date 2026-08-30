package com.cohors.app.ui.screens.squad

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cohors.app.presentation.common.UiState
import com.cohors.app.presentation.teamsquad.SquadTab
import com.cohors.app.presentation.teamsquad.TeamSquadSideEffect
import com.cohors.app.presentation.teamsquad.TeamSquadUiEvent
import com.cohors.app.presentation.teamsquad.TeamSquadViewModel
import com.cohors.app.ui.components.InjuryRow
import com.cohors.app.ui.components.ListSectionHeader
import com.cohors.app.ui.components.PlayerRow
import com.cohors.app.ui.components.PlayerStatus
import com.cohors.app.ui.components.PositionHeader
import com.cohors.app.ui.components.UiStateContent

/**
 * Squad detail screen: shows the team's squad grouped by position, and a
 * second tab with injuries/suspensions. Reuses [TeamSquadViewModel] — when
 * opened via navigation (deep link from the Leagues screen or directly),
 * [TeamSquadViewModel.loadTeamById] seeds the team and loads its data.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SquadScreen(
    teamId: Int,
    teamName: String,
    onBack: () -> Unit,
    onViewLineup: (teamId: Int, teamName: String) -> Unit,
    viewModel: TeamSquadViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(teamId) {
        viewModel.loadTeamById(teamId, teamName)
    }

    LaunchedEffect(Unit) {
        viewModel.sideEffect.collect { effect ->
            if (effect is TeamSquadSideEffect.ShowSnackbar) {
                snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    val injuredIds by remember(state.injuries) {
        derivedStateOf {
            (state.injuries as? UiState.Success)?.data?.injuries?.mapNotNull { it.playerId }?.toSet().orEmpty()
        }
    }
    val suspendedIds by remember(state.injuries) {
        derivedStateOf {
            (state.injuries as? UiState.Success)?.data?.suspensions?.mapNotNull { it.playerId }?.toSet().orEmpty()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.selectedTeam?.name ?: teamName) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                },
                actions = {
                    IconButton(onClick = { onViewLineup(teamId, state.selectedTeam?.name ?: teamName) }) {
                        Icon(Icons.Filled.SportsSoccer, contentDescription = "Muhtemel 11")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            val selectedIndex = if (state.selectedTab == SquadTab.SQUAD) 0 else 1
            TabRow(selectedTabIndex = selectedIndex) {
                Tab(
                    selected = selectedIndex == 0,
                    onClick = { viewModel.onEvent(TeamSquadUiEvent.OnTabSelected(SquadTab.SQUAD)) },
                    text = { Text("Kadro") }
                )
                Tab(
                    selected = selectedIndex == 1,
                    onClick = { viewModel.onEvent(TeamSquadUiEvent.OnTabSelected(SquadTab.INJURIES)) },
                    text = { Text("Sakat / Ceza") }
                )
            }

            Box(modifier = Modifier.fillMaxSize()) {
                when (state.selectedTab) {
                    SquadTab.SQUAD -> UiStateContent(
                        state = state.squadByPosition,
                        emptyMessage = "Bu takım için kadro bilgisi bulunamadı.",
                        onRetry = { viewModel.onEvent(TeamSquadUiEvent.OnRetry) }
                    ) { grouped ->
                        LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
                            grouped.forEach { (position, players) ->
                                if (players.isNotEmpty()) {
                                    item(key = "header_$position") { PositionHeader(position) }
                                    items(players, key = { it.id }) { player ->
                                        val status = when {
                                            suspendedIds.contains(player.id) -> PlayerStatus.SUSPENDED
                                            injuredIds.contains(player.id) -> PlayerStatus.INJURED
                                            else -> null
                                        }
                                        PlayerRow(player = player, status = status)
                                    }
                                }
                            }
                        }
                    }

                    SquadTab.INJURIES -> UiStateContent(
                        state = state.injuries,
                        emptyMessage = "Sakat veya cezalı oyuncu bulunmuyor.",
                        onRetry = { viewModel.onEvent(TeamSquadUiEvent.OnRetry) }
                    ) { result ->
                        LazyColumn(contentPadding = PaddingValues(bottom = 16.dp)) {
                            if (result.injuries.isNotEmpty()) {
                                item(key = "injuries_header") { ListSectionHeader("Sakatlıklar") }
                                items(result.injuries, key = { "inj_${it.playerId}_${it.playerName}" }) {
                                    InjuryRow(injury = it, status = PlayerStatus.INJURED)
                                }
                            }
                            if (result.suspensions.isNotEmpty()) {
                                item(key = "suspensions_header") { ListSectionHeader("Cezalılar") }
                                items(result.suspensions, key = { "sus_${it.playerId}_${it.playerName}" }) {
                                    InjuryRow(injury = it, status = PlayerStatus.SUSPENDED)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
