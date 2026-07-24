package com.epatay.digitalwallet.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "savings_goals",
    indices = [
        Index(value = ["isArchived"])
    ]
)
data class SavingsGoal(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val targetAmount: Double,
    val targetDateKey: Int? = null,
    val createdAtMillis: Long,
    val isArchived: Boolean = false
)
