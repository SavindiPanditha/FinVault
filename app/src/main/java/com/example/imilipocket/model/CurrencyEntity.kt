package com.example.imilipocket.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "currencies")
data class CurrencyEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val code: String,
    val isDefault: Boolean = false
)