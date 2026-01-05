package com.mychoi.linencontrol.ui.navigation

sealed class Routes(val route: String) {
    // 홈 화면(시작 화면)
    data object Home: Routes("home")
    // 재고 조사 화면
    data object SumCalculator: Routes("sum_calculator")
    // 오전 린넨 투입 계산 화면
    data object StockCalculator: Routes("stock_calculator")
}