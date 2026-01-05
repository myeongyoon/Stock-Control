package com.mychoi.linencontrol.domain.repository

interface StockRepository {
    // 공백으로 구분된 숫자들의 합 구하는 로직
    fun calculateSum(input: String): Int

    // 현재 재고에서 나갈 수량 계산하는 로직
    fun calculateRequiredStock(current: Int, dailyOut: Int): Int
}