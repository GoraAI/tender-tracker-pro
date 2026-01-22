package com.tendertracker.app.ui.navigation

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.tendertracker.app.ui.screens.analytics.AnalyticsScreen
import com.tendertracker.app.ui.screens.auth.AuthViewModel
import com.tendertracker.app.ui.screens.auth.LoginScreen
import com.tendertracker.app.ui.screens.auth.RegisterScreen
import com.tendertracker.app.ui.screens.chat.ChatScreen
import com.tendertracker.app.ui.screens.compliance.ComplianceScreen
import com.tendertracker.app.ui.screens.dashboard.DashboardScreen
import com.tendertracker.app.ui.screens.model.ModelManagerScreen
import com.tendertracker.app.ui.screens.pipeline.PipelineScreen
import com.tendertracker.app.ui.screens.settings.SettingsScreen
import com.tendertracker.app.ui.screens.tender.TenderDetailScreen
import com.tendertracker.app.ui.screens.tender.TenderListScreen
import com.tendertracker.app.ui.theme.GlassBorder
import com.tendertracker.app.ui.theme.GlassCardBackground
import com.tendertracker.app.ui.theme.GlassPrimary
import com.tendertracker.app.ui.theme.GlassTextSecondary
import com.tendertracker.app.ui.theme.glassBackground

data class BottomNavItem(
    val screen: Screen,
    val icon: ImageVector,
    val label: String
)

@Composable
fun TenderTrackerNavHost(
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val authState by authViewModel.state.collectAsState()
    val navController = rememberNavController()
    val navItems = listOf(
        BottomNavItem(Screen.Dashboard, Icons.Default.Home, "Home"),
        BottomNavItem(Screen.TenderList, Icons.Default.List, "Tenders"),
        BottomNavItem(Screen.Chat(), Icons.AutoMirrored.Filled.Chat, "Chat"),
        BottomNavItem(Screen.Settings, Icons.Default.Settings, "Settings")
    )

    // Determine start destination based on auth state
    val startDestination: Screen = if (authState.isLoggedIn) Screen.Dashboard else Screen.Login

    // Apply glass background to entire scaffold
    Box(
        modifier = Modifier
            .fillMaxSize()
            .glassBackground()
    ) {
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.fillMaxSize()
        ) {
            // Auth screens (no bottom nav)
            composable<Screen.Login> {
                LoginScreen(
                    onNavigateToRegister = {
                        navController.navigate(Screen.Register)
                    },
                    onLoginSuccess = {
                        navController.navigate(Screen.Dashboard) {
                            popUpTo(Screen.Login) { inclusive = true }
                        }
                    }
                )
            }

            composable<Screen.Register> {
                RegisterScreen(
                    onNavigateToLogin = {
                        navController.popBackStack()
                    },
                    onRegisterSuccess = {
                        navController.navigate(Screen.Dashboard) {
                            popUpTo(Screen.Login) { inclusive = true }
                        }
                    }
                )
            }

            // Dashboard - home screen
            composable<Screen.Dashboard> {
                MainScaffold(
                    navController = navController,
                    navItems = navItems,
                    currentScreen = Screen.Dashboard
                ) {
                    DashboardScreen(
                        onTenderClick = { tenderId ->
                            navController.navigate(Screen.TenderDetail(tenderId))
                        },
                        onViewAllTenders = {
                            navController.navigate(Screen.TenderList) {
                                launchSingleTop = true
                            }
                        },
                        onChatClick = {
                            navController.navigate(Screen.Chat()) {
                                launchSingleTop = true
                            }
                        },
                        onModelManagerClick = {
                            navController.navigate(Screen.ModelManager)
                        },
                        onAnalyticsClick = {
                            navController.navigate(Screen.Analytics)
                        },
                        onComplianceClick = {
                            navController.navigate(Screen.Compliance)
                        },
                        onPipelineClick = {
                            navController.navigate(Screen.Pipeline)
                        }
                    )
                }
            }

            // Tender List screen
            composable<Screen.TenderList> {
                MainScaffold(
                    navController = navController,
                    navItems = navItems,
                    currentScreen = Screen.TenderList
                ) {
                    TenderListScreen(
                        onTenderClick = { tenderId ->
                            navController.navigate(Screen.TenderDetail(tenderId))
                        }
                    )
                }
            }

            composable<Screen.TenderDetail> { backStackEntry ->
                val detail: Screen.TenderDetail = backStackEntry.toRoute()
                TenderDetailScreen(
                    tenderId = detail.tenderId,
                    onBackClick = { navController.popBackStack() },
                    onNavigateToChat = { tenderId ->
                        navController.navigate(Screen.Chat(tenderId = tenderId))
                    }
                )
            }

            composable<Screen.Chat> { backStackEntry ->
                val chat: Screen.Chat = backStackEntry.toRoute()
                MainScaffold(
                    navController = navController,
                    navItems = navItems,
                    currentScreen = Screen.Chat(tenderId = null)
                ) {
                    ChatScreen(tenderId = chat.tenderId)
                }
            }

            composable<Screen.Settings> {
                MainScaffold(
                    navController = navController,
                    navItems = navItems,
                    currentScreen = Screen.Settings
                ) {
                    SettingsScreen(
                        onModelManagerClick = {
                            navController.navigate(Screen.ModelManager)
                        },
                        onLogoutClick = {
                            authViewModel.onLogout()
                            navController.navigate(Screen.Login) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    )
                }
            }

            composable<Screen.ModelManager> {
                ModelManagerScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }

            // Visualization screens
            composable<Screen.Analytics> {
                AnalyticsScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable<Screen.Compliance> {
                ComplianceScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable<Screen.Pipeline> {
                PipelineScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onTenderClick = { tenderId ->
                        navController.navigate(Screen.TenderDetail(tenderId))
                    }
                )
            }
        }
    }
}

@Composable
private fun MainScaffold(
    navController: androidx.navigation.NavController,
    navItems: List<BottomNavItem>,
    currentScreen: Screen,
    content: @Composable () -> Unit
) {
    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            // Glassmorphism Navigation Bar
            NavigationBar(
                modifier = Modifier
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .border(
                        width = 1.dp,
                        color = GlassBorder,
                        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                    ),
                containerColor = GlassCardBackground,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                navItems.forEach { item ->
                    // Compare using the route string pattern for type-safe navigation
                    val itemRoute = item.screen::class.qualifiedName
                    val selected = currentDestination?.route?.contains(itemRoute ?: "") == true

                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        selected = selected,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = GlassPrimary,
                            selectedTextColor = GlassPrimary,
                            unselectedIconColor = GlassTextSecondary,
                            unselectedTextColor = GlassTextSecondary,
                            indicatorColor = GlassPrimary.copy(alpha = 0.2f)
                        ),
                        onClick = {
                            navController.navigate(item.screen) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            content()
        }
    }
}
