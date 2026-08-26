package com.epatay.digitalwallet.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "investments_table")
data class InvestmentItem(
    @PrimaryKey
    val uuid: String = UUID.randomUUID().toString(),
    val assetName: String = "",
    val amount: Double = 0.0,
    val buyPrice: Double = 0.0,
    val buyDate: String = "",
    val note: String? = null,
    val updated_at: Long = System.currentTimeMillis(),
    val is_deleted: Boolean = false,
    val is_synced: Boolean = false,
    val user_id: String? = null
)
