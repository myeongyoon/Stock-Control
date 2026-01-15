package com.mychoi.linencontrol.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.mychoi.linencontrol.ui.screen.HomeScreen

@Composable
fun AppNavigation(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Routes.Home.route
    ) {
        // 홈 화면
        composable(route = Routes.Home.route) {
            HomeScreen(
                onNavigateToSumCalculator = {
                    navController.navigate(Routes.SumCalculator.route)
                },
                onNavigateToStockCalculator = {
                    navController.navigate(Routes.StockCalculator.route)
                }
            )
        }
        /*
        // 숫자 합산 계산 화면
        composable(route = Routes.SumCalculator.route) {
            SumCalculatorScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // 재고 차감 계산 화면
        composable(route = Routes.StockCalculator.route) {
            StockCalculatorScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
         */
    }
}