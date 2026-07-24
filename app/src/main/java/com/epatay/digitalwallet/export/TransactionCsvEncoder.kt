package com.epatay.digitalwallet.export

import com.epatay.digitalwallet.data.Transaction
import com.epatay.digitalwallet.data.TransactionType
import java.math.BigDecimal
import java.math.RoundingMode

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
    ): ByteArray {
        val totals = TransactionExportTotals.from(transactions)
        val csv = buildString {
            appendRow(
                "Tarih",
                "Açıklama",
                "Kategori",
                "Tip",
                "Tutar (₺)"
            )

            transactions.forEach { transaction ->
                appendRow(
                    transaction.date,
                    transaction.title,
                    transaction.category,
                    transaction.type.displayName(),
                    formatAmount(transaction.amount),
                    protectFormulas = true
                )
            }

            append(LINE_SEPARATOR)
            appendRow(
                "",
                "Özet",
                "",
                "",
                ""
            )
            appendSummaryRow(
                "Toplam Gelir",
                totals.totalIncome
            )
            appendSummaryRow(
                "Toplam Gider",
                totals.totalExpense
            )
            appendSummaryRow(
                "Net Toplam",
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
            "",
            label,
            "",
            "",
            formatAmount(amount)
        )
    }

    private fun StringBuilder.appendRow(
        vararg cells: String,
        protectFormulas: Boolean = false
    ) {
        cells.forEachIndexed { index, cell ->
            if (index > 0) {
                append(COLUMN_SEPARATOR)
            }

            append(
                escapeCell(
                    value = cell,
                    protectFormula =
                        protectFormulas &&
                            index != AMOUNT_COLUMN_INDEX
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

        return buildString(
            safeValue.length + 2
        ) {
            append('"')
            append(
                safeValue.replace(
                    "\"",
                    "\"\""
                )
            )
            append('"')
        }
    }

    private fun formatAmount(
        value: Double
    ): String {
        require(value.isFinite()) {
            "Dışa aktarılacak tutar sonlu olmalıdır."
        }

        return formatAmount(
            BigDecimal.valueOf(value)
        )
    }

    private fun formatAmount(
        value: BigDecimal
    ): String {
        return value
            .setScale(2, RoundingMode.HALF_UP)
            .toPlainString()
            .replace('.', ',')
    }

    private fun TransactionType.displayName(): String {
        return when (this) {
            TransactionType.INCOME -> "Gelir"
            TransactionType.EXPENSE -> "Gider"
        }
    }

    private const val AMOUNT_COLUMN_INDEX = 4

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
                require(transaction.amount.isFinite()) {
                    "Dışa aktarılacak tutar sonlu olmalıdır."
                }

                val amount =
                    BigDecimal.valueOf(transaction.amount)

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
