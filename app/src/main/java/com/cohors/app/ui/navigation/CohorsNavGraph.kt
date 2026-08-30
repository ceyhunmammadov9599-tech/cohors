package com.cohors.app.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.cohors.app.ui.navigation.Routes.LEAGUES
import com.cohors.app.ui.navigation.Routes.LINEUP
import com.cohors.app.ui.navigation.Routes.SPLASH
import com.cohors.app.ui.navigation.Routes.SQUAD
import com.cohors.app.ui.screens.leagues.LeaguesScreen
import com.cohors.app.ui.screens.lineup.LineupScreen
import com.cohors.app.ui.screens.splash.SplashScreen
import com.cohors.app.ui.screens.squad.SquadScreen

object Routes {
    const val SPLASH = "splash"
    const val LEAGUES = "leagues"
    const val SQUAD = "squad/{teamId}?teamName={teamName}"
    const val LINEUP = "lineup/{teamId}?teamName={teamName}"

    fun squad(teamId: Int, teamName: String) = "squad/$teamId?teamName=${Uri.encode(teamName)}"
    fun lineup(teamId: Int, teamName: String) = "lineup/$teamId?teamName=${Uri.encode(teamName)}"
}

@Composable
fun CohorsNavGraph(navController: NavHostController = rememberNavController()) {
    NavHost(navController = navController, startDestination = SPLASH) {
        composable(SPLASH) {
            SplashScreen(
                onFinished = {
                    navController.navigate(LEAGUES) {
                        popUpTo(SPLASH) { inclusive = true }
                    }
                }
            )
        }

        composable(LEAGUES) {
            LeaguesScreen(
                onTeamSelected = { teamId, teamName ->
                    navController.navigate(Routes.squad(teamId, teamName))
                }
            )
        }

        composable(
            route = SQUAD,
            arguments = listOf(
                navArgument("teamId") { type = NavType.IntType },
                navArgument("teamName") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val teamId = backStackEntry.arguments?.getInt("teamId") ?: -1
            val teamName = backStackEntry.arguments?.getString("teamName").orEmpty()
            SquadScreen(
                teamId = teamId,
                teamName = teamName,
                onBack = { navController.popBackStack() },
                onViewLineup = { id, name -> navController.navigate(Routes.lineup(id, name)) }
            )
        }

        composable(
            route = LINEUP,
            arguments = listOf(
                navArgument("teamId") { type = NavType.IntType },
                navArgument("teamName") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val teamId = backStackEntry.arguments?.getInt("teamId") ?: -1
            val teamName = backStackEntry.arguments?.getString("teamName").orEmpty()
            LineupScreen(
                teamId = teamId,
                teamName = teamName,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
