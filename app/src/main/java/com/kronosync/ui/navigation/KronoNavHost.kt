package com.kronosync.ui.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.kronosync.ui.schedule.ScheduleScreen
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.kronosync.ui.analytics.AnalyticsTabScreen
import com.kronosync.ui.settings.SettingsScreen

private enum class TopDest(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val icon: ImageVector
) {
    Schedule("schedule", "Schedule", Icons.Filled.Schedule, Icons.Outlined.Schedule),
    Analytics("analytics", "Analytics", Icons.Filled.BarChart, Icons.Outlined.BarChart),
    Settings("settings", "Settings", Icons.Filled.Settings, Icons.Outlined.Settings)
}

/**
 * Owns only the bottom navigation chrome. Each top-level screen owns its own
 * Scaffold/TopAppBar — keeping a top bar here too previously caused a double
 * app bar to render above every screen.
 */
@Composable
fun KronoNavHost(): NavHostController {
    val navController: NavHostController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route ?: TopDest.Schedule.route

    Scaffold(
        // No topBar here (each screen owns its own header), so Scaffold's default
        // contentWindowInsets would otherwise push the status-bar inset into `padding`
        // below — and each screen ALSO applies its own statusBarsPadding(), double-counting
        // it. Zero it out here; only the per-screen padding should apply it.
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            NavigationBar {
                TopDest.values().forEach { dest ->
                    val selected = currentRoute == dest.route
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(dest.route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(if (selected) dest.selectedIcon else dest.icon, contentDescription = dest.label) },
                        label = { Text(dest.label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = TopDest.Schedule.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(TopDest.Schedule.route) { ScheduleScreen() }
            composable(TopDest.Analytics.route) { AnalyticsTabScreen() }
            composable(TopDest.Settings.route) { SettingsScreen() }
        }
    }
    return navController
}
