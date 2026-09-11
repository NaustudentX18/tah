package app.tah.shell

import android.content.Intent
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
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
fun TahApp(deepLinkIntent: Intent? = null) {
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
        currentDestination?.hierarchy?.any { it.route?.startsWith(tab.route) == true } == true
    }

    LaunchedEffect(deepLinkIntent) {
        val data = deepLinkIntent?.data ?: return@LaunchedEffect
        if (data.scheme == "tah" && data.host == "session") {
            val id = data.pathSegments.firstOrNull() ?: return@LaunchedEffect
            val focus = data.getQueryParameter("focus").orEmpty()
            navController.navigate(TahDestinations.sessionDetail(id, focus))
        }
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    tabs.forEach { tab ->
                        val selected = currentDestination?.hierarchy?.any {
                            it.route?.startsWith(tab.route) == true
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
                    onNewRun = {
                        navController.navigate(TahDestinations.DISPATCH)
                    },
                )
            }
            composable(
                route = TahDestinations.SESSION_DETAIL,
                arguments = listOf(
                    navArgument("sessionId") { type = NavType.StringType },
                    navArgument("focus") {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                ),
                deepLinks = listOf(
                    navDeepLink { uriPattern = "tah://session/{sessionId}?focus={focus}" },
                    navDeepLink { uriPattern = "tah://session/{sessionId}" },
                ),
            ) { entry ->
                val id = entry.arguments?.getString("sessionId") ?: "unknown"
                val focus = entry.arguments?.getString("focus").orEmpty()
                SessionDetailScreen(
                    sessionId = id,
                    focusPermission = focus == "permission",
                    onBack = { navController.popBackStack() },
                )
            }
            composable(TahDestinations.DISPATCH) {
                DispatchScreen(
                    onStarted = { id ->
                        navController.navigate(TahDestinations.sessionDetail(id))
                    },
                    onConfigureProvider = {
                        navController.navigate(TahDestinations.PROVIDERS)
                    },
                )
            }
            composable(TahDestinations.PROVIDERS) { ProvidersScreen() }
            composable(TahDestinations.SKILLS) { SkillsMemoryScreen() }
            composable(TahDestinations.SETTINGS) { SettingsScreen() }
        }
    }
}
