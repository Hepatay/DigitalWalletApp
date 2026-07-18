package com.epatay.digitalwallet.data

data class CurrencyItem(
    val code: String,       // Örn: "USD"
    val name: String,       // Örn: "*ABD Doları"
    val rateValue: Double,  // Örn: 32
    val flagIcon: Int
)