package com.epatay.digitalwallet.data

data class CurrencyRate(
    val currencyCode: String,
    val unit: Int,
    val name: String,
    val currencyName: String,
    val forexBuying: Double?,
    val forexSelling: Double?,
    val updateDateTime: String
)

data class CurrencyRateDocument(
    val updateDateTime: String,
    val rates: List<CurrencyRate>
)
