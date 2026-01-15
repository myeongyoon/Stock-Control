package com.mychoi.linencontrol.ui.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import com.mychoi.linencontrol.domain.model.StockItem
import com.mychoi.linencontrol.domain.repository.StockRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SumCalculatorViewModel @Inject constructor(
    private val repository: StockRepository
): ViewModel() {

    var stockItems = mutableStateListOf<StockItem>().apply {
        if (isEmpty()) {
            repeat(13) { add(StockItem(id = it)) }
        }
    }

    fun onInputChanged(id: Int, newInput: String) {
        val index = stockItems.indexOfFirst { it.id == id }
        if (index != -1) {
            // 입력을 합산 로직으로 계산
            val calculatedSum = repository.calculateSum(newInput)
            // 객체 교체 (Data Class의 copy 사용)
            // mutableStateListOf는 요소 자체가 '교체'될 때 Recomposition을 일으킵니다.
            stockItems[index] = stockItems[index].copy(
                input = newInput,
                result = calculatedSum
            )
        }
    }
}