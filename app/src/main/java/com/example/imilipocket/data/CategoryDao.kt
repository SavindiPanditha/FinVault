package com.example.imilipocket.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.imilipocket.model.CategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Insert
    suspend fun insert(category: CategoryEntity)

    @Query("SELECT * FROM categories")
    fun getAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE type = :type")
    fun getByType(type: String): Flow<List<CategoryEntity>>

    @Query("DELETE FROM categories")
    suspend fun deleteAll()
}