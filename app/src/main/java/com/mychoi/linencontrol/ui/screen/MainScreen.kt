package com.mychoi.linencontrol.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mychoi.linencontrol.ui.viewmodel.MainViewModel
import com.mychoi.linencontrol.ui.viewmodel.StockResult

@Composable
fun MainScreen(viewModel: MainViewModel = hiltViewModel()) {
    Column(modifier = Modifier.padding(16.dp)) {
        // --- 섹션 1: 총 재고 합산 (공백 구분) ---
        Text("총 재고 합산 (띄어쓰기로 입력)", style = MaterialTheme.typography.titleMedium)
        TextField(
            value = viewModel.rawInputText,
            onValueChange = { viewModel.rawInputText = it },
            placeholder = { Text("예: 10 20 30") }
        )
        Button(onClick = { viewModel.calculateTotalSum() }) {
            Text("합산하기")
        }
        Text("현재 총 재고: ${viewModel.totalStockSum}")

        Spacer(modifier = Modifier.height(32.dp))

        // --- 섹션 2: 재고 차감 계산 ---
        Text("재고 차감 계산", style = MaterialTheme.typography.titleMedium)
        TextField(
            value = viewModel.currentStockInput,
            onValueChange = { viewModel.currentStockInput = it },
            label = { Text("현재고") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        TextField(
            value = viewModel.dailyOutInput,
            onValueChange = { viewModel.dailyOutInput = it },
            label = { Text("출고예정") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        Button(onClick = { viewModel.calculateStockGap() }) {
            Text("계산하기")
        }

        // 결과 표시 로직
        when (val result = viewModel.calculationResult) {
            is StockResult.Sufficient -> Text("여유: ${result.remaining}개 남음")
            is StockResult.Shortage -> Text("부족: ${result.required}개 더 필요함", color = Color.Red)
            else -> {}
        }
    }
}