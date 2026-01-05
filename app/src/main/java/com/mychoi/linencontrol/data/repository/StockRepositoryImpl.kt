package com.mychoi.linencontrol.data.repository

import com.mychoi.linencontrol.domain.repository.StockRepository

class StockRepositoryImpl : StockRepository {
    override fun calculateSum(input: String): Int {
        return input.split(" ")
            .filter { it.isNotBlank() }
            .mapNotNull { it.toIntOrNull() }
            .sum()
    }

    override fun calculateRequiredStock(current: Int, dailyOut: Int): Int {
        return current - dailyOut
    }

}