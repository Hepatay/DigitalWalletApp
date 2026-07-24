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
        val normalizedCategories =
            categories
                .asSequence()
                .map(String::trim)
                .filter(String::isNotEmpty)
                .filterNot { category ->
                    category.equals(
                        ALL_CATEGORIES_LABEL,
                        ignoreCase = true
                    )
                }
                .distinct()
                .toMutableList()

        selectedCategory
            ?.takeIf { selected ->
                normalizedCategories.none { category ->
                    category.equals(
                        selected,
                        ignoreCase = true
                    )
                }
            }
            ?.let(normalizedCategories::add)

        categoryOptions =
            buildList {
                add(ALL_CATEGORIES_LABEL)
                addAll(
                    normalizedCategories.sortedWith(
                        String.CASE_INSENSITIVE_ORDER
                    )
                )
            }

        binding.etFilterCategory.setAdapter(
            ArrayAdapter(
                requireContext(),
                android.R.layout
                    .simple_dropdown_item_1line,
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
