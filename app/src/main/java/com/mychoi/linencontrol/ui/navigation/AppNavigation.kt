package com.mychoi.linencontrol.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.mychoi.linencontrol.ui.screen.HomeScreen
import com.mychoi.linencontrol.ui.screen.StockCalculatorScreen
import com.mychoi.linencontrol.ui.screen.StockHistoryScreen
import com.mychoi.linencontrol.ui.screen.SumCalculatorScreen

@Composable
fun AppNavigation(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Routes.Home.route
    ) {
        composable(route = Routes.Home.route) {
            HomeScreen(
                onNavigateToSumCalculator = { navController.navigate(Routes.SumCalculator.route) },
                onNavigateToStockCalculator = { navController.navigate(Routes.StockCalculator.route) },
                onNavigateToHistory = { navController.navigate(Routes.StockHistory.route) }
            )
        }

        composable(route = Routes.SumCalculator.route) {
            SumCalculatorScreen(onNavigationBack = { navController.popBackStack() })
        }

        composable(route = Routes.StockCalculator.route) {
            StockCalculatorScreen(onNavigationBack = { navController.popBackStack() })
        }

        composable(route = Routes.StockHistory.route) {
            StockHistoryScreen(onNavigationBack = { navController.popBackStack() })
        }
    }
}
