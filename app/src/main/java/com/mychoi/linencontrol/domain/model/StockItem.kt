package com.mychoi.linencontrol.domain.model

data class StockItem(
    val id: Int,
    val label: String,
    val input: String = "",
    val result: Int = 0
)
