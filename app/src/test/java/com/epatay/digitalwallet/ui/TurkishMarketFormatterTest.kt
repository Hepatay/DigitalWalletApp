package com.epatay.digitalwallet.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class TurkishMarketFormatterTest {

    @Test
    fun price_usesTurkishGroupingDecimalAndLiraSuffix() {
        assertEquals("6.174,46 TL", GoldRateFormatter.price(6_174.46))
        assertEquals("0,91 TL", GoldRateFormatter.price(0.91))
    }
}
