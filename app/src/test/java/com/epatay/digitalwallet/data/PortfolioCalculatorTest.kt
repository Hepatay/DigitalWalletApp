package com.epatay.digitalwallet.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class PortfolioCalculatorTest {

    @Test
    fun equalMarketBuyingAndUserPurchasePrice_returnsExactlyZero() {
        val result = calculate(1.0, 6_174.46, 6_174.46)

        assertEquals(BigDecimal("0.00"), result.profitLoss)
        assertEquals(BigDecimal("0.00"), result.profitLossPercentage)
    }

    @Test
    fun oneGram_spreadStartsAtMinusNinetyOneKurus() {
        val result = calculate(1.0, 6_175.37, 6_174.46)

        assertEquals(BigDecimal("6175.37"), result.totalCost)
        assertEquals(BigDecimal("6174.46"), result.estimatedSaleValue)
        assertEquals(BigDecimal("-0.91"), result.profitLoss)
    }

    @Test
    fun fiveHundredUsd_spreadStartsAtMinusFortyTwoLiraSixtyFive() {
        val result = calculate(500.0, 47.4305, 47.3452)

        assertEquals(BigDecimal("23715.25"), result.totalCost)
        assertEquals(BigDecimal("23672.60"), result.estimatedSaleValue)
        assertEquals(BigDecimal("-42.65"), result.profitLoss)
    }

    @Test
    fun quantityAndPrice_areMultipliedExactlyOnce() {
        val one = calculate(1.0, 100.0, 90.0)
        val two = calculate(2.0, 100.0, 90.0)

        assertEquals(BigDecimal("100.00"), one.totalCost)
        assertEquals(BigDecimal("200.00"), two.totalCost)
        assertEquals(BigDecimal("180.00"), two.estimatedSaleValue)
    }

    @Test
    fun nullZeroNegativeAndExtremeInputs_doNotProduceCalculation() {
        assertNull(PortfolioCalculator.calculate(null, 1.0, 1.0))
        assertNull(PortfolioCalculator.calculate(0.0, 1.0, 1.0))
        assertNull(PortfolioCalculator.calculate(1.0, -1.0, 1.0))
        assertNull(PortfolioCalculator.calculate(1.0, 1.0, 0.0))
        assertNull(PortfolioCalculator.calculate(1.0, 1_000_000_001.0, 1.0))
    }

    @Test
    fun reopeningWithPersistedInputs_keepsCostAndProfitLossStable() {
        val beforeClose = calculate(500.0, 47.4305, 47.3452)
        val persistedQuantity = 500.0
        val persistedPurchasePrice = 47.4305
        val afterOpen = calculate(persistedQuantity, persistedPurchasePrice, 47.3452)

        assertEquals(beforeClose, afterOpen)
    }

    @Test
    fun apiRefresh_preservesUserCost_andOnlyChangesEstimatedValue() {
        val before = calculate(2.0, 6_200.0, 6_174.46)
        val after = calculate(2.0, 6_200.0, 6_300.0)

        assertEquals(before.totalCost, after.totalCost)
        assertTrue(before.estimatedSaleValue != after.estimatedSaleValue)
        assertTrue(before.profitLoss != after.profitLoss)
    }

    @Test
    fun spread_isSellingMinusBuying_withTurkishDisplayPrecision() {
        val prices = MarketPriceValidator.validate(6_174.46, 6_175.37)!!

        assertEquals(BigDecimal("0.91"), prices.spread)
        assertEquals(BigDecimal("0.01"), prices.spreadPercentage)
    }

    private fun calculate(
        quantity: Double,
        purchasePrice: Double,
        marketBuying: Double
    ): PortfolioCalculation =
        requireNotNull(
            PortfolioCalculator.calculate(quantity, purchasePrice, marketBuying)
        )
}
