package com.epatay.digitalwallet.ui

import android.Manifest
import android.content.ClipData
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.epatay.digitalwallet.R
import com.epatay.digitalwallet.data.DecimalInputResult
import com.epatay.digitalwallet.data.DecimalInputValidator
import com.epatay.digitalwallet.data.RecurringTransaction
import com.epatay.digitalwallet.data.Transaction
import com.epatay.digitalwallet.data.TransactionDatabase
import com.epatay.digitalwallet.data.TransactionDateUtils
import com.epatay.digitalwallet.data.TransactionFilter
import com.epatay.digitalwallet.data.TransactionType
import com.epatay.digitalwallet.databinding.BottomSheetAddExpenseBinding
import com.epatay.digitalwallet.databinding.BottomSheetAddRecurringBinding
import com.epatay.digitalwallet.databinding.FragmentDashboardBinding
import com.epatay.digitalwallet.export.TransactionExportManager
import com.epatay.digitalwallet.export.WalletExportData
import com.epatay.digitalwallet.recurring.RecurringDateUtils
import com.epatay.digitalwallet.recurring.RecurringTransactionScheduler
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private const val XLSX_MIME_TYPE =
    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"

class DashboardFragment : Fragment(R.layout.fragment_dashboard) {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private val transactionViewModel: TransactionViewModel by activityViewModels()

    private val recurringTransactionViewModel:
        RecurringTransactionViewModel by activityViewModels()

    private lateinit var adapter: TransactionAdapter

    private var currentTransactions: List<Transaction> = emptyList()
    private var currentRecurringTransactions:
        List<RecurringTransaction> = emptyList()

    private var pendingCsvExportData:
        WalletExportData? = null

    private var pendingXlsxExportData:
        WalletExportData? = null

    private var pendingPdfTransactions:
        List<Transaction>? = null

    private val activeBottomSheetDialogs =
        mutableSetOf<BottomSheetDialog>()

    private val createCsvDocumentLauncher =
        registerForActivityResult(
            ActivityResultContracts.CreateDocument(
                "text/csv"
            )
        ) { uri ->
            if (uri != null) {
                exportTransactions(
                    uri = uri,
                    format = ExportFormat.CSV
                )
            } else {
                pendingCsvExportData = null
            }
        }

    private val createXlsxDocumentLauncher =
        registerForActivityResult(
            ActivityResultContracts.CreateDocument(
                XLSX_MIME_TYPE
            )
        ) { uri ->
            if (uri != null) {
                exportTransactions(
                    uri = uri,
                    format = ExportFormat.XLSX
                )
            } else {
                pendingXlsxExportData = null
            }
        }

    private val createPdfDocumentLauncher =
        registerForActivityResult(
            ActivityResultContracts.CreateDocument(
                "application/pdf"
            )
        ) { uri ->
            if (uri != null) {
                exportTransactions(
                    uri = uri,
                    format = ExportFormat.PDF
                )
            } else {
                pendingPdfTransactions = null
            }
        }

    private val notificationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->

            if (isGranted && isAdded) {
                RecurringTransactionScheduler.runNow(
                    requireContext()
                )
            } else if (isAdded) {
                showInAppMessage(
                    "Hatırlatma bildirimi için bildirim izni gerekli.",
                    Snackbar.LENGTH_LONG
                )
            }
        }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentDashboardBinding.bind(view)

        binding.btnSettings.setOnClickListener {
            showSetLimitDialog()
        }

        adapter = TransactionAdapter(
            transactionList = emptyList(),

            onEditClick = { transaction ->
                showTransactionBottomSheet(transaction)
            },

            onDeleteClick = { transaction ->
                showDeleteTransactionDialog(transaction)
            }
        )

        binding.rvTransactions.adapter = adapter

        binding.rvTransactions.layoutManager =
            LinearLayoutManager(requireContext())

        /*
         * Bütçe özeti ve raporlar için tüm işlemleri gözlemler.
         */
        viewLifecycleOwner.lifecycleScope.launch {

            transactionViewModel.allTransactions
                .collectLatest { transactions ->

                    currentTransactions = transactions

                    val expensesOnly = transactions.filter {
                        it.type == TransactionType.EXPENSE
                    }

                    Log.d(
                        "DEBUG_PIE",
                        "Gider sayısı: ${expensesOnly.size}"
                    )

                    updatePieChart(expensesOnly)
                    updateDailyBudgetUI()
                }
        }

        /*
         * Arama ve filtreler yalnız işlem listesini etkiler;
         * üstteki genel bakiye değerleri değişmez.
         */
        viewLifecycleOwner.lifecycleScope.launch {
            transactionViewModel.filteredTransactions
                .collectLatest { transactions ->
                    adapter.updateData(transactions)

                    val isEmpty =
                        transactions.isEmpty()

                    binding.rvTransactions.visibility =
                        if (isEmpty) View.GONE else View.VISIBLE

                    binding.layoutEmptyTransactions.visibility =
                        if (isEmpty) View.VISIBLE else View.GONE

                    binding.tvTransactionsTitle.text =
                        "İşlemler (${transactions.size})"
                }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            transactionViewModel.filters
                .collectLatest(::updateFilterSummary)
        }

        /*
         * Toplam gelir, toplam gider ve
         * mevcut bakiye bilgisini gözlemler.
         */
        viewLifecycleOwner.lifecycleScope.launch {

            combine(
                transactionViewModel.totalIncome,
                transactionViewModel.totalExpense
            ) { income, expense ->

                Pair(
                    income ?: 0.0,
                    expense ?: 0.0
                )

            }.collect { (income, expense) ->

                val balance = income - expense

                binding.tvTotalIncome.text =
                    formatCurrency(income)

                binding.tvTotalExpense.text =
                    formatCurrency(expense)

                binding.tvTotalBalance.text =
                    formatCurrency(balance)

                binding.tvTotalBalance.setTextColor(
                    if (balance < 0.0) {
                        Color.parseColor("#F44336")
                    } else {
                        Color.parseColor("#4CAF50")
                    }
                )
            }
        }

        binding.fabAddExpense.setOnClickListener {
            showTransactionBottomSheet()
        }

        binding.btnAddFirstTransaction.setOnClickListener {
            showTransactionBottomSheet()
        }

        binding.btnAddRecurring.setOnClickListener {
            showRecurringBottomSheet()
        }

        binding.etTransactionSearch
            .doAfterTextChanged { text ->
                transactionViewModel.setSearchQuery(
                    text?.toString().orEmpty()
                )
            }

        ViewCompat.setOnApplyWindowInsetsListener(
            binding.root
        ) { _, insets ->
            val compactSearch =
                insets.isVisible(
                    WindowInsetsCompat.Type.ime()
                ) && binding.etTransactionSearch.hasFocus()

            val imeBottom =
                insets.getInsets(
                    WindowInsetsCompat.Type.ime()
                ).bottom

            binding.root.setPadding(
                binding.root.paddingLeft,
                binding.root.paddingTop,
                binding.root.paddingRight,
                if (compactSearch) {
                    imeBottom
                } else {
                    0
                }
            )

            updateSearchLayout(compactSearch)
            insets
        }

        binding.etTransactionSearch.setOnFocusChangeListener {
                _, _ ->
            ViewCompat.requestApplyInsets(binding.root)
        }

        binding.btnFilterTransactions.setOnClickListener {
            TransactionFilterBottomSheetDialogFragment()
                .show(
                    parentFragmentManager,
                    "transaction_filters"
                )
        }

        binding.btnMonthlyReport.setOnClickListener {
            MonthlyReportBottomSheetDialogFragment()
                .show(
                    parentFragmentManager,
                    "monthly_report"
                )
        }

        binding.btnSavingsGoals.setOnClickListener {
            SavingsGoalsBottomSheetDialogFragment()
                .show(
                    parentFragmentManager,
                    "savings_goals"
                )
        }

        binding.btnExportTransactions.setOnClickListener {
            showExportFormatDialog()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            recurringTransactionViewModel
                .allRecurringTransactions
                .collectLatest { recurringTransactions ->

                    currentRecurringTransactions =
                        recurringTransactions

                    updateRecurringPreview()
                }
        }
    }

    override fun onResume() {
        super.onResume()

        if (_binding != null) {
            updateDailyBudgetUI()
            updateRecurringPreview()
        }
    }

    private fun updateSearchLayout(
        compact: Boolean
    ) {
        val supportingVisibility =
            if (compact) View.GONE else View.VISIBLE

        binding.cardDashboard.visibility =
            supportingVisibility
        binding.cardRecurring.visibility =
            supportingVisibility
        binding.scrollQuickActions.visibility =
            supportingVisibility
        binding.fabAddExpense.visibility =
            supportingVisibility
    }

    private fun updateFilterSummary(
        filters: TransactionFilter
    ) {
        val searchText =
            binding.etTransactionSearch.text
                ?.toString()
                .orEmpty()

        if (searchText != filters.query) {
            binding.etTransactionSearch.setText(
                filters.query
            )
            binding.etTransactionSearch.setSelection(
                filters.query.length
            )
        }

        val parts = mutableListOf<String>()

        filters.query
            .takeIf(String::isNotBlank)
            ?.let { query ->
                parts += "Arama: “$query”"
            }

        filters.category?.let { category ->
            parts += category
        }

        filters.type?.let { type ->
            parts +=
                if (type == TransactionType.EXPENSE) {
                    "Giderler"
                } else {
                    "Gelirler"
                }
        }

        when {
            filters.startDateKey != null &&
                filters.endDateKey != null -> {
                parts +=
                    "${formatDateKey(filters.startDateKey)} – " +
                    formatDateKey(filters.endDateKey)
            }

            filters.startDateKey != null -> {
                parts +=
                    "${formatDateKey(filters.startDateKey)} sonrası"
            }

            filters.endDateKey != null -> {
                parts +=
                    "${formatDateKey(filters.endDateKey)} öncesi"
            }
        }

        binding.tvActiveFilters.text =
            if (parts.isEmpty()) {
                "Tüm işlemler"
            } else {
                parts.joinToString(" • ")
            }

        binding.tvActiveFilters.visibility =
            if (parts.isEmpty()) {
                View.GONE
            } else {
                View.VISIBLE
            }
    }

    private fun formatDateKey(
        dateKey: Int
    ): String {
        if (!TransactionDateUtils.isValidDateKey(dateKey)) {
            return "Tarihi bilinmiyor"
        }

        return String.format(
            Locale.ROOT,
            "%02d.%02d.%04d",
            dateKey % 100,
            dateKey / 100 % 100,
            dateKey / 10_000
        )
    }

    private fun showExportFormatDialog() {
        viewLifecycleOwner.lifecycleScope.launch {
            runCatching {
                loadWalletExportData()
            }.onSuccess { exportData ->
                if (!isAdded) {
                    return@onSuccess
                }

                if (exportData.isEmpty) {
                    showInAppMessage(
                        "Dışa aktarılacak kayıt bulunamadı."
                    )
                    return@onSuccess
                }

                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Kayıtları dışa aktar")
                    .setItems(
                        arrayOf(
                            "Excel dosyası (.xlsx)",
                            "Excel uyumlu CSV (.csv)",
                            "İşlem raporu PDF (.pdf)"
                        )
                    ) { _, selectedIndex ->
                        val timestamp =
                            SimpleDateFormat(
                                "yyyyMMdd-HHmm",
                                Locale.ROOT
                            ).format(Date())

                        if (selectedIndex == 0) {
                            pendingXlsxExportData = exportData
                            createXlsxDocumentLauncher.launch(
                                "VarlıkCep-kayıtlar-$timestamp.xlsx"
                            )
                        } else if (selectedIndex == 1) {
                            pendingCsvExportData = exportData
                            createCsvDocumentLauncher.launch(
                                "VarlıkCep-kayıtlar-$timestamp.csv"
                            )
                        } else if (
                            exportData.transactions.isNotEmpty()
                        ) {
                            pendingPdfTransactions =
                                exportData.transactions
                            createPdfDocumentLauncher.launch(
                                "VarlıkCep-işlemler-$timestamp.pdf"
                            )
                        } else {
                            showInAppMessage(
                                "PDF için gelir veya gider kaydı bulunamadı."
                            )
                        }
                    }
                    .setNegativeButton("İptal", null)
                    .show()
            }.onFailure { error ->
                if (isAdded) {
                    showInAppMessage(
                        error.message
                            ?: "Kayıtlar hazırlanamadı.",
                        Snackbar.LENGTH_LONG
                    )
                }
            }
        }
    }

    private suspend fun loadWalletExportData():
        WalletExportData {
        val database =
            TransactionDatabase.getDatabase(
                requireContext()
            )

        return WalletExportData(
            transactions =
                database.transactionDao()
                    .getAllTransactionsSnapshot(),
            categoryBudgets =
                database.categoryBudgetDao().getAll(),
            currencyInvestments =
                database.investmentDao()
                    .getAllInvestmentsSnapshot(),
            goldInvestments =
                database.userGoldAssetDao()
                    .getAllSnapshot()
        )
    }

    private fun exportTransactions(
        uri: Uri,
        format: ExportFormat
    ) {
        if (!isAdded) {
            return
        }

        lifecycleScope.launch {
            runCatching {
                val manager =
                    TransactionExportManager(
                        requireContext().contentResolver
                    )

                when (format) {
                    ExportFormat.XLSX -> {
                        val exportData =
                            pendingXlsxExportData
                                ?: error(
                                    "Dışa aktarılacak kayıt bulunamadı."
                                )
                        pendingXlsxExportData = null

                        manager.exportXlsx(
                            uri,
                            exportData
                        )
                    }

                    ExportFormat.CSV -> {
                        val exportData =
                            pendingCsvExportData
                                ?: error(
                                    "Dışa aktarılacak kayıt bulunamadı."
                                )
                        pendingCsvExportData = null

                        manager.exportCsv(
                            uri,
                            exportData
                        )
                    }

                    ExportFormat.PDF -> {
                        val transactions =
                            pendingPdfTransactions
                                ?: error(
                                    "Dışa aktarılacak işlem bulunamadı."
                                )
                        pendingPdfTransactions = null

                        manager.exportPdf(
                            uri,
                            transactions
                        )
                    }
                }
            }.onSuccess { result ->
                if (isAdded) {
                    showExportCompletedDialog(
                        uri = uri,
                        mimeType = format.mimeType,
                        recordCount = result.recordCount
                    )
                }
            }.onFailure { error ->
                if (isAdded) {
                    showInAppMessage(
                        error.message
                            ?: "Dosya oluşturulamadı.",
                        Snackbar.LENGTH_LONG
                    )
                }
            }
        }
    }

    private fun showExportCompletedDialog(
        uri: Uri,
        mimeType: String,
        recordCount: Int
    ) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Dosya oluşturuldu")
            .setMessage(
                "$recordCount kayıt seçtiğiniz konuma kaydedildi."
            )
            .setNegativeButton("Kapat", null)
            .setPositiveButton("Paylaş") { _, _ ->
                val shareIntent =
                    Intent(Intent.ACTION_SEND).apply {
                        type = mimeType
                        putExtra(Intent.EXTRA_STREAM, uri)
                        clipData =
                            ClipData.newRawUri(
                                "VarlıkCep dışa aktarımı",
                                uri
                            )
                        addFlags(
                            Intent.FLAG_GRANT_READ_URI_PERMISSION
                        )
                    }

                runCatching {
                    startActivity(
                        Intent.createChooser(
                            shareIntent,
                            "Dosyayı paylaş"
                        )
                    )
                }.onFailure {
                    showInAppMessage(
                        "Dosyayı paylaşabilecek bir uygulama bulunamadı.",
                        Snackbar.LENGTH_LONG
                    )
                }
            }
            .show()
    }

    private fun updateDailyBudgetUI() {

        val summary =
            transactionViewModel.getMonthlyBudgetSummary(
                requireContext(),
                currentTransactions
            )

        val isExceeded =
            summary.exceededAmount > 0.0

        val indicatorColor =
            MaterialColors.getColor(
                binding.root,
                if (isExceeded) {
                    com.google.android.material.R.attr.colorError
                } else {
                    com.google.android.material.R.attr.colorPrimary
                },
                Color.BLUE
            )

        binding.progressMonthlyBudget.setIndicatorColor(
            indicatorColor
        )

        binding.progressMonthlyBudget.setProgressCompat(
            summary.progressPercent,
            true
        )

        binding.tvBudgetPercent.text =
            if (isExceeded) {
                "%${summary.usagePercent} • limit aşıldı"
            } else {
                "%${summary.usagePercent} kullanıldı"
            }

        binding.tvBudgetPercent.setTextColor(
            indicatorColor
        )

        binding.tvBudgetRemainingLabel.text =
            if (isExceeded) {
                "Limit aşımı"
            } else {
                "Kalan bütçe"
            }

        binding.tvBudgetRemaining.text =
            formatCurrency(
                if (isExceeded) {
                    summary.exceededAmount
                } else {
                    summary.remainingLimit
                }
            )

        binding.tvBudgetRemaining.setTextColor(
            indicatorColor
        )

        binding.tvBudgetDaily.text =
            formatCurrency(
                summary.dailySpendingLimit
            )

        binding.tvBudgetDetail.text =
            "Bu ay ${formatCurrency(summary.currentMonthExpense)} / " +
                "${formatCurrency(summary.monthlyLimit)} • " +
                "${summary.daysUntilMonthEnd} gün kaldı"

        binding.progressMonthlyBudget.contentDescription =
            binding.tvBudgetPercent.text
    }

    private fun formatCurrency(value: Double): String {

        return String.format(
            Locale.forLanguageTag("tr-TR"),
            "%,.2f ₺",
            value
        )
    }

    private fun updatePieChart(
        expenses: List<Transaction>
    ) {

        val pieChart = binding.pieChart

        pieChart.clear()
        binding.llCategoryDetails.removeAllViews()

        val surfaceColor = MaterialColors.getColor(
            binding.root,
            com.google.android.material.R.attr.colorSurface,
            Color.WHITE
        )

        val onSurfaceColor = MaterialColors.getColor(
            binding.root,
            com.google.android.material.R.attr.colorOnSurface,
            Color.BLACK
        )

        pieChart.setNoDataText(
            ""
        )

        pieChart.setBackgroundColor(
            Color.TRANSPARENT
        )

        if (expenses.isEmpty()) {
            binding.tvExpenseChartEmpty.visibility =
                View.VISIBLE
            pieChart.invalidate()
            return
        }

        binding.tvExpenseChartEmpty.visibility =
            View.GONE

        val categorySums = expenses
            .groupBy { transaction ->
                transaction.category
            }
            .mapValues { entry ->
                entry.value.sumOf { transaction ->
                    transaction.amount
                }
            }
            .toList()
            .sortedByDescending { (_, total) ->
                total
            }

        val colorMap = mapOf(
            "Gıda" to Color.parseColor("#FF5722"),
            "Ulaşım" to Color.parseColor("#2196F3"),
            "Fatura" to Color.parseColor("#9C27B0"),
            "Eğitim" to Color.parseColor("#FFC107"),
            "Eğlence" to Color.parseColor("#E91E63"),
            "Diğer" to Color.parseColor("#795548")
        )

        val entries = ArrayList<PieEntry>()
        val chartColors = ArrayList<Int>()

        val visibleCategoryCount = 4

        categorySums.forEachIndexed {
                index,
                (categoryName, sum) ->

            val categoryColor =
                colorMap[categoryName] ?: Color.GRAY

            entries.add(
                PieEntry(
                    sum.toFloat(),
                    categoryName
                )
            )

            chartColors.add(categoryColor)

            if (index >= visibleCategoryCount) {
                return@forEachIndexed
            }

            val row = LinearLayout(
                requireContext()
            ).apply {

                orientation =
                    LinearLayout.HORIZONTAL

                gravity =
                    android.view.Gravity.CENTER_VERTICAL

                setPadding(
                    0,
                    8,
                    0,
                    8
                )
            }

            val colorView = View(
                requireContext()
            ).apply {

                layoutParams =
                    LinearLayout.LayoutParams(
                        30,
                        30
                    ).apply {

                        setMargins(
                            0,
                            0,
                            16,
                            0
                        )
                    }

                setBackgroundColor(
                    categoryColor
                )
            }

            val textView = TextView(
                requireContext()
            ).apply {

                text =
                    "$categoryName  ${formatCurrency(sum)}"

                textSize = 14f

                setTextColor(
                    onSurfaceColor
                )
            }

            row.addView(colorView)
            row.addView(textView)

            binding.llCategoryDetails.addView(row)
        }

        val hiddenCategoryCount =
            categorySums.size - visibleCategoryCount

        if (hiddenCategoryCount > 0) {

            val moreText =
                TextView(requireContext()).apply {

                    text =
                        "+$hiddenCategoryCount kategori daha"

                    textSize = 12f

                    setTextColor(
                        MaterialColors.getColor(
                            binding.root,
                            com.google.android.material.R.attr.colorPrimary,
                            Color.BLUE
                        )
                    )

                    setPadding(
                        0,
                        8,
                        0,
                        0
                    )
                }

            binding.llCategoryDetails.addView(
                moreText
            )
        }

        val dataSet =
            PieDataSet(
                entries,
                ""
            ).apply {

                colors = chartColors
                sliceSpace = 2.5f
                selectionShift = 7f

                setDrawValues(false)

                valueTextColor =
                    onSurfaceColor

                valueTextSize = 11f
            }

        val pieData =
            PieData(dataSet).apply {

                setValueTextColor(
                    onSurfaceColor
                )
            }

        pieChart.apply {

            data = pieData

            description.isEnabled = false
            legend.isEnabled = false

            setEntryLabelColor(
                onSurfaceColor
            )

            setDrawEntryLabels(false)

            isDrawHoleEnabled = true
            holeRadius = 58f
            transparentCircleRadius = 63f

            setHoleColor(
                surfaceColor
            )

            setTransparentCircleColor(
                surfaceColor
            )

            setTransparentCircleAlpha(100)

            setDrawCenterText(true)

            centerText = "Giderler"

            setCenterTextColor(
                onSurfaceColor
            )

            setCenterTextSize(13f)

            isRotationEnabled = true
            isHighlightPerTapEnabled = true

            setUsePercentValues(false)

            animateY(
                800,
                com.github.mikephil.charting.animation
                    .Easing
                    .EaseInOutQuad
            )

            invalidate()
        }
    }

    private fun updateRecurringPreview() {

        val now = Calendar.getInstance()

        val upcomingAll =
            currentRecurringTransactions
                .filter { recurring ->
                    recurring.isActive
                }
                .map { recurring ->
                    recurring to
                        RecurringDateUtils.nextDueDate(
                            recurring.dayOfMonth,
                            now
                        )
                }
                .sortedWith(
                    compareBy<Pair<RecurringTransaction, Calendar>> {
                        it.second.timeInMillis
                    }.thenBy {
                        it.first.id
                    }
                )

        val upcoming =
            upcomingAll.take(1)

        binding.llRecurringPreview.removeAllViews()

        binding.tvRecurringEmpty.visibility =
            if (upcoming.isEmpty()) {
                View.VISIBLE
            } else {
                View.GONE
            }

        binding.llRecurringPreview.visibility =
            if (upcoming.isEmpty()) {
                View.GONE
            } else {
                View.VISIBLE
            }

        binding.tvRecurringEmpty.text =
            when {
                currentRecurringTransactions.isEmpty() ->
                    "Henüz düzenli kayıt yok."

                else ->
                    "Aktif düzenli kayıt yok."
            }

        upcoming.forEach { (recurring, dueDate) ->
            binding.llRecurringPreview.addView(
                createRecurringPreviewRow(
                    recurring,
                    dueDate,
                    now
                )
            )
        }
    }

    private fun createRecurringPreviewRow(
        recurring: RecurringTransaction,
        dueDate: Calendar,
        now: Calendar
    ): View {

        val effectiveDay =
            dueDate.get(Calendar.DAY_OF_MONTH)

        val dueLabel =
            if (
                TransactionDateUtils.currentDateKey(
                    dueDate
                ) ==
                TransactionDateUtils.currentDateKey(
                    now
                )
            ) {
                "Bugün"
            } else {
                SimpleDateFormat(
                    "d MMM",
                    Locale.forLanguageTag("tr-TR")
                ).format(dueDate.time)
            }

        val detailLabel =
            buildString {
                append(dueLabel)
                append(" • ")
                append(
                    if (recurring.autoCreate) {
                        "otomatik kayıt"
                    } else {
                        "manuel kayıt"
                    }
                )

                if (
                    recurring.dayOfMonth !=
                    effectiveDay
                ) {
                    append(" • ayın son günü")
                }
            }

        val row =
            LinearLayout(requireContext()).apply {
                orientation =
                    LinearLayout.HORIZONTAL
                gravity =
                    android.view.Gravity.CENTER_VERTICAL
                isClickable = true
                isFocusable = true
                minimumHeight = dp(48)
                setPadding(
                    0,
                    dp(3),
                    dp(4),
                    dp(3)
                )
                setOnClickListener {
                    showRecurringBottomSheet(
                        recurring
                    )
                }
            }

        val textColumn =
            LinearLayout(requireContext()).apply {
                orientation =
                    LinearLayout.VERTICAL
                layoutParams =
                    LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                    )
            }

        val titleView =
            TextView(requireContext()).apply {
                text =
                    recurring.title.ifBlank {
                        if (
                            recurring.type ==
                            TransactionType.INCOME
                        ) {
                            "Düzenli gelir"
                        } else {
                            "Düzenli ödeme"
                        }
                    }
                setTextColor(
                    MaterialColors.getColor(
                        binding.root,
                        com.google.android.material.R.attr
                            .colorOnSurface,
                        Color.BLACK
                    )
                )
                textSize = 14f
                maxLines = 1
                ellipsize =
                    TextUtils.TruncateAt.END
            }

        val detailView =
            TextView(requireContext()).apply {
                text = detailLabel
                setTextColor(
                    MaterialColors.getColor(
                        binding.root,
                        com.google.android.material.R.attr
                            .colorOnSurfaceVariant,
                        Color.DKGRAY
                    )
                )
                textSize = 11f
                maxLines = 1
            }

        val isIncome =
            recurring.type ==
                TransactionType.INCOME

        val amountView =
            TextView(requireContext()).apply {
                text =
                    if (isIncome) {
                        "+${formatCurrency(recurring.amount)}"
                    } else {
                        "−${formatCurrency(recurring.amount)}"
                    }
                setTextColor(
                    if (isIncome) {
                        Color.parseColor("#4CAF50")
                    } else {
                        MaterialColors.getColor(
                            binding.root,
                            com.google.android.material.R.attr
                                .colorError,
                            Color.RED
                        )
                    }
                )
                textSize = 13f
                setTypeface(
                    typeface,
                    android.graphics.Typeface.BOLD
                )
                maxLines = 1
                setPadding(
                    dp(10),
                    0,
                    0,
                    0
                )
            }

        textColumn.addView(titleView)
        textColumn.addView(detailView)
        row.addView(textColumn)
        row.addView(amountView)

        row.contentDescription =
            "${recurring.title}, $detailLabel, " +
                amountView.text

        return row
    }

    private fun showRecurringManager() {

        if (currentRecurringTransactions.isEmpty()) {
            MaterialAlertDialogBuilder(
                requireContext()
            )
                .setTitle("Düzenli kayıtlar")
                .setMessage(
                    "Henüz düzenli ödeme veya gelir " +
                        "tanımlamadınız."
                )
                .setNegativeButton(
                    "Kapat",
                    null
                )
                .setPositiveButton(
                    "Yeni kayıt"
                ) { _, _ ->
                    showRecurringBottomSheet()
                }
                .show()

            return
        }

        val items =
            currentRecurringTransactions.map { recurring ->

                val status =
                    if (recurring.isActive) {
                        ""
                    } else {
                        "Pasif • "
                    }

                val type =
                    if (
                        recurring.type ==
                        TransactionType.INCOME
                    ) {
                        "gelir"
                    } else {
                        "gider"
                    }

                "$status${recurring.title} • " +
                    "ayın ${recurring.dayOfMonth}. günü • " +
                    "$type • ${formatCurrency(recurring.amount)}"
            }.toTypedArray()

        MaterialAlertDialogBuilder(
            requireContext()
        )
            .setTitle("Düzenli kayıtlar")
            .setItems(items) { _, position ->
                showRecurringBottomSheet(
                    currentRecurringTransactions[
                        position
                    ]
                )
            }
            .setNegativeButton(
                "Kapat",
                null
            )
            .setPositiveButton(
                "Yeni kayıt"
            ) { _, _ ->
                showRecurringBottomSheet()
            }
            .show()
    }

    private fun showRecurringBottomSheet(
        recurringToEdit: RecurringTransaction? = null
    ) {

        val bottomSheetDialog =
            BottomSheetDialog(requireContext())
                .also(::trackBottomSheet)

        val dialogBinding =
            BottomSheetAddRecurringBinding.inflate(
                layoutInflater
            )

        bottomSheetDialog.setContentView(
            dialogBinding.root
        )

        bottomSheetDialog.behavior.apply {
            state =
                BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
        }

        val categories = EXPENSE_CATEGORIES
        var selectedRecurringCategoryId: String? =
            recurringToEdit
                ?.category
                ?.let { category ->
                    categoryIdFor(
                        categories,
                        category
                    )
                }

        dialogBinding.etRecurringCategory.apply {
            setAdapter(
                ArrayAdapter(
                    requireContext(),
                    android.R.layout
                        .simple_dropdown_item_1line,
                    categories
                )
            )
            inputType =
                android.text.InputType.TYPE_NULL
            showSoftInputOnFocus = false
            isCursorVisible = false
            setOnClickListener {
                hideKeyboard(dialogBinding.etRecurringAmount)
                showDropDown()
            }
            setOnItemClickListener { _, _, position, _ ->
                selectedRecurringCategoryId =
                    categories.getOrNull(position)
                        ?.let { category ->
                            categoryIdFor(
                                categories,
                                category
                            )
                        }
                hideKeyboard(dialogBinding.etRecurringAmount)
            }
            setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    hideKeyboard(dialogBinding.etRecurringAmount)
                    showDropDown()
                }
            }
        }

        fun updateFormForType(
            isIncome: Boolean
        ) {

            dialogBinding
                .layoutRecurringCategory
                .visibility =
                if (isIncome) {
                    View.GONE
                } else {
                    View.VISIBLE
                }

            dialogBinding
                .layoutRecurringTitle
                .helperText =
                if (isIncome) {
                    "Örnek: Maaş, burs veya kira geliri"
                } else {
                    "Örnek: Kira, elektrik faturası veya abonelik"
                }
        }

        dialogBinding.rgRecurringType
            .setOnCheckedChangeListener { _, checkedId ->
                updateFormForType(
                    checkedId ==
                        R.id.rbRecurringIncome
                )
            }

        if (recurringToEdit != null) {

            dialogBinding.tvRecurringFormTitle.text =
                "Düzenli Kaydı Düzenle"

            dialogBinding.btnSaveRecurring.text =
                "Değişiklikleri Kaydet"

            dialogBinding.btnDeleteRecurring.visibility =
                View.VISIBLE

            dialogBinding.rgRecurringType.check(
                if (
                    recurringToEdit.type ==
                    TransactionType.INCOME
                ) {
                    R.id.rbRecurringIncome
                } else {
                    R.id.rbRecurringExpense
                }
            )

            dialogBinding.etRecurringTitle.setText(
                recurringToEdit.title
            )

            dialogBinding.etRecurringAmount.setText(
                recurringToEdit.amount.toString()
            )

            if (
                recurringToEdit.type ==
                TransactionType.EXPENSE
            ) {
                dialogBinding.etRecurringCategory.setText(
                    recurringToEdit.category,
                    false
                )
            }

            dialogBinding.etRecurringDay.setText(
                recurringToEdit.dayOfMonth.toString()
            )

            dialogBinding.switchRecurringAuto.isChecked =
                recurringToEdit.autoCreate

            dialogBinding
                .switchRecurringNotification
                .isChecked =
                recurringToEdit.notificationEnabled

            dialogBinding.switchRecurringActive.isChecked =
                recurringToEdit.isActive

            updateFormForType(
                recurringToEdit.type ==
                    TransactionType.INCOME
            )

        } else {

            dialogBinding.etRecurringDay.setText(
                Calendar.getInstance()
                    .get(Calendar.DAY_OF_MONTH)
                    .toString()
            )

            updateFormForType(
                isIncome = false
            )
        }

        dialogBinding.btnDeleteRecurring
            .setOnClickListener {

                val recurring =
                    recurringToEdit
                        ?: return@setOnClickListener

                MaterialAlertDialogBuilder(
                    requireContext()
                )
                    .setTitle(
                        "Düzenli kaydı sil"
                    )
                    .setMessage(
                        "\"${recurring.title}\" kaydını " +
                            "silmek istediğinizden emin misiniz?"
                    )
                    .setNegativeButton(
                        "İptal",
                        null
                    )
                    .setPositiveButton(
                        "Sil"
                    ) { _, _ ->

                        recurringTransactionViewModel
                            .delete(recurring)

                        showInAppMessage(
                            "Düzenli kayıt silindi"
                        )

                        bottomSheetDialog.dismiss()
                    }
                    .show()
            }

        dialogBinding.btnSaveRecurring
            .setOnClickListener {

                val title =
                    dialogBinding.etRecurringTitle
                        .text
                        ?.toString()
                        ?.trim()
                        .orEmpty()

                val amountResult =
                    DecimalInputValidator.positiveMoney(
                        rawValue =
                            dialogBinding.etRecurringAmount.text,
                        fieldName = "Tutar"
                    )

                val dayOfMonth =
                    dialogBinding.etRecurringDay
                        .text
                        ?.toString()
                        ?.trim()
                        ?.toIntOrNull()

                val isIncome =
                    dialogBinding.rgRecurringType
                        .checkedRadioButtonId ==
                        R.id.rbRecurringIncome

                val category =
                    dialogBinding.etRecurringCategory
                        .text
                        ?.toString()
                        ?.trim()
                        .orEmpty()

                if (title.isBlank()) {
                    dialogBinding.layoutRecurringTitle.error =
                        "Başlık girin"
                    return@setOnClickListener
                }

                dialogBinding.layoutRecurringTitle.error =
                    null

                if (amountResult is DecimalInputResult.Invalid) {
                    dialogBinding.layoutRecurringAmount.error =
                        amountResult.message
                    return@setOnClickListener
                }

                val amount =
                    (amountResult as DecimalInputResult.Valid)
                        .value
                        .toDouble()

                dialogBinding.layoutRecurringAmount.error =
                    null

                if (
                    !isIncome &&
                    selectedRecurringCategoryId == null
                ) {
                    dialogBinding
                        .layoutRecurringCategory
                        .error =
                        "Listeden geçerli bir kategori seçin"
                    return@setOnClickListener
                }

                dialogBinding
                    .layoutRecurringCategory
                    .error =
                    null

                if (
                    dayOfMonth == null ||
                    dayOfMonth !in 1..31
                ) {
                    dialogBinding.layoutRecurringDay.error =
                        "1 ile 31 arasında bir gün girin"
                    return@setOnClickListener
                }

                dialogBinding.layoutRecurringDay.error =
                    null

                val autoCreate =
                    dialogBinding
                        .switchRecurringAuto
                        .isChecked

                val notificationEnabled =
                    dialogBinding
                        .switchRecurringNotification
                        .isChecked

                val now = Calendar.getInstance()

                val dueDateHasPassed =
                    now.get(Calendar.DAY_OF_MONTH) >
                        RecurringDateUtils
                            .effectiveDueDay(
                                dayOfMonth,
                                now
                            )

                val autoScheduleIsNewOrChanged =
                    recurringToEdit == null ||
                        !recurringToEdit.autoCreate ||
                        recurringToEdit.dayOfMonth !=
                        dayOfMonth

                val lastGeneratedPeriod =
                    if (
                        autoCreate &&
                        dueDateHasPassed &&
                        autoScheduleIsNewOrChanged
                    ) {
                        RecurringDateUtils.currentPeriod(
                            now
                        )
                    } else {
                        recurringToEdit
                            ?.lastGeneratedPeriod
                    }

                val recurringToSave =
                    RecurringTransaction(
                        id =
                        recurringToEdit?.id ?: 0,
                        title = title,
                        amount = amount,
                        category =
                        if (isIncome) {
                            "Gelir"
                        } else {
                            category
                        },
                        type =
                        if (isIncome) {
                            TransactionType.INCOME
                        } else {
                            TransactionType.EXPENSE
                        },
                        dayOfMonth = dayOfMonth,
                        autoCreate = autoCreate,
                        notificationEnabled =
                        notificationEnabled,
                        isActive =
                        dialogBinding
                            .switchRecurringActive
                            .isChecked,
                        lastGeneratedPeriod =
                        lastGeneratedPeriod,
                        lastNotifiedPeriod =
                        recurringToEdit
                            ?.lastNotifiedPeriod
                    )

                if (recurringToEdit == null) {
                    recurringTransactionViewModel.insert(
                        recurringToSave
                    )
                } else {
                    recurringTransactionViewModel.update(
                        recurringToSave
                    )
                }

                if (notificationEnabled) {
                    requestNotificationPermissionIfNeeded()
                }

                showInAppMessage(
                    if (recurringToEdit == null) {
                        "Düzenli kayıt eklendi"
                    } else {
                        "Düzenli kayıt güncellendi"
                    }
                )

                bottomSheetDialog.dismiss()
            }

        bottomSheetDialog.show()
    }

    private fun requestNotificationPermissionIfNeeded() {

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(
                Manifest.permission.POST_NOTIFICATIONS
            )
        }
    }

    private fun dp(
        value: Int
    ): Int {
        return (
            value *
                resources.displayMetrics.density
            ).toInt()
    }

    private fun hideKeyboard(
        view: View
    ) {
        requireContext()
            .getSystemService<InputMethodManager>()
            ?.hideSoftInputFromWindow(
                view.windowToken,
                0
            )
        view.clearFocus()
    }

    private fun showInAppMessage(
        message: String,
        length: Int = Snackbar.LENGTH_SHORT
    ) {
        if (_binding != null) {
            Snackbar
                .make(
                    binding.root,
                    message,
                    length
                )
                .show()
        } else {
            Toast
                .makeText(
                    requireContext(),
                    message,
                    Toast.LENGTH_SHORT
                )
                .show()
        }
    }

    private fun categoryIdFor(
        categories: List<String>,
        category: String
    ): String? {
        val index =
            categories.indexOf(category)

        return if (index >= 0) {
            "expense_category_$index"
        } else {
            null
        }
    }

    private fun trackBottomSheet(
        dialog: BottomSheetDialog
    ) {
        activeBottomSheetDialogs += dialog

        dialog.setOnDismissListener {
            activeBottomSheetDialogs -= dialog
        }
    }

    private fun showTransactionBottomSheet(
        transactionToEdit: Transaction? = null
    ) {

        val bottomSheetDialog =
            BottomSheetDialog(requireContext())
                .also(::trackBottomSheet)

        val dialogBinding =
            BottomSheetAddExpenseBinding.inflate(
                layoutInflater
            )

        bottomSheetDialog.setContentView(
            dialogBinding.root
        )

        bottomSheetDialog.behavior.apply {
            state =
                BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
        }

        val isEditing =
            transactionToEdit != null

        val categories = EXPENSE_CATEGORIES
        var selectedTransactionCategoryId: String? =
            transactionToEdit
                ?.category
                ?.let { category ->
                    categoryIdFor(
                        categories,
                        category
                    )
                }

        dialogBinding.etCategory.apply {

            setAdapter(
                ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_dropdown_item_1line,
                    categories
                )
            )

            inputType =
                android.text.InputType.TYPE_NULL

            keyListener = null
            showSoftInputOnFocus = false
            isCursorVisible = false
            isFocusable = false
            isFocusableInTouchMode = false
            isClickable = true

            setOnClickListener {
                hideKeyboard(dialogBinding.etExpenseAmount)
                showDropDown()
            }
            setOnItemClickListener { _, _, position, _ ->
                selectedTransactionCategoryId =
                    categories.getOrNull(position)
                        ?.let { category ->
                            categoryIdFor(
                                categories,
                                category
                            )
                        }
                hideKeyboard(dialogBinding.etExpenseAmount)
            }

        }

        fun updateTransactionForm(
            isIncome: Boolean
        ) {

            dialogBinding.layoutCategory.visibility =
                if (isIncome) {
                    View.GONE
                } else {
                    View.VISIBLE
                }

            dialogBinding.tvAddTransactionTitle.text =
                when {

                    isEditing && isIncome ->
                        "Geliri Düzenle"

                    isEditing ->
                        "Gideri Düzenle"

                    isIncome ->
                        "Yeni Gelir Ekle"

                    else ->
                        "Yeni Gider Ekle"
                }

            dialogBinding.layoutExpenseTitle.hint =
                if (isIncome) {
                    "Gelir açıklaması"
                } else {
                    "Gider açıklaması"
                }

            dialogBinding.layoutExpenseTitle.helperText =
                if (isIncome) {
                    "Örnek: Maaş, burs veya ek gelir"
                } else {
                    "Örnek: Market alışverişi"
                }

            dialogBinding.btnSaveExpense.text =
                when {

                    isEditing && isIncome ->
                        "Geliri Güncelle"

                    isEditing ->
                        "Gideri Güncelle"

                    isIncome ->
                        "Geliri Kaydet"

                    else ->
                        "Gideri Kaydet"
                }

            if (isIncome) {

                dialogBinding.etCategory.setText(
                    "",
                    false
                )

                dialogBinding.layoutCategory.error =
                    null

                dialogBinding.etCategory.clearFocus()
                selectedTransactionCategoryId = null
            }
        }

        /*
         * Düzenleme ekranındaysa gelir-gider
         * seçimi tamamen gizlenir.
         */
        dialogBinding.rgTransactionType.visibility =
            if (isEditing) {
                View.GONE
            } else {
                View.VISIBLE
            }

        /*
         * RadioGroup yalnızca yeni işlem eklenirken
         * gelir-gider seçimi için kullanılır.
         */
        if (!isEditing) {

            dialogBinding.rgTransactionType
                .setOnCheckedChangeListener { _, checkedId ->

                    val isIncome =
                        checkedId == R.id.rbIncome

                    updateTransactionForm(isIncome)
                }
        }

        /*
         * Mevcut kayıt düzenleniyorsa işlem türü
         * değiştirilmeden bilgiler forma doldurulur.
         */
        if (transactionToEdit != null) {

            val isIncome =
                transactionToEdit.type ==
                        TransactionType.INCOME

            updateTransactionForm(isIncome)

            dialogBinding.etExpenseTitle.setText(
                transactionToEdit.title
            )

            dialogBinding.etExpenseAmount.setText(
                transactionToEdit.amount.toString()
            )

            if (!isIncome) {

                dialogBinding.etCategory.setText(
                    transactionToEdit.category,
                    false
                )
            }

        } else {

            val isIncome =
                dialogBinding.rgTransactionType
                    .checkedRadioButtonId ==
                        R.id.rbIncome

            updateTransactionForm(isIncome)
        }

        dialogBinding.btnSaveExpense
            .setOnClickListener {

                val title =
                    dialogBinding.etExpenseTitle.text
                        ?.toString()
                        ?.trim()
                        .orEmpty()

                val amountText =
                    dialogBinding.etExpenseAmount.text
                        ?.toString()
                        ?.trim()
                        .orEmpty()

                val selectedCategory =
                    dialogBinding.etCategory.text
                        ?.toString()
                        ?.trim()
                        .orEmpty()

                /*
                 * Düzenleme sırasında işlem türü
                 * mevcut kayıttan alınır.
                 *
                 * Yeni kayıt sırasında RadioGroup kullanılır.
                 */
                val isIncome =
                    if (transactionToEdit != null) {

                        transactionToEdit.type ==
                                TransactionType.INCOME

                    } else {

                        dialogBinding.rgTransactionType
                            .checkedRadioButtonId ==
                                R.id.rbIncome
                    }

                val amountResult =
                    DecimalInputValidator.positiveMoney(
                        rawValue = amountText,
                        fieldName = "Tutar"
                    )

                if (title.isEmpty()) {
                    dialogBinding.layoutExpenseTitle.error =
                        "Açıklama boş bırakılamaz"

                    return@setOnClickListener
                }

                dialogBinding.layoutExpenseTitle.error = null

                if (amountResult is DecimalInputResult.Invalid) {

                    dialogBinding.layoutExpenseAmount.error =
                        amountResult.message

                    return@setOnClickListener
                }

                val amount =
                    (amountResult as DecimalInputResult.Valid)
                        .value
                        .toDouble()

                dialogBinding.layoutExpenseAmount.error =
                    null

                if (
                    !isIncome &&
                    selectedTransactionCategoryId == null
                ) {

                    dialogBinding.layoutCategory.error =
                        "Listeden geçerli bir kategori seçin"

                    return@setOnClickListener
                }

                dialogBinding.layoutCategory.error =
                    null

                /*
                 * Düzenlenen eski gideri limit
                 * hesabından çıkarıyoruz.
                 */
                val transactionsForLimit =
                    if (transactionToEdit != null) {

                        currentTransactions.filterNot {
                                transaction ->

                            transaction.id ==
                                    transactionToEdit.id
                        }

                    } else {

                        currentTransactions
                    }

                if (
                    !isIncome &&
                    transactionViewModel
                        .isOverMonthlyLimit(
                            newExpenseAmount = amount,

                            monthlyLimit =
                            transactionViewModel
                                .getMonthlyLimit(
                                    requireContext()
                                ),

                            transactions =
                            transactionsForLimit
                        )
                ) {

                    showInAppMessage(
                        "DİKKAT: Bu işlem aylık gider limitini aşacak!",
                        Snackbar.LENGTH_LONG
                    )
                }

                val finalCategory =
                    if (isIncome) {
                        "Gelir"
                    } else {
                        selectedCategory
                    }

                val transactionToSave =
                    if (transactionToEdit != null) {

                        /*
                         * id, tarih ve işlem türü korunur.
                         */
                        transactionToEdit.copy(
                            title = title,
                            amount = amount,
                            category = finalCategory
                        )

                    } else {

                        Transaction(
                            title = title,
                            amount = amount,
                            category = finalCategory,

                            date =
                            SimpleDateFormat(
                                "dd.MM.yyyy HH:mm",
                                Locale.ROOT
                            ).format(Date()),

                            type =
                            if (isIncome) {
                                TransactionType.INCOME
                            } else {
                                TransactionType.EXPENSE
                            }
                        )
                    }

                if (isEditing) {

                    transactionViewModel.update(
                        transactionToSave
                    )

                } else {

                    transactionViewModel.insert(
                        transactionToSave
                    )
                }

                showInAppMessage(
                    when {

                        isEditing && isIncome ->
                            "Gelir güncellendi"

                        isEditing ->
                            "Gider güncellendi"

                        isIncome ->
                            "Gelir eklendi"

                        else ->
                            "Gider eklendi"
                    }
                )

                bottomSheetDialog.dismiss()
            }

        bottomSheetDialog.show()
    }

    private fun showDeleteTransactionDialog(
        transaction: Transaction
    ) {

        val transactionName =
            transaction.title
                .takeIf {
                    it.isNotBlank()
                }
                ?: if (
                    transaction.type ==
                    TransactionType.INCOME
                ) {
                    "Gelir"
                } else {
                    "Gider"
                }

        androidx.appcompat.app.AlertDialog.Builder(
            requireContext()
        )
            .setTitle("İşlemi sil")
            .setMessage(
                "\"$transactionName\" adlı " +
                        "${formatCurrency(transaction.amount)} " +
                        "tutarındaki kaydı silmek " +
                        "istediğinizden emin misiniz?"
            )
            .setNegativeButton(
                "İptal",
                null
            )
            .setPositiveButton(
                "Sil"
            ) { _, _ ->

                transactionViewModel.delete(
                    transaction
                )

                showInAppMessage(
                    "İşlem silindi"
                )
            }
            .show()
    }

    private fun showSetLimitDialog() {

        val builder =
            androidx.appcompat.app.AlertDialog.Builder(
                requireContext()
            )

        val currentLimit =
            transactionViewModel.getMonthlyLimit(
                requireContext()
            )

        val input =
            android.widget.EditText(
                requireContext()
            )

        input.inputType =
            android.text.InputType.TYPE_CLASS_NUMBER or
                    android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL

        input.keyListener =
            android.text.method.DigitsKeyListener.getInstance(
                "0123456789,."
            )
        input.filters =
            arrayOf(
                android.text.InputFilter.LengthFilter(24)
            )

        input.setText(
            currentLimit.toString()
        )

        input.setSelection(
            input.text?.length ?: 0
        )

        builder
            .setTitle(
                "Aylık Gider Limiti"
            )
            .setMessage(
                "Mevcut Limit: ${
                    formatCurrency(currentLimit)
                }"
            )
            .setView(input)

        builder.setPositiveButton("Kaydet", null)

        builder.setNegativeButton(
            "İptal",
            null
        )

        val dialog = builder.create()

        dialog.setOnShowListener {
            dialog.getButton(
                androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE
            ).setOnClickListener {
                when (
                    val result =
                        DecimalInputValidator.positiveMoney(
                            rawValue = input.text,
                            fieldName = "Aylık limit"
                        )
                ) {
                    is DecimalInputResult.Invalid ->
                        input.error = result.message

                    is DecimalInputResult.Valid -> {
                        input.error = null
                        transactionViewModel.saveMonthlyLimit(
                            requireContext(),
                            result.value.toDouble()
                        )

                        showInAppMessage(
                            "Limit güncellendi"
                        )

                        updateDailyBudgetUI()
                        dialog.dismiss()
                    }
                }
            }
        }

        dialog.show()
    }

    override fun onDestroyView() {
        activeBottomSheetDialogs
            .toList()
            .forEach { dialog ->
                dialog.dismiss()
            }
        activeBottomSheetDialogs.clear()

        super.onDestroyView()
        _binding = null
    }

    private enum class ExportFormat(
        val mimeType: String
    ) {
        XLSX(XLSX_MIME_TYPE),
        CSV("text/csv"),
        PDF("application/pdf")
    }

    private companion object {
        val EXPENSE_CATEGORIES =
            listOf(
                "Gıda",
                "Ulaşım",
                "Fatura",
                "Eğitim",
                "Eğlence",
                "Diğer"
            )
    }
}
