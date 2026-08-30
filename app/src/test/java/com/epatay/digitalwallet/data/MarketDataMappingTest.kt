package com.epatay.digitalwallet.data

import com.epatay.digitalwallet.R
import com.google.gson.JsonObject
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class MarketDataMappingTest {

    private val now = 1_785_659_500_000L

    @Test
    fun currencyFlags_andChartColors_areStableLocalResources() {
        assertEquals(R.drawable.flag_usd, CurrencyFlagProvider.getFlagResId("usd"))
        assertEquals(R.drawable.flag_dak, CurrencyFlagProvider.getFlagResId("DKK"))
        assertEquals(R.drawable.flag_azn, CurrencyFlagProvider.getFlagResId("AZN"))
        assertThrows(IllegalArgumentException::class.java) {
            CurrencyFlagProvider.getFlagResId("UNKNOWN")
        }
        assertEquals(0xFF003399.toInt(), CurrencyFlagProvider.getChartColor("EUR"))
    }

    @Test
    fun sortRates_filtersXdr_andUsesPriorityOrder() {
        val rates = listOf("ZAR", "XDR", "EUR", "USD", "CNY").map { code ->
            CurrencyRate(code, 1, code, code, 1.0, 1.1, "02.08.2026", System.currentTimeMillis())
        }

        val sorted = TcmbXmlParser.sortRates(rates)

        assertEquals(listOf("USD", "EUR", "CNY"), sorted.map(CurrencyRate::currencyCode))
        assertFalse(sorted.any { it.currencyCode == "XDR" })
        assertFalse(sorted.any { it.currencyCode == "ZAR" })
    }

    @Test
    fun truncgilMapper_mapsExactFiveProducts() = runBlocking {
        val json = JsonObject().apply {
            addProperty("Update_Date", "2026-08-28 16:45:02")
            add("gram-altin", createItemObj("7.142,80", "7.143,76", "%0,29"))
            add("ceyrek-altin", createItemObj("11.331,11", "11.589,28", "%0,68"))
            add("yarim-altin", createItemObj("22.591,39", "23.178,56", "%0,68"))
            add("tam-altin", createItemObj("45.324,43", "46.215,36", "%0,68"))
            add("cumhuriyet-altini", createItemObj("46.877,00", "47.587,00", "%0,45"))
            add("ata-altin", createItemObj("46.740,82", "47.916,54", "%0,68"))
        }

        val api = FakeTruncgilApi(json)
        val rates = TruncgilGoldRemoteDataSource(
            api = api,
            nowMillis = { now }
        ).fetchRates()

        assertEquals(GoldType.entries, rates.map(GoldRate::type))
        assertEquals(7142.80, rates.first().buyingPrice, 0.001)
        assertEquals(7143.76, rates.first().sellingPrice, 0.001)
        assertEquals("2026-08-28 16:45:02", rates.first().sourceDate)
        assertEquals("Trunçgil Finans", rates.first().source)
    }

    @Test
    fun truncgilMapper_rejectsEmptyResponse() {
        assertThrows(GoldDataValidationException::class.java) {
            runBlocking {
                TruncgilGoldRemoteDataSource(
                    api = FakeTruncgilApi(JsonObject()),
                    nowMillis = { now }
                ).fetchRates()
            }
        }
    }

    @Test
    fun offlineFallback_returnsLastSuccessfulRoomSnapshot() {
        val cached = rate()
        val result = GoldFallbackPolicy.resolve(
            cachedRates = listOf(cached),
            reason = GoldCacheReason.NO_INTERNET,
            error = GoldLoadResult.NoInternet
        )

        assertTrue(result is GoldLoadResult.Cached)
        assertEquals(listOf(cached), (result as GoldLoadResult.Cached).rates)
    }

    @Test
    fun marketValidator_rejectsReversedPair_soFieldSwapCannotPass() {
        assertNull(MarketPriceValidator.validate(47.4305, 47.3452))
    }

    private fun createItemObj(buying: String, selling: String, change: String): JsonObject {
        return JsonObject().apply {
            addProperty("Buying", buying)
            addProperty("Selling", selling)
            addProperty("Change", change)
            addProperty("Type", "Gold")
        }
    }

    private fun rate() = GoldRate(
        type = GoldType.GRAM_GOLD,
        buyingPrice = 7_142.80,
        sellingPrice = 7_143.76,
        source = "Trunçgil Finans",
        sourceDate = "2026-08-28 16:45:02",
        sourceUpdatedAt = now - 1_000,
        fetchedAt = now
    )

    private inner class FakeTruncgilApi(
        private val json: JsonObject
    ) : TruncgilGoldApi {
        override suspend fun getTodayRates(): JsonObject = json
    }
}
