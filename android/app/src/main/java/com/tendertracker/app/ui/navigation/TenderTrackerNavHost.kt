package com.tendertracker.app.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
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
import androidx.navigation.toRoute
import com.tendertracker.app.ui.screens.chat.ChatScreen
import com.tendertracker.app.ui.screens.settings.SettingsScreen
import com.tendertracker.app.ui.screens.model.ModelManagerScreen
import com.tendertracker.app.ui.screens.tender.TenderDetailScreen
import com.tendertracker.app.ui.screens.tender.TenderListScreen

data class BottomNavItem(
    val screen: Screen,
    val icon: ImageVector,
    val label: String
)

@Composable
fun TenderTrackerNavHost() {
    val navController = rememberNavController()
    val navItems = listOf(
        BottomNavItem(Screen.TenderList, Icons.Default.List, "Tenders"),
        BottomNavItem(Screen.Chat, Icons.Default.Chat, "Chat"),
        BottomNavItem(Screen.Settings, Icons.Default.Settings, "Settings")
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination

                navItems.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        selected = currentDestination?.hierarchy?.any {
                            it.route == item.screen::class.qualifiedName
                        } == true,
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
        NavHost(
            navController = navController,
            startDestination = Screen.TenderList,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable<Screen.TenderList> {
                TenderListScreen(
                    onTenderClick = { tenderId ->
                        navController.navigate(Screen.TenderDetail(tenderId))
                    }
                )
            }

            composable<Screen.TenderDetail> { backStackEntry ->
                val detail: Screen.TenderDetail = backStackEntry.toRoute()
                TenderDetailScreen(
                    tenderId = detail.tenderId,
                    onBackClick = { navController.popBackStack() }
                )
            }

            composable<Screen.Chat> {
                ChatScreen()
            }

            composable<Screen.Settings> {
                SettingsScreen(
                    onModelManagerClick = {
                        navController.navigate(Screen.ModelManager)
                    }
                )
            }

            composable<Screen.ModelManager> {
                ModelManagerScreen(
                    onBackClick = { navController.popBackStack() }
                )
            }
        }
    }
}
