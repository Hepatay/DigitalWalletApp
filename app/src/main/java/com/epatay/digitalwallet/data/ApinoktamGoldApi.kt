package com.epatay.digitalwallet.data

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET

interface ApinoktamGoldApi {
    @GET("public/v1/altin")
    suspend fun getGoldRates(): ApinoktamGoldResponse
}

data class ApinoktamGoldResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("data")
    val data: ApinoktamGoldData?
)

data class ApinoktamGoldData(
    @SerializedName("guncellemeZamani")
    val updateEpochMillis: Long?,
    @SerializedName("updateDate")
    val updateDate: String?,
    @SerializedName("kalemler")
    val items: List<ApinoktamGoldItem> = emptyList()
)

data class ApinoktamGoldItem(
    @SerializedName("tur")
    val type: String?,
    @SerializedName("alis")
    val buyingPrice: Double?,
    @SerializedName("satis")
    val sellingPrice: Double?
)
