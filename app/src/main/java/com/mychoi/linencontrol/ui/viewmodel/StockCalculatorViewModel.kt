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
    data class ConfirmInventorySheet(val building: String, val bitmap: Bitmap) : CalculatorStep()
    data class CaptureRoomLog(val building: String, val inventoryBitmap: Bitmap) : CalculatorStep()
    data class ConfirmRoomLog(
        val building: String,
        val inventoryBitmap: Bitmap,
        val roomLogBitmap: Bitmap
    ) : CalculatorStep()
    data class Analyzing(
        val building: String,
        val inventoryBitmap: Bitmap,
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
            it.copy(step = CalculatorStep.ConfirmInventorySheet(current.building, bitmap))
        }
    }

    fun confirmInventorySheet() {
        val current = _uiState.value.step as? CalculatorStep.ConfirmInventorySheet ?: return
        _uiState.update {
            it.copy(step = CalculatorStep.CaptureRoomLog(current.building, current.bitmap))
        }
    }

    fun retakeInventorySheet() {
        val current = _uiState.value.step as? CalculatorStep.ConfirmInventorySheet ?: return
        _uiState.update {
            it.copy(step = CalculatorStep.CaptureInventorySheet(current.building))
        }
    }

    // 객실 관리일지 촬영 완료 → 확인 화면으로
    fun onRoomLogCaptured(bitmap: Bitmap) {
        val current = _uiState.value.step as? CalculatorStep.CaptureRoomLog ?: return
        _uiState.update {
            it.copy(
                step = CalculatorStep.ConfirmRoomLog(
                    current.building,
                    current.inventoryBitmap,
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
                    current.inventoryBitmap,
                    current.roomLogBitmap
                ),
                isLoading = true,
                errorMessage = null
            )
        }
        analyzeImages(current.building, current.inventoryBitmap, current.roomLogBitmap)
    }

    fun retakeRoomLog() {
        val current = _uiState.value.step as? CalculatorStep.ConfirmRoomLog ?: return
        _uiState.update {
            it.copy(
                step = CalculatorStep.CaptureRoomLog(current.building, current.inventoryBitmap)
            )
        }
    }

    // CaptureRoomLog 화면 뒤로가기 → 재고 시트 확인으로 복귀
    fun backFromRoomLog() {
        val current = _uiState.value.step as? CalculatorStep.CaptureRoomLog ?: return
        _uiState.update {
            it.copy(
                step = CalculatorStep.ConfirmInventorySheet(current.building, current.inventoryBitmap)
            )
        }
    }

    private fun analyzeImages(building: String, inventoryBitmap: Bitmap, roomLogBitmap: Bitmap) {
        viewModelScope.launch {
            val inventoryBase64 = ImageUtils.bitmapToBase64(inventoryBitmap)
            val roomLogBase64 = ImageUtils.bitmapToBase64(roomLogBitmap)

            val inventoryResult = claudeRepository.parseInventorySheet(inventoryBase64)
            val roomLogResult = claudeRepository.parseRoomLog(roomLogBase64, building)

            if (inventoryResult.isFailure) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "재고 시트 분석 실패: ${inventoryResult.exceptionOrNull()?.message}"
                    )
                }
                return@launch
            }
            if (roomLogResult.isFailure) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "객실 관리일지 분석 실패: ${roomLogResult.exceptionOrNull()?.message}"
                    )
                }
                return@launch
            }

            val inventory = inventoryResult.getOrThrow()
            val roomLog = roomLogResult.getOrThrow()
            val calculationResult = calculateStock(building, inventory, roomLog)

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

    fun reset() {
        _uiState.update { StockCalculatorUiState() }
    }
}