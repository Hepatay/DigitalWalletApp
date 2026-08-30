package com.epatay.digitalwallet.export

import com.epatay.digitalwallet.data.CategoryBudget
import com.epatay.digitalwallet.data.InvestmentItem
import com.epatay.digitalwallet.data.Transaction
import com.epatay.digitalwallet.data.TransactionType
import com.epatay.digitalwallet.data.UserGoldAssetEntity
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class TransactionCsvEncoderTest {

    @Test
    fun encode_writesEveryRecordTypeWithUtf8BomAndCorrectColumns() {
        val exportData =
            WalletExportData(
                transactions =
                    listOf(
                        transaction(
                            title = "Temmuz Maaşı",
                            amount = 10_000.50,
                            category = "Gelir",
                            type = TransactionType.INCOME
                        ),
                        transaction(
                            title = "Market alışverişi",
                            amount = 250.25,
                            category = "Gıda",
                            type = TransactionType.EXPENSE
                        )
                    ),
                categoryBudgets =
                    listOf(
                        CategoryBudget(
                            monthKey = 202607,
                            category = "Gıda",
                            limitAmount = 4_500.0,
                            updatedAtMillis = 1L
                        )
                    ),
                currencyInvestments =
                    listOf(
                        InvestmentItem(
                            uuid = "7",
                            assetName = "USD",
                            amount = 10.5,
                            buyPrice = 40.25,
                            buyDate = "02.07.2026",
                            note = "Uzun vade"
                        )
                    ),
                goldInvestments =
                    listOf(
                        UserGoldAssetEntity(
                            uuid = "9",
                            goldType = "GRAM_GOLD",
                            quantity = 2.25,
                            unit = "GRAM",
                            purchaseUnitPrice = 4_250.0,
                            totalPurchaseCost = 9_562.5,
                            purchaseDate = 1_751_410_800_000L,
                            note = "Fiziki",
                            createdAt = 1L,
                            updatedAt = 1L
                        )
                    )
            )

        val bytes = TransactionCsvEncoder.encode(exportData)

        assertArrayEquals(
            byteArrayOf(
                0xEF.toByte(),
                0xBB.toByte(),
                0xBF.toByte()
            ),
            bytes.copyOfRange(0, 3)
        )

        val content = csvText(bytes)

        assertTrue(content.contains("\"Kayıt türü\""))
        assertTrue(
            content.contains(
                "\"İşlem\";\"01.07.2026 10:00\";\"Temmuz Maaşı\";\"Gelir\";\"Gelir\""
            )
        )
        assertTrue(
            content.contains(
                "\"Bütçe\";\"\";\"07.2026\";\"Aylık kategori bütçesi\";\"Gıda\""
            )
        )
        assertTrue(
            content.contains(
                "\"Yatırım\";\"02.07.2026\";\"USD\""
            )
        )
        assertTrue(content.contains("\"Gram Altın\""))
        assertTrue(content.contains("\"Net bakiye\""))
        assertTrue(content.contains("\"9750,25\""))
        assertFalse(hasBareLineFeed(content))
    }

    @Test
    fun encode_escapesFieldsAndProtectsFormulaPrefixes() {
        val content =
            csvText(
                TransactionCsvEncoder.encode(
                    listOf(
                        transaction(
                            title =
                                "=HYPERLINK(\"https://example.test\";\"Aç\")",
                            amount = 1.0,
                            category = "  +SUM(1;1)",
                            date = "@NOW()",
                            type = TransactionType.EXPENSE
                        ),
                        transaction(
                            title = "Birinci satır\nİkinci \"satır\"",
                            amount = 2.0,
                            category = "-1+2",
                            type = TransactionType.INCOME
                        )
                    )
                )
            )

        assertTrue(content.contains("\"'@NOW()\""))
        assertTrue(content.contains("\"'=HYPERLINK("))
        assertTrue(content.contains("\"'  +SUM(1;1)\""))
        assertTrue(
            content.contains(
                "\"Birinci satır\r\nİkinci \"\"satır\"\"\""
            )
        )
        assertFalse(content.contains("\"=HYPERLINK"))
        assertFalse(hasBareLineFeed(content))
    }

    @Test
    fun encode_emptyDataDoesNotCreateAFilePayload() {
        try {
            TransactionCsvEncoder.encode(WalletExportData())
            fail("Boş veri kabul edilmemeliydi")
        } catch (expected: IllegalArgumentException) {
            assertTrue(
                expected.message.orEmpty().contains(
                    "kayıt bulunamadı"
                )
            )
        }
    }

    @Test
    fun exportResult_countsAllRowsAndUsesIncomeMinusExpense() {
        val result =
            TransactionExportResult.from(
                WalletExportData(
                    transactions =
                        listOf(
                            transaction(
                                amount = 100.10,
                                type = TransactionType.INCOME
                            ),
                            transaction(
                                amount = 30.05,
                                type = TransactionType.EXPENSE
                            )
                        ),
                    categoryBudgets =
                        listOf(
                            CategoryBudget(
                                monthKey = 202607,
                                category = "Gıda",
                                limitAmount = 100.0,
                                updatedAtMillis = 1L
                            )
                        )
                )
            )

        assertEquals(2, result.transactionCount)
        assertEquals(1, result.budgetCount)
        assertEquals(0, result.investmentCount)
        assertEquals(3, result.recordCount)
        assertEquals(70.05, result.netTotal, 0.000_001)
    }

    private fun transaction(
        title: String = "İşlem",
        amount: Double,
        category: String = "Diğer",
        date: String = "01.07.2026 10:00",
        type: TransactionType
    ): Transaction =
        Transaction(
            uuid = "0",
            title = title,
            amount = amount,
            category = category,
            date = date,
            type = type,
            occurredOn = 20260701
        )

    private fun csvText(bytes: ByteArray): String {
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

    private fun hasBareLineFeed(value: String): Boolean =
        value.indices.any { index ->
            value[index] == '\n' &&
                (index == 0 || value[index - 1] != '\r')
        }
}
