package com.mychoi.linencontrol.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.mychoi.linencontrol.data.local.entity.StockSaveEntity

@Dao
interface StockSaveDao {
    @Insert
    suspend fun insert(entity: StockSaveEntity): Long

    @Query("SELECT * FROM stock_saves ORDER BY savedAt DESC")
    suspend fun getAll(): List<StockSaveEntity>

    @Query("DELETE FROM stock_saves WHERE id = :id")
    suspend fun deleteById(id: Long)
}
