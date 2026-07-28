package com.epatay.digitalwallet.data

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
    val buyingPrice: Double?,
    val sellingPrice: Double?,
    val source: String,
    val sourceDate: String?,
    val fetchedAt: Long,
    val isReference: Boolean = true
)
