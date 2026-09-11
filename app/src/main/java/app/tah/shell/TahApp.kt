package app.tah.shell

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.tah.shell.navigation.TahDestinations
import app.tah.shell.ui.board.SessionBoardScreen
import app.tah.shell.ui.detail.SessionDetailScreen
import app.tah.shell.ui.dispatch.DispatchScreen
import app.tah.shell.ui.providers.ProvidersScreen
import app.tah.shell.ui.settings.SettingsScreen
import app.tah.shell.ui.skills.SkillsMemoryScreen

private data class TabItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

@Composable
fun TahApp() {
    val navController = rememberNavController()
    val tabs = listOf(
        TabItem(TahDestinations.BOARD, "Board", Icons.Filled.Dashboard),
        TabItem(TahDestinations.DISPATCH, "Dispatch", Icons.Filled.Send),
        TabItem(TahDestinations.PROVIDERS, "Providers", Icons.Filled.Storage),
        TabItem(TahDestinations.SKILLS, "Skills", Icons.Filled.Memory),
        TabItem(TahDestinations.SETTINGS, "Settings", Icons.Filled.Settings),
    )
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val showBottomBar = tabs.any { tab ->
        currentDestination?.hierarchy?.any { it.route == tab.route } == true
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    tabs.forEach { tab ->
                        val selected = currentDestination?.hierarchy?.any {
                            it.route == tab.route
                        } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = TahDestinations.BOARD,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(TahDestinations.BOARD) {
                SessionBoardScreen(
                    onOpenSession = { id ->
                        navController.navigate(TahDestinations.sessionDetail(id))
                    },
                )
            }
            composable(TahDestinations.SESSION_DETAIL) { entry ->
                val id = entry.arguments?.getString("sessionId") ?: "unknown"
                SessionDetailScreen(
                    sessionId = id,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(TahDestinations.DISPATCH) {
                DispatchScreen()
            }
            composable(TahDestinations.PROVIDERS) {
                ProvidersScreen()
            }
            composable(TahDestinations.SKILLS) {
                SkillsMemoryScreen()
            }
            composable(TahDestinations.SETTINGS) {
                SettingsScreen()
            }
        }
    }
}
