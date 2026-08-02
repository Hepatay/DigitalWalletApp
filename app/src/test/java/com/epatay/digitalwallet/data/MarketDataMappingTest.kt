package com.epatay.digitalwallet.data

import com.epatay.digitalwallet.R
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
            CurrencyRate(code, 1, code, code, 1.0, 1.1, "02.08.2026")
        }

        val sorted = TcmbXmlParser.sortRates(rates)

        assertEquals(listOf("USD", "EUR", "CNY"), sorted.map(CurrencyRate::currencyCode))
        assertFalse(sorted.any { it.currencyCode == "XDR" })
        assertFalse(sorted.any { it.currencyCode == "ZAR" })
    }

    @Test
    fun apinoktamMapper_mapsExactFiveProducts_andNeverUsesHasForGram() = runBlocking {
        val api = FakeGoldApi(
            items = validItems() + item("has", 6_100.0, 6_101.0)
        )

        val rates = ApinoktamGoldRemoteDataSource(
            api = api,
            apiKey = "test-key",
            nowMillis = { now }
        ).fetchRates()

        assertEquals(GoldType.entries, rates.map(GoldRate::type))
        assertEquals(6_174.46, rates.first().buyingPrice, 0.0)
        assertEquals(6_175.37, rates.first().sellingPrice, 0.0)
        assertEquals(40_436.04, rates.last().buyingPrice, 0.0)
        assertEquals("v1/altin", api.requestedEndpoint)
        assertEquals("test-key", api.requestedApiKey)
        assertEquals(now - 1_000, rates.first().sourceUpdatedAt)
        assertEquals(now, rates.first().fetchedAt)
    }

    @Test
    fun apinoktamMapper_rejectsSwappedMissingAndStalePrices() {
        val swapped = validItems().map {
            if (it.type == "gram") item("gram", 6_175.37, 6_174.46) else it
        }
        assertInvalid(swapped, now - 1_000)
        assertInvalid(
            validItems().filterNot { it.type == "ata" || it.type == "cumhuriyet" },
            now - 1_000
        )
        assertInvalid(validItems(), now - 7L * 60L * 60L * 1000L)
    }

    @Test
    fun apinoktamMapper_rejectsNullZeroNegativeAndExtremePrices() {
        listOf<Double?>(null, 0.0, -1.0, 1_000_000_001.0).forEach { invalid ->
            val items = validItems().map {
                if (it.type == "gram") it.copy(buyingPrice = invalid) else it
            }
            assertInvalid(items, now - 1_000)
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

    private fun assertInvalid(items: List<ApinoktamGoldItem>, sourceTime: Long) {
        assertThrows(GoldDataValidationException::class.java) {
            runBlocking {
                ApinoktamGoldRemoteDataSource(
                    api = FakeGoldApi(items, sourceTime),
                    apiKey = "",
                    nowMillis = { now }
                ).fetchRates()
            }
        }
    }

    private fun validItems() =
        listOf(
            item("gram", 6_174.46, 6_175.37),
            item("ceyrek", 9_802.68, 10_027.02),
            item("yarim", 19_544.09, 20_054.04),
            item("tam", 39_210.71, 39_985.43),
            item("cumhuriyet", 40_632.0, 41_252.0),
            item("ata", 40_436.04, 41_457.28)
        )

    private fun rate() = GoldRate(
        type = GoldType.GRAM_GOLD,
        buyingPrice = 6_174.46,
        sellingPrice = 6_175.37,
        source = "API Noktam / Trunçgil Finans",
        sourceDate = "2026-08-02 11:30:02",
        sourceUpdatedAt = now - 1_000,
        fetchedAt = now
    )

    private fun item(type: String, buying: Double?, selling: Double?) =
        ApinoktamGoldItem(type, buying, selling)

    private inner class FakeGoldApi(
        private val items: List<ApinoktamGoldItem>,
        private val sourceTime: Long = now - 1_000
    ) : ApinoktamGoldApi {
        var requestedEndpoint: String? = null
        var requestedApiKey: String? = null

        override suspend fun getGoldRates(
            endpoint: String,
            apiKey: String?
        ): ApinoktamGoldResponse {
            requestedEndpoint = endpoint
            requestedApiKey = apiKey
            return ApinoktamGoldResponse(
                success = true,
                data = ApinoktamGoldData(
                    updateEpochMillis = sourceTime,
                    updateDate = "2026-08-02 11:30:02",
                    items = items
                )
            )
        }
    }
}
