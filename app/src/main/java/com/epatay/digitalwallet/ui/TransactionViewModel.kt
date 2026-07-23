package com.epatay.digitalwallet.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.epatay.digitalwallet.data.Transaction
import com.epatay.digitalwallet.data.TransactionDatabase
import com.epatay.digitalwallet.data.TransactionDateUtils
import com.epatay.digitalwallet.data.TransactionFilter
import com.epatay.digitalwallet.data.TransactionRepository
import com.epatay.digitalwallet.data.TransactionType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.roundToInt

data class MonthlyBudgetSummary(
    val monthlyLimit: Double,
    val currentMonthExpense: Double,
    val remainingLimit: Double,
    val exceededAmount: Double,
    val dailySpendingLimit: Double,
    val daysUntilMonthEnd: Int,
    val usagePercent: Int,
    val progressPercent: Int
)

internal fun calculateMonthlyExpense(
    transactions: List<Transaction>,
    monthKey: Int
): Double {
    return transactions
        .asSequence()
        .filter { transaction ->
            transaction.type ==
                TransactionType.EXPENSE &&
                TransactionDateUtils.monthKeyFromDateKey(
                    transaction.occurredOn
                ) == monthKey
        }
        .sumOf(Transaction::amount)
}

internal fun calculateMonthlyBudgetSummary(
    monthlyLimit: Double,
    transactions: List<Transaction>,
    calendar: Calendar
): MonthlyBudgetSummary {
    val currentMonthExpense =
        calculateMonthlyExpense(
            transactions = transactions,
            monthKey =
                TransactionDateUtils.currentMonthKey(
                    calendar
                )
        )

    val rawRemainingLimit =
        monthlyLimit - currentMonthExpense
    val remainingLimit =
        rawRemainingLimit.coerceAtLeast(0.0)
    val exceededAmount =
        (-rawRemainingLimit).coerceAtLeast(0.0)

    val daysInMonth =
        calendar.getActualMaximum(
            Calendar.DAY_OF_MONTH
        )
    val currentDay =
        calendar.get(Calendar.DAY_OF_MONTH)
    val daysUntilMonthEnd =
        (daysInMonth - currentDay)
            .coerceAtLeast(0)
    val budgetingDayCount =
        (daysUntilMonthEnd + 1)
            .coerceAtLeast(1)
    val dailySpendingLimit =
        if (rawRemainingLimit > 0.0) {
            remainingLimit / budgetingDayCount
        } else {
            0.0
        }

    val usagePercent =
        if (monthlyLimit > 0.0) {
            (
                currentMonthExpense /
                    monthlyLimit *
                    100.0
                )
                .roundToInt()
                .coerceAtLeast(0)
        } else if (currentMonthExpense > 0.0) {
            100
        } else {
            0
        }

    return MonthlyBudgetSummary(
        monthlyLimit = monthlyLimit,
        currentMonthExpense = currentMonthExpense,
        remainingLimit = remainingLimit,
        exceededAmount = exceededAmount,
        dailySpendingLimit = dailySpendingLimit,
        daysUntilMonthEnd = daysUntilMonthEnd,
        usagePercent = usagePercent,
        progressPercent =
            usagePercent.coerceIn(0, 100)
    )
}

@OptIn(ExperimentalCoroutinesApi::class)
class TransactionViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository: TransactionRepository

    val allTransactions: StateFlow<List<Transaction>>
    val totalIncome: StateFlow<Double?>
    val totalExpense: StateFlow<Double?>
    val filteredTransactions: StateFlow<List<Transaction>>
    val availableCategories: StateFlow<List<String>>
    val unknownDateCount: StateFlow<Int>

    private val _filters =
        MutableStateFlow(TransactionFilter())

    val filters: StateFlow<TransactionFilter> =
        _filters.asStateFlow()

    // CurrencyFragment tarafından kullanılan kur değerleri
    val dolarKuru = MutableLiveData(1.0)
    val euroKuru = MutableLiveData(1.0)
    val sterlinKuru = MutableLiveData(1.0)

    init {
        val transactionDao =
            TransactionDatabase
                .getDatabase(application)
                .transactionDao()

        repository = TransactionRepository(transactionDao)

        allTransactions = repository.allTransactions.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        totalIncome = repository.totalIncome.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0.0
        )

        totalExpense = repository.totalExpense.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0.0
        )

        filteredTransactions =
            filters
                .flatMapLatest(repository::observeFiltered)
                .stateIn(
                    scope = viewModelScope,
                    started =
                        SharingStarted.WhileSubscribed(5000),
                    initialValue = emptyList()
                )

        availableCategories =
            repository.observeCategories().stateIn(
                scope = viewModelScope,
                started =
                    SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

        unknownDateCount =
            repository.observeUnknownDateCount().stateIn(
                scope = viewModelScope,
                started =
                    SharingStarted.WhileSubscribed(5000),
                initialValue = 0
            )
    }

    fun insert(transaction: Transaction) = viewModelScope.launch {
        repository.insert(transaction)
    }
    fun update(
        transaction: Transaction
    ) = viewModelScope.launch {

        repository.update(
            transaction
        )
    }

    fun delete(transaction: Transaction) = viewModelScope.launch {
        repository.delete(transaction)
    }

    fun setSearchQuery(
        query: String
    ) {
        _filters.value =
            _filters.value.copy(query = query)
    }

    fun setDateRange(
        startDateKey: Int?,
        endDateKey: Int?
    ) {
        require(
            startDateKey == null ||
                TransactionDateUtils.isValidDateKey(
                    startDateKey
                )
        ) {
            "Geçersiz başlangıç tarihi."
        }
        require(
            endDateKey == null ||
                TransactionDateUtils.isValidDateKey(
                    endDateKey
                )
        ) {
            "Geçersiz bitiş tarihi."
        }

        val normalizedRange =
            if (
                startDateKey != null &&
                endDateKey != null &&
                startDateKey > endDateKey
            ) {
                endDateKey to startDateKey
            } else {
                startDateKey to endDateKey
            }

        _filters.value =
            _filters.value.copy(
                startDateKey = normalizedRange.first,
                endDateKey = normalizedRange.second
            )
    }

    fun setCategoryFilter(
        category: String?
    ) {
        _filters.value =
            _filters.value.copy(
                category =
                    category
                        ?.trim()
                        ?.takeIf(String::isNotEmpty)
            )
    }

    fun setTypeFilter(
        type: TransactionType?
    ) {
        _filters.value =
            _filters.value.copy(type = type)
    }

    fun clearFilters() {
        _filters.value = TransactionFilter()
    }

    suspend fun getFilteredSnapshot():
        List<Transaction> {
        return repository.getFilteredSnapshot(
            filters.value
        )
    }

    /*
     * Eski limit kontrollerini bırakıyoruz.
     * Başka bir dosyada kullanılıyorsa hata oluşmaz.
     */
    fun checkLimit(
        yeniTutar: Double,
        limit: Double
    ): Boolean {
        return (totalExpense.value ?: 0.0) + yeniTutar > limit
    }

    fun isOverLimit(
        amount: Double,
        limit: Double
    ): Boolean {
        val currentTotal = totalExpense.value ?: 0.0
        return currentTotal + amount > limit
    }

    // Aylık limiti kaydeder
    fun saveMonthlyLimit(
        context: Context,
        limit: Double
    ) {
        val prefs = context.getSharedPreferences(
            "wallet_prefs",
            Context.MODE_PRIVATE
        )

        prefs.edit()
            .putFloat("monthly_limit", limit.toFloat())
            .apply()
    }

    // Aylık limiti okur
    fun getMonthlyLimit(context: Context): Double {
        val prefs = context.getSharedPreferences(
            "wallet_prefs",
            Context.MODE_PRIVATE
        )

        return prefs.getFloat(
            "monthly_limit",
            50000.0F
        ).toDouble()
    }

    /**
     * Yeni gider eklendiğinde yalnızca içinde bulunulan ayın
     * giderlerini dikkate alarak limit kontrolü yapar.
     */
    fun isOverMonthlyLimit(
        newExpenseAmount: Double,
        monthlyLimit: Double,
        transactions: List<Transaction>
    ): Boolean {

        val currentMonthExpense =
            calculateMonthlyExpense(
                transactions = transactions,
                monthKey =
                    TransactionDateUtils
                        .currentMonthKey()
            )

        return currentMonthExpense + newExpenseAmount > monthlyLimit
    }

    /**
     * Dashboard kartında ayrı görsel bileşenlerde gösterilecek
     * aylık bütçe özetini hazırlar.
     */
    fun getMonthlyBudgetSummary(
        context: Context,
        transactions: List<Transaction>
    ): MonthlyBudgetSummary {
        return getMonthlyBudgetSummary(
            context = context,
            transactions = transactions,
            calendar = Calendar.getInstance()
        )
    }

    fun getMonthlyBudgetSummary(
        context: Context,
        transactions: List<Transaction>,
        calendar: Calendar
    ): MonthlyBudgetSummary {
        return calculateMonthlyBudgetSummary(
            monthlyLimit = getMonthlyLimit(context),
            transactions = transactions,
            calendar = calendar
        )
    }

}
