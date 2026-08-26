package com.epatay.digitalwallet.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "user_gold_assets")
data class UserGoldAssetEntity(
    @PrimaryKey
    val uuid: String = UUID.randomUUID().toString(),
    val goldType: String = "",
    val quantity: Double = 0.0,
    val unit: String = "",
    val purchaseUnitPrice: Double? = null,
    val totalPurchaseCost: Double? = null,
    val purchaseDate: Long? = null,
    val note: String? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val updated_at: Long = System.currentTimeMillis(),
    val is_deleted: Boolean = false,
    val is_synced: Boolean = false,
    val user_id: String? = null
)
