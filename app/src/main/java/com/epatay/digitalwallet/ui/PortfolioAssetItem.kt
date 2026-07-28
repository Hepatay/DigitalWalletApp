package com.epatay.digitalwallet.ui

import com.epatay.digitalwallet.data.GoldInputUnit
import com.epatay.digitalwallet.data.GoldType
import com.epatay.digitalwallet.data.InvestmentItem
import com.epatay.digitalwallet.data.UserGoldAssetEntity

enum class PortfolioAssetKind {
    CURRENCY,
    GOLD
}

data class PortfolioAssetItem(
    val stableKey: String,
    val kind: PortfolioAssetKind,
    val displayName: String,
    val code: String,
    val quantity: Double,
    val unitLabel: String,
    val purchaseUnitPrice: Double?,
    val totalPurchaseCost: Double?,
    val purchaseDateText: String,
    val note: String?,
    val marketBuyingPrice: Double?,
    val currentValue: Double?,
    val profitLoss: Double?,
    val source: String?,
    val sourceUpdatedAt: Long?,
    val legacyInvestment: InvestmentItem? = null,
    val goldAsset: UserGoldAssetEntity? = null,
    val goldType: GoldType? = null
) {
    val requiresWholeQuantity: Boolean
        get() =
            goldType?.inputUnit == GoldInputUnit.PIECE
}
