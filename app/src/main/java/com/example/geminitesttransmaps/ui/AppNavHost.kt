package com.example.geminitesttransmaps.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.geminitesttransmaps.ui.home.HomeScreen
import com.example.geminitesttransmaps.ui.settings.SettingsScreen

@Composable
fun AppNavHost(
    navController: NavHostController = rememberNavController(),
    viewModel: MainViewModel = viewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    NavHost(
        navController = navController,
        startDestination = Routes.Home,
    ) {
        composable(Routes.Home) {
            HomeScreen(
                uiState = uiState,
                onSearchQueryChange = viewModel::onSearchQueryChange,
                onSearchActiveChange = viewModel::setSearchActive,
                onSearchSubmit = viewModel::onSearchSubmit,
                onStopSelected = viewModel::selectStop,
                onClearSelection = viewModel::clearSelection,
                onOpenSettings = { navController.navigate(Routes.Settings) },
            )
        }
        composable(Routes.Settings) {
            SettingsScreen(
                uiState = uiState,
                onImportGtfs = viewModel::importGtfs,
                onImportMbtiles = viewModel::setMbtilesUri,
                onBack = { navController.popBackStack() },
            )
        }
    }
}

private object Routes {
    const val Home = "home"
    const val Settings = "settings"
}
