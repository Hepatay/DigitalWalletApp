package com.epatay.digitalwallet.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recurring_transactions_table")
data class RecurringTransaction(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val amount: Double,
    val category: String,
    val type: TransactionType,
    val dayOfMonth: Int,
    val autoCreate: Boolean,
    val notificationEnabled: Boolean,
    val isActive: Boolean = true,
    val lastGeneratedPeriod: String? = null,
    val lastNotifiedPeriod: String? = null
)
