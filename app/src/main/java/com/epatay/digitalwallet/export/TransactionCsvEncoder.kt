package com.epatay.digitalwallet.export

import com.epatay.digitalwallet.data.CategoryBudget
import com.epatay.digitalwallet.data.DecimalMath
import com.epatay.digitalwallet.data.GoldInputUnit
import com.epatay.digitalwallet.data.GoldType
import com.epatay.digitalwallet.data.InvestmentItem
import com.epatay.digitalwallet.data.Transaction
import com.epatay.digitalwallet.data.TransactionType
import com.epatay.digitalwallet.data.UserGoldAssetEntity
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class WalletExportData(
    val transactions: List<Transaction> = emptyList(),
    val categoryBudgets: List<CategoryBudget> = emptyList(),
    val currencyInvestments: List<InvestmentItem> = emptyList(),
    val goldInvestments: List<UserGoldAssetEntity> = emptyList()
) {
    val isEmpty: Boolean
        get() =
            transactions.isEmpty() &&
                categoryBudgets.isEmpty() &&
                currencyInvestments.isEmpty() &&
                goldInvestments.isEmpty()

    val recordCount: Int
        get() =
            transactions.size +
                categoryBudgets.size +
                currencyInvestments.size +
                goldInvestments.size
}

internal object TransactionCsvEncoder {

    private const val COLUMN_SEPARATOR = ";"
    private const val LINE_SEPARATOR = "\r\n"

    private val utf8Bom =
        byteArrayOf(
            0xEF.toByte(),
            0xBB.toByte(),
            0xBF.toByte()
        )

    fun encode(
        transactions: List<Transaction>
    ): ByteArray =
        encode(
            WalletExportData(
                transactions = transactions
            )
        )

    fun encode(
        exportData: WalletExportData
    ): ByteArray {
        require(!exportData.isEmpty) {
            "Dışa aktarılacak kayıt bulunamadı."
        }

        val totals =
            TransactionExportTotals.from(
                exportData.transactions
            )
        val csv = buildString {
            appendRow(
                "Kayıt türü",
                "Tarih / Ay",
                "Açıklama / Varlık",
                "Kategori",
                "İşlem / Varlık türü",
                "Miktar",
                "Birim",
                "Birim alış fiyatı (₺)",
                "Tutar / Limit (₺)",
                "Not",
                numericColumns = NUMERIC_COLUMNS
            )

            exportData.transactions.forEach { transaction ->
                appendRow(
                    "İşlem",
                    transaction.date,
                    transaction.title,
                    transaction.category,
                    transaction.type.displayName(),
                    "",
                    "",
                    "",
                    formatAmountOrBlank(transaction.amount),
                    "",
                    numericColumns = NUMERIC_COLUMNS
                )
            }

            exportData.categoryBudgets.forEach { budget ->
                appendRow(
                    "Bütçe",
                    "",
                    formatMonthKey(budget.monthKey),
                    "Aylık kategori bütçesi",
                    budget.category,
                    "Bütçe limiti",
                    "",
                    "",
                    "",
                    formatAmountOrBlank(budget.limitAmount),
                    "",
                    numericColumns = NUMERIC_COLUMNS
                )
            }

            exportData.currencyInvestments.forEach { investment ->
                val totalPurchaseCost =
                    DecimalMath.multiplyMoney(
                        investment.amount,
                        investment.buyPrice
                    )

                appendRow(
                    "Yatırım",
                    investment.buyDate,
                    investment.assetName,
                    "",
                    "Döviz",
                    formatDecimalOrBlank(
                        investment.amount,
                        scale = 8
                    ),
                    "birim",
                    formatDecimalOrBlank(
                        investment.buyPrice,
                        scale = 6
                    ),
                    totalPurchaseCost
                        ?.let(::formatAmountOrBlank)
                        .orEmpty(),
                    investment.note.orEmpty(),
                    numericColumns = NUMERIC_COLUMNS
                )
            }

            exportData.goldInvestments.forEach { investment ->
                val goldType =
                    runCatching {
                        GoldType.valueOf(investment.goldType)
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

                appendRow(
                    "Yatırım",
                    formatDate(investment.purchaseDate),
                    goldType?.displayName ?: investment.goldType,
                    "",
                    "Altın",
                    formatDecimalOrBlank(
                        investment.quantity,
                        scale = 8
                    ),
                    when (goldType?.inputUnit) {
                        GoldInputUnit.GRAM -> "gram"
                        GoldInputUnit.PIECE -> "adet"
                        null -> investment.unit
                    },
                    investment.purchaseUnitPrice
                        ?.let { price ->
                            formatDecimalOrBlank(
                                price,
                                scale = 6
                            )
                        }
                        .orEmpty(),
                    totalPurchaseCost
                        ?.let(::formatAmountOrBlank)
                        .orEmpty(),
                    investment.note.orEmpty(),
                    numericColumns = NUMERIC_COLUMNS
                )
            }

            append(LINE_SEPARATOR)
            appendSummaryRow(
                "Toplam gelir",
                totals.totalIncome
            )
            appendSummaryRow(
                "Toplam gider",
                totals.totalExpense
            )
            appendSummaryRow(
                "Net bakiye",
                totals.netTotal
            )
        }

        return utf8Bom + csv.toByteArray(Charsets.UTF_8)
    }

    private fun StringBuilder.appendSummaryRow(
        label: String,
        amount: BigDecimal
    ) {
        appendRow(
            "Özet",
            "",
            label,
            "",
            "",
            "",
            "",
            "",
            formatAmount(amount),
            "",
            numericColumns = NUMERIC_COLUMNS
        )
    }

    private fun StringBuilder.appendRow(
        vararg cells: String,
        numericColumns: Set<Int>
    ) {
        cells.forEachIndexed { index, cell ->
            if (index > 0) {
                append(COLUMN_SEPARATOR)
            }

            append(
                escapeCell(
                    value = cell,
                    protectFormula = index !in numericColumns
                )
            )
        }
        append(LINE_SEPARATOR)
    }

    private fun escapeCell(
        value: String,
        protectFormula: Boolean
    ): String {
        val normalizedLineEndings =
            value
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .replace("\n", LINE_SEPARATOR)

        val safeValue =
            if (
                protectFormula &&
                normalizedLineEndings
                    .firstOrNull { character ->
                        !character.isWhitespace()
                    } in FORMULA_PREFIXES
            ) {
                "'$normalizedLineEndings"
            } else {
                normalizedLineEndings
            }

        return buildString(safeValue.length + 2) {
            append('"')
            append(safeValue.replace("\"", "\"\""))
            append('"')
        }
    }

    private fun formatAmountOrBlank(
        value: Double
    ): String =
        value
            .takeIf(Double::isFinite)
            ?.let(BigDecimal::valueOf)
            ?.let(::formatAmount)
            .orEmpty()

    private fun formatDecimalOrBlank(
        value: Double,
        scale: Int
    ): String =
        value
            .takeIf(Double::isFinite)
            ?.let(BigDecimal::valueOf)
            ?.setScale(scale, RoundingMode.HALF_UP)
            ?.stripTrailingZeros()
            ?.toPlainString()
            ?.replace('.', ',')
            .orEmpty()

    private fun formatAmount(
        value: BigDecimal
    ): String =
        value
            .setScale(2, RoundingMode.HALF_UP)
            .toPlainString()
            .replace('.', ',')

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
                    Locale.ROOT
                ).format(Date(timestamp))
            }
            .orEmpty()

    private fun TransactionType.displayName(): String =
        when (this) {
            TransactionType.INCOME -> "Gelir"
            TransactionType.EXPENSE -> "Gider"
        }

    private val NUMERIC_COLUMNS =
        setOf(5, 7, 8)

    private val FORMULA_PREFIXES =
        setOf('=', '+', '-', '@')
}

internal data class TransactionExportTotals(
    val totalIncome: BigDecimal,
    val totalExpense: BigDecimal
) {
    val netTotal: BigDecimal
        get() = totalIncome.subtract(totalExpense)

    companion object {
        fun from(
            transactions: List<Transaction>
        ): TransactionExportTotals {
            var income = BigDecimal.ZERO
            var expense = BigDecimal.ZERO

            transactions.forEach { transaction ->
                val amount =
                    transaction.amount
                        .takeIf(Double::isFinite)
                        ?.let(BigDecimal::valueOf)
                        ?: return@forEach

                when (transaction.type) {
                    TransactionType.INCOME ->
                        income = income.add(amount)

                    TransactionType.EXPENSE ->
                        expense = expense.add(amount)
                }
            }

            return TransactionExportTotals(
                totalIncome = income,
                totalExpense = expense
            )
        }
    }
}
