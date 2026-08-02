package com.epatay.digitalwallet.data

import com.epatay.digitalwallet.BuildConfig
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

interface GoldRemoteDataSource {
    suspend fun fetchRates(): List<GoldRate>
}

class ApinoktamGoldRemoteDataSource(
    private val api: ApinoktamGoldApi =
        Retrofit.Builder()
            .baseUrl("https://api.apinoktam.erenozdemir.com.tr/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApinoktamGoldApi::class.java),
    private val apiKey: String = BuildConfig.APINOKTAM_API_KEY,
    private val nowMillis: () -> Long = System::currentTimeMillis
) : GoldRemoteDataSource {

    override suspend fun fetchRates(): List<GoldRate> {
        val normalizedApiKey = apiKey.trim().takeIf(String::isNotEmpty)
        val response =
            api.getGoldRates(
                endpoint =
                    if (normalizedApiKey == null) PUBLIC_ENDPOINT
                    else AUTHENTICATED_ENDPOINT,
                apiKey = normalizedApiKey
            )
        val data =
            response.data
                ?.takeIf { response.success }
                ?: throw GoldDataValidationException(
                    "Altın servisi geçersiz yanıt döndürdü."
                )

        val fetchedAt = nowMillis()
        val sourceUpdatedAt =
            normalizeSourceTime(data, fetchedAt)
                ?: throw GoldDataValidationException(
                    "Altın verisinin kaynak zamanı geçersiz."
                )
        val byType =
            data.items
                .mapNotNull { item ->
                    item.type
                        ?.trim()
                        ?.lowercase(Locale.ROOT)
                        ?.takeIf(String::isNotEmpty)
                        ?.let { it to item }
                }
                .groupBy(
                    keySelector = { it.first },
                    valueTransform = { it.second }
                )
                .mapValues { (_, values) -> values.singleOrNull() }

        val rates = GoldType.entries.map { goldType ->
            val remoteItem =
                when (goldType) {
                    GoldType.GRAM_GOLD -> byType["gram"]
                    GoldType.QUARTER_GOLD -> byType["ceyrek"]
                    GoldType.HALF_GOLD -> byType["yarim"]
                    GoldType.FULL_GOLD -> byType["tam"]
                    GoldType.ATA_REPUBLIC_GOLD ->
                        byType["ata"] ?: byType["cumhuriyet"]
                } ?: throw GoldDataValidationException(
                    "${goldType.displayName} verisi eksik veya yinelenmiş."
                )

            val prices =
                MarketPriceValidator.validate(
                    buyingPrice = remoteItem.buyingPrice,
                    sellingPrice = remoteItem.sellingPrice
                ) ?: throw GoldDataValidationException(
                    "${goldType.displayName} alış/satış verisi geçersiz."
                )

            GoldRate(
                type = goldType,
                buyingPrice = prices.buyingPrice.toDouble(),
                sellingPrice = prices.sellingPrice.toDouble(),
                source = "API Noktam / Trunçgil Finans",
                sourceDate = data.updateDate?.takeIf(String::isNotBlank),
                sourceUpdatedAt = sourceUpdatedAt,
                fetchedAt = fetchedAt,
                isReference = true
            )
        }

        if (rates.map(GoldRate::type).toSet() != GoldType.entries.toSet()) {
            throw GoldDataValidationException("Desteklenen altın listesi eksik.")
        }

        return rates
    }

    private fun normalizeSourceTime(
        data: ApinoktamGoldData,
        fetchedAt: Long
    ): Long? {
        val epoch =
            data.updateEpochMillis
                ?.let { if (it in 1..9_999_999_999L) it * 1000L else it }
                ?: parseSourceDate(data.updateDate)

        return epoch?.takeIf {
            it in (fetchedAt - MAX_SOURCE_AGE_MILLIS)..
                (fetchedAt + MAX_FUTURE_SKEW_MILLIS)
        }
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

    private companion object {
        const val AUTHENTICATED_ENDPOINT = "v1/altin"
        const val PUBLIC_ENDPOINT = "public/v1/altin"
        const val MAX_SOURCE_AGE_MILLIS = 6L * 60L * 60L * 1000L
        const val MAX_FUTURE_SKEW_MILLIS = 10L * 60L * 1000L
    }
}

class GoldDataValidationException(message: String) : IllegalStateException(message)
