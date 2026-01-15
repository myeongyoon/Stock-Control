package com.mychoi.linencontrol.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.mychoi.linencontrol.domain.repository.StockRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: StockRepository
): ViewModel() {
    var rawInputText by mutableStateOf("")
    var totalStockSum by mutableIntStateOf(0)
        private set

    var currentStockInput by mutableStateOf("")
    var dailyOutInput by mutableStateOf("")
    var calculationResult by mutableStateOf<StockResult>(StockResult.Empty)
        private set

    fun calculateTotalSum() {
        totalStockSum = repository.calculateSum(rawInputText)
    }

    fun calculateStockGap() {
        val current = currentStockInput.toIntOrNull() ?: 0
        val out = dailyOutInput.toIntOrNull() ?: 0
        val gap = repository.calculateRequiredStock(current, out)

        calculationResult = when {
            gap < 0 -> StockResult.Shortage(Math.abs(gap))
            else -> StockResult.Sufficient(gap)
        }
    }
}

// 결과 상태를 명확히 표현하기 위한 Sealed Class
sealed class StockResult {
    object Empty : StockResult()
    data class Sufficient(val remaining: Int) : StockResult()
    data class Shortage(val required: Int) : StockResult()
}