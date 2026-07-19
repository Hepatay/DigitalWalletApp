package com.epatay.digitalwallet.data

import com.google.gson.annotations.SerializedName

data class GoldPriceResponse(
    @SerializedName("name")
    val name: String?,
    @SerializedName("price")
    val price: Double,
    @SerializedName("symbol")
    val symbol: String?,
    @SerializedName("updatedAt")
    val updatedAt: String?
)
