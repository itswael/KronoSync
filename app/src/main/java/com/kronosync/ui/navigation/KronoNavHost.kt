package com.kronosync.ui.navigation

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.kronosync.ui.schedule.ScheduleScreen
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.kronosync.ui.schedule.grid.ScheduleGridScreen
import com.kronosync.ui.analytics.AnalyticsTabScreen
import com.kronosync.ui.settings.SettingsScreen

private enum class TopDest(val route: String, val label: String, val icon: ImageVector) {
    Schedule("schedule", "Schedule", Icons.Outlined.Schedule),
    Analytics("analytics", "Analytics", Icons.Outlined.BarChart),
    Settings("settings", "Settings", Icons.Outlined.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KronoNavHost() {
    val navController: NavHostController = rememberNavController()
    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route ?: TopDest.Schedule.route

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("KronoSync") },
                colors = TopAppBarDefaults.topAppBarColors()
            )
        },
        bottomBar = {
            NavigationBar {
                TopDest.values().forEach { dest ->
                    NavigationBarItem(
                        selected = currentRoute == dest.route,
                        onClick = { navController.navigate(dest.route) },
                        icon = { Icon(dest.icon, contentDescription = dest.label) },
                        label = { Text(dest.label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = TopDest.Schedule.route
        ) {
            composable(TopDest.Schedule.route) { ScheduleScreen() }
            composable(TopDest.Analytics.route) { AnalyticsTabScreen() }
            composable(TopDest.Settings.route) { SettingsScreen() }
        }
    }
}
