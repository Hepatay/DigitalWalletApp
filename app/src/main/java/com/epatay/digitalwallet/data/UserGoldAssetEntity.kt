package com.epatay.digitalwallet.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_gold_assets")
data class UserGoldAssetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val goldType: String,
    val quantity: Double,
    val unit: String,
    val purchaseUnitPrice: Double?,
    val totalPurchaseCost: Double?,
    val purchaseDate: Long?,
    val note: String?,
    val createdAt: Long,
    val updatedAt: Long
)
