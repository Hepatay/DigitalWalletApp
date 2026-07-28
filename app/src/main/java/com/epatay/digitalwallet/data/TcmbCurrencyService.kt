package com.epatay.digitalwallet.data

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class TcmbCurrencyService {

    fun fetchTodayXml(): String {
        val connection =
            (URL(TCMB_TODAY_XML_URL).openConnection()
                as HttpURLConnection)

        return try {
            connection.connectTimeout = 15_000
            connection.readTimeout = 15_000
            connection.requestMethod = "GET"
            connection.setRequestProperty(
                "Accept",
                "application/xml,text/xml,*/*"
            )
            connection.setRequestProperty(
                "User-Agent",
                "VarlikCep Android"
            )

            val responseCode =
                connection.responseCode

            if (responseCode !in 200..299) {
                throw IOException(
                    "TCMB service returned HTTP $responseCode"
                )
            }

            connection.inputStream
                .bufferedReader(Charsets.UTF_8)
                .use { reader ->
                    reader.readText()
                }
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        const val TCMB_TODAY_XML_URL: String =
            "https://www.tcmb.gov.tr/kurlar/today.xml"
    }
}
