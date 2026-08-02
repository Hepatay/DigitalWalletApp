package com.epatay.digitalwallet.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class PortfolioSourceLabelFormatterTest {
    @Test
    fun currencySourceDateAndDeviceFetchTimeAreShownSeparately() {
        val label =
            PortfolioSourceLabelFormatter.format(
                source = "TCMB",
                sourceUpdatedText = "31.07.2026",
                fetchedAtText = "02.08.2026 11:57"
            )

        assertEquals(
            "Kaynak: TCMB • Veri: 31.07.2026 • Çekildi: 02.08.2026 11:57",
            label
        )
    }
}
