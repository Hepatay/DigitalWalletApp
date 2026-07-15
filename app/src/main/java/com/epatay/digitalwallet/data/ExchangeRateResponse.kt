package com.epatay.digitalwallet.data

// Dikkat: Süslü parantez yerine normal parantez kullanıyoruz
data class ExchangeRateResponse(
    val base_code: String,
    val conversion_rates: Map<String, Double>
)