package com.onfaa.tunnel

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.onfaa.tunnel.domain.model.UserPreferences
import com.onfaa.tunnel.feature.appearance.AppearanceScreen
import com.onfaa.tunnel.feature.configs.ConfigsScreen
import com.onfaa.tunnel.feature.home.HomeScreen
import com.onfaa.tunnel.feature.logs.LogsScreen
import com.onfaa.tunnel.feature.settings.AdvancedModesScreen
import com.onfaa.tunnel.feature.settings.AntiCensorshipSettingsScreen
import com.onfaa.tunnel.feature.settings.AboutScreen
import com.onfaa.tunnel.feature.settings.AppFilterSettingsScreen
import com.onfaa.tunnel.feature.settings.AutomationSettingsScreen
import com.onfaa.tunnel.feature.settings.ConnectionSettingsScreen
import com.onfaa.tunnel.feature.settings.DiagnosticsSettingsScreen
import com.onfaa.tunnel.feature.settings.DnsSettingsScreen
import com.onfaa.tunnel.feature.settings.HotspotSettingsScreen
import com.onfaa.tunnel.feature.settings.NetworkEngineScreen
import com.onfaa.tunnel.feature.settings.RoutingSettingsScreen
import com.onfaa.tunnel.feature.settings.SettingsRoutes
import com.onfaa.tunnel.feature.settings.SettingsScreen
import com.onfaa.tunnel.onfaaAppContainer
import com.onfaa.tunnel.ui.components.OnfaaBackground
import com.onfaa.tunnel.ui.components.OnfaaBottomBar
import com.onfaa.tunnel.ui.components.OnfaaNavItem
import com.onfaa.tunnel.ui.theme.OnfaaTheme
import com.onfaa.tunnel.ui.theme.OnfaaTunnelTheme
import com.onfaa.tunnel.ui.theme.ThemeMode
import com.onfaa.tunnel.ui.theme.ThemePaletteId
import com.onfaa.tunnel.ui.theme.ThemeSettings

private const val ROUTE_HOME = "home"
private const val ROUTE_CONFIGS = "configs"
private const val ROUTE_SETTINGS = "settings"
private const val ROUTE_LOGS = "logs"

private val bottomItems = listOf(
    OnfaaNavItem(ROUTE_HOME, "Connexion", Icons.Filled.Shield),
    OnfaaNavItem(ROUTE_CONFIGS, "Configs", Icons.Filled.Layers),
    OnfaaNavItem(ROUTE_SETTINGS, "Paramètres", Icons.Filled.Settings),
    OnfaaNavItem(ROUTE_LOGS, "Journaux", Icons.Filled.Description)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            OnfaaTunnelAppRoot()
        }
    }
}

@Composable
fun OnfaaTunnelAppRoot() {
    val context = LocalContext.current
    val repository = context.onfaaAppContainer.userPreferencesRepository
    val prefs by repository.preferences.collectAsStateWithLifecycle(
        initialValue = UserPreferences()
    )
    val settings = ThemeSettings(
        paletteId = ThemePaletteId.fromKey(prefs.themeKey),
        mode = ThemeMode.fromKey(prefs.themeMode)
    )

    OnfaaTunnelTheme(settings = settings) {
        val palette = OnfaaTheme.colors
        val view = LocalView.current
        val activity = context as? Activity

        SideEffect {
            if (activity != null) {
                val controller = WindowCompat.getInsetsController(activity.window, view)
                controller.isAppearanceLightStatusBars = !palette.isDark
                controller.isAppearanceLightNavigationBars = !palette.isDark
            }
        }

        OnfaaBackground {
            val navController = rememberNavController()
            val backStackEntry by navController.currentBackStackEntryAsState()
            val route = backStackEntry?.destination?.route
            val selectedTab = when {
                route == ROUTE_HOME -> ROUTE_HOME
                route == ROUTE_CONFIGS -> ROUTE_CONFIGS
                route == ROUTE_LOGS -> ROUTE_LOGS
                route == ROUTE_SETTINGS ||
                    route?.startsWith("$ROUTE_SETTINGS/") == true ||
                    route == SettingsRoutes.APPEARANCE -> ROUTE_SETTINGS
                else -> null
            }

            Scaffold(
                containerColor = Color.Transparent,
                bottomBar = {
                    OnfaaBottomBar(
                        items = bottomItems,
                        selectedRoute = selectedTab,
                        onNavigate = { destination ->
                            navController.navigateTopLevel(destination)
                        }
                    )
                }
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = ROUTE_HOME,
                    modifier = Modifier.padding(innerPadding)
                ) {
                    composable(ROUTE_HOME) {
                        HomeScreen(
                            onOpenConfigs = {
                                navController.navigateTopLevel(ROUTE_CONFIGS)
                            },
                            onOpenSettings = {
                                navController.navigateTopLevel(ROUTE_SETTINGS)
                            }
                        )
                    }
                    composable(ROUTE_CONFIGS) {
                        ConfigsScreen(
                            onBack = { navController.backToHome() }
                        )
                    }
                    composable(ROUTE_SETTINGS) {
                        SettingsScreen(
                            onBack = { navController.backToHome() },
                            onNavigate = { destination ->
                                if (destination == ROUTE_LOGS) {
                                    navController.navigateTopLevel(ROUTE_LOGS)
                                } else {
                                    navController.navigate(destination) { launchSingleTop = true }
                                }
                            }
                        )
                    }
                    composable(ROUTE_LOGS) {
                        LogsScreen(onBack = { navController.backToHome() })
                    }
                    composable(SettingsRoutes.APPEARANCE) {
                        AppearanceScreen(onBack = { navController.popBackStackOrSettings() })
                    }
                    composable(SettingsRoutes.CONNECTION) {
                        ConnectionSettingsScreen(onBack = { navController.popBackStackOrSettings() })
                    }
                    composable(SettingsRoutes.ANTI_CENSORSHIP) {
                        AntiCensorshipSettingsScreen(onBack = { navController.popBackStackOrSettings() })
                    }
                    composable(SettingsRoutes.ADVANCED_MODES) {
                        AdvancedModesScreen(
                            onBack = { navController.popBackStackOrSettings() },
                            onOpenNetwork = {
                                navController.navigate(SettingsRoutes.NETWORK_ENGINE) {
                                    launchSingleTop = true
                                }
                            },
                            onOpenHotspot = {
                                navController.navigate(SettingsRoutes.HOTSPOT) {
                                    launchSingleTop = true
                                }
                            }
                        )
                    }
                    composable(SettingsRoutes.DNS) {
                        DnsSettingsScreen(onBack = { navController.popBackStackOrSettings() })
                    }
                    composable(SettingsRoutes.ROUTING) {
                        RoutingSettingsScreen(
                            onBack = { navController.popBackStackOrSettings() },
                            onOpenAppFilter = {
                                navController.navigate(SettingsRoutes.APP_FILTER) {
                                    launchSingleTop = true
                                }
                            }
                        )
                    }
                    composable(SettingsRoutes.APP_FILTER) {
                        AppFilterSettingsScreen(onBack = { navController.popBackStackOrSettings() })
                    }
                    composable(SettingsRoutes.NETWORK_ENGINE) {
                        NetworkEngineScreen(onBack = { navController.popBackStackOrSettings() })
                    }
                    composable(SettingsRoutes.HOTSPOT) {
                        HotspotSettingsScreen(onBack = { navController.popBackStackOrSettings() })
                    }
                    composable(SettingsRoutes.AUTOMATION) {
                        AutomationSettingsScreen(onBack = { navController.popBackStackOrSettings() })
                    }
                    composable(SettingsRoutes.DIAGNOSTICS) {
                        DiagnosticsSettingsScreen(
                            onBack = { navController.popBackStackOrSettings() },
                            onOpenLogs = { navController.navigateTopLevel(ROUTE_LOGS) }
                        )
                    }
                    composable(SettingsRoutes.ABOUT) {
                        AboutScreen(onBack = { navController.popBackStackOrSettings() })
                    }
                }
            }
        }
    }
}

private fun NavHostController.navigateTopLevel(route: String) {
    navigate(route) {
        popUpTo(ROUTE_HOME) { saveState = true }
        launchSingleTop = true
        // Reopening Paramètres must show the overview, not the last sub-screen.
        restoreState = route != ROUTE_SETTINGS
    }
}

private fun NavHostController.backToHome() {
    if (!popBackStack(ROUTE_HOME, false)) {
        navigate(ROUTE_HOME) {
            launchSingleTop = true
        }
    }
}

private fun NavHostController.popBackStackOrSettings() {
    if (!popBackStack()) {
        navigate(ROUTE_SETTINGS) {
            launchSingleTop = true
        }
    }
}
