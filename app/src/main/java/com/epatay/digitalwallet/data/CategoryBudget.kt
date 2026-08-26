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
    val monthKey: Int = 0,
    val category: String = "",
    val limitAmount: Double = 0.0,
    val updatedAtMillis: Long = 0L,
    val updated_at: Long = System.currentTimeMillis(),
    val is_deleted: Boolean = false,
    val is_synced: Boolean = false,
    val user_id: String? = null
)
