package com.epatay.digitalwallet.ui

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.epatay.digitalwallet.R
import com.epatay.digitalwallet.data.Transaction
import com.epatay.digitalwallet.data.TransactionType
import com.epatay.digitalwallet.databinding.BottomSheetAddExpenseBinding
import com.epatay.digitalwallet.databinding.FragmentDashboardBinding
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.color.MaterialColors
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DashboardFragment : Fragment(R.layout.fragment_dashboard) {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!

    private val transactionViewModel: TransactionViewModel by activityViewModels()

    private lateinit var adapter: TransactionAdapter

    private var currentTransactions: List<Transaction> = emptyList()

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
         * İşlem listesi, gider grafiği ve bütçe
         * bilgilerini gözlemler.
         */
        viewLifecycleOwner.lifecycleScope.launch {

            transactionViewModel.allTransactions
                .collectLatest { transactions ->

                    currentTransactions = transactions

                    adapter.updateData(transactions)

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
    }

    private fun updateDailyBudgetUI() {

        binding.tvDailyBudgetInfo.text =
            transactionViewModel.getDailyBudgetInfo(
                requireContext(),
                currentTransactions
            )
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

        val onSurfaceVariantColor = MaterialColors.getColor(
            binding.root,
            com.google.android.material.R.attr.colorOnSurfaceVariant,
            Color.DKGRAY
        )

        pieChart.setNoDataText(
            "Henüz gider kaydı yok"
        )

        pieChart.setNoDataTextColor(
            onSurfaceVariantColor
        )

        pieChart.setBackgroundColor(
            Color.TRANSPARENT
        )

        if (expenses.isEmpty()) {
            pieChart.invalidate()
            return
        }

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

        for ((categoryName, sum) in categorySums) {

            val categoryColor =
                colorMap[categoryName] ?: Color.GRAY

            entries.add(
                PieEntry(
                    sum.toFloat(),
                    categoryName
                )
            )

            chartColors.add(categoryColor)

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

                text = "$categoryName  ${
                    String.format(
                        Locale.getDefault(),
                        "%.2f",
                        sum
                    )
                } ₺"

                textSize = 14f

                setTextColor(
                    onSurfaceColor
                )
            }

            row.addView(colorView)
            row.addView(textView)

            binding.llCategoryDetails.addView(row)
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

    private fun showTransactionBottomSheet(
        transactionToEdit: Transaction? = null
    ) {

        val bottomSheetDialog =
            BottomSheetDialog(requireContext())

        val dialogBinding =
            BottomSheetAddExpenseBinding.inflate(
                layoutInflater
            )

        bottomSheetDialog.setContentView(
            dialogBinding.root
        )

        val isEditing =
            transactionToEdit != null

        val categories = arrayOf(
            "Gıda",
            "Ulaşım",
            "Fatura",
            "Eğitim",
            "Eğlence",
            "Diğer"
        )

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

            showSoftInputOnFocus = false
            isCursorVisible = false

            setOnClickListener {
                showDropDown()
            }

            setOnFocusChangeListener { _, hasFocus ->

                if (hasFocus) {
                    showDropDown()
                }
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

                val amount =
                    amountText
                        .replace(",", ".")
                        .toDoubleOrNull()

                if (
                    amount == null ||
                    !amount.isFinite() ||
                    amount <= 0.0
                ) {

                    dialogBinding.layoutExpenseAmount.error =
                        "Geçerli bir tutar girin"

                    return@setOnClickListener
                }

                dialogBinding.layoutExpenseAmount.error =
                    null

                if (
                    !isIncome &&
                    selectedCategory.isEmpty()
                ) {

                    dialogBinding.layoutCategory.error =
                        "Lütfen kategori seçin"

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

                    Toast.makeText(
                        requireContext(),
                        "DİKKAT: Bu işlem aylık gider limitini aşacak!",
                        Toast.LENGTH_LONG
                    ).show()
                }

                val finalTitle =
                    when {

                        title.isNotEmpty() ->
                            title

                        isIncome ->
                            "Gelir"

                        else ->
                            "Gider"
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
                            title = finalTitle,
                            amount = amount,
                            category = finalCategory
                        )

                    } else {

                        Transaction(
                            title = finalTitle,
                            amount = amount,
                            category = finalCategory,

                            date =
                            SimpleDateFormat(
                                "dd.MM.yyyy HH:mm",
                                Locale.getDefault()
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

                Toast.makeText(
                    requireContext(),

                    when {

                        isEditing && isIncome ->
                            "Gelir güncellendi"

                        isEditing ->
                            "Gider güncellendi"

                        isIncome ->
                            "Gelir eklendi"

                        else ->
                            "Gider eklendi"
                    },

                    Toast.LENGTH_SHORT
                ).show()

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

                Toast.makeText(
                    requireContext(),
                    "İşlem silindi",
                    Toast.LENGTH_SHORT
                ).show()
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

        builder.setPositiveButton(
            "Kaydet"
        ) { _, _ ->

            val newLimit =
                input.text
                    ?.toString()
                    ?.trim()
                    ?.replace(",", ".")
                    ?.toDoubleOrNull()
                    ?: currentLimit

            transactionViewModel.saveMonthlyLimit(
                requireContext(),
                newLimit
            )

            Toast.makeText(
                requireContext(),
                "Limit güncellendi!",
                Toast.LENGTH_SHORT
            ).show()

            updateDailyBudgetUI()
        }

        builder.setNegativeButton(
            "İptal",
            null
        )

        builder.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
