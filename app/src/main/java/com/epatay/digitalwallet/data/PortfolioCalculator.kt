package com.epatay.digitalwallet.data

import java.math.BigDecimal
import java.math.RoundingMode

data class ValidMarketPrices(
    val buyingPrice: BigDecimal,
    val sellingPrice: BigDecimal,
    val spread: BigDecimal,
    val spreadPercentage: BigDecimal
)

object MarketPriceValidator {
    private val maxPrice = BigDecimal("1000000000")
    private val maxSpreadPercentage = BigDecimal("50")

    fun validate(
        buyingPrice: Double?,
        sellingPrice: Double?
    ): ValidMarketPrices? {
        val buying = buyingPrice.toPriceDecimal() ?: return null
        val selling = sellingPrice.toPriceDecimal() ?: return null
        if (buying > maxPrice || selling > maxPrice || selling < buying) return null

        val spread = selling.subtract(buying)
        val percentage =
            spread.divide(buying, 8, RoundingMode.HALF_UP)
                .multiply(BigDecimal("100"))
        if (percentage > maxSpreadPercentage) return null

        return ValidMarketPrices(
            buyingPrice = buying,
            sellingPrice = selling,
            spread = spread.setScale(2, RoundingMode.HALF_UP),
            spreadPercentage = percentage.setScale(2, RoundingMode.HALF_UP)
        )
    }

    private fun Double?.toPriceDecimal(): BigDecimal? =
        this?.takeIf { it.isFinite() && it > 0.0 }
            ?.let(BigDecimal::valueOf)
}

data class PortfolioCalculation(
    val totalCost: BigDecimal,
    val estimatedSaleValue: BigDecimal,
    val profitLoss: BigDecimal,
    val profitLossPercentage: BigDecimal
)

object PortfolioCalculator {
    private val maxQuantity = BigDecimal("1000000000")
    private val maxUnitPrice = BigDecimal("1000000000")
    private val maxTotal = BigDecimal("1000000000000000")

    fun calculate(
        quantity: Double?,
        userPurchaseUnitPrice: Double?,
        marketBuyingPrice: Double?,
        feesAndTaxes: Double = 0.0
    ): PortfolioCalculation? {
        val quantityDecimal = quantity.toPositiveDecimal() ?: return null
        val purchasePrice = userPurchaseUnitPrice.toPositiveDecimal() ?: return null
        val marketBuying = marketBuyingPrice.toPositiveDecimal() ?: return null
        val fees =
            feesAndTaxes.takeIf { it.isFinite() && it >= 0.0 }
                ?.let(BigDecimal::valueOf)
                ?: return null

        if (
            quantityDecimal > maxQuantity ||
            purchasePrice > maxUnitPrice ||
            marketBuying > maxUnitPrice
        ) return null

        val totalCost = quantityDecimal.multiply(purchasePrice).add(fees)
        val estimatedSaleValue = quantityDecimal.multiply(marketBuying)
        if (totalCost <= BigDecimal.ZERO || totalCost > maxTotal || estimatedSaleValue > maxTotal) {
            return null
        }

        val profitLoss = estimatedSaleValue.subtract(totalCost)
        val profitLossPercentage =
            profitLoss.divide(totalCost, 8, RoundingMode.HALF_UP)
                .multiply(BigDecimal("100"))

        return PortfolioCalculation(
            totalCost = totalCost.setScale(2, RoundingMode.HALF_UP),
            estimatedSaleValue = estimatedSaleValue.setScale(2, RoundingMode.HALF_UP),
            profitLoss = profitLoss.setScale(2, RoundingMode.HALF_UP),
            profitLossPercentage = profitLossPercentage.setScale(2, RoundingMode.HALF_UP)
        )
    }

    private fun Double?.toPositiveDecimal(): BigDecimal? =
        this?.takeIf { it.isFinite() && it > 0.0 }
            ?.let(BigDecimal::valueOf)
}
