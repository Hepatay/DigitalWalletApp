package com.epatay.digitalwallet.data

import com.google.gson.JsonObject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

interface GoldRemoteDataSource {
    suspend fun fetchRates(): List<GoldRate>
}

class TruncgilGoldRemoteDataSource(
    private val api: TruncgilGoldApi =
        Retrofit.Builder()
            .baseUrl("https://finans.truncgil.com/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TruncgilGoldApi::class.java),
    private val nowMillis: () -> Long = System::currentTimeMillis
) : GoldRemoteDataSource {

    override suspend fun fetchRates(): List<GoldRate> {
        val rootObject: JsonObject = api.getTodayRates()
        val updateDateText = rootObject.get("Update_Date")?.asString
        val fetchedAt = nowMillis()
        val sourceUpdatedAt = parseSourceDate(updateDateText) ?: fetchedAt

        // Map all dynamic keys normalized (handles space, hyphen, turkish chars)
        val normalizedItems = mutableMapOf<String, TruncgilItemData>()
        for (entry in rootObject.entrySet()) {
            if (entry.key == "Update_Date" || !entry.value.isJsonObject) continue
            val itemObj = entry.value.asJsonObject

            val buyingStr = itemObj.get("Buying")?.asString
                ?: itemObj.get("Alış")?.asString
                ?: itemObj.get("alis")?.asString
            val sellingStr = itemObj.get("Selling")?.asString
                ?: itemObj.get("Satış")?.asString
                ?: itemObj.get("satis")?.asString
            val typeStr = itemObj.get("Type")?.asString
                ?: itemObj.get("Tür")?.asString
            val changeStr = itemObj.get("Change")?.asString
                ?: itemObj.get("Değişim")?.asString

            val buying = parseTrPrice(buyingStr)
            val selling = parseTrPrice(sellingStr)

            if (buying != null && selling != null) {
                val itemData = TruncgilItemData(
                    rawKey = entry.key,
                    buying = buying,
                    selling = selling,
                    type = typeStr,
                    change = changeStr
                )
                normalizedItems[normalizeKey(entry.key)] = itemData
            }
        }

        val rates = GoldType.entries.mapNotNull { goldType ->
            val item = when (goldType) {
                GoldType.GRAM_GOLD -> normalizedItems["gramaltin"] ?: normalizedItems["gramhasaltin"]
                GoldType.QUARTER_GOLD -> normalizedItems["ceyrekaltin"]
                GoldType.HALF_GOLD -> normalizedItems["yarimaltin"]
                GoldType.FULL_GOLD -> normalizedItems["tamaltin"]
                GoldType.ATA_REPUBLIC_GOLD -> normalizedItems["cumhuriyetaltini"] ?: normalizedItems["ataaltin"]
            } ?: return@mapNotNull null

            val prices = MarketPriceValidator.validate(
                buyingPrice = item.buying,
                sellingPrice = item.selling
            ) ?: ValidMarketPrices(
                buyingPrice = BigDecimal.valueOf(item.buying),
                sellingPrice = BigDecimal.valueOf(item.selling),
                spread = BigDecimal.valueOf(item.selling - item.buying),
                spreadPercentage = BigDecimal.ZERO
            )

            GoldRate(
                type = goldType,
                buyingPrice = prices.buyingPrice.toDouble(),
                sellingPrice = prices.sellingPrice.toDouble(),
                source = "Trunçgil Finans",
                sourceDate = updateDateText,
                sourceUpdatedAt = sourceUpdatedAt,
                fetchedAt = fetchedAt,
                isReference = true
            )
        }

        if (rates.isEmpty()) {
            throw GoldDataValidationException("Trunçgil servisinden geçerli altın verisi alınamadı.")
        }

        return rates
    }

    private fun normalizeKey(key: String): String {
        return key.trim().lowercase(Locale.ROOT)
            .replace("ı", "i")
            .replace("ğ", "g")
            .replace("ü", "u")
            .replace("ş", "s")
            .replace("ö", "o")
            .replace("ç", "c")
            .replace("-", "")
            .replace(" ", "")
            .replace("/", "")
            .replace("_", "")
    }

    private fun parseTrPrice(raw: String?): Double? {
        if (raw.isNullOrBlank()) return null
        val clean = raw.trim()
            .replace("$", "")
            .replace("TL", "")
            .replace("₺", "")
            .replace(" ", "")
        val normalized = if (clean.contains(",")) {
            clean.replace(".", "").replace(",", ".")
        } else {
            clean
        }
        return normalized.toDoubleOrNull()
    }

    private fun parseSourceDate(value: String?): Long? =
        value?.trim()?.takeIf(String::isNotEmpty)?.let { text ->
            runCatching {
                SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ROOT).apply {
                    isLenient = false
                    timeZone = TimeZone.getTimeZone("Europe/Istanbul")
                }.parse(text)?.time
            }.getOrNull()
        }
}

data class TruncgilItemData(
    val rawKey: String,
    val buying: Double,
    val selling: Double,
    val type: String?,
    val change: String?
)

class GoldDataValidationException(message: String) : IllegalStateException(message)
