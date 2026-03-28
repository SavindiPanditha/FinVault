package com.example.imilipocket.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.imilipocket.model.CurrencyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CurrencyDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(currency: CurrencyEntity): Long

    @Query("SELECT * FROM currencies")
    fun getAll(): Flow<List<CurrencyEntity>>

    @Query("SELECT * FROM currencies WHERE isDefault = 1 LIMIT 1")
    fun getDefault(): Flow<CurrencyEntity?>

    @Query("SELECT * FROM currencies WHERE isDefault = 1 LIMIT 1")
    suspend fun getDefaultSync(): CurrencyEntity?

    @Query("SELECT * FROM currencies WHERE code = :code LIMIT 1")
    suspend fun getByCodeSync(code: String): CurrencyEntity?

    @Query("UPDATE currencies SET isDefault = 0")
    suspend fun clearDefaultFlags()

    @Query("UPDATE currencies SET isDefault = 1 WHERE id = :currencyId")
    suspend fun setDefaultById(currencyId: Int)

    @Query("DELETE FROM currencies WHERE code = :code AND id != :keepId")
    suspend fun deleteDuplicatesByCode(code: String, keepId: Int)

    @Query("SELECT * FROM currencies ORDER BY id ASC LIMIT 1")
    suspend fun getFirstCurrencySync(): CurrencyEntity?

    @Query("DELETE FROM currencies")
    suspend fun deleteAll()
}