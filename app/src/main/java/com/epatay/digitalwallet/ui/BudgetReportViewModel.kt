package com.epatay.digitalwallet.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.epatay.digitalwallet.data.CategoryBudget
import com.epatay.digitalwallet.data.CategoryBudgetRepository
import com.epatay.digitalwallet.data.CategoryTransactionTotal
import com.epatay.digitalwallet.data.MonthlyTransactionTotals
import com.epatay.digitalwallet.data.TransactionDatabase
import com.epatay.digitalwallet.data.TransactionDateUtils
import com.epatay.digitalwallet.data.TransactionRepository
import com.epatay.digitalwallet.data.TransactionType
import com.epatay.digitalwallet.data.DecimalMath
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

data class CategoryBudgetProgress(
    val category: String,
    val spentAmount: Double,
    val transactionCount: Int,
    val limitAmount: Double?,
    val remainingAmount: Double,
    val exceededAmount: Double,
    val usagePercent: Int,
    val progressBarPercent: Int
) {
    val hasBudget: Boolean
        get() = limitAmount != null

    val isExceeded: Boolean
        get() = exceededAmount > 0.0
}

fun buildCategoryBudgetProgress(
    categoryTotals: List<CategoryTransactionTotal>,
    budgets: List<CategoryBudget>
): List<CategoryBudgetProgress> {
    val totalsByCategory =
        categoryTotals.associateBy(
            CategoryTransactionTotal::category
        )
    val budgetsByCategory =
        budgets.associateBy(CategoryBudget::category)

    return (
        totalsByCategory.keys +
            budgetsByCategory.keys
        )
        .distinct()
        .map { category ->
            val total = totalsByCategory[category]
            val budget = budgetsByCategory[category]
            val spentAmount =
                DecimalMath.normalizeMoney(
                    total?.totalAmount ?: 0.0
                ) ?: 0.0
            val limitAmount =
                budget?.limitAmount
                    ?.let(DecimalMath::normalizeMoney)
            val rawRemaining =
                if (limitAmount != null) {
                    DecimalMath.subtractMoney(
                        limitAmount,
                        spentAmount
                    ) ?: 0.0
                } else {
                    0.0
                }
            val usagePercent =
                if (
                    limitAmount != null &&
                    limitAmount > 0.0
                ) {
                    (
                        spentAmount /
                            limitAmount *
                            100.0
                        )
                        .roundToInt()
                        .coerceAtLeast(0)
                } else {
                    0
                }

            CategoryBudgetProgress(
                category = category,
                spentAmount = spentAmount,
                transactionCount =
                    total?.transactionCount ?: 0,
                limitAmount = limitAmount,
                remainingAmount =
                    rawRemaining.coerceAtLeast(0.0),
                exceededAmount =
                    (-rawRemaining).coerceAtLeast(0.0),
                usagePercent = usagePercent,
                progressBarPercent =
                    usagePercent.coerceIn(0, 100)
            )
        }
        .sortedWith(
            compareByDescending<CategoryBudgetProgress> {
                it.spentAmount
            }.thenBy {
                it.category
            }
        )
}

@OptIn(ExperimentalCoroutinesApi::class)
class BudgetReportViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val transactionRepository:
        TransactionRepository
    private val categoryBudgetRepository:
        CategoryBudgetRepository

    private val _selectedMonthKey =
        MutableStateFlow(
            TransactionDateUtils.currentMonthKey()
        )

    val selectedMonthKey: StateFlow<Int> =
        _selectedMonthKey.asStateFlow()

    val monthlyTotals:
        StateFlow<MonthlyTransactionTotals>
    val categoryBudgetProgress:
        StateFlow<List<CategoryBudgetProgress>>
    val availableCategories:
        StateFlow<List<String>>

    init {
        val database =
            TransactionDatabase.getDatabase(application)

        transactionRepository =
            TransactionRepository(
                database.transactionDao()
            )
        categoryBudgetRepository =
            CategoryBudgetRepository(
                database.categoryBudgetDao()
            )

        monthlyTotals =
            selectedMonthKey
                .flatMapLatest(
                    transactionRepository::observeMonthlyTotals
                )
                .stateIn(
                    scope = viewModelScope,
                    started =
                        SharingStarted.WhileSubscribed(5_000),
                    initialValue =
                        MonthlyTransactionTotals(
                            totalIncome = 0.0,
                            totalExpense = 0.0,
                            transactionCount = 0
                        )
                )

        categoryBudgetProgress =
            selectedMonthKey
                .flatMapLatest { monthKey ->
                    combine(
                        transactionRepository
                            .observeCategoryTotals(
                                monthKey = monthKey,
                                type =
                                    TransactionType.EXPENSE
                            ),
                        categoryBudgetRepository
                            .observeForMonth(monthKey)
                    ) { categoryTotals, budgets ->
                        buildCategoryBudgetProgress(
                            categoryTotals = categoryTotals,
                            budgets = budgets
                        )
                    }
                }
                .stateIn(
                    scope = viewModelScope,
                    started =
                        SharingStarted.WhileSubscribed(5_000),
                    initialValue = emptyList()
                )

        availableCategories =
            transactionRepository
                .observeCategories()
                .stateIn(
                    scope = viewModelScope,
                    started =
                        SharingStarted.WhileSubscribed(5_000),
                    initialValue = emptyList()
                )
    }

    fun selectMonth(
        monthKey: Int
    ) {
        require(
            TransactionDateUtils.isValidMonthKey(monthKey)
        ) {
            "Geçersiz ay anahtarı: $monthKey"
        }

        _selectedMonthKey.value = monthKey
    }

    fun upsertBudget(
        category: String,
        limitAmount: Double
    ) = viewModelScope.launch {
        categoryBudgetRepository.upsert(
            CategoryBudget(
                monthKey = selectedMonthKey.value,
                category = category,
                limitAmount = limitAmount,
                updatedAtMillis =
                    System.currentTimeMillis(),
                user_id = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            )
        )
        com.epatay.digitalwallet.sync.FirebaseSyncWorker.trigger(getApplication())
    }

    fun deleteBudget(
        category: String
    ) = viewModelScope.launch {
        categoryBudgetRepository.delete(
            monthKey = selectedMonthKey.value,
            category = category
        )
        com.epatay.digitalwallet.sync.FirebaseSyncWorker.trigger(getApplication())
    }
}
