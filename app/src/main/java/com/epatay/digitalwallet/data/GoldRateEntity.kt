package com.epatay.digitalwallet.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "gold_rates")
data class GoldRateEntity(
    @PrimaryKey
    val type: String,
    val displayName: String,
    val buyingPrice: Double?,
    val sellingPrice: Double?,
    val source: String,
    val sourceDate: String?,
    val fetchedAt: Long,
    val isReference: Boolean
)

fun GoldRateEntity.toGoldRate(): GoldRate? {
    val goldType =
        runCatching { GoldType.valueOf(type) }
            .getOrNull()
            ?: return null

    return GoldRate(
        type = goldType,
        buyingPrice = buyingPrice,
        sellingPrice = sellingPrice,
        source = source,
        sourceDate = sourceDate,
        fetchedAt = fetchedAt,
        isReference = isReference
    )
}

fun GoldRate.toEntity(): GoldRateEntity =
    GoldRateEntity(
        type = type.name,
        displayName = type.displayName,
        buyingPrice = buyingPrice,
        sellingPrice = sellingPrice,
        source = source,
        sourceDate = sourceDate,
        fetchedAt = fetchedAt,
        isReference = isReference
    )
