package com.epatay.digitalwallet.ui

internal object PortfolioSourceLabelFormatter {
    fun format(
        source: String?,
        sourceUpdatedText: String?,
        fetchedAtText: String?
    ): String =
        buildString {
            append("Kaynak: ")
            append(source?.takeIf(String::isNotBlank) ?: "Bulunamadı")

            sourceUpdatedText
                ?.takeIf(String::isNotBlank)
                ?.let {
                    append(" • Veri: ")
                    append(it)
                }

            fetchedAtText
                ?.takeIf(String::isNotBlank)
                ?.let {
                    append(" • Çekildi: ")
                    append(it)
                }
        }
}
