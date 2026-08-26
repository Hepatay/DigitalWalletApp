package com.epatay.digitalwallet.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "recurring_transactions_table")
data class RecurringTransaction(
    @PrimaryKey
    val uuid: String = UUID.randomUUID().toString(),
    val title: String = "",
    val amount: Double = 0.0,
    val category: String = "",
    val type: TransactionType = TransactionType.EXPENSE,
    val dayOfMonth: Int = 1,
    val autoCreate: Boolean = false,
    val notificationEnabled: Boolean = false,
    val isActive: Boolean = true,
    val lastGeneratedPeriod: String? = null,
    val lastNotifiedPeriod: String? = null,
    val updated_at: Long = System.currentTimeMillis(),
    val is_deleted: Boolean = false,
    val is_synced: Boolean = false,
    val user_id: String? = null
)
