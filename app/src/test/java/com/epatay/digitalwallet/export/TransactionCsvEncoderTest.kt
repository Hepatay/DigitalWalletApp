package com.epatay.digitalwallet.export

import com.epatay.digitalwallet.data.Transaction
import com.epatay.digitalwallet.data.TransactionType
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionCsvEncoderTest {

    @Test
    fun encode_writesUtf8BomTurkishColumnsCrlfAndTotals() {
        val transactions =
            listOf(
                transaction(
                    title = "Temmuz Maaşı",
                    amount = 10_000.50,
                    category = "Gelir",
                    date = "01.07.2026 09:00",
                    type = TransactionType.INCOME
                ),
                transaction(
                    title = "Market alışverişi",
                    amount = 250.25,
                    category = "Gıda",
                    date = "02.07.2026 18:30",
                    type = TransactionType.EXPENSE
                )
            )

        val bytes =
            TransactionCsvEncoder.encode(
                transactions
            )

        assertArrayEquals(
            byteArrayOf(
                0xEF.toByte(),
                0xBB.toByte(),
                0xBF.toByte()
            ),
            bytes.copyOfRange(0, 3)
        )

        val content =
            bytes
                .copyOfRange(3, bytes.size)
                .toString(Charsets.UTF_8)

        assertTrue(
            content.startsWith(
                "\"Tarih\";\"Açıklama\";\"Kategori\";" +
                    "\"Tip\";\"Tutar (₺)\"\r\n"
            )
        )
        assertTrue(
            content.contains(
                "\"01.07.2026 09:00\";\"Temmuz Maaşı\";" +
                    "\"Gelir\";\"Gelir\";\"10000,50\"\r\n"
            )
        )
        assertTrue(
            content.contains(
                "\"02.07.2026 18:30\";\"Market alışverişi\";" +
                    "\"Gıda\";\"Gider\";\"250,25\"\r\n"
            )
        )
        assertTrue(
            content.contains(
                "\"\";\"Toplam Gelir\";\"\";\"\";\"10000,50\"\r\n"
            )
        )
        assertTrue(
            content.contains(
                "\"\";\"Toplam Gider\";\"\";\"\";\"250,25\"\r\n"
            )
        )
        assertTrue(
            content.endsWith(
                "\"\";\"Net Toplam\";\"\";\"\";\"9750,25\"\r\n"
            )
        )
        assertFalse(
            hasBareLineFeed(content)
        )
    }

    @Test
    fun encode_escapesCsvFieldsAndProtectsFormulaPrefixes() {
        val transactions =
            listOf(
                transaction(
                    title = "=HYPERLINK(\"https://example.test\";\"Aç\")",
                    amount = 1.0,
                    category = "  +SUM(1;1)",
                    date = "@NOW()",
                    type = TransactionType.EXPENSE
                ),
                transaction(
                    title = "Birinci satır\nİkinci \"satır\"",
                    amount = 2.0,
                    category = "-1+2",
                    date = "03.07.2026 12:00",
                    type = TransactionType.INCOME
                )
            )

        val content =
            csvText(
                TransactionCsvEncoder.encode(
                    transactions
                )
            )

        assertTrue(
            content.contains(
                "\"'@NOW()\";" +
                    "\"'=HYPERLINK(\"\"https://example.test\"\";" +
                    "\"\"Aç\"\")\";" +
                    "\"'  +SUM(1;1)\";\"Gider\";\"1,00\""
            )
        )
        assertTrue(
            content.contains(
                "\"Birinci satır\r\nİkinci \"\"satır\"\"\""
            )
        )
        assertTrue(
            content.contains(
                "\"'-1+2\""
            )
        )
        assertFalse(
            content.contains(
                "\"=HYPERLINK"
            )
        )
        assertFalse(
            hasBareLineFeed(content)
        )
    }

    @Test
    fun encode_emptyListStillContainsZeroSummary() {
        val content =
            csvText(
                TransactionCsvEncoder.encode(
                    emptyList()
                )
            )

        assertTrue(
            content.contains(
                "\"\";\"Toplam Gelir\";\"\";\"\";\"0,00\""
            )
        )
        assertTrue(
            content.contains(
                "\"\";\"Toplam Gider\";\"\";\"\";\"0,00\""
            )
        )
        assertTrue(
            content.contains(
                "\"\";\"Net Toplam\";\"\";\"\";\"0,00\""
            )
        )
    }

    @Test
    fun exportResult_usesIncomeMinusExpenseForNetTotal() {
        val result =
            TransactionExportResult.from(
                listOf(
                    transaction(
                        amount = 100.10,
                        type = TransactionType.INCOME
                    ),
                    transaction(
                        amount = 30.05,
                        type = TransactionType.EXPENSE
                    )
                )
            )

        assertEquals(
            2,
            result.transactionCount
        )
        assertEquals(
            100.10,
            result.totalIncome,
            0.000_001
        )
        assertEquals(
            30.05,
            result.totalExpense,
            0.000_001
        )
        assertEquals(
            70.05,
            result.netTotal,
            0.000_001
        )
    }

    private fun transaction(
        title: String = "İşlem",
        amount: Double,
        category: String = "Diğer",
        date: String = "01.07.2026 10:00",
        type: TransactionType
    ): Transaction {
        return Transaction(
            title = title,
            amount = amount,
            category = category,
            date = date,
            type = type
        )
    }

    private fun csvText(
        bytes: ByteArray
    ): String {
        assertArrayEquals(
            byteArrayOf(
                0xEF.toByte(),
                0xBB.toByte(),
                0xBF.toByte()
            ),
            bytes.copyOfRange(0, 3)
        )

        return bytes
            .copyOfRange(3, bytes.size)
            .toString(Charsets.UTF_8)
    }

    private fun hasBareLineFeed(
        value: String
    ): Boolean {
        return value.indices.any { index ->
            value[index] == '\n' &&
                (
                    index == 0 ||
                        value[index - 1] != '\r'
                    )
        }
    }
}
