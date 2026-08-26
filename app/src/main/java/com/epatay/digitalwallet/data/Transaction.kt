package com.epatay.digitalwallet.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

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
    @PrimaryKey
    val uuid: String = UUID.randomUUID().toString(),
    val title: String = "",
    val amount: Double = 0.0,
    val category: String = "",
    val date: String = "",
    val type: TransactionType = TransactionType.EXPENSE,
    @ColumnInfo(defaultValue = "0")
    val occurredOn: Int =
        TransactionDateUtils.toDateKey(date),
        
    val updated_at: Long = System.currentTimeMillis(),
    val is_deleted: Boolean = false,
    val is_synced: Boolean = false,
    val user_id: String? = null
)
