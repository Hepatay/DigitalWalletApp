package com.epatay.digitalwallet.export

import android.content.ContentResolver
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.epatay.digitalwallet.data.Transaction
import com.epatay.digitalwallet.data.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TransactionExportManager(
    private val contentResolver: ContentResolver
) {

    suspend fun exportCsv(
        uri: Uri,
        exportData: WalletExportData
    ): TransactionExportResult =
        withContext(Dispatchers.IO) {
            check(!exportData.isEmpty) {
                "Dışa aktarılacak kayıt bulunamadı."
            }

            writeToUri(uri) { outputStream ->
                outputStream.write(
                    TransactionCsvEncoder.encode(
                        exportData
                    )
                )
            }

            TransactionExportResult.from(
                exportData
            )
        }

    suspend fun exportXlsx(
        uri: Uri,
        exportData: WalletExportData
    ): TransactionExportResult =
        withContext(Dispatchers.IO) {
            check(!exportData.isEmpty) {
                "Dışa aktarılacak kayıt bulunamadı."
            }

            writeToUri(uri) { outputStream ->
                TransactionExcelEncoder.write(
                    outputStream,
                    exportData
                )
            }

            TransactionExportResult.from(
                exportData
            )
        }

    suspend fun exportPdf(
        uri: Uri,
        transactions: List<Transaction>
    ): TransactionExportResult =
        withContext(Dispatchers.IO) {
            val document = PdfDocument()

            try {
                TransactionPdfRenderer.render(
                    document,
                    transactions
                )

                writeToUri(uri) { outputStream ->
                    document.writeTo(outputStream)
                }
            } finally {
                document.close()
            }

            TransactionExportResult.from(
                transactions
            )
        }

    private inline fun writeToUri(
        uri: Uri,
        write: (java.io.OutputStream) -> Unit
    ) {
        val outputStream =
            contentResolver.openOutputStream(
                uri,
                "w"
            ) ?: throw IOException(
                "Seçilen dosya konumu yazma akışı sağlamadı."
            )

        outputStream.use { stream ->
            write(stream)
            stream.flush()
        }
    }
}

data class TransactionExportResult(
    val transactionCount: Int,
    val budgetCount: Int,
    val investmentCount: Int,
    val totalIncome: Double,
    val totalExpense: Double,
    val netTotal: Double
) {
    val recordCount: Int
        get() =
            transactionCount +
                budgetCount +
                investmentCount

    internal companion object {
        fun from(
            transactions: List<Transaction>
        ): TransactionExportResult {
            val totals =
                TransactionExportTotals.from(
                    transactions
                )

            return TransactionExportResult(
                transactionCount = transactions.size,
                budgetCount = 0,
                investmentCount = 0,
                totalIncome = totals.totalIncome.toDouble(),
                totalExpense = totals.totalExpense.toDouble(),
                netTotal = totals.netTotal.toDouble()
            )
        }

        fun from(
            exportData: WalletExportData
        ): TransactionExportResult {
            val totals =
                TransactionExportTotals.from(
                    exportData.transactions
                )

            return TransactionExportResult(
                transactionCount = exportData.transactions.size,
                budgetCount = exportData.categoryBudgets.size,
                investmentCount =
                    exportData.currencyInvestments.size +
                        exportData.goldInvestments.size,
                totalIncome = totals.totalIncome.toDouble(),
                totalExpense = totals.totalExpense.toDouble(),
                netTotal = totals.netTotal.toDouble()
            )
        }
    }
}

private object TransactionPdfRenderer {

    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val PAGE_MARGIN = 36f
    private const val FOOTER_BASELINE = 820f
    private const val TABLE_TOP = 82f
    private const val HEADER_HEIGHT = 26f
    private const val ROW_HEIGHT = 25f
    private const val CELL_PADDING = 5f
    private const val LAST_ROW_BOTTOM = 792f

    private val columnWidths =
        floatArrayOf(
            102f,
            150f,
            92f,
            63f,
            116f
        )

    private val headerLabels =
        arrayOf(
            "Tarih",
            "Açıklama",
            "Kategori",
            "Tip",
            "Tutar"
        )

    fun render(
        document: PdfDocument,
        transactions: List<Transaction>
    ) {
        val state = PageState(document)

        try {
            if (transactions.isEmpty()) {
                state.ensureSpace(ROW_HEIGHT)
                drawEmptyRow(
                    state.canvas,
                    state.cursorY
                )
                state.cursorY += ROW_HEIGHT
            } else {
                transactions.forEach { transaction ->
                    state.ensureSpace(ROW_HEIGHT)
                    drawTransactionRow(
                        canvas = state.canvas,
                        top = state.cursorY,
                        transaction = transaction
                    )
                    state.cursorY += ROW_HEIGHT
                }
            }

            val totals =
                TransactionExportTotals.from(
                    transactions
                )

            state.ensureSpace(
                ROW_HEIGHT * 4
            )

            drawSectionRow(
                canvas = state.canvas,
                top = state.cursorY,
                label = "Özet"
            )
            state.cursorY += ROW_HEIGHT

            drawSummaryRow(
                canvas = state.canvas,
                top = state.cursorY,
                label = "Toplam Gelir",
                amount = totals.totalIncome
            )
            state.cursorY += ROW_HEIGHT

            drawSummaryRow(
                canvas = state.canvas,
                top = state.cursorY,
                label = "Toplam Gider",
                amount = totals.totalExpense
            )
            state.cursorY += ROW_HEIGHT

            drawSummaryRow(
                canvas = state.canvas,
                top = state.cursorY,
                label = "Net Toplam",
                amount = totals.netTotal
            )
            state.cursorY += ROW_HEIGHT
        } finally {
            state.finish()
        }
    }

    private fun drawTransactionRow(
        canvas: Canvas,
        top: Float,
        transaction: Transaction
    ) {
        drawGridRow(
            canvas = canvas,
            top = top,
            cells = arrayOf(
                transaction.date,
                transaction.title,
                transaction.category,
                when (transaction.type) {
                    TransactionType.INCOME -> "Gelir"
                    TransactionType.EXPENSE -> "Gider"
                },
                formatPdfAmount(transaction.amount)
            ),
            amountColumn = true
        )
    }

    private fun drawEmptyRow(
        canvas: Canvas,
        top: Float
    ) {
        canvas.drawRect(
            PAGE_MARGIN,
            top,
            PAGE_WIDTH - PAGE_MARGIN,
            top + ROW_HEIGHT,
            borderPaint
        )

        canvas.drawText(
            "Dışa aktarılacak işlem bulunmuyor.",
            PAGE_MARGIN + CELL_PADDING,
            textBaseline(top),
            mutedTextPaint
        )
    }

    private fun drawSectionRow(
        canvas: Canvas,
        top: Float,
        label: String
    ) {
        canvas.drawRect(
            PAGE_MARGIN,
            top,
            PAGE_WIDTH - PAGE_MARGIN,
            top + ROW_HEIGHT,
            summaryBackgroundPaint
        )
        canvas.drawRect(
            PAGE_MARGIN,
            top,
            PAGE_WIDTH - PAGE_MARGIN,
            top + ROW_HEIGHT,
            borderPaint
        )
        canvas.drawText(
            label,
            PAGE_MARGIN + CELL_PADDING,
            textBaseline(top),
            boldTextPaint
        )
    }

    private fun drawSummaryRow(
        canvas: Canvas,
        top: Float,
        label: String,
        amount: BigDecimal
    ) {
        drawGridRow(
            canvas = canvas,
            top = top,
            cells = arrayOf(
                "",
                label,
                "",
                "",
                formatPdfAmount(amount)
            ),
            amountColumn = true,
            bold = true
        )
    }

    private fun drawGridRow(
        canvas: Canvas,
        top: Float,
        cells: Array<String>,
        amountColumn: Boolean,
        bold: Boolean = false
    ) {
        var left = PAGE_MARGIN

        cells.forEachIndexed { index, value ->
            val right = left + columnWidths[index]

            canvas.drawRect(
                left,
                top,
                right,
                top + ROW_HEIGHT,
                borderPaint
            )

            val paint =
                if (bold) {
                    boldTextPaint
                } else {
                    bodyTextPaint
                }

            val availableWidth =
                columnWidths[index] -
                    CELL_PADDING * 2

            val displayValue =
                ellipsize(
                    value,
                    paint,
                    availableWidth
                )

            val textX =
                if (
                    amountColumn &&
                    index == cells.lastIndex
                ) {
                    right -
                        CELL_PADDING -
                        paint.measureText(displayValue)
                } else {
                    left + CELL_PADDING
                }

            canvas.drawText(
                displayValue,
                textX,
                textBaseline(top),
                paint
            )

            left = right
        }
    }

    private fun drawPageHeader(
        canvas: Canvas,
        pageNumber: Int
    ) {
        canvas.drawText(
            "Dijital Cüzdan İşlem Raporu",
            PAGE_MARGIN,
            37f,
            titlePaint
        )

        val createdAt =
            SimpleDateFormat(
                "dd.MM.yyyy HH:mm",
                Locale.forLanguageTag("tr-TR")
            ).format(Date())

        canvas.drawText(
            "Oluşturulma: $createdAt",
            PAGE_MARGIN,
            57f,
            mutedTextPaint
        )

        var left = PAGE_MARGIN

        headerLabels.forEachIndexed { index, label ->
            val right = left + columnWidths[index]

            canvas.drawRect(
                left,
                TABLE_TOP,
                right,
                TABLE_TOP + HEADER_HEIGHT,
                headerBackgroundPaint
            )
            canvas.drawRect(
                left,
                TABLE_TOP,
                right,
                TABLE_TOP + HEADER_HEIGHT,
                borderPaint
            )
            canvas.drawText(
                ellipsize(
                    label,
                    headerTextPaint,
                    columnWidths[index] -
                        CELL_PADDING * 2
                ),
                left + CELL_PADDING,
                TABLE_TOP + 17f,
                headerTextPaint
            )

            left = right
        }

        val footerText = "Sayfa $pageNumber"
        canvas.drawText(
            footerText,
            (
                PAGE_WIDTH -
                    footerPaint.measureText(footerText)
                ) / 2f,
            FOOTER_BASELINE,
            footerPaint
        )
    }

    private fun ellipsize(
        value: String,
        paint: Paint,
        availableWidth: Float
    ): String {
        val singleLine =
            value
                .replace("\r\n", " ")
                .replace('\r', ' ')
                .replace('\n', ' ')

        if (
            paint.measureText(singleLine) <=
            availableWidth
        ) {
            return singleLine
        }

        val ellipsis = "…"
        val textWidth =
            (
                availableWidth -
                    paint.measureText(ellipsis)
                ).coerceAtLeast(0f)
        val fittingCharacterCount =
            paint.breakText(
                singleLine,
                true,
                textWidth,
                null
            )

        return singleLine
            .take(fittingCharacterCount)
            .trimEnd() +
            ellipsis
    }

    private fun textBaseline(
        top: Float
    ): Float {
        return top +
            (ROW_HEIGHT -
                (bodyTextPaint.descent() +
                    bodyTextPaint.ascent())) /
            2f
    }

    private fun formatPdfAmount(
        value: Double
    ): String {
        require(value.isFinite()) {
            "Dışa aktarılacak tutar sonlu olmalıdır."
        }

        return formatPdfAmount(
            BigDecimal.valueOf(value)
        )
    }

    private fun formatPdfAmount(
        value: BigDecimal
    ): String {
        val decimalText =
            value
                .setScale(2, RoundingMode.HALF_UP)
                .toPlainString()

        val parts = decimalText.split('.')
        val integerPart = parts[0]
        val sign =
            if (integerPart.startsWith("-")) {
                "-"
            } else {
                ""
            }
        val digits = integerPart.removePrefix("-")
        val grouped =
            digits
                .reversed()
                .chunked(3)
                .joinToString(".")
                .reversed()

        return "$sign$grouped,${parts[1]} ₺"
    }

    private class PageState(
        private val document: PdfDocument
    ) {
        private var pageNumber = 0
        private var page: PdfDocument.Page? = null

        lateinit var canvas: Canvas
            private set

        var cursorY = TABLE_TOP + HEADER_HEIGHT

        init {
            startPage()
        }

        fun ensureSpace(
            requiredHeight: Float
        ) {
            if (
                cursorY + requiredHeight >
                LAST_ROW_BOTTOM
            ) {
                finishCurrentPage()
                startPage()
            }
        }

        fun finish() {
            finishCurrentPage()
        }

        private fun startPage() {
            pageNumber += 1

            val pageInfo =
                PdfDocument.PageInfo.Builder(
                    PAGE_WIDTH,
                    PAGE_HEIGHT,
                    pageNumber
                ).create()

            page = document.startPage(pageInfo)
            canvas = requireNotNull(page).canvas
            cursorY = TABLE_TOP + HEADER_HEIGHT

            drawPageHeader(
                canvas,
                pageNumber
            )
        }

        private fun finishCurrentPage() {
            page?.let { currentPage ->
                document.finishPage(currentPage)
            }
            page = null
        }
    }

    private val borderPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(190, 198, 207)
            style = Paint.Style.STROKE
            strokeWidth = 0.7f
        }

    private val headerBackgroundPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(226, 236, 247)
            style = Paint.Style.FILL
        }

    private val summaryBackgroundPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(242, 246, 250)
            style = Paint.Style.FILL
        }

    private val titlePaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(25, 49, 75)
            textSize = 17f
            typeface =
                Typeface.create(
                    Typeface.DEFAULT,
                    Typeface.BOLD
                )
        }

    private val headerTextPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(25, 49, 75)
            textSize = 9f
            typeface =
                Typeface.create(
                    Typeface.DEFAULT,
                    Typeface.BOLD
                )
        }

    private val bodyTextPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(35, 38, 42)
            textSize = 8.5f
        }

    private val boldTextPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(35, 38, 42)
            textSize = 8.5f
            typeface =
                Typeface.create(
                    Typeface.DEFAULT,
                    Typeface.BOLD
                )
        }

    private val mutedTextPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(95, 101, 109)
            textSize = 8.5f
        }

    private val footerPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(95, 101, 109)
            textSize = 8f
        }
}
