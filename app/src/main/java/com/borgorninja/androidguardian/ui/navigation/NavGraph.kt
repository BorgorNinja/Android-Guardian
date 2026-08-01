package com.borgorninja.androidguardian.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.borgorninja.androidguardian.ui.components.ShizukuStatusBanner
import com.borgorninja.androidguardian.ui.screens.AppListScreen
import com.borgorninja.androidguardian.ui.screens.DashboardScreen
import com.borgorninja.androidguardian.ui.viewmodel.MainViewModel

private sealed class Destination(val route: String, val label: String) {
    data object Apps : Destination("apps", "Apps")
    data object Dashboard : Destination("dashboard", "Dashboard")
}

@Composable
fun AndroidGuardianNavGraph(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        bottomBar = {
            NavigationBar {
                val backStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = backStackEntry?.destination

                listOf(Destination.Apps, Destination.Dashboard).forEach { dest ->
                    val selected = currentDestination?.hierarchy?.any { it.route == dest.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(dest.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                if (dest == Destination.Apps) Icons.Filled.Apps else Icons.Filled.Speed,
                                contentDescription = dest.label
                            )
                        },
                        label = { Text(dest.label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Destination.Apps.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Destination.Apps.route) {
                androidx.compose.foundation.layout.Column {
                    ShizukuStatusBanner(
                        state = uiState.shizukuState,
                        onRequestPermission = viewModel::onRequestPermission,
                        onRetry = viewModel::onRefreshShizukuState
                    )
                    AppListScreen(
                        apps = uiState.filteredApps,
                        isLoading = uiState.isLoadingApps,
                        searchQuery = uiState.searchQuery,
                        filter = uiState.filter,
                        selectedPackage = uiState.selectedPackage,
                        onSearchQueryChanged = viewModel::onSearchQueryChanged,
                        onFilterChanged = viewModel::onFilterChanged,
                        onAppSelected = { pkg ->
                            viewModel.onAppSelected(pkg)
                            navController.navigate(Destination.Dashboard.route) {
                                launchSingleTop = true
                            }
                        }
                    )
                }
            }
            composable(Destination.Dashboard.route) {
                DashboardScreen(
                    selectedApp = uiState.apps.find { it.packageName == uiState.selectedPackage },
                    isCompiling = uiState.isCompiling,
                    consoleLog = uiState.consoleLog,
                    onOptimize = viewModel::optimizeSelectedApp,
                    onReset = viewModel::resetCompilation,
                    onCancel = viewModel::cancelRunningCommand
                )
            }
        }
    }
}
