package com.mychoi.linencontrol.ui.navigation

sealed class Routes(val route: String) {
    data object Home : Routes("home")
    data object SumCalculator : Routes("sum_calculator")
    data object StockCalculator : Routes("stock_calculator")
    data object StockHistory : Routes("stock_history")
}
