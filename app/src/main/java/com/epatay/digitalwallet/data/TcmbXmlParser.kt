package com.epatay.digitalwallet.data

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader
import java.util.Locale

class TcmbXmlParser {

    fun parse(
        xml: String
    ): CurrencyRateDocument {
        val parser =
            Xml.newPullParser()

        runCatching {
            parser.setFeature(
                "http://xmlpull.org/v1/doc/features.html#process-docdecl",
                false
            )
        }

        parser.setInput(
            StringReader(xml)
        )

        val rates =
            mutableListOf<CurrencyRate>()

        var updateDateTime: String? = null
        var eventType =
            parser.eventType

        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (
                eventType == XmlPullParser.START_TAG &&
                parser.name == "Tarih_Date"
            ) {
                val date =
                    parser.getAttributeValue(
                        null,
                        "Tarih"
                    )

                if (!date.isNullOrBlank()) {
                    updateDateTime = date.trim()
                }
            }

            if (
                eventType == XmlPullParser.START_TAG &&
                parser.name == "Currency"
            ) {
                val currency =
                    readCurrency(
                        parser = parser,
                        updateDateTime =
                            updateDateTime.orEmpty()
                    )

                if (currency != null) {
                    rates.add(currency)
                }
            }

            eventType =
                parser.next()
        }

        return CurrencyRateDocument(
            updateDateTime = updateDateTime.orEmpty(),
            rates = sortRates(rates)
        )
    }

    private fun readCurrency(
        parser: XmlPullParser,
        updateDateTime: String
    ): CurrencyRate? {
        val code =
            parser.getAttributeValue(
                null,
                "CurrencyCode"
            )
                ?: parser.getAttributeValue(
                    null,
                    "Kod"
                )
                ?: return null

        var unit = 1
        var name = ""
        var currencyName = ""
        var forexBuying: Double? = null
        var forexSelling: Double? = null

        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) {
                continue
            }

            when (parser.name) {
                "Unit" -> {
                    unit =
                        readText(parser)
                            .toIntOrNull()
                            ?.takeIf { it > 0 }
                            ?: 1
                }

                "Isim" -> {
                    name =
                        readText(parser)
                }

                "CurrencyName" -> {
                    currencyName =
                        readText(parser)
                }

                "ForexBuying" -> {
                    forexBuying =
                        readNullableDouble(parser)
                }

                "ForexSelling" -> {
                    forexSelling =
                        readNullableDouble(parser)
                }

                else -> {
                    skip(parser)
                }
            }
        }

        val normalizedCode =
            code.trim().uppercase(Locale.US)

        if (normalizedCode !in CurrencyFlagProvider.supportedCodes) {
            return null
        }

        return CurrencyRate(
            currencyCode = normalizedCode,
            unit = unit,
            name =
                name.ifBlank { currencyName },
            currencyName = currencyName,
            forexBuying = forexBuying,
            forexSelling = forexSelling,
            updateDateTime = updateDateTime
        )
    }

    private fun readNullableDouble(
        parser: XmlPullParser
    ): Double? {
        return readText(parser)
            .takeIf { text ->
                text.isNotBlank()
            }
            ?.toDoubleOrNull()
            ?.takeIf {
                it.isFinite() &&
                    it > 0.0
            }
    }

    private fun readText(
        parser: XmlPullParser
    ): String {
        var result = ""

        if (parser.next() == XmlPullParser.TEXT) {
            result =
                parser.text?.trim().orEmpty()

            parser.nextTag()
        }

        return result
    }

    private fun skip(
        parser: XmlPullParser
    ) {
        if (parser.eventType != XmlPullParser.START_TAG) {
            return
        }

        var depth = 1

        while (depth != 0) {
            when (parser.next()) {
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.START_TAG -> depth++
            }
        }
    }

    companion object {
        val currencyPriority: List<String> =
            listOf(
                "USD",
                "EUR",
                "GBP",
                "CHF",
                "JPY",
                "CAD",
                "AUD",
                "CNY",
                "SAR",
                "AED",
                "KWD",
                "QAR",
                "RUB",
                "NOK",
                "SEK",
                "DKK"
            )

        fun sortRates(
            rates: List<CurrencyRate>
        ): List<CurrencyRate> {
            val priorityIndex =
                currencyPriority
                    .withIndex()
                    .associate {
                        it.value to it.index
                    }

            return rates
                .filterNot {
                    it.currencyCode.uppercase(Locale.ROOT) !in
                        CurrencyFlagProvider.supportedCodes
                }
                .sortedWith(
                compareBy<CurrencyRate> { rate ->
                    priorityIndex[rate.currencyCode]
                        ?: Int.MAX_VALUE
                }.thenBy { rate ->
                    if (rate.currencyCode in priorityIndex) {
                        ""
                    } else {
                        rate.currencyCode
                    }
                }
            )
        }
    }
}
