package com.epatay.digitalwallet.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "savings_goals",
    indices = [
        Index(value = ["isArchived"])
    ]
)
data class SavingsGoal(
    @PrimaryKey
    val uuid: String = UUID.randomUUID().toString(),
    val title: String = "",
    val targetAmount: Double = 0.0,
    val targetDateKey: Int? = null,
    val createdAtMillis: Long = 0L,
    val isArchived: Boolean = false,
    val updated_at: Long = System.currentTimeMillis(),
    val is_deleted: Boolean = false,
    val is_synced: Boolean = false,
    val user_id: String? = null
)
