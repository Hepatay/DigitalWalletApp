package com.epatay.digitalwallet.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

interface GoldRemoteDataSource {
    suspend fun fetchRates(): List<GoldRate>
}

class ApinoktamGoldRemoteDataSource(
    private val api: ApinoktamGoldApi =
        Retrofit.Builder()
            .baseUrl("https://api.apinoktam.erenozdemir.com.tr/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApinoktamGoldApi::class.java)
) : GoldRemoteDataSource {

    override suspend fun fetchRates(): List<GoldRate> {
        val response = api.getGoldRates()
        val data =
            response.data
                ?.takeIf { response.success }
                ?: error("Altın servisi geçersiz yanıt döndürdü.")

        val fetchedAt = System.currentTimeMillis()
        val byType =
            data.items
                .mapNotNull { item ->
                    item.type?.lowercase()?.let { it to item }
                }
                .toMap()

        return GoldType.entries.mapNotNull { goldType ->
            val remoteItem =
                when (goldType) {
                    GoldType.GRAM_GOLD -> byType["gram"]
                    GoldType.QUARTER_GOLD -> byType["ceyrek"]
                    GoldType.HALF_GOLD -> byType["yarim"]
                    GoldType.FULL_GOLD -> byType["tam"]
                    GoldType.ATA_REPUBLIC_GOLD ->
                        byType["cumhuriyet"] ?: byType["ata"]
                } ?: return@mapNotNull null

            GoldRate(
                type = goldType,
                buyingPrice = remoteItem.buyingPrice.validPrice(),
                sellingPrice = remoteItem.sellingPrice.validPrice(),
                source = "apinoktam (truncgil.com verisi)",
                sourceDate = data.updateDate?.takeIf(String::isNotBlank),
                fetchedAt = fetchedAt,
                isReference = true
            )
        }
    }

    private fun Double?.validPrice(): Double? =
        this?.takeIf { it.isFinite() && it > 0.0 }
}
