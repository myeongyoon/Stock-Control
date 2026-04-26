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
    val yellowRooms: Int,
    val pinkRooms: Int,
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

    // 린넨 구성표 차감 계산:
    // 노랑 객실 = 침구(이불피+요피+베개피) + 타올(FT+BT)
    // 분홍 객실 = 타올(FT+BT)만
    private fun calculateStock(
        building: String,
        inventory: InventoryParseResult,
        roomLog: RoomLogParseResult
    ): StockCalculationResult {
        val yellow = roomLog.yellowCount
        val pink = roomLog.pinkCount

        val items = listOf(
            StockResultItem("한실이불피", inventory.한실이불피, yellow, inventory.한실이불피 - yellow),
            StockResultItem("요피", inventory.요피, yellow, inventory.요피 - yellow),
            StockResultItem("한실베개피", inventory.한실베개피, yellow, inventory.한실베개피 - yellow),
            StockResultItem("양실이불피", inventory.양실이불피, yellow, inventory.양실이불피 - yellow),
            StockResultItem("시트피", inventory.시트피, yellow, inventory.시트피 - yellow),
            StockResultItem("양실베개피", inventory.양실베개피, yellow, inventory.양실베개피 - yellow),
            StockResultItem("FT(페이스타올)", inventory.ft, yellow + pink, inventory.ft - (yellow + pink)),
            StockResultItem("BT(배스타올)", inventory.bt, yellow + pink, inventory.bt - (yellow + pink)),
            StockResultItem("걸레", inventory.걸레, 0, inventory.걸레)
        )

        return StockCalculationResult(
            building = building,
            yellowRooms = yellow,
            pinkRooms = pink,
            items = items
        )
    }

    fun saveResult() {
        val result = (_uiState.value.step as? CalculatorStep.Result)?.result ?: return
        if (_uiState.value.saveState is SaveState.Saved) return
        _uiState.update { it.copy(saveState = SaveState.Saving) }
        viewModelScope.launch {
            runCatching {
                val itemsJson = Gson().toJson(result.items)
                stockSaveDao.insert(
                    StockSaveEntity(
                        building = result.building,
                        savedAt = System.currentTimeMillis(),
                        yellowRooms = result.yellowRooms,
                        pinkRooms = result.pinkRooms,
                        itemsJson = itemsJson
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
