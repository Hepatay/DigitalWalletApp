package com.epatay.digitalwallet.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "currency_rates")
data class CurrencyRateEntity(
    @PrimaryKey
    val currencyCode: String,
    val unit: Int,
    val name: String,
    val currencyName: String,
    val forexBuying: Double?,
    val forexSelling: Double?,
    val updateDateTime: String,
    val fetchedAtMillis: Long
) {
    fun toCurrencyRate(): CurrencyRate {
        return CurrencyRate(
            currencyCode = currencyCode,
            unit = unit,
            name = name,
            currencyName = currencyName,
            forexBuying = forexBuying,
            forexSelling = forexSelling,
            updateDateTime = updateDateTime
        )
    }
}

fun CurrencyRate.toEntity(
    fetchedAtMillis: Long
): CurrencyRateEntity {
    return CurrencyRateEntity(
        currencyCode = currencyCode,
        unit = unit,
        name = name,
        currencyName = currencyName,
        forexBuying = forexBuying,
        forexSelling = forexSelling,
        updateDateTime = updateDateTime,
        fetchedAtMillis = fetchedAtMillis
    )
}
