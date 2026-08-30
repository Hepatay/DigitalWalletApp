package com.epatay.digitalwallet.data

import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import com.epatay.digitalwallet.R
import java.util.Locale

object CurrencyFlagProvider {

    private val localFlagsByCode: Map<String, Int> =
        mapOf(
            "USD" to R.drawable.flag_usd,
            "EUR" to R.drawable.flag_eur,
            "GBP" to R.drawable.flag_gbp,
            "CHF" to R.drawable.flag_chf,
            "JPY" to R.drawable.flag_jpy,
            "CAD" to R.drawable.flag_cad,
            "AUD" to R.drawable.flag_aud,
            "CNY" to R.drawable.flag_cny,
            "RUB" to R.drawable.flag_rub,
            "SAR" to R.drawable.flag_sar,
            "AED" to R.drawable.flag_aed,
            "KWD" to R.drawable.flag_kwd,
            "QAR" to R.drawable.flag_qar,
            "NOK" to R.drawable.flag_nok,
            "SEK" to R.drawable.flag_sek,
            "DKK" to R.drawable.flag_dak,
            "RON" to R.drawable.flag_ron,
            "PKR" to R.drawable.flag_pkr,
            "KRW" to R.drawable.flag_krw,
            "AZN" to R.drawable.flag_azn,
            "KZT" to R.drawable.flag_kzt
        )

    val supportedCodes: Set<String> =
        localFlagsByCode.keys

    @DrawableRes
    fun getFlagResId(currencyCode: String): Int =
        requireNotNull(
            localFlagsByCode[
                currencyCode.uppercase(Locale.ROOT)
            ]
        ) {
            "Yerel bayrağı bulunmayan para birimi: $currencyCode"
        }

    @DrawableRes
    fun getFlagResIdSafe(currencyCode: String): Int =
        localFlagsByCode[
            currencyCode.uppercase(Locale.ROOT)
        ] ?: R.drawable.ic_exchange_horizontal

    fun getCurrencyDisplayName(currencyCode: String): String =
        when (currencyCode.uppercase(Locale.ROOT)) {
            "USD" -> "ABD Doları"
            "EUR" -> "Euro"
            "GBP" -> "İngiliz Sterlini"
            "CHF" -> "İsviçre Frangı"
            "JPY" -> "Japon Yeni"
            "CAD" -> "Kanada Doları"
            "AUD" -> "Avustralya Doları"
            "CNY" -> "Çin Yuanı"
            "RUB" -> "Rus Rublesi"
            "SAR" -> "Suudi Arabistan Riyali"
            "AED" -> "BAE Dirhemi"
            "KWD" -> "Kuveyt Dinarı"
            "QAR" -> "Katar Riyali"
            "NOK" -> "Norveç Kronu"
            "SEK" -> "İsveç Kronu"
            "DKK" -> "Danimarka Kronu"
            "RON" -> "Rumen Leyi"
            "PKR" -> "Pakistan Rupisi"
            "KRW" -> "Güney Kore Wonu"
            "AZN" -> "Azerbaycan Manatı"
            "KZT" -> "Kazakistan Tengesi"
            else -> currencyCode
        }

    @ColorInt
    fun getChartColor(currencyCode: String): Int =
        when (currencyCode.uppercase(Locale.ROOT)) {
            "USD" -> 0xFF3C3B6E.toInt()
            "EUR" -> 0xFF003399.toInt()
            "GBP" -> 0xFFC8102E.toInt()
            "CHF" -> 0xFFD52B1E.toInt()
            "JPY" -> 0xFFBC002D.toInt()
            "CAD" -> 0xFFD80621.toInt()
            "AUD" -> 0xFF012169.toInt()
            "CNY" -> 0xFFDE2910.toInt()
            "RUB" -> 0xFF1C3578.toInt()
            "SAR" -> 0xFF006C35.toInt()
            "AED" -> 0xFF00732F.toInt()
            "KWD" -> 0xFF007A3D.toInt()
            "QAR" -> 0xFF8A1538.toInt()
            "NOK" -> 0xFFBA0C2F.toInt()
            "SEK" -> 0xFF006AA7.toInt()
            "DKK" -> 0xFFC60C30.toInt()
            "BGN" -> 0xFF00966E.toInt()
            "RON" -> 0xFF002B7F.toInt()
            "IRR" -> 0xFF239F40.toInt()
            "KRW" -> 0xFFCD2E3A.toInt()
            "AZN" -> 0xFF00A3CC.toInt()
            "PKR" -> 0xFF01411C.toInt()
            "KZT" -> 0xFF00AFCA.toInt()
            "PLN", "HUF", "CZK" -> 0xFFB22234.toInt()
            "INR" -> 0xFFFF8C1A.toInt()
            else -> 0xFF607D8B.toInt()
        }
}
