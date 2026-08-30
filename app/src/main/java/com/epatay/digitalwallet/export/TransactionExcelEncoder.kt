package com.epatay.digitalwallet.export

import com.epatay.digitalwallet.data.DecimalMath
import com.epatay.digitalwallet.data.GoldInputUnit
import com.epatay.digitalwallet.data.GoldType
import com.epatay.digitalwallet.data.TransactionType
import org.dhatim.fastexcel.Workbook
import java.io.OutputStream
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal object TransactionExcelEncoder {

    private val headers =
        listOf(
            "Kayıt türü",
            "Tarih / Ay",
            "Açıklama / Varlık",
            "Kategori",
            "İşlem / Varlık türü",
            "Miktar",
            "Birim",
            "Birim alış fiyatı (₺)",
            "Tutar / Limit (₺)",
            "Not"
        )

    fun write(
        outputStream: OutputStream,
        exportData: WalletExportData
    ) {
        require(!exportData.isEmpty) {
            "Dışa aktarılacak kayıt bulunamadı."
        }

        val workbook =
            Workbook(
                outputStream,
                "VarlıkCep",
                "1.0"
            )

        try {
            val sheet =
                workbook.newWorksheet(
                    "Kayıtlar"
                )
            var rowIndex = 0

            writeRow(
                sheet,
                rowIndex++,
                headers
            )

            exportData.transactions.forEach { transaction ->
                writeRow(
                    sheet,
                    rowIndex++,
                    listOf(
                        "İşlem",
                        transaction.date,
                        transaction.title,
                        transaction.category,
                        transaction.type.displayName(),
                        "",
                        "",
                        "",
                        safeNumber(transaction.amount),
                        ""
                    )
                )
            }

            exportData.categoryBudgets.forEach { budget ->
                writeRow(
                    sheet,
                    rowIndex++,
                    listOf(
                        "Bütçe",
                        formatMonthKey(budget.monthKey),
                        "Aylık kategori bütçesi",
                        budget.category,
                        "Bütçe limiti",
                        "",
                        "",
                        "",
                        safeNumber(budget.limitAmount),
                        ""
                    )
                )
            }

            exportData.currencyInvestments.forEach { investment ->
                val totalPurchaseCost =
                    DecimalMath.multiplyMoney(
                        investment.amount,
                        investment.buyPrice
                    )

                writeRow(
                    sheet,
                    rowIndex++,
                    listOf(
                        "Yatırım",
                        investment.buyDate,
                        investment.assetName,
                        "",
                        "Döviz",
                        safeNumber(investment.amount),
                        "birim",
                        safeNumber(investment.buyPrice),
                        totalPurchaseCost?.toDouble().orBlankNumber(),
                        investment.note.orEmpty()
                    )
                )
            }

            exportData.goldInvestments.forEach { investment ->
                val goldType =
                    runCatching {
                        GoldType.valueOf(
                            investment.goldType
                        )
                    }.getOrNull()
                val totalPurchaseCost =
                    investment.totalPurchaseCost
                        ?.takeIf(Double::isFinite)
                        ?: investment.purchaseUnitPrice?.let { price ->
                            DecimalMath.multiplyMoney(
                                investment.quantity,
                                price
                            )
                        }

                writeRow(
                    sheet,
                    rowIndex++,
                    listOf(
                        "Yatırım",
                        formatDate(investment.purchaseDate),
                        goldType?.displayName ?: investment.goldType,
                        "",
                        "Altın",
                        safeNumber(investment.quantity),
                        when (goldType?.inputUnit) {
                            GoldInputUnit.GRAM -> "gram"
                            GoldInputUnit.PIECE -> "adet"
                            null -> investment.unit
                        },
                        investment.purchaseUnitPrice.orBlankNumber(),
                        totalPurchaseCost.orBlankNumber(),
                        investment.note.orEmpty()
                    )
                )
            }

            val totals =
                TransactionExportTotals.from(
                    exportData.transactions
                )

            rowIndex++
            writeRow(
                sheet,
                rowIndex++,
                listOf("Özet")
            )
            writeRow(
                sheet,
                rowIndex++,
                summaryRow(
                    "Toplam gelir",
                    totals.totalIncome
                )
            )
            writeRow(
                sheet,
                rowIndex++,
                summaryRow(
                    "Toplam gider",
                    totals.totalExpense
                )
            )
            writeRow(
                sheet,
                rowIndex,
                summaryRow(
                    "Net bakiye",
                    totals.netTotal
                )
            )
        } finally {
            workbook.close()
        }
    }

    private fun writeRow(
        sheet: org.dhatim.fastexcel.Worksheet,
        rowIndex: Int,
        cells: List<Any?>
    ) {
        cells.forEachIndexed { columnIndex, value ->
            when (value) {
                null -> Unit
                is Number ->
                    sheet.value(
                        rowIndex,
                        columnIndex,
                        value.toDouble()
                    )
                else ->
                    sheet.value(
                        rowIndex,
                        columnIndex,
                        value.toString()
                    )
            }
        }
    }

    private fun summaryRow(
        label: String,
        amount: BigDecimal
    ): List<Any?> =
        listOf(
            "Özet",
            "",
            label,
            "",
            "",
            "",
            "",
            "",
            amount.toDouble(),
            ""
        )

    private fun safeNumber(
        value: Double
    ): Any =
        if (value.isFinite()) {
            value
        } else {
            ""
        }

    private fun Double?.orBlankNumber(): Any =
        this
            ?.takeIf(Double::isFinite)
            ?: ""

    private fun BigDecimal?.orBlankNumber(): Any =
        this?.toDouble() ?: ""

    private fun formatMonthKey(
        monthKey: Int
    ): String {
        val year = monthKey / 100
        val month = monthKey % 100

        return if (year in 1..9999 && month in 1..12) {
            String.format(Locale.ROOT, "%02d.%04d", month, year)
        } else {
            ""
        }
    }

    private fun formatDate(
        millis: Long?
    ): String =
        millis?.takeIf { it > 0L }
            ?.let { timestamp ->
                SimpleDateFormat(
                    "dd.MM.yyyy",
                    Locale.forLanguageTag("tr-TR")
                ).format(Date(timestamp))
            }
            .orEmpty()

    private fun TransactionType.displayName(): String =
        when (this) {
            TransactionType.INCOME -> "Gelir"
            TransactionType.EXPENSE -> "Gider"
        }
}
