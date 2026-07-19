package com.epatay.digitalwallet.data

import com.google.gson.annotations.SerializedName

data class ExchangeRateResponse(

    val result: String? = null,

    @SerializedName(
        value = "rates",
        alternate = ["conversion_rates"]
    )
    val conversion_rates: Map<String, Double> =
        emptyMap()
)