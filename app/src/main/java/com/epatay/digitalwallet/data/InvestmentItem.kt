package com.epatay.digitalwallet.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "investment_table")
data class InvestmentItem(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,               // Otomatik artan takip numarası
    val assetName: String,         // Örn: "Altın", "USD", "Euro"
    val amount: Double,            // Ne kadar alındı? (Örn: 10.5)
    val buyPrice: Double,          // Alındığı anki fiyat/kur (Örn: 32.50)
    val buyDate: String            // Alış tarihi ve saati
)