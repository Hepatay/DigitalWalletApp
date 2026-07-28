package com.epatay.digitalwallet.data

import com.epatay.digitalwallet.R
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class MarketDataMappingTest {

    @Test
    fun currencyFlags_andChartColors_areStableLocalResources() {
        assertEquals(
            R.drawable.flag_usd,
            CurrencyFlagProvider.getFlagResId("usd")
        )
        assertEquals(
            R.drawable.flag_dak,
            CurrencyFlagProvider.getFlagResId("DKK")
        )
        assertEquals(
            R.drawable.flag_azn,
            CurrencyFlagProvider.getFlagResId("AZN")
        )
        assertThrows(IllegalArgumentException::class.java) {
            CurrencyFlagProvider.getFlagResId("UNKNOWN")
        }
        assertEquals(
            0xFF003399.toInt(),
            CurrencyFlagProvider.getChartColor("EUR")
        )
    }

    @Test
    fun sortRates_filtersXdr_andUsesPriorityOrder() {
        val rates =
            listOf("ZAR", "XDR", "EUR", "USD", "CNY").map { code ->
                CurrencyRate(
                    currencyCode = code,
                    unit = 1,
                    name = code,
                    currencyName = code,
                    forexBuying = 1.0,
                    forexSelling = 1.1,
                    updateDateTime = "28.07.2026"
                )
            }

        val sorted = TcmbXmlParser.sortRates(rates)

        assertEquals(
            listOf("USD", "EUR", "CNY"),
            sorted.map(CurrencyRate::currencyCode)
        )
        assertFalse(sorted.any { it.currencyCode == "XDR" })
        assertFalse(sorted.any { it.currencyCode == "ZAR" })
    }

    @Test
    fun apinoktamMapper_returnsOnlySupportedFiveProducts() = runBlocking {
        val fakeApi =
            object : ApinoktamGoldApi {
                override suspend fun getGoldRates(): ApinoktamGoldResponse =
                    ApinoktamGoldResponse(
                        success = true,
                        data = ApinoktamGoldData(
                            updateEpochMillis = 1L,
                            updateDate = "2026-07-28 15:32:00",
                            items =
                                listOf(
                                    item("gram", 10.0, 11.0),
                                    item("ceyrek", 20.0, 21.0),
                                    item("yarim", 30.0, 31.0),
                                    item("tam", 40.0, 41.0),
                                    item("cumhuriyet", 50.0, 51.0),
                                    item("ons", 60.0, 61.0)
                                )
                        )
                    )
            }

        val rates = ApinoktamGoldRemoteDataSource(fakeApi).fetchRates()

        assertEquals(GoldType.entries, rates.map(GoldRate::type))
        assertEquals(50.0, rates.last().buyingPrice ?: 0.0, 0.0)
        assertEquals("2026-07-28 15:32:00", rates.first().sourceDate)
    }

    @Test
    fun apinoktamMapper_turnsInvalidPricesIntoMissingValues() = runBlocking {
        val fakeApi =
            object : ApinoktamGoldApi {
                override suspend fun getGoldRates(): ApinoktamGoldResponse =
                    ApinoktamGoldResponse(
                        success = true,
                        data = ApinoktamGoldData(
                            updateEpochMillis = null,
                            updateDate = null,
                            items = listOf(item("gram", 0.0, -1.0))
                        )
                    )
            }

        val rate = ApinoktamGoldRemoteDataSource(fakeApi).fetchRates().single()

        assertNull(rate.buyingPrice)
        assertNull(rate.sellingPrice)
        assertNull(rate.sourceDate)
    }

    private fun item(
        type: String,
        buying: Double,
        selling: Double
    ) = ApinoktamGoldItem(
        type = type,
        buyingPrice = buying,
        sellingPrice = selling
    )
}
