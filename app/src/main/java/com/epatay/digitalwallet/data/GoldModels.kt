package com.epatay.digitalwallet.data

import java.math.BigDecimal
import java.math.RoundingMode

enum class GoldInputUnit {
    GRAM,
    PIECE
}

enum class GoldType(
    val displayName: String,
    val inputUnit: GoldInputUnit
) {
    GRAM_GOLD("Gram Altın", GoldInputUnit.GRAM),
    QUARTER_GOLD("Çeyrek Altın", GoldInputUnit.PIECE),
    HALF_GOLD("Yarım Altın", GoldInputUnit.PIECE),
    FULL_GOLD("Tam Altın", GoldInputUnit.PIECE),
    ATA_REPUBLIC_GOLD("Ata / Cumhuriyet Altını", GoldInputUnit.PIECE)
}

data class GoldRate(
    val type: GoldType,
    val buyingPrice: Double,
    val sellingPrice: Double,
    val source: String,
    val sourceDate: String?,
    val sourceUpdatedAt: Long,
    val fetchedAt: Long,
    val isReference: Boolean = true
) {
    val spread: Double
        get() =
            BigDecimal.valueOf(sellingPrice)
                .subtract(BigDecimal.valueOf(buyingPrice))
                .setScale(2, RoundingMode.HALF_UP)
                .toDouble()

    val spreadPercentage: Double
        get() =
            BigDecimal.valueOf(sellingPrice)
                .subtract(BigDecimal.valueOf(buyingPrice))
                .divide(BigDecimal.valueOf(buyingPrice), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal("100"))
                .setScale(2, RoundingMode.HALF_UP)
                .toDouble()
}
