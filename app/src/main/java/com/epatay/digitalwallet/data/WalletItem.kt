package com.epatay.digitalwallet.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wallet_table")
data class WalletItem(
    @PrimaryKey
    val currencyCode: String, // Örn: "USD", "EUR", "TRY"
    val balance: Double       // Örn: 150.50
)