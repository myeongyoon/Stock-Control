package com.mychoi.linencontrol.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.mychoi.linencontrol.data.local.dao.StockSaveDao
import com.mychoi.linencontrol.data.local.entity.StockSaveEntity

@Database(entities = [StockSaveEntity::class], version = 1, exportSchema = false)
abstract class LinenDatabase : RoomDatabase() {
    abstract fun stockSaveDao(): StockSaveDao
}
