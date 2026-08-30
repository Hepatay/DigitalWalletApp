package com.epatay.digitalwallet.ui

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.epatay.digitalwallet.R
import com.epatay.digitalwallet.data.TransactionDateUtils
import com.epatay.digitalwallet.data.TransactionFilter
import com.epatay.digitalwallet.data.TransactionType
import com.epatay.digitalwallet.databinding.BottomSheetTransactionFiltersBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.epatay.digitalwallet.util.setupMoneyInput
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.Locale

class TransactionFilterBottomSheetDialogFragment :
    BottomSheetDialogFragment() {

    private var _binding:
        BottomSheetTransactionFiltersBinding? = null

    private val binding:
        BottomSheetTransactionFiltersBinding
        get() = requireNotNull(_binding)

    private val transactionViewModel:
        TransactionViewModel by activityViewModels()

    private var selectedCategory: String? = null
    private var selectedStartDateKey: Int? = null
    private var selectedEndDateKey: Int? = null
    private var categoryOptions: List<String> =
        listOf(ALL_CATEGORIES_LABEL)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding =
            BottomSheetTransactionFiltersBinding.inflate(
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

        restoreFilterState(
            transactionViewModel.filters.value
        )
        configureAmountInput()
        configureCategoryInput()
        configureDateButtons()
        configureActions()
        observeCategories()
    }

    override fun onStart() {
        super.onStart()

        val bottomSheetDialog =
            dialog as? BottomSheetDialog

        bottomSheetDialog
            ?.behavior
            ?.apply {
                state =
                    BottomSheetBehavior.STATE_EXPANDED
                skipCollapsed = true
            }
    }

    private fun configureAmountInput() {
        binding.etFilterMinAmount.setupMoneyInput(layout = binding.layoutFilterMinAmount)
        binding.etFilterMaxAmount.setupMoneyInput(layout = binding.layoutFilterMaxAmount)

        binding.chipGroupAmountPresets.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isEmpty()) return@setOnCheckedStateChangeListener
            when (checkedIds.first()) {
                R.id.chipAmountAll -> {
                    binding.etFilterMinAmount.setText("")
                    binding.etFilterMaxAmount.setText("")
                }
                R.id.chipAmount0To500 -> {
                    binding.etFilterMinAmount.setText("")
                    binding.etFilterMaxAmount.setText("500")
                }
                R.id.chipAmount500To2500 -> {
                    binding.etFilterMinAmount.setText("500")
                    binding.etFilterMaxAmount.setText("2500")
                }
                R.id.chipAmount2500To10000 -> {
                    binding.etFilterMinAmount.setText("2500")
                    binding.etFilterMaxAmount.setText("10000")
                }
                R.id.chipAmount10000Plus -> {
                    binding.etFilterMinAmount.setText("10000")
                    binding.etFilterMaxAmount.setText("")
                }
            }
        }
    }

    private fun restoreFilterState(
        filters: TransactionFilter
    ) {
        selectedCategory =
            filters.category
                ?.trim()
                ?.takeIf(String::isNotEmpty)

        selectedStartDateKey =
            filters.startDateKey
                ?.takeIf(
                    TransactionDateUtils::isValidDateKey
                )

        selectedEndDateKey =
            filters.endDateKey
                ?.takeIf(
                    TransactionDateUtils::isValidDateKey
                )

        binding.rgFilterType.check(
            when (filters.type) {
                TransactionType.EXPENSE ->
                    R.id.rbFilterExpense

                TransactionType.INCOME ->
                    R.id.rbFilterIncome

                null ->
                    R.id.rbFilterAll
            }
        )

        val minStr = filters.minAmount?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() }.orEmpty()
        val maxStr = filters.maxAmount?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() }.orEmpty()
        binding.etFilterMinAmount.setText(minStr)
        binding.etFilterMaxAmount.setText(maxStr)

        when {
            filters.minAmount == null && filters.maxAmount == null ->
                binding.chipGroupAmountPresets.check(R.id.chipAmountAll)
            filters.minAmount == null && filters.maxAmount == 500.0 ->
                binding.chipGroupAmountPresets.check(R.id.chipAmount0To500)
            filters.minAmount == 500.0 && filters.maxAmount == 2500.0 ->
                binding.chipGroupAmountPresets.check(R.id.chipAmount500To2500)
            filters.minAmount == 2500.0 && filters.maxAmount == 10000.0 ->
                binding.chipGroupAmountPresets.check(R.id.chipAmount2500To10000)
            filters.minAmount == 10000.0 && filters.maxAmount == null ->
                binding.chipGroupAmountPresets.check(R.id.chipAmount10000Plus)
            else ->
                binding.chipGroupAmountPresets.clearCheck()
        }

        updateDateButtonLabels()
    }

    private fun configureCategoryInput() {
        binding.etFilterCategory.apply {
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

            setOnItemClickListener {
                    _,
                    _,
                    position,
                    _ ->

                selectedCategory =
                    categoryOptions
                        .getOrNull(position)
                        ?.takeUnless {
                            it == ALL_CATEGORIES_LABEL
                        }
            }
        }

        updateCategoryOptions(
            transactionViewModel
                .availableCategories
                .value
        )
    }

    private fun observeCategories() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {
                transactionViewModel
                    .availableCategories
                    .collectLatest { categories ->
                        updateCategoryOptions(categories)
                    }
            }
        }
    }

    private fun updateCategoryOptions(
        categories: List<String>
    ) {
        val baseCategories = CategoryUiUtils.POPULAR_EXPENSE_CATEGORIES.toMutableList()
        
        val extraCategories = categories
            .asSequence()
            .map(String::trim)
            .filter(String::isNotEmpty)
            .filterNot { cat ->
                cat.equals(ALL_CATEGORIES_LABEL, ignoreCase = true) ||
                cat.equals("Gelir", ignoreCase = true) ||
                cat.equals("Birikim", ignoreCase = true) ||
                baseCategories.any { it.equals(cat, ignoreCase = true) }
            }
            .distinct()
            .toList()

        if (extraCategories.isNotEmpty()) {
            val digerIdx = baseCategories.indexOfFirst { it.equals("Diğer", ignoreCase = true) }
            if (digerIdx != -1) {
                baseCategories.addAll(digerIdx, extraCategories)
            } else {
                baseCategories.addAll(extraCategories)
            }
        }

        categoryOptions = buildList {
            add(ALL_CATEGORIES_LABEL)
            addAll(baseCategories)
        }

        binding.etFilterCategory.setAdapter(
            CategoryUiUtils.createCategoryDropdownAdapter(
                requireContext(),
                categoryOptions
            )
        )

        binding.etFilterCategory.setText(
            selectedCategory
                ?: ALL_CATEGORIES_LABEL,
            false
        )
    }

    private fun configureDateButtons() {
        binding.btnFilterStartDate.setOnClickListener {
            showDatePicker(
                title = "Başlangıç tarihi",
                currentDateKey =
                    selectedStartDateKey
                        ?: selectedEndDateKey
            ) { selectedDateKey ->
                selectedStartDateKey = selectedDateKey
                updateDateButtonLabels()
            }
        }

        binding.btnFilterEndDate.setOnClickListener {
            showDatePicker(
                title = "Bitiş tarihi",
                currentDateKey =
                    selectedEndDateKey
                        ?: selectedStartDateKey
            ) { selectedDateKey ->
                selectedEndDateKey = selectedDateKey
                updateDateButtonLabels()
            }
        }
    }

    private fun showDatePicker(
        title: String,
        currentDateKey: Int?,
        onDateSelected: (Int) -> Unit
    ) {
        val initialDate =
            currentDateKey
                ?.takeIf(
                    TransactionDateUtils::isValidDateKey
                )
                ?.toCalendar()
                ?: Calendar.getInstance()

        DatePickerDialog(
            requireContext(),
            {
                    _,
                    year,
                    month,
                    dayOfMonth ->

                val selectedDate =
                    GregorianCalendar().apply {
                        isLenient = false
                        clear()
                        set(
                            year,
                            month,
                            dayOfMonth
                        )
                    }

                onDateSelected(
                    TransactionDateUtils.currentDateKey(
                        selectedDate
                    )
                )
            },
            initialDate.get(Calendar.YEAR),
            initialDate.get(Calendar.MONTH),
            initialDate.get(Calendar.DAY_OF_MONTH)
        ).apply {
            setTitle(title)
        }.show()
    }

    private fun updateDateButtonLabels() {
        binding.btnFilterStartDate.text =
            selectedStartDateKey
                ?.toDisplayDate()
                ?: "Başlangıç"

        binding.btnFilterEndDate.text =
            selectedEndDateKey
                ?.toDisplayDate()
                ?: "Bitiş"
    }

    private fun configureActions() {
        binding.btnApplyTransactionFilters
            .setOnClickListener {
                applyFilters()
                dismiss()
            }

        binding.btnClearTransactionFilters
            .setOnClickListener {
                transactionViewModel.clearFilters()
                dismiss()
            }
    }

    private fun applyFilters() {
        val category =
            binding.etFilterCategory.text
                ?.toString()
                ?.trim()
                ?.takeIf(String::isNotEmpty)
                ?.takeUnless {
                    it == ALL_CATEGORIES_LABEL
                }
                ?: selectedCategory

        transactionViewModel.setCategoryFilter(
            category
        )

        transactionViewModel.setTypeFilter(
            when (
                binding.rgFilterType
                    .checkedRadioButtonId
            ) {
                R.id.rbFilterExpense ->
                    TransactionType.EXPENSE

                R.id.rbFilterIncome ->
                    TransactionType.INCOME

                else ->
                    null
            }
        )

        transactionViewModel.setDateRange(
            startDateKey = selectedStartDateKey,
            endDateKey = selectedEndDateKey
        )

        val minAmount = com.epatay.digitalwallet.util.parseMoneyValue(binding.etFilterMinAmount.text?.toString())
        val maxAmount = com.epatay.digitalwallet.util.parseMoneyValue(binding.etFilterMaxAmount.text?.toString())
        transactionViewModel.setAmountRange(minAmount, maxAmount)
    }

    private fun Int.toDisplayDate(): String {
        return String.format(
            Locale.forLanguageTag("tr-TR"),
            "%02d.%02d.%04d",
            this % 100,
            this / 100 % 100,
            this / 10_000
        )
    }

    private fun Int.toCalendar(): Calendar {
        return GregorianCalendar(
            this / 10_000,
            this / 100 % 100 - 1,
            this % 100
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private companion object {
        const val ALL_CATEGORIES_LABEL =
            "Tüm kategoriler"
    }
}
