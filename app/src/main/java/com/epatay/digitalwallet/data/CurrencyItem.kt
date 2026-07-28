package com.epatay.digitalwallet.data

import androidx.annotation.DrawableRes

data class CurrencyItem(
    val code: String,
    val name: String,
    val unit: Int,
    val forexBuying: Double?,
    val forexSelling: Double?,
    @DrawableRes val flagResId: Int
)
