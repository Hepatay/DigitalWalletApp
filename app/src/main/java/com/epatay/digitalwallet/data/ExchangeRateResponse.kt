package com.epatay.digitalwallet.data

import com.google.gson.annotations.SerializedName

data class ExchangeRateResponse(

    @SerializedName("result")
    val result: String? = null,

    @SerializedName(
        value = "rates",
        alternate = ["conversion_rates"]
    )
    val conversion_rates: Map<String, Double> =
        emptyMap(),

    @SerializedName("base_code")
    val baseCode: String? = null,

    @SerializedName("time_last_update_unix")
    val lastUpdateUnix: Long? = null
)
