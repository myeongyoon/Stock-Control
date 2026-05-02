package com.mychoi.linencontrol.ui.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.mychoi.linencontrol.data.local.dao.StockSaveDao
import com.mychoi.linencontrol.data.local.entity.StockSaveEntity
import com.mychoi.linencontrol.data.remote.repository.ClaudeRepository
import com.mychoi.linencontrol.data.remote.repository.InventoryParseResult
import com.mychoi.linencontrol.data.remote.repository.RoomLogParseResult
import com.mychoi.linencontrol.util.ImageUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StockResultItem(
    val label: String,
    val current: Int,
    val used: Int,
    val remaining: Int
)

data class StockCalculationResult(
    val building: String,
    val roomCounts: Map<String, Int>,
    val items: List<StockResultItem>
)

sealed class SaveState {
    object Idle : SaveState()
    object Saving : SaveState()
    object Saved : SaveState()
    data class Error(val message: String) : SaveState()
}

sealed class CalculatorStep {
    object SelectBuilding : CalculatorStep()
    data class CaptureInventorySheet(val building: String) : CalculatorStep()
    data class ConfirmInventorySheet(val building: String, val bitmaps: List<Bitmap>) : CalculatorStep()
    data class AddMoreInventorySheet(val building: String, val existingBitmaps: List<Bitmap>) : CalculatorStep()
    data class CaptureRoomLog(val building: String, val inventoryBitmaps: List<Bitmap>) : CalculatorStep()
    data class ConfirmRoomLog(
        val building: String,
        val inventoryBitmaps: List<Bitmap>,
        val roomLogBitmap: Bitmap
    ) : CalculatorStep()
    data class Analyzing(
        val building: String,
        val inventoryBitmaps: List<Bitmap>,
        val roomLogBitmap: Bitmap
    ) : CalculatorStep()
    data class Result(val result: StockCalculationResult) : CalculatorStep()
}

data class StockCalculatorUiState(
    val step: CalculatorStep = CalculatorStep.SelectBuilding,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val saveState: SaveState = SaveState.Idle
)

@HiltViewModel
class StockCalculatorViewModel @Inject constructor(
    private val claudeRepository: ClaudeRepository,
    private val stockSaveDao: StockSaveDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(StockCalculatorUiState())
    val uiState: StateFlow<StockCalculatorUiState> = _uiState.asStateFlow()

    fun selectBuilding(building: String) {
        _uiState.update { it.copy(step = CalculatorStep.CaptureInventorySheet(building)) }
    }

    // 재고 시트 촬영 완료 → 확인 화면으로
    fun onInventorySheetCaptured(bitmap: Bitmap) {
        val current = _uiState.value.step as? CalculatorStep.CaptureInventorySheet ?: return
        _uiState.update {
            it.copy(step = CalculatorStep.ConfirmInventorySheet(current.building, listOf(bitmap)))
        }
    }

    fun onMoreInventorySheetCaptured(bitmap: Bitmap) {
        val current = _uiState.value.step as? CalculatorStep.AddMoreInventorySheet ?: return
        _uiState.update {
            it.copy(step = CalculatorStep.ConfirmInventorySheet(current.building, current.existingBitmaps + bitmap))
        }
    }

    fun addMoreInventorySheet() {
        val current = _uiState.value.step as? CalculatorStep.ConfirmInventorySheet ?: return
        _uiState.update {
            it.copy(step = CalculatorStep.AddMoreInventorySheet(current.building, current.bitmaps))
        }
    }

    fun removeInventorySheet(index: Int) {
        val current = _uiState.value.step as? CalculatorStep.ConfirmInventorySheet ?: return
        val newBitmaps = current.bitmaps.toMutableList().also { it.removeAt(index) }
        if (newBitmaps.isEmpty()) {
            _uiState.update { it.copy(step = CalculatorStep.CaptureInventorySheet(current.building)) }
        } else {
            _uiState.update { it.copy(step = CalculatorStep.ConfirmInventorySheet(current.building, newBitmaps)) }
        }
    }

    fun confirmInventorySheet() {
        val current = _uiState.value.step as? CalculatorStep.ConfirmInventorySheet ?: return
        _uiState.update {
            it.copy(step = CalculatorStep.CaptureRoomLog(current.building, current.bitmaps))
        }
    }

    fun retakeInventorySheet() {
        val current = _uiState.value.step as? CalculatorStep.ConfirmInventorySheet ?: return
        _uiState.update {
            it.copy(step = CalculatorStep.CaptureInventorySheet(current.building))
        }
    }

    fun backFromAddMore() {
        val current = _uiState.value.step as? CalculatorStep.AddMoreInventorySheet ?: return
        _uiState.update {
            it.copy(step = CalculatorStep.ConfirmInventorySheet(current.building, current.existingBitmaps))
        }
    }

    // 객실 관리일지 촬영 완료 → 확인 화면으로
    fun onRoomLogCaptured(bitmap: Bitmap) {
        val current = _uiState.value.step as? CalculatorStep.CaptureRoomLog ?: return
        _uiState.update {
            it.copy(
                step = CalculatorStep.ConfirmRoomLog(
                    current.building,
                    current.inventoryBitmaps,
                    bitmap
                )
            )
        }
    }

    fun confirmRoomLog() {
        val current = _uiState.value.step as? CalculatorStep.ConfirmRoomLog ?: return
        _uiState.update {
            it.copy(
                step = CalculatorStep.Analyzing(
                    current.building,
                    current.inventoryBitmaps,
                    current.roomLogBitmap
                ),
                isLoading = true,
                errorMessage = null
            )
        }
        analyzeImages(current.building, current.inventoryBitmaps, current.roomLogBitmap)
    }

    fun retakeRoomLog() {
        val current = _uiState.value.step as? CalculatorStep.ConfirmRoomLog ?: return
        _uiState.update {
            it.copy(
                step = CalculatorStep.CaptureRoomLog(current.building, current.inventoryBitmaps)
            )
        }
    }

    // CaptureRoomLog 화면 뒤로가기 → 재고 시트 확인으로 복귀
    fun backFromRoomLog() {
        val current = _uiState.value.step as? CalculatorStep.CaptureRoomLog ?: return
        _uiState.update {
            it.copy(
                step = CalculatorStep.ConfirmInventorySheet(current.building, current.inventoryBitmaps)
            )
        }
    }

    private fun analyzeImages(building: String, inventoryBitmaps: List<Bitmap>, roomLogBitmap: Bitmap) {
        viewModelScope.launch {
            // 재고 시트 여러 장 순차 분석
            val inventoryResults = inventoryBitmaps.map { bitmap ->
                claudeRepository.parseInventorySheet(ImageUtils.bitmapToBase64(bitmap))
            }

            val failedInventory = inventoryResults.firstOrNull { it.isFailure }
            if (failedInventory != null) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "재고 시트 분석 실패: ${failedInventory.exceptionOrNull()?.message}"
                    )
                }
                return@launch
            }

            // 모든 층 재고 합산
            val combinedInventory = inventoryResults
                .map { it.getOrThrow() }
                .fold(InventoryParseResult()) { acc, result ->
                    InventoryParseResult(
                        한실이불피 = acc.한실이불피 + result.한실이불피,
                        요피 = acc.요피 + result.요피,
                        한실베개피 = acc.한실베개피 + result.한실베개피,
                        양실이불피 = acc.양실이불피 + result.양실이불피,
                        시트피 = acc.시트피 + result.시트피,
                        양실베개피 = acc.양실베개피 + result.양실베개피,
                        ft = acc.ft + result.ft,
                        bt = acc.bt + result.bt,
                        걸레 = acc.걸레 + result.걸레
                    )
                }

            val roomLogResult = claudeRepository.parseRoomLog(ImageUtils.bitmapToBase64(roomLogBitmap), building)

            if (roomLogResult.isFailure) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "객실 관리일지 분석 실패: ${roomLogResult.exceptionOrNull()?.message}"
                    )
                }
                return@launch
            }

            val calculationResult = calculateStock(building, combinedInventory, roomLogResult.getOrThrow())

            _uiState.update {
                it.copy(
                    step = CalculatorStep.Result(calculationResult),
                    isLoading = false
                )
            }
        }
    }

    private data class LinenRequirement(
        val 한실이불피: Int, val 요피: Int, val 한실베개피: Int,
        val 양실이불피: Int, val 시트피: Int, val 양실베개피: Int,
        val ft: Int, val bt: Int
    )

    private fun calculateStock(
        building: String,
        inventory: InventoryParseResult,
        roomLog: RoomLogParseResult
    ): StockCalculationResult {
        val requirements = mapOf(
            "HGS" to LinenRequirement(3, 3, 3, 1, 1, 2, 7, 1),
            "HGD" to LinenRequirement(3, 3, 3, 1, 1, 2, 7, 0),
            "HSO" to LinenRequirement(4, 4, 4, 0, 0, 0, 7, 0),
            "HSR" to LinenRequirement(3, 3, 3, 1, 1, 2, 7, 0),
            "HPR" to LinenRequirement(3, 3, 3, 1, 1, 2, 7, 1),
            "HTS" to LinenRequirement(3, 3, 3, 1, 1, 2, 7, 1),
            "HTD" to LinenRequirement(1, 1, 1, 2, 2, 4, 7, 1)
        )
        val roomCounts = mapOf(
            "HGS" to roomLog.hgs, "HGD" to roomLog.hgd, "HSO" to roomLog.hso,
            "HSR" to roomLog.hsr, "HPR" to roomLog.hpr, "HTS" to roomLog.hts,
            "HTD" to roomLog.htd
        ).filter { it.value > 0 }

        var used한실이불피 = 0; var used요피 = 0; var used한실베개피 = 0
        var used양실이불피 = 0; var used시트피 = 0; var used양실베개피 = 0
        var usedFt = 0; var usedBt = 0

        roomCounts.forEach { (type, count) ->
            val req = requirements[type] ?: return@forEach
            used한실이불피 += req.한실이불피 * count
            used요피 += req.요피 * count
            used한실베개피 += req.한실베개피 * count
            used양실이불피 += req.양실이불피 * count
            used시트피 += req.시트피 * count
            used양실베개피 += req.양실베개피 * count
            usedFt += req.ft * count
            usedBt += req.bt * count
        }

        val items = listOf(
            StockResultItem("한실이불피", inventory.한실이불피, used한실이불피, inventory.한실이불피 - used한실이불피),
            StockResultItem("요피", inventory.요피, used요피, inventory.요피 - used요피),
            StockResultItem("한실베개피", inventory.한실베개피, used한실베개피, inventory.한실베개피 - used한실베개피),
            StockResultItem("양실이불피", inventory.양실이불피, used양실이불피, inventory.양실이불피 - used양실이불피),
            StockResultItem("시트피", inventory.시트피, used시트피, inventory.시트피 - used시트피),
            StockResultItem("양실베개피", inventory.양실베개피, used양실베개피, inventory.양실베개피 - used양실베개피),
            StockResultItem("FT(페이스타올)", inventory.ft, usedFt, inventory.ft - usedFt),
            StockResultItem("BT(배스타올)", inventory.bt, usedBt, inventory.bt - usedBt),
            StockResultItem("걸레", inventory.걸레, 0, inventory.걸레)
        )

        return StockCalculationResult(
            building = building,
            roomCounts = roomCounts,
            items = items
        )
    }

    fun saveResult() {
        val result = (_uiState.value.step as? CalculatorStep.Result)?.result ?: return
        if (_uiState.value.saveState is SaveState.Saved) return
        _uiState.update { it.copy(saveState = SaveState.Saving) }
        viewModelScope.launch {
            runCatching {
                val gson = Gson()
                stockSaveDao.insert(
                    StockSaveEntity(
                        building = result.building,
                        savedAt = System.currentTimeMillis(),
                        roomCountsJson = gson.toJson(result.roomCounts),
                        itemsJson = gson.toJson(result.items)
                    )
                )
            }.onSuccess {
                _uiState.update { it.copy(saveState = SaveState.Saved) }
            }.onFailure { e ->
                _uiState.update { it.copy(saveState = SaveState.Error(e.message ?: "저장 실패")) }
            }
        }
    }

    fun retryFromInventory() {
        val current = _uiState.value.step
        val building = when (current) {
            is CalculatorStep.Analyzing -> current.building
            is CalculatorStep.Result -> current.result.building
            else -> return
        }
        _uiState.update {
            it.copy(
                step = CalculatorStep.CaptureInventorySheet(building),
                isLoading = false,
                errorMessage = null
            )
        }
    }

    fun retryRoomLogOnly() {
        val current = _uiState.value.step
        when (current) {
            is CalculatorStep.Analyzing -> {
                _uiState.update {
                    it.copy(
                        step = CalculatorStep.CaptureRoomLog(current.building, current.inventoryBitmaps),
                        isLoading = false,
                        errorMessage = null
                    )
                }
            }
            else -> return
        }
    }

    fun reset() {
        _uiState.update { StockCalculatorUiState() }
    }
}