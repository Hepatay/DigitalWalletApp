package com.epatay.digitalwallet.data

import androidx.room.Entity

@Entity(
    tableName = "category_budgets",
    primaryKeys = [
        "monthKey",
        "category"
    ]
)
data class CategoryBudget(
    val monthKey: Int,
    val category: String,
    val limitAmount: Double,
    val updatedAtMillis: Long
)
