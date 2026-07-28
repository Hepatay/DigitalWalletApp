package com.epatay.digitalwallet.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.epatay.digitalwallet.data.Transaction
import com.epatay.digitalwallet.data.TransactionDatabase
import com.epatay.digitalwallet.data.TransactionDateUtils
import com.epatay.digitalwallet.data.DecimalMath
import com.epatay.digitalwallet.data.TransactionFilter
import com.epatay.digitalwallet.data.TransactionRepository
import com.epatay.digitalwallet.data.TransactionType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
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
    return DecimalMath.sumMoney(
        transactions
        .asSequence()
        .filter { transaction ->
            transaction.type ==
                TransactionType.EXPENSE &&
                TransactionDateUtils.monthKeyFromDateKey(
                    transaction.occurredOn
                ) == monthKey
        }
        .map(Transaction::amount)
        .asIterable()
    )
}

internal fun calculateMonthlyBudgetSummary(
    monthlyLimit: Double,
    transactions: List<Transaction>,
    calendar: Calendar
): MonthlyBudgetSummary {
    val normalizedMonthlyLimit =
        DecimalMath.normalizeMoney(monthlyLimit)
            ?.coerceAtLeast(0.0)
            ?: 0.0
    val currentMonthExpense =
        calculateMonthlyExpense(
            transactions = transactions,
            monthKey =
                TransactionDateUtils.currentMonthKey(
                    calendar
                )
        )

    val rawRemainingLimit =
        DecimalMath.subtractMoney(
            normalizedMonthlyLimit,
            currentMonthExpense
        ) ?: 0.0
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
            java.math.BigDecimal
                .valueOf(remainingLimit)
                .divide(
                    java.math.BigDecimal.valueOf(
                        budgetingDayCount.toLong()
                    ),
                    2,
                    java.math.RoundingMode.HALF_UP
                )
                .toDouble()
        } else {
            0.0
        }

    val usagePercent =
        if (normalizedMonthlyLimit > 0.0) {
            (
                currentMonthExpense /
                    normalizedMonthlyLimit *
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
        monthlyLimit = normalizedMonthlyLimit,
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

        totalIncome = allTransactions.map { transactions ->
            DecimalMath.sumMoney(
                transactions
                    .asSequence()
                    .filter { it.type == TransactionType.INCOME }
                    .map(Transaction::amount)
                    .asIterable()
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0.0
        )

        totalExpense = allTransactions.map { transactions ->
            DecimalMath.sumMoney(
                transactions
                    .asSequence()
                    .filter { it.type == TransactionType.EXPENSE }
                    .map(Transaction::amount)
                    .asIterable()
            )
        }.stateIn(
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
        val normalized =
            DecimalMath.normalizeMoney(limit)
                ?.takeIf { it > 0.0 }
                ?: return

        val prefs = context.getSharedPreferences(
            "wallet_prefs",
            Context.MODE_PRIVATE
        )

        prefs.edit()
            .putString(
                MONTHLY_LIMIT_DECIMAL_KEY,
                java.math.BigDecimal
                    .valueOf(normalized)
                    .toPlainString()
            )
            .apply()
    }

    // Aylık limiti okur
    fun getMonthlyLimit(context: Context): Double {
        val prefs = context.getSharedPreferences(
            "wallet_prefs",
            Context.MODE_PRIVATE
        )

        val decimalLimit =
            prefs.getString(
                MONTHLY_LIMIT_DECIMAL_KEY,
                null
            )
                ?.toBigDecimalOrNull()
                ?.takeIf { it > java.math.BigDecimal.ZERO }
                ?.toDouble()

        if (decimalLimit != null && decimalLimit.isFinite()) {
            return decimalLimit
        }

        val legacyLimit =
            prefs.getFloat(
                LEGACY_MONTHLY_LIMIT_KEY,
                DEFAULT_MONTHLY_LIMIT.toFloat()
            ).toDouble()

        return DecimalMath.normalizeMoney(legacyLimit)
            ?.takeIf { it > 0.0 }
            ?: DEFAULT_MONTHLY_LIMIT
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

private const val MONTHLY_LIMIT_DECIMAL_KEY =
    "monthly_limit_decimal"
private const val LEGACY_MONTHLY_LIMIT_KEY =
    "monthly_limit"
private const val DEFAULT_MONTHLY_LIMIT =
    50_000.0
