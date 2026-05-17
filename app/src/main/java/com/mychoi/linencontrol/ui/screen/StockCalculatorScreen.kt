package com.mychoi.linencontrol.ui.screen

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mychoi.linencontrol.ui.components.CameraScreen
import com.mychoi.linencontrol.ui.viewmodel.CalculatorStep
import com.mychoi.linencontrol.ui.viewmodel.SaveState
import com.mychoi.linencontrol.ui.viewmodel.StockCalculationResult
import com.mychoi.linencontrol.ui.viewmodel.StockCalculatorViewModel

private val BUILDINGS = listOf("A", "B", "C", "D", "E", "F")

@Composable
fun StockCalculatorScreen(
    onNavigationBack: () -> Unit,
    viewModel: StockCalculatorViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    when (val step = uiState.step) {
        is CalculatorStep.SelectBuilding -> {
            BuildingSelectionScreen(
                onBack = onNavigationBack,
                onBuildingSelected = viewModel::selectBuilding
            )
        }

        is CalculatorStep.CaptureInventorySheet -> {
            CameraScreen(
                title = "${step.building}동 재고 시트 촬영",
                onImageCaptured = viewModel::onInventorySheetCaptured,
                onBack = { viewModel.reset() }
            )
        }

        is CalculatorStep.ConfirmInventorySheet -> {
            MultiInventoryConfirmScreen(
                building = step.building,
                bitmaps = step.bitmaps,
                onConfirm = viewModel::confirmInventorySheet,
                onRetakeAll = viewModel::retakeInventorySheet,
                onAddMore = viewModel::addMoreInventorySheet,
                onRemove = viewModel::removeInventorySheet
            )
        }

        is CalculatorStep.AddMoreInventorySheet -> {
            CameraScreen(
                title = "${step.building}동 재고 시트 추가 촬영 (${step.existingBitmaps.size + 1}번째)",
                onImageCaptured = viewModel::onMoreInventorySheetCaptured,
                onBack = viewModel::backFromAddMore
            )
        }

        is CalculatorStep.CaptureRoomLog -> {
            CameraScreen(
                title = "${step.building}동 객실 관리일지 촬영",
                onImageCaptured = viewModel::onRoomLogCaptured,
                onBack = { viewModel.backFromRoomLog() }
            )
        }

        is CalculatorStep.ConfirmRoomLog -> {
            ImageCaptureConfirmScreen(
                title = "${step.building}동 객실 관리일지 확인",
                bitmap = step.roomLogBitmap,
                confirmLabel = "분석 시작",
                onConfirm = viewModel::confirmRoomLog,
                onRetake = viewModel::retakeRoomLog
            )
        }

        is CalculatorStep.Analyzing -> {
            AnalyzingScreen(
                isLoading = uiState.isLoading,
                errorMessage = uiState.errorMessage,
                onRetryInventory = viewModel::retryFromInventory,
                onRetryRoomLog = viewModel::retryRoomLogOnly,
                onReset = viewModel::reset
            )
        }

        is CalculatorStep.Result -> {
            ResultScreen(
                result = step.result,
                saveState = uiState.saveState,
                onSave = viewModel::saveResult,
                onReset = viewModel::reset,
                onBack = onNavigationBack
            )
        }
    }
}

// ─── 동 선택 ────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BuildingSelectionScreen(
    onBack: () -> Unit,
    onBuildingSelected: (String) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("재고 계산") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "동을 선택하세요",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "재고 계산을 진행할 동을 선택해주세요.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            BUILDINGS.chunked(3).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    row.forEach { building ->
                        BuildingCard(
                            building = building,
                            onClick = { onBuildingSelected(building) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    repeat(3 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun BuildingCard(
    building: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(
                text = "${building}동",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

// ─── 재고 시트 다중 확인 화면 ────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MultiInventoryConfirmScreen(
    building: String,
    bitmaps: List<Bitmap>,
    onConfirm: () -> Unit,
    onRetakeAll: () -> Unit,
    onAddMore: () -> Unit,
    onRemove: (Int) -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${building}동 재고 시트 확인") },
                navigationIcon = {
                    IconButton(onClick = onRetakeAll) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "처음부터")
                    }
                }
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onAddMore,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.AddAPhoto, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("층 추가 촬영")
                }
                Button(
                    onClick = onConfirm,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("다음 단계 (총 ${bitmaps.size}장)")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "촬영된 재고 시트 ${bitmaps.size}장",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                itemsIndexed(bitmaps) { index, bitmap ->
                    Box {
                        Card(
                            shape = RoundedCornerShape(8.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Image(
                                painter = remember(bitmap) { BitmapPainter(bitmap.asImageBitmap()) },
                                contentDescription = "${index + 1}번째 재고 시트",
                                modifier = Modifier.size(140.dp),
                                contentScale = ContentScale.Crop
                            )
                        }
                        // 삭제 버튼
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .size(24.dp)
                                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                .clickable { onRemove(index) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "삭제",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        // 번호 뱃지
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(4.dp)
                                .background(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${index + 1}층",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─── 이미지 확인 화면 ────────────────────────────────────────────────────────

@Composable
private fun ImageCaptureConfirmScreen(
    title: String,
    bitmap: Bitmap,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onRetake: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Image(
            painter = remember(bitmap) { BitmapPainter(bitmap.asImageBitmap()) },
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )

        // 상단 타이틀
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .align(Alignment.TopStart),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onRetake) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "뒤로",
                    tint = Color.White
                )
            }
            Text(
                text = title,
                color = Color.White,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        // 하단 버튼
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 40.dp, start = 24.dp, end = 24.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onRetake,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White)
            ) {
                Icon(
                    Icons.Default.CameraAlt,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text("재촬영")
            }
            Button(
                onClick = onConfirm,
                modifier = Modifier.weight(1f)
            ) {
                Text(confirmLabel)
            }
        }
    }
}

// ─── 분석 중 / 오류 ─────────────────────────────────────────────────────────

@Composable
private fun AnalyzingScreen(
    isLoading: Boolean,
    errorMessage: String?,
    onRetryInventory: () -> Unit,
    onRetryRoomLog: () -> Unit,
    onReset: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(64.dp))
                Text(
                    text = "Claude AI가 이미지를 분석 중입니다...",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
            } else if (errorMessage != null) {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(64.dp)
                )
                Text(
                    text = errorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onRetryRoomLog,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("객실 관리일지 재촬영") }
                    OutlinedButton(
                        onClick = onRetryInventory,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("재고 시트부터 다시") }
                    OutlinedButton(
                        onClick = onReset,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("처음부터") }
                }
            }
        }
    }
}

// ─── 결과 화면 ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ResultScreen(
    result: StockCalculationResult,
    saveState: SaveState,
    onSave: () -> Unit,
    onReset: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${result.building}동 재고 계산 결과") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "뒤로")
                    }
                }
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 저장 버튼
                Button(
                    onClick = onSave,
                    enabled = saveState is SaveState.Idle || saveState is SaveState.Error,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (saveState is SaveState.Saved)
                            MaterialTheme.colorScheme.secondary
                        else
                            MaterialTheme.colorScheme.primary
                    )
                ) {
                    when (saveState) {
                        is SaveState.Saving -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("저장 중...")
                        }
                        is SaveState.Saved -> {
                            Icon(Icons.Default.Check, contentDescription = null,
                                modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("저장됨")
                        }
                        is SaveState.Error -> {
                            Icon(Icons.Default.Save, contentDescription = null,
                                modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("다시 저장")
                        }
                        else -> {
                            Icon(Icons.Default.Save, contentDescription = null,
                                modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("저장")
                        }
                    }
                }
                // 오류 메시지
                if (saveState is SaveState.Error) {
                    Text(
                        text = saveState.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                // 다시 계산 버튼
                OutlinedButton(
                    onClick = onReset,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null,
                        modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("다시 계산")
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                RoomSummaryCard(
                    checkoutRoomCounts = result.checkoutRoomCounts,
                    stayoverRoomCounts = result.stayoverRoomCounts
                )
            }
            if (result.floorInventories.size > 1) {
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "층별 재고 현황",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                items(result.floorInventories) { floor ->
                    FloorInventoryCard(floorInventory = floor)
                }
            }
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "항목별 잔여 재고",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
            items(result.items) { item ->
                StockResultRow(item = item)
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun RoomSummaryCard(
    checkoutRoomCounts: Map<String, Int>,
    stayoverRoomCounts: Map<String, Int>
) {
    val checkoutChipColor = Color(0xFFFFF176)
    val stayoverChipColors = listOf(
        Color(0xFFFFAB91), Color(0xFFFFCC80), Color(0xFFF48FB1),
        Color(0xFFCE93D8), Color(0xFF80DEEA), Color(0xFFA5D6A7), Color(0xFF90CAF9)
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "퇴실 객실 (체크아웃)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (checkoutRoomCounts.isEmpty()) {
                Text(
                    text = "퇴실 객실 없음",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            } else {
                checkoutRoomCounts.entries.chunked(4).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEach { (type, count) ->
                            RoomCountChip(label = type, count = count, color = checkoutChipColor)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            if (stayoverRoomCounts.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "재실 객실",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.height(8.dp))
                stayoverRoomCounts.entries.chunked(4).forEachIndexed { chunkIdx, row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        row.forEachIndexed { idx, (type, count) ->
                            RoomCountChip(
                                label = type,
                                count = count,
                                color = stayoverChipColors[(chunkIdx * 4 + idx) % stayoverChipColors.size]
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun FloorInventoryCard(floorInventory: com.mychoi.linencontrol.ui.viewmodel.FloorInventory) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "${floorInventory.floor}층",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(6.dp))
            val labels = listOf(
                "한실이불피" to floorInventory.한실이불피,
                "요피" to floorInventory.요피,
                "한실베개피" to floorInventory.한실베개피,
                "양실이불피" to floorInventory.양실이불피,
                "시트피" to floorInventory.시트피,
                "양실베개피" to floorInventory.양실베개피,
                "FT" to floorInventory.ft,
                "BT" to floorInventory.bt,
                "걸레" to floorInventory.걸레
            ).filter { it.second > 0 }
            labels.chunked(3).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    row.forEach { (label, value) ->
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = value.toString(),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    repeat(3 - row.size) { Spacer(modifier = Modifier.weight(1f)) }
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun RoomCountChip(label: String, count: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .background(color, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}

@Composable
private fun StockResultRow(item: com.mychoi.linencontrol.ui.viewmodel.StockResultItem) {
    val isShortage = item.remaining < 0
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isShortage)
                MaterialTheme.colorScheme.errorContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = item.label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            StockNumberCell(label = "재고", value = item.current)
            Spacer(modifier = Modifier.width(8.dp))
            Text("−", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.width(8.dp))
            StockNumberCell(label = "사용", value = item.used)
            Spacer(modifier = Modifier.width(8.dp))
            Text("=", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = item.remaining.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isShortage) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "잔여",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StockNumberCell(label: String, value: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}