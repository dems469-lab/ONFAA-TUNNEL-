package com.onfaa.tunnel

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.onfaa.tunnel.feature.configs.ConfigsScreen
import com.onfaa.tunnel.feature.home.HomeScreen
import com.onfaa.tunnel.feature.logs.LogsScreen
import com.onfaa.tunnel.feature.settings.SettingsScreen
import com.onfaa.tunnel.feature.stats.StatsScreen
import com.onfaa.tunnel.ui.theme.OnfaaTunnelTheme

sealed class Dest(val route: String, val labelRes: Int) {
    data object Home : Dest("home", R.string.nav_home)
    data object Configs : Dest("configs", R.string.nav_configs)
    data object Stats : Dest("stats", R.string.nav_stats)
    data object Logs : Dest("logs", R.string.nav_logs)
    data object Settings : Dest("settings", R.string.nav_settings)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OnfaaTunnelTheme {
                OnfaaTunnelAppRoot()
            }
        }
    }
}

@Composable
fun OnfaaTunnelAppRoot() {
    val navController = rememberNavController()
    val items = listOf(Dest.Home, Dest.Configs, Dest.Stats, Dest.Logs, Dest.Settings)

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                items.forEach { dest ->
                    val selected = currentDestination?.hierarchy?.any { it.route == dest.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(dest.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = when (dest) {
                                    Dest.Home -> Icons.Filled.Home
                                    Dest.Configs -> Icons.Filled.List
                                    Dest.Stats -> Icons.Filled.ShowChart
                                    Dest.Logs -> Icons.Filled.Description
                                    Dest.Settings -> Icons.Filled.Settings
                                },
                                contentDescription = stringResource(dest.labelRes)
                            )
                        },
                        label = { Text(stringResource(dest.labelRes)) }
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Dest.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Dest.Home.route) { HomeScreen() }
            composable(Dest.Configs.route) { ConfigsScreen() }
            composable(Dest.Stats.route) { StatsScreen() }
            composable(Dest.Logs.route) { LogsScreen() }
            composable(Dest.Settings.route) { SettingsScreen() }
        }
    }
}
