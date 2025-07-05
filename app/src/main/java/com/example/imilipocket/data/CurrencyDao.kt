package com.example.imilipocket.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.imilipocket.model.CurrencyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CurrencyDao {
    @Insert
    suspend fun insert(currency: CurrencyEntity)

    @Query("SELECT * FROM currencies")
    fun getAll(): Flow<List<CurrencyEntity>>

    @Query("SELECT * FROM currencies WHERE isDefault = 1 LIMIT 1")
    fun getDefault(): Flow<CurrencyEntity?>

    @Query("SELECT * FROM currencies WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultSync(): CurrencyEntity?

    @Query("DELETE FROM currencies")
    suspend fun deleteAll()
}