package com.epatay.digitalwallet.data

import androidx.annotation.DrawableRes

data class CurrencyItem(
    val code: String,
    val name: String,
    val unit: Int,
    val forexBuying: Double?,
    val forexSelling: Double?,
    @DrawableRes val flagResId: Int,
    val source: String = "TCMB",
    val sourceUpdatedAt: String = "",
    val fetchedAtMillis: Long = 0L,
    val isReference: Boolean = true
) {
    val spreadTl: Double?
        get() = if (forexSelling != null && forexBuying != null) {
            forexSelling - forexBuying
        } else null

    val spreadPercentage: Double?
        get() = if (forexSelling != null && forexBuying != null && forexBuying > 0.0) {
            ((forexSelling - forexBuying) / forexBuying) * 100.0
        } else null
}
