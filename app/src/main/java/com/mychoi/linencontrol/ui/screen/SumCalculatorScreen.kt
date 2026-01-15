package com.mychoi.linencontrol.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mychoi.linencontrol.domain.model.StockItem
import com.mychoi.linencontrol.ui.viewmodel.SumCalculatorViewModel

@Composable
fun SumCalculatorScreen(
    viewModel: SumCalculatorViewModel = hiltViewModel(),
    onNavigationBack: () -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("항목별 합산 계산기", style = MaterialTheme.typography.headlineSmall)

        TextButton(onClick = { viewModel.clearAllInputs() }) {
            Text("전체 비우기", color = androidx.compose.ui.graphics.Color.Red)
        }

        // ViewModel의 리스트를 순회하며 UI 생성
        viewModel.stockItems.forEach { item ->
            StockInputRow(
                item = item,
                onValueChange = { newValue ->
                    viewModel.onInputChanged(item.id, newValue)
                }
            )
        }
    }
}

@Composable
fun StockInputRow(item: StockItem, onValueChange: (String) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = item.input,
                onValueChange = onValueChange,
                label = { Text("숫자 입력 (comma split)") },
                modifier = Modifier.weight(1f),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "합계: ${item.result}",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
        }
    }
}