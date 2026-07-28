package com.epatay.digitalwallet.ui

import android.app.Dialog
import android.content.DialogInterface
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.getSystemService
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.epatay.digitalwallet.data.DecimalInputResult
import com.epatay.digitalwallet.data.DecimalInputValidator
import com.epatay.digitalwallet.data.TransactionDateUtils
import com.epatay.digitalwallet.databinding.BottomSheetCategoryBudgetsBinding
import com.epatay.digitalwallet.databinding.BottomSheetEditCategoryBudgetBinding
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
import java.text.Collator
import java.text.SimpleDateFormat
import java.util.GregorianCalendar
import java.util.Locale

class CategoryBudgetsBottomSheetDialogFragment :
    BottomSheetDialogFragment() {

    private var _binding:
        BottomSheetCategoryBudgetsBinding? = null

    private val binding:
        BottomSheetCategoryBudgetsBinding
        get() = requireNotNull(_binding)

    private val budgetReportViewModel:
        BudgetReportViewModel by activityViewModels()

    private val childDialogs =
        linkedSetOf<Dialog>()

    private var currentProgress:
        List<CategoryBudgetProgress> = emptyList()

    private var availableCategories:
        List<String> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding =
            BottomSheetCategoryBudgetsBinding.inflate(
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
        configureActions()
        observeBudgetData()
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
        binding.btnPreviousCategoryBudgetMonth
            .setOnClickListener {
                selectAdjacentMonth(-1)
            }

        binding.btnNextCategoryBudgetMonth
            .setOnClickListener {
                selectAdjacentMonth(1)
            }
    }

    private fun configureActions() {
        binding.btnAddCategoryBudget
            .setOnClickListener {
                showBudgetForm()
            }
    }

    private fun observeBudgetData() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {
                launch {
                    budgetReportViewModel
                        .selectedMonthKey
                        .collectLatest { monthKey ->
                            binding.tvCategoryBudgetMonth.text =
                                monthKey.toMonthLabel()
                        }
                }

                launch {
                    budgetReportViewModel
                        .categoryBudgetProgress
                        .collectLatest { progress ->
                            currentProgress = progress
                            renderBudgetRows(progress)
                        }
                }

                launch {
                    budgetReportViewModel
                        .availableCategories
                        .collectLatest { categories ->
                            availableCategories =
                                categories
                                    .map(String::trim)
                                    .filter(String::isNotEmpty)
                        }
                }
            }
        }
    }

    private fun renderBudgetRows(
        progress: List<CategoryBudgetProgress>
    ) {
        binding.llCategoryBudgetRows.removeAllViews()

        val isEmpty = progress.isEmpty()

        binding.tvCategoryBudgetsEmpty.isVisible =
            isEmpty

        binding.llCategoryBudgetRows.isVisible =
            !isEmpty

        val totalLimit =
            progress.sumOf { item ->
                item.limitAmount ?: 0.0
            }

        val totalSpent =
            progress.sumOf(
                CategoryBudgetProgress::spentAmount
            )

        binding.tvCategoryBudgetSummary.text =
            "Toplam bütçe: ${formatCurrency(totalLimit)} • " +
                "Harcanan: ${formatCurrency(totalSpent)}"

        progress.forEach { item ->
            binding.llCategoryBudgetRows.addView(
                createBudgetRow(item)
            )
        }
    }

    private fun createBudgetRow(
        item: CategoryBudgetProgress
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

        val primaryColor =
            MaterialColors.getColor(
                binding.root,
                com.google.android.material.R.attr
                    .colorPrimary,
                Color.BLUE
            )

        val errorColor =
            MaterialColors.getColor(
                binding.root,
                com.google.android.material.R.attr
                    .colorError,
                Color.RED
            )

        val statusColor =
            if (item.isExceeded) {
                errorColor
            } else {
                primaryColor
            }

        val statusText =
            when {
                !item.hasBudget ->
                    "Bütçe belirlenmedi"

                item.isExceeded ->
                    "Aşım ${formatCurrency(item.exceededAmount)}"

                else ->
                    "Kalan ${formatCurrency(item.remainingAmount)}"
            }

        val card =
            MaterialCardView(context).apply {
                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        bottomMargin = dp(8)
                    }

                minimumHeight = dp(72)
                radius = dp(12).toFloat()
                cardElevation = 0f
                strokeWidth = dp(1)
                strokeColor =
                    MaterialColors.getColor(
                        binding.root,
                        com.google.android.material.R.attr
                            .colorOutlineVariant,
                        Color.LTGRAY
                    )
                setCardBackgroundColor(
                    MaterialColors.getColor(
                        binding.root,
                        com.google.android.material.R.attr
                            .colorSurfaceVariant,
                        Color.WHITE
                    )
                )
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    showBudgetForm(item)
                }
            }

        val content =
            LinearLayout(context).apply {
                layoutParams =
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )
                orientation = LinearLayout.VERTICAL
                setPadding(
                    dp(12),
                    dp(10),
                    dp(12),
                    dp(10)
                )
            }

        val headingRow =
            LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity =
                    android.view.Gravity.CENTER_VERTICAL
            }

        val title =
            TextView(context).apply {
                layoutParams =
                    LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1f
                    )
                text = item.category
                setTextColor(onSurfaceColor)
                textSize = 15f
                setTypeface(typeface, Typeface.BOLD)
                maxLines = 1
                ellipsize =
                    android.text.TextUtils.TruncateAt.END
            }

        val status =
            TextView(context).apply {
                text = statusText
                setTextColor(statusColor)
                textSize = 12f
                setTypeface(typeface, Typeface.BOLD)
                maxLines = 1
            }

        val details =
            TextView(context).apply {
                layoutParams =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply {
                        topMargin = dp(3)
                    }

                text =
                    buildString {
                        append(
                            "Harcanan "
                        )
                        append(
                            formatCurrency(item.spentAmount)
                        )
                        append(" • ")
                        append(item.transactionCount)
                        append(" işlem")

                        if (item.limitAmount != null) {
                            append(" • Limit ")
                            append(
                                formatCurrency(
                                    item.limitAmount
                                )
                            )
                        }
                    }

                setTextColor(onSurfaceVariantColor)
                textSize = 12f
                maxLines = 2
            }

        headingRow.addView(title)
        headingRow.addView(status)
        content.addView(headingRow)
        content.addView(details)

        if (item.hasBudget) {
            content.addView(
                LinearProgressIndicator(context).apply {
                    layoutParams =
                        LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT,
                            dp(6)
                        ).apply {
                            topMargin = dp(8)
                        }
                    max = 100
                    trackThickness = dp(6)
                    trackCornerRadius = dp(3)
                    trackColor =
                        MaterialColors.getColor(
                            binding.root,
                            com.google.android.material.R.attr
                                .colorOutlineVariant,
                            Color.LTGRAY
                        )
                    setIndicatorColor(statusColor)
                    setProgressCompat(
                        item.progressBarPercent,
                        false
                    )
                    contentDescription =
                        "Bütçenin yüzde " +
                            "${item.usagePercent} kadarı kullanıldı"
                }
            )
        }

        card.contentDescription =
            buildString {
                append(item.category)
                append(". Harcanan ")
                append(formatCurrency(item.spentAmount))
                append(". ")

                if (item.limitAmount != null) {
                    append("Limit ")
                    append(
                        formatCurrency(item.limitAmount)
                    )
                    append(". ")
                }

                append(statusText)
                append(". Düzenlemek için dokunun.")
            }

        card.addView(content)

        return card
    }

    private fun showBudgetForm(
        itemToEdit: CategoryBudgetProgress? = null
    ) {
        val dialogContext =
            context ?: return
        val viewModel =
            budgetReportViewModel
        val formDialog =
            trackChildDialog(
                BottomSheetDialog(dialogContext)
            )

        val formBinding =
            BottomSheetEditCategoryBudgetBinding.inflate(
                LayoutInflater.from(dialogContext)
            )

        formDialog.setContentView(
            formBinding.root
        )

        formDialog.setOnShowListener {
            formDialog.behavior.apply {
                state =
                    BottomSheetBehavior.STATE_EXPANDED
                skipCollapsed = true
            }
        }

        val isEditing =
            itemToEdit?.hasBudget == true

        formBinding.tvCategoryBudgetFormTitle.text =
            if (isEditing) {
                "Kategori Bütçesini Düzenle"
            } else {
                "Kategori Bütçesi Ekle"
            }

        val categoryOptions =
            buildCategoryOptions(
                selectedCategory =
                    itemToEdit?.category
            )
        var selectedCategoryId: String? =
            itemToEdit
                ?.category
                ?.let { category ->
                    categoryIdFor(
                        categoryOptions,
                        category
                    )
                }

        formBinding.etCategoryBudgetCategory.apply {
            setAdapter(
                ArrayAdapter(
                    dialogContext,
                    android.R.layout
                        .simple_dropdown_item_1line,
                    categoryOptions
                )
            )
            threshold = 0
            inputType = InputType.TYPE_NULL
            isCursorVisible = false
            showSoftInputOnFocus = false
            setOnClickListener {
                hideKeyboard(formBinding.etCategoryBudgetAmount)
                showDropDown()
            }
            setOnItemClickListener { _, _, position, _ ->
                selectedCategoryId =
                    categoryOptions.getOrNull(position)
                        ?.let { category ->
                            categoryIdFor(
                                categoryOptions,
                                category
                            )
                        }
                hideKeyboard(formBinding.etCategoryBudgetAmount)
            }
            setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    hideKeyboard(formBinding.etCategoryBudgetAmount)
                    showDropDown()
                }
            }
        }

        if (itemToEdit != null) {
            formBinding.etCategoryBudgetCategory
                .setText(
                    itemToEdit.category,
                    false
                )

            itemToEdit.limitAmount?.let { limit ->
                formBinding.etCategoryBudgetAmount
                    .setText(
                        limit.toEditableAmount()
                    )
            }
        }

        if (isEditing) {
            formBinding.etCategoryBudgetCategory
                .isEnabled = false
            formBinding.btnDeleteCategoryBudget
                .isVisible = true
        }

        formBinding.btnSaveCategoryBudget
            .setOnClickListener {
                val category =
                    formBinding.etCategoryBudgetCategory
                        .text
                        ?.toString()
                        ?.trim()
                        .orEmpty()

                val amountResult =
                    DecimalInputValidator.positiveMoney(
                        rawValue =
                            formBinding.etCategoryBudgetAmount.text,
                        fieldName = "Bütçe tutarı"
                    )

                if (selectedCategoryId == null) {
                    formBinding
                        .layoutCategoryBudgetCategory
                        .error =
                        "Listeden geçerli bir kategori seçin"
                    return@setOnClickListener
                }

                formBinding
                    .layoutCategoryBudgetCategory
                    .error = null

                if (amountResult is DecimalInputResult.Invalid) {
                    formBinding
                        .layoutCategoryBudgetAmount
                        .error =
                        amountResult.message
                    return@setOnClickListener
                }

                val amount =
                    (amountResult as DecimalInputResult.Valid)
                        .value
                        .toDouble()

                formBinding
                    .layoutCategoryBudgetAmount
                    .error = null

                viewModel.upsertBudget(
                    category = category,
                    limitAmount = amount
                )

                showInAppMessage(
                    if (isEditing) {
                        "Kategori bütçesi güncellendi"
                    } else {
                        "Kategori bütçesi eklendi"
                    }
                )

                formDialog.dismiss()
            }

        formBinding.btnDeleteCategoryBudget
            .setOnClickListener {
                val category =
                    itemToEdit
                        ?.category
                        ?: return@setOnClickListener

                val confirmationDialog =
                    MaterialAlertDialogBuilder(
                        dialogContext
                    )
                    .setTitle(
                        "Kategori bütçesini sil"
                    )
                    .setMessage(
                        "\"$category\" bütçesini silmek " +
                            "istediğinizden emin misiniz?"
                    )
                    .setNegativeButton(
                        "İptal",
                        null
                    )
                    .setPositiveButton(
                        "Sil"
                    ) { _, _ ->
                        viewModel
                            .deleteBudget(category)

                        showInAppMessage(
                            "Kategori bütçesi silindi"
                        )

                        formDialog.dismiss()
                    }
                    .create()

                trackChildDialog(
                    confirmationDialog
                ).show()
            }

        formDialog.show()
    }

    private fun buildCategoryOptions(
        selectedCategory: String?
    ): List<String> {
        val collator =
            Collator.getInstance(TURKISH_LOCALE).apply {
                strength = Collator.PRIMARY
            }

        return (
            DEFAULT_EXPENSE_CATEGORIES +
                availableCategories +
                currentProgress.map(
                    CategoryBudgetProgress::category
                ) +
                listOfNotNull(selectedCategory)
            )
            .asSequence()
            .map(String::trim)
            .filter(String::isNotEmpty)
            .filterNot { category ->
                category.equals(
                    "Gelir",
                    ignoreCase = true
                )
            }
            .distinctBy { category ->
                category.lowercase(TURKISH_LOCALE)
            }
            .sortedWith { first, second ->
                collator.compare(first, second)
            }
            .toList()
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

        if (adjacent != current) {
            budgetReportViewModel.selectMonth(
                adjacent
            )
        }
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

    private fun Double.toEditableAmount(): String {
        return if (
            this % 1.0 == 0.0
        ) {
            toLong().toString()
        } else {
            toString().replace(".", ",")
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
        message: String
    ) {
        Snackbar
            .make(
                binding.root,
                message,
                Snackbar.LENGTH_SHORT
            )
            .show()
    }

    private fun categoryIdFor(
        categories: List<String>,
        category: String
    ): String? {
        val index =
            categories.indexOf(category)

        return if (index >= 0) {
            "budget_category_$index"
        } else {
            null
        }
    }

    private fun <T : Dialog> trackChildDialog(
        childDialog: T
    ): T {
        childDialogs.add(childDialog)

        childDialog.setOnDismissListener {
            childDialogs.remove(childDialog)
        }

        return childDialog
    }

    private fun dismissChildDialogs() {
        val dialogsToDismiss =
            childDialogs.toList()

        childDialogs.clear()

        dialogsToDismiss.forEach { childDialog ->
            runCatching {
                childDialog.dismiss()
            }
        }
    }

    override fun onDismiss(
        dialog: DialogInterface
    ) {
        dismissChildDialogs()
        super.onDismiss(dialog)
    }

    override fun onDestroyView() {
        dismissChildDialogs()
        _binding = null
        super.onDestroyView()
    }

    companion object {
        const val TAG =
            "category_budgets"

        private val TURKISH_LOCALE =
            Locale.forLanguageTag("tr-TR")

        private val DEFAULT_EXPENSE_CATEGORIES =
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
