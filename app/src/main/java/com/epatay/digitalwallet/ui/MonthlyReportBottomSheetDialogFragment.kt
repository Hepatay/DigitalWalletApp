package com.epatay.digitalwallet.ui

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.epatay.digitalwallet.data.MonthlyTransactionTotals
import com.epatay.digitalwallet.data.TransactionDateUtils
import com.epatay.digitalwallet.databinding.BottomSheetMonthlyReportBinding
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.card.MaterialCardView
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.Locale
import kotlin.math.roundToInt

class MonthlyReportBottomSheetDialogFragment :
    BottomSheetDialogFragment() {

    private var _binding:
        BottomSheetMonthlyReportBinding? = null

    private val binding:
        BottomSheetMonthlyReportBinding
        get() = requireNotNull(_binding)

    private val budgetReportViewModel:
        BudgetReportViewModel by activityViewModels()

    private var lastTotals: MonthlyTransactionTotals? = null
    private var lastCategories: List<CategoryBudgetProgress> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding =
            BottomSheetMonthlyReportBinding.inflate(
                inflater,
                container,
                false
            )

        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(
            view,
            savedInstanceState
        )

        configureMonthNavigation()
        configureChart()
        observeReportData()
    }

    override fun onStart() {
        super.onStart()

        (dialog as? BottomSheetDialog)
            ?.behavior
            ?.apply {
                state =
                    BottomSheetBehavior.STATE_EXPANDED
                skipCollapsed = true
            }
    }

    private fun configureMonthNavigation() {
        binding.btnPreviousReportMonth
            .setOnClickListener {
                selectAdjacentMonth(-1)
            }

        binding.btnNextReportMonth
            .setOnClickListener {
                selectAdjacentMonth(1)
            }

        binding.tvReportMonth
            .setOnClickListener {
                showMonthYearPicker()
            }
    }

    private fun configureChart() {
        binding.reportPieChart.apply {
            description.isEnabled = false
            legend.isEnabled = false
            setDrawEntryLabels(false)
            setUsePercentValues(false)
            isRotationEnabled = true
            isHighlightPerTapEnabled = true
            isDrawHoleEnabled = true
            holeRadius = 58f
            transparentCircleRadius = 63f
            setNoDataText("")
        }
    }

    private fun observeReportData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {
                launch {
                    budgetReportViewModel
                        .selectedMonthKey
                        .collectLatest { monthKey ->
                            binding.tvReportMonth.text =
                                monthKey.toMonthLabel()
                        }
                }

                launch {
                    budgetReportViewModel
                        .monthlyTotals
                        .collectLatest { totals ->
                            renderMonthlyTotals(totals)
                        }
                }

                launch {
                    budgetReportViewModel
                        .categoryBudgetProgress
                        .collectLatest { categories ->
                            renderCategoryReport(
                                categories
                            )
                        }
                }
            }
        }
    }

    private fun renderMonthlyTotals(
        totals: MonthlyTransactionTotals
    ) {
        lastTotals = totals
        binding.tvReportIncome.text =
            formatCurrency(totals.totalIncome)

        binding.tvReportExpense.text =
            formatCurrency(totals.totalExpense)

        binding.tvReportBalance.text =
            formatCurrency(totals.balance)

        binding.tvReportBalance.setTextColor(
            MaterialColors.getColor(
                binding.root,
                if (totals.balance < 0.0) {
                    com.google.android.material.R.attr
                        .colorError
                } else {
                    com.google.android.material.R.attr
                        .colorSecondary
                },
                if (totals.balance < 0.0) {
                    Color.RED
                } else {
                    Color.parseColor("#4CAF50")
                }
            )
        )

        binding.tvReportIncome.contentDescription =
            "Toplam gelir ${formatCurrency(totals.totalIncome)}"

        binding.tvReportExpense.contentDescription =
            "Toplam gider ${formatCurrency(totals.totalExpense)}"

        binding.tvReportBalance.contentDescription =
            "Aylık bakiye ${formatCurrency(totals.balance)}"

        updateFinancialInsights()
    }

    private fun renderCategoryReport(
        allCategories: List<CategoryBudgetProgress>
    ) {
        lastCategories = allCategories
        val categories =
            allCategories.filter { category ->
                category.spentAmount > 0.0
            }

        binding.llReportCategories.removeAllViews()

        val isEmpty = categories.isEmpty()

        binding.tvReportEmpty.isVisible =
            isEmpty

        binding.reportPieChart.isVisible =
            !isEmpty

        if (isEmpty) {
            binding.reportPieChart.clear()
            binding.reportPieChart.invalidate()
            updateFinancialInsights()
            return
        }

        val colors =
            categories.indices.map { index ->
                CHART_COLORS[
                    index % CHART_COLORS.size
                ]
            }

        val entries =
            categories.map { category ->
                PieEntry(
                    category.spentAmount.toFloat(),
                    category.category
                )
            }

        val onSurfaceColor =
            MaterialColors.getColor(
                binding.root,
                com.google.android.material.R.attr
                    .colorOnSurface,
                Color.BLACK
            )

        val surfaceColor =
            MaterialColors.getColor(
                binding.root,
                com.google.android.material.R.attr
                    .colorSurface,
                Color.WHITE
            )

        val dataSet =
            PieDataSet(
                entries,
                ""
            ).apply {
                this.colors = colors
                sliceSpace = 2.5f
                selectionShift = 6f
                setDrawValues(false)
            }

        val totalExpense =
            categories.sumOf(
                CategoryBudgetProgress::spentAmount
            )

        binding.reportPieChart.apply {
            data = PieData(dataSet)
            setHoleColor(surfaceColor)
            setTransparentCircleColor(surfaceColor)
            setTransparentCircleAlpha(100)
            setDrawCenterText(true)
            centerText =
                "Giderler\n${formatCurrency(totalExpense)}"
            setCenterTextColor(onSurfaceColor)
            setCenterTextSize(14f)
            animateY(500)
            invalidate()
        }

        categories.forEachIndexed {
                index,
                category ->

            binding.llReportCategories.addView(
                createCategoryRow(
                    category = category,
                    totalExpense = totalExpense,
                    color = colors[index]
                )
            )
        }

        updateFinancialInsights()
    }

    private fun updateFinancialInsights() {
        val totals = lastTotals ?: return
        val categories = lastCategories.filter { it.spentAmount > 0.0 }

        // 1. Tasarruf Oranı
        if (totals.totalIncome > 0.0) {
            val savingsRate = ((totals.totalIncome - totals.totalExpense) / totals.totalIncome * 100.0).roundToInt()
            if (savingsRate >= 0) {
                binding.tvReportSavingsRate.text = "%$savingsRate Tasarruf"
                binding.tvReportSavingsRate.setTextColor(Color.parseColor("#4CAF50"))
            } else {
                binding.tvReportSavingsRate.text = "%${-savingsRate} Açık"
                binding.tvReportSavingsRate.setTextColor(Color.parseColor("#E53935"))
            }
        } else if (totals.totalExpense > 0.0) {
            binding.tvReportSavingsRate.text = "Gelir Yok"
            binding.tvReportSavingsRate.setTextColor(Color.parseColor("#E65100"))
        } else {
            binding.tvReportSavingsRate.text = "-%"
            binding.tvReportSavingsRate.setTextColor(
                MaterialColors.getColor(binding.root, com.google.android.material.R.attr.colorOnSurface, Color.BLACK)
            )
        }

        // 2. Günlük Ortalama Gider
        val monthKey = budgetReportViewModel.selectedMonthKey.value
        val year = monthKey / 100
        val month = (monthKey % 100) - 1
        val cal = GregorianCalendar(year, month, 1)
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val dailyAvg = totals.totalExpense / daysInMonth.coerceAtLeast(1)
        binding.tvReportDailyAvg.text = "${formatCurrency(dailyAvg)} / gün"

        // 3. En Yüksek Gider Kategorisi
        if (categories.isNotEmpty()) {
            val topCategory = categories.maxByOrNull { it.spentAmount }
            if (topCategory != null && totals.totalExpense > 0.0) {
                val percent = ((topCategory.spentAmount / totals.totalExpense) * 100.0).roundToInt()
                binding.tvReportTopCategory.text = "${topCategory.category} (%$percent)"
            } else {
                binding.tvReportTopCategory.text = "Gider Yok"
            }
        } else {
            binding.tvReportTopCategory.text = "Gider Yok"
        }

        // 4. Toplam İşlem
        binding.tvReportTransactionCount.text = "${totals.transactionCount} Kayıt"

        // 5. Finansal Tavsiye / Analiz Notu
        val advice = when {
            totals.totalIncome == 0.0 && totals.totalExpense == 0.0 ->
                "ℹ️ Bu ay için henüz bir gelir veya gider kaydı bulunmuyor."
            totals.balance > 0.0 ->
                "💡 Harika! Bu ay geliriniz giderlerinizden ${formatCurrency(totals.balance)} daha fazla. Birikim hedeflerinize katkı yapabilirsiniz."
            totals.balance < 0.0 ->
                "⚠️ Dikkat: Bu ay giderleriniz gelirinizi ${formatCurrency(-totals.balance)} aştı. Harcamalarınızı gözden geçirmenizi öneririz."
            else ->
                "⚖️ Bu ay gelir ve giderleriniz tam olarak dengelendi."
        }
        binding.tvReportAdvice.text = advice
    }

    private fun createCategoryRow(
        category: CategoryBudgetProgress,
        totalExpense: Double,
        color: Int
    ): View {
        val context = requireContext()

        val onSurfaceColor =
            MaterialColors.getColor(
                binding.root,
                com.google.android.material.R.attr
                    .colorOnSurface,
                Color.BLACK
            )

        val onSurfaceVariantColor =
            MaterialColors.getColor(
                binding.root,
                com.google.android.material.R.attr
                    .colorOnSurfaceVariant,
                Color.DKGRAY
            )

        val row =
            LinearLayout(context).apply {
                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                orientation = LinearLayout.VERTICAL
                setPadding(
                    0,
                    dp(6),
                    0,
                    dp(6)
                )
            }

        val topRow =
            LinearLayout(context).apply {
                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
            }

        val colorMarker =
            MaterialCardView(context).apply {
                layoutParams =
                    LinearLayout.LayoutParams(
                        dp(12),
                        dp(12)
                    ).apply {
                        marginEnd = dp(10)
                    }
                radius = dp(6).toFloat()
                cardElevation = 0f
                setCardBackgroundColor(color)
                importantForAccessibility =
                    View.IMPORTANT_FOR_ACCESSIBILITY_NO
            }

        val textColumn =
            LinearLayout(context).apply {
                layoutParams =
                    LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                    )
                orientation = LinearLayout.VERTICAL
            }

        val categoryName =
            TextView(context).apply {
                text = category.category
                setTextColor(onSurfaceColor)
                textSize = 14f
                setTypeface(typeface, Typeface.BOLD)
                maxLines = 1
                ellipsize =
                    android.text.TextUtils.TruncateAt.END
            }

        val categoryPercent =
            if (totalExpense > 0.0) {
                ((category.spentAmount / totalExpense) * 100.0).roundToInt()
            } else 0

        val countAndBudget =
            TextView(context).apply {
                text =
                    buildString {
                        append("%$categoryPercent • ${category.transactionCount} işlem")

                        category.limitAmount?.let { limit ->
                            append(" • Bütçe ")
                            append(
                                formatCurrency(limit)
                            )
                        }
                    }
                setTextColor(onSurfaceVariantColor)
                textSize = 12f
                maxLines = 1
                ellipsize =
                    android.text.TextUtils.TruncateAt.END
            }

        val amount =
            TextView(context).apply {
                text =
                    formatCurrency(
                        category.spentAmount
                    )
                setTextColor(onSurfaceColor)
                textSize = 14f
                setTypeface(typeface, Typeface.BOLD)
                maxLines = 1
                setPadding(
                    dp(10),
                    0,
                    0,
                    0
                )
            }

        val progressBar =
            LinearProgressIndicator(context).apply {
                layoutParams =
                    LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(4)
                    ).apply {
                        topMargin = dp(4)
                        marginStart = dp(22)
                    }
                max = 100
                progress = categoryPercent.coerceIn(0, 100)
                trackCornerRadius = dp(2)
                setIndicatorColor(color)
                trackColor = Color.parseColor("#18000000")
            }

        textColumn.addView(categoryName)
        textColumn.addView(countAndBudget)
        topRow.addView(colorMarker)
        topRow.addView(textColumn)
        topRow.addView(amount)

        row.addView(topRow)
        row.addView(progressBar)

        row.contentDescription =
            buildString {
                append(category.category)
                append(". ")
                append(category.transactionCount)
                append(" işlem. Toplam ")
                append(
                    formatCurrency(
                        category.spentAmount
                    )
                )

                category.limitAmount?.let { limit ->
                    append(". Bütçe ")
                    append(formatCurrency(limit))
                }
            }

        return row
    }

    private fun selectAdjacentMonth(
        amount: Int
    ) {
        val current =
            budgetReportViewModel
                .selectedMonthKey
                .value

        val adjacent =
            current.shiftMonth(amount)

        if (adjacent > TransactionDateUtils.currentMonthKey()) {
            com.epatay.digitalwallet.util.InAppNotification.show(
                activity,
                "Gelecek aylar için rapor seçilemez.",
                com.epatay.digitalwallet.util.NotificationType.WARNING
            )
            return
        }

        if (adjacent != current) {
            budgetReportViewModel.selectMonth(
                adjacent
            )
        }
    }

    private fun showMonthYearPicker() {
        val currentKey =
            budgetReportViewModel
                .selectedMonthKey
                .value
                .takeIf(TransactionDateUtils::isValidMonthKey)
                ?: TransactionDateUtils.currentMonthKey()

        val nowKey =
            TransactionDateUtils.currentMonthKey()
        val nowYear = nowKey / 100
        val nowMonth = nowKey % 100

        val pickerContainer =
            LinearLayout(requireContext()).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity =
                    android.view.Gravity.CENTER
                setPadding(
                    dp(20),
                    dp(8),
                    dp(20),
                    0
                )
            }

        val monthPicker =
            NumberPicker(requireContext()).apply {
                minValue = 1
                maxValue = 12
                displayedValues = MONTH_NAMES
                value = currentKey % 100
                wrapSelectorWheel = false
            }

        val yearPicker =
            NumberPicker(requireContext()).apply {
                minValue = nowYear - 10
                maxValue = nowYear
                value =
                    (currentKey / 100)
                        .coerceIn(minValue, maxValue)
                wrapSelectorWheel = false
            }

        fun updateMonthLimitForYear(year: Int) {
            val currentMonthValue =
                monthPicker.value

            monthPicker.displayedValues = null
            monthPicker.minValue = 1
            monthPicker.maxValue =
                if (year == nowYear) {
                    nowMonth
                } else {
                    12
                }
            monthPicker.displayedValues =
                MONTH_NAMES.take(monthPicker.maxValue)
                    .toTypedArray()
            monthPicker.value =
                currentMonthValue
                    .coerceIn(1, monthPicker.maxValue)
        }

        updateMonthLimitForYear(yearPicker.value)

        yearPicker.setOnValueChangedListener { _, _, newValue ->
            updateMonthLimitForYear(newValue)
        }

        pickerContainer.addView(monthPicker)
        pickerContainer.addView(yearPicker)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Ay ve yıl seç")
            .setView(pickerContainer)
            .setNegativeButton("İptal", null)
            .setPositiveButton("Uygula") { _, _ ->
                val selectedKey =
                    yearPicker.value * 100 +
                        monthPicker.value

                if (selectedKey > nowKey) {
                    com.epatay.digitalwallet.util.InAppNotification.show(
                        activity,
                        "Gelecek aylar için rapor seçilemez.",
                        com.epatay.digitalwallet.util.NotificationType.WARNING
                    )
                    return@setPositiveButton
                }

                budgetReportViewModel.selectMonth(
                    selectedKey
                )
            }
            .show()
    }

    private fun Int.shiftMonth(
        amount: Int
    ): Int {
        if (
            !TransactionDateUtils.isValidMonthKey(this)
        ) {
            return TransactionDateUtils.currentMonthKey()
        }

        val year = this / 100
        val month = this % 100

        val absoluteMonth =
            year.toLong() * 12L +
                month.toLong() -
                1L +
                amount.toLong()

        val shiftedYear =
            (absoluteMonth / 12L).toInt()
        val shiftedMonth =
            (absoluteMonth % 12L).toInt() + 1
        val shiftedKey =
            shiftedYear * 100 +
                shiftedMonth

        return shiftedKey.takeIf(
            TransactionDateUtils::isValidMonthKey
        ) ?: this
    }

    private fun Int.toMonthLabel(): String {
        if (
            !TransactionDateUtils.isValidMonthKey(this)
        ) {
            return ""
        }

        val calendar =
            GregorianCalendar(
                this / 100,
                this % 100 - 1,
                1
            )

        val label =
            SimpleDateFormat(
                "LLLL yyyy",
                TURKISH_LOCALE
            ).format(calendar.time)

        return label.replaceFirstChar { character ->
            character.titlecase(TURKISH_LOCALE)
        }
    }

    private fun formatCurrency(
        value: Double
    ): String {
        return String.format(
            TURKISH_LOCALE,
            "%,.2f ₺",
            value
        )
    }

    private fun dp(
        value: Int
    ): Int {
        return (
            value *
                resources.displayMetrics.density
            ).toInt()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG =
            "monthly_report"

        private val TURKISH_LOCALE =
            Locale.forLanguageTag("tr-TR")

        private val CHART_COLORS =
            listOf(
                Color.parseColor("#4CAF50"), // Market (Green)
                Color.parseColor("#FF5722"), // Yiyecek/İçecek (Deep Orange)
                Color.parseColor("#9C27B0"), // Fatura (Purple)
                Color.parseColor("#2196F3"), // Ulaşım (Blue)
                Color.parseColor("#E91E63"), // Alışveriş (Pink)
                Color.parseColor("#795548"), // Ev (Brown)
                Color.parseColor("#607D8B"), // Araç (Blue Grey)
                Color.parseColor("#00BCD4"), // Kişisel (Cyan)
                Color.parseColor("#F44336"), // Sağlık (Red)
                Color.parseColor("#673AB7"), // Eğlence (Deep Purple)
                Color.parseColor("#FF9800"), // Eğitim (Orange)
                Color.parseColor("#009688"), // Spor/Hobi (Teal)
                Color.parseColor("#3F51B5"), // Seyahat (Indigo)
                Color.parseColor("#455A64"), // İş (Slate)
                Color.parseColor("#2E7D32"), // Birikim (Dark Green)
                Color.parseColor("#9E9E9E")  // Diğer (Grey)
            )

        private val MONTH_NAMES =
            arrayOf(
                "Ocak",
                "Şubat",
                "Mart",
                "Nisan",
                "Mayıs",
                "Haziran",
                "Temmuz",
                "Ağustos",
                "Eylül",
                "Ekim",
                "Kasım",
                "Aralık"
            )
    }
}
