package com.cohors.app.ui.screens.lineup

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cohors.app.domain.model.LineupPlayer
import com.cohors.app.presentation.matchlineup.MatchLineupSideEffect
import com.cohors.app.presentation.matchlineup.MatchLineupUiEvent
import com.cohors.app.presentation.matchlineup.MatchLineupViewModel
import com.cohors.app.ui.components.FixtureHeader
import com.cohors.app.ui.components.PlayerInfoDialog
import com.cohors.app.ui.components.SubstitutesRow
import com.cohors.app.ui.components.TacticalPitchCanvas
import com.cohors.app.ui.components.UiStateContent

/**
 * Tactical pitch screen: resolves the team's upcoming/live fixture and
 * draws its starting XI on a Canvas-rendered football pitch, with
 * clickable player pins and a substitutes strip below.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LineupScreen(
    teamId: Int,
    teamName: String,
    onBack: () -> Unit,
    viewModel: MatchLineupViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedPlayer by remember { mutableStateOf<LineupPlayer?>(null) }

    LaunchedEffect(teamId) {
        viewModel.onEvent(MatchLineupUiEvent.LoadForTeam(teamId))
    }

    LaunchedEffect(Unit) {
        viewModel.sideEffect.collect { effect ->
            if (effect is MatchLineupSideEffect.ShowSnackbar) {
                snackbarHostState.showSnackbar(effect.message)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("$teamName • Muhtemel 11") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Geri")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            UiStateContent(
                state = state.matchLineup,
                emptyMessage = "Bu takım için ilk 11 dizilişi henüz yayınlanmadı.",
                onRetry = { viewModel.onEvent(MatchLineupUiEvent.OnRetry) }
            ) { result ->
                val myLineup = result.lineups.firstOrNull { it.teamId == teamId }
                    ?: result.lineups.firstOrNull()

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    FixtureHeader(fixture = result.fixture)

                    if (myLineup != null) {
                        Text(
                            text = "Diziliş: ${myLineup.formation ?: "Bilinmiyor"}",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                        TacticalPitchCanvas(
                            lineup = myLineup,
                            onPlayerClick = { selectedPlayer = it },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        )
                        SubstitutesRow(substitutes = myLineup.substitutes)
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "İlk 11 dizilişi henüz yayınlanmadı.",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }

        selectedPlayer?.let { player ->
            PlayerInfoDialog(player = player, onDismiss = { selectedPlayer = null })
        }
    }
}
