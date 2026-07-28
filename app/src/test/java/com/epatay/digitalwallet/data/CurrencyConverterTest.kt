package com.epatay.digitalwallet.data

import java.math.BigDecimal
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CurrencyConverterTest {

    @Test
    fun convert_usesSellingRateForCurrencyToTry() {
        val result =
            CurrencyConverter.convert(
                request =
                    CurrencyConversionRequest(
                        amount = BigDecimal("2"),
                        fromCode = "USD",
                        toCode = "TRY",
                        rateKind = CurrencyRateKind.SELLING
                    ),
                rates = sampleRates
            )

        assertBigDecimalEquals("64", result?.targetAmount)
    }

    @Test
    fun convert_supportsCrossCurrencyConversion() {
        val result =
            CurrencyConverter.convert(
                request =
                    CurrencyConversionRequest(
                        amount = BigDecimal("100"),
                        fromCode = "USD",
                        toCode = "EUR",
                        rateKind = CurrencyRateKind.SELLING
                    ),
                rates = sampleRates
            )

        assertBigDecimalEquals("80", result?.targetAmount)
    }

    @Test
    fun convert_dividesRatesByCurrencyUnit() {
        val result =
            CurrencyConverter.convert(
                request =
                    CurrencyConversionRequest(
                        amount = BigDecimal("100"),
                        fromCode = "JPY",
                        toCode = "TRY",
                        rateKind = CurrencyRateKind.BUYING
                    ),
                rates = sampleRates
            )

        assertBigDecimalEquals("20", result?.targetAmount)
    }

    @Test
    fun convert_rejectsMissingRatesAndNonPositiveAmount() {
        assertNull(
            CurrencyConverter.convert(
                request =
                    CurrencyConversionRequest(
                        amount = BigDecimal.ZERO,
                        fromCode = "USD",
                        toCode = "TRY",
                        rateKind = CurrencyRateKind.SELLING
                    ),
                rates = sampleRates
            )
        )

        assertNull(
            CurrencyConverter.convert(
                request =
                    CurrencyConversionRequest(
                        amount = BigDecimal.ONE,
                        fromCode = "USD",
                        toCode = "AED",
                        rateKind = CurrencyRateKind.SELLING
                    ),
                rates = sampleRates
            )
        )
    }

    private companion object {
        fun assertBigDecimalEquals(
            expected: String,
            actual: BigDecimal?
        ) {
            assertTrue(
                "Beklenen $expected, gelen $actual",
                actual?.compareTo(BigDecimal(expected)) == 0
            )
        }

        val sampleRates =
            listOf(
                CurrencyConversionRate(
                    code = "TRY",
                    name = "Turk Lirasi",
                    unit = 1,
                    buying = BigDecimal.ONE,
                    selling = BigDecimal.ONE
                ),
                CurrencyConversionRate(
                    code = "USD",
                    name = "ABD Dolari",
                    unit = 1,
                    buying = BigDecimal("31"),
                    selling = BigDecimal("32")
                ),
                CurrencyConversionRate(
                    code = "EUR",
                    name = "Euro",
                    unit = 1,
                    buying = BigDecimal("39"),
                    selling = BigDecimal("40")
                ),
                CurrencyConversionRate(
                    code = "JPY",
                    name = "Japon Yeni",
                    unit = 100,
                    buying = BigDecimal("20"),
                    selling = BigDecimal("22")
                )
            )
    }
}
