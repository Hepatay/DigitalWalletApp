package com.epatay.digitalwallet.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DecimalInputValidatorTest {

    @Test
    fun positiveMoney_acceptsTurkishAndDotDecimalFormats() {
        assertValidMoney("1.234,56", "1234.56")
        assertValidMoney("1234.56", "1234.56")
        assertValidMoney("1,25", "1.25")
    }

    @Test
    fun positiveMoney_rejectsEmptyLettersNegativeAndOversizedValues() {
        listOf(
            "",
            "12TL",
            "-1",
            "0",
            "1,2,3",
            "1234567890123456,00"
        ).forEach { raw ->
            assertTrue(
                "$raw geçersiz olmalıydı",
                DecimalInputValidator.positiveMoney(raw) is
                    DecimalInputResult.Invalid
            )
        }
    }

    @Test
    fun positiveQuantity_enforcesWholePiecesAndScaleLimit() {
        assertTrue(
            DecimalInputValidator.positiveQuantity(
                rawValue = "2,5",
                wholeNumberOnly = true
            ) is DecimalInputResult.Invalid
        )
        assertTrue(
            DecimalInputValidator.positiveQuantity(
                rawValue = "2",
                wholeNumberOnly = true
            ) is DecimalInputResult.Valid
        )
        assertTrue(
            DecimalInputValidator.positiveQuantity(
                rawValue = "0,123456789"
            ) is DecimalInputResult.Invalid
        )
    }

    private fun assertValidMoney(
        raw: String,
        expected: String
    ) {
        val result =
            DecimalInputValidator.positiveMoney(raw)

        assertTrue(result is DecimalInputResult.Valid)
        assertEquals(
            expected,
            (result as DecimalInputResult.Valid)
                .value
                .toPlainString()
        )
    }
}
