package com.epatay.digitalwallet.ui

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object GoldRateFormatter {
    private val locale = Locale.forLanguageTag("tr-TR")
    private val currency =
        NumberFormat.getNumberInstance(locale).apply {
            minimumFractionDigits = 2
            maximumFractionDigits = 2
        }
    private val fetchedAt =
        SimpleDateFormat("dd.MM.yyyy HH:mm", locale).apply {
            timeZone = TimeZone.getTimeZone("Europe/Istanbul")
        }

    fun price(value: Double?): String =
        value?.let { "${currency.format(it)} TL" } ?: "-"

    fun percentage(value: Double): String = currency.format(value)

    fun fetchedAt(value: Long): String = fetchedAt.format(Date(if (value < 1000000000000L) value * 1000 else value))

    fun sourceDate(value: String?): String =
        value?.takeIf(String::isNotBlank) ?: "Kaynak tarihi belirtilmemiş"
}
