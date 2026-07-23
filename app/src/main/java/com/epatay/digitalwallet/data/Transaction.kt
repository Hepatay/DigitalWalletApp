package com.epatay.digitalwallet.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

// --- 1. YENİ EKLENEN KISIM: İşlem Tipi ---
// Bu veri tabanına kaydedeceğimiz şeyin gelir mi gider mi olduğunu belirler.
enum class TransactionType {
    INCOME,   // Gelir (Örn: Maaş, Harçlık)
    EXPENSE   // Gider (Örn: Market, Fatura)
}

// --- 2. GÜNCELLENEN KISIM: Sınıf ve Tablo Adı ---
@Entity(
    tableName = "transactions_table",
    indices = [
        Index(value = ["occurredOn"]),
        Index(value = ["type", "occurredOn"]),
        Index(value = ["category", "occurredOn"])
    ]
)
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,      // Örn: "Market Alışverişi" veya "Temmuz Maaşı"
    val amount: Double,     // Örn: 150.50 veya 15000.00
    val category: String,   // Örn: "Gıda" veya "Maaş"
    val date: String,       // Örn: "14.07.2026"
    val type: TransactionType, // YENİ EKLENEN ALAN: Bu işlem GELİR mi, GİDER mi?
    @ColumnInfo(defaultValue = "0")
    val occurredOn: Int =
        TransactionDateUtils.toDateKey(date)
)
