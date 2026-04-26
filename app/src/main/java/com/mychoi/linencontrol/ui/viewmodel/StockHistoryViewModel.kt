package com.mychoi.linencontrol.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mychoi.linencontrol.data.local.dao.StockSaveDao
import com.mychoi.linencontrol.data.local.entity.StockSaveEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StockHistoryViewModel @Inject constructor(
    private val dao: StockSaveDao
) : ViewModel() {

    private val _records = MutableStateFlow<List<StockSaveEntity>>(emptyList())
    val records: StateFlow<List<StockSaveEntity>> = _records.asStateFlow()

    init {
        loadAll()
    }

    fun loadAll() {
        viewModelScope.launch {
            _records.value = dao.getAll()
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            dao.deleteById(id)
            loadAll()
        }
    }
}
