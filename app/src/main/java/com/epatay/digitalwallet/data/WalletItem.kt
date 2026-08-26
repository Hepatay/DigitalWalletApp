package com.epatay.digitalwallet.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "wallet_table")
data class WalletItem(
    @PrimaryKey
    val uuid: String = UUID.randomUUID().toString(),
    val currencyCode: String, // Örn: "USD", "EUR", "TRY"
    val balance: Double,      // Örn: 150.50
    val updated_at: Long = System.currentTimeMillis(),
    val is_deleted: Boolean = false,
    val is_synced: Boolean = false,
    val user_id: String? = null
)