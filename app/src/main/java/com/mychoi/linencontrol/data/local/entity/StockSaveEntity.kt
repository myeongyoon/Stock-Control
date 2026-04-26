package com.mychoi.linencontrol.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stock_saves")
data class StockSaveEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val building: String,
    val savedAt: Long,
    val yellowRooms: Int,
    val pinkRooms: Int,
    val itemsJson: String
)
