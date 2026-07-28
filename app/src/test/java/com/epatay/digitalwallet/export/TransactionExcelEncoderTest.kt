package com.epatay.digitalwallet.export

import com.epatay.digitalwallet.data.CategoryBudget
import com.epatay.digitalwallet.data.Transaction
import com.epatay.digitalwallet.data.TransactionType
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.zip.ZipFile

class TransactionExcelEncoderTest {

    @Test
    fun write_createsValidOpenXmlWorkbookWithTurkishText() {
        val output =
            ByteArrayOutputStream()

        TransactionExcelEncoder.write(
            output,
            WalletExportData(
                transactions =
                    listOf(
                        Transaction(
                            title = "İşlem çığlığı ölçüşü",
                            amount = 123.45,
                            category = "Gıda",
                            date = "28.07.2026 13:30",
                            type = TransactionType.INCOME
                        )
                    ),
                categoryBudgets =
                    listOf(
                        CategoryBudget(
                            monthKey = 202607,
                            category = "Eğitim",
                            limitAmount = 5_000.0,
                            updatedAtMillis = 1L
                        )
                    )
            )
        )

        val entries =
            unzipEntries(
                output.toByteArray()
            )
        assertTrue(
            entries.containsKey("[Content_Types].xml")
        )
        assertTrue(
            entries.containsKey("xl/workbook.xml")
        )
        assertTrue(
            entries.containsKey("xl/worksheets/sheet1.xml")
        )

        val workbookXml =
            decodeNumericXmlEntities(
                entries.values.joinToString("\n")
            )

        assertTrue(
            workbookXml.contains("Kayıtlar")
        )
        assertTrue(
            workbookXml.contains("İşlem çığlığı ölçüşü")
        )
        assertTrue(
            workbookXml.contains("Gıda")
        )
        assertTrue(
            workbookXml.contains("Eğitim")
        )
    }

    @Test
    fun write_emptyDataDoesNotCreateWorkbook() {
        try {
            TransactionExcelEncoder.write(
                ByteArrayOutputStream(),
                WalletExportData()
            )
            fail("Boş veri kabul edilmemeliydi")
        } catch (expected: IllegalArgumentException) {
            assertTrue(
                expected.message.orEmpty().contains(
                    "kayıt bulunamadı"
                )
            )
        }
    }

    private fun unzipEntries(
        bytes: ByteArray
    ): Map<String, String> {
        val entries =
            linkedMapOf<String, String>()

        val file =
            File.createTempFile(
                "varlikcep-test-",
                ".xlsx"
            )

        try {
            file.writeBytes(bytes)

            ZipFile(file).use { zipFile ->
                zipFile.entries().asSequence().forEach { entry ->
                    entries[entry.name] =
                        zipFile
                            .getInputStream(entry)
                            .use { inputStream ->
                                inputStream
                                    .readBytes()
                                    .toString(Charsets.UTF_8)
                            }
                }
            }
        } finally {
            file.delete()
        }

        return entries
    }

    private fun decodeNumericXmlEntities(
        value: String
    ): String =
        Regex("&#x([0-9a-fA-F]+);")
            .replace(value) { match ->
                match
                    .groupValues[1]
                    .toInt(16)
                    .toChar()
                    .toString()
            }
}
