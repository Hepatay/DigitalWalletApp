package com.epatay.digitalwallet.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.epatay.digitalwallet.data.Transaction
import com.epatay.digitalwallet.data.TransactionDatabase
import com.epatay.digitalwallet.data.TransactionRepository
import com.epatay.digitalwallet.data.TransactionType
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs

class TransactionViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository: TransactionRepository

    val allTransactions: StateFlow<List<Transaction>>
    val totalIncome: StateFlow<Double?>
    val totalExpense: StateFlow<Double?>

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
            calculateCurrentMonthExpense(transactions)

        return currentMonthExpense + newExpenseAmount > monthlyLimit
    }

    /**
     * Dashboard'daki çok satırlı bütçe bilgisini hazırlar.
     */
    fun getDailyBudgetInfo(
        context: Context,
        transactions: List<Transaction>
    ): String {

        val monthlyLimit = getMonthlyLimit(context)

        // Yalnızca bu ay yapılan giderlerin toplamı
        val currentMonthExpense =
            calculateCurrentMonthExpense(transactions)

        // Negatif olabilen gerçek kalan tutar
        val rawRemainingLimit =
            monthlyLimit - currentMonthExpense

        // Normal ekranda negatif kalan limit göstermiyoruz
        val remainingLimit =
            rawRemainingLimit.coerceAtLeast(0.0)

        val calendar = Calendar.getInstance()

        val daysInMonth = calendar.getActualMaximum(
            Calendar.DAY_OF_MONTH
        )

        val currentDay = calendar.get(
            Calendar.DAY_OF_MONTH
        )

        /*
         * Kullanıcıya gösterilecek ay sonuna kalan gün.
         * Bugün ayın 18'i ve ay 31 günse 13 gün gösterir.
         */
        val daysUntilMonthEnd =
            (daysInMonth - currentDay).coerceAtLeast(0)

        /*
         * Günlük bütçe hesabında bugün de kullanılabilir bir gün
         * olduğu için bugünü hesaplamaya dahil ediyoruz.
         */
        val budgetingDayCount =
            (daysUntilMonthEnd + 1).coerceAtLeast(1)

        val dailySpendingLimit =
            if (rawRemainingLimit > 0.0) {
                remainingLimit / budgetingDayCount
            } else {
                0.0
            }

        return if (rawRemainingLimit >= 0.0) {

            """
            Aylık limit: ${formatMoney(monthlyLimit)}
            Bu ayki gider: ${formatMoney(currentMonthExpense)}
            Kalan limit: ${formatMoney(remainingLimit)}
            Ay sonuna $daysUntilMonthEnd gün kaldı
            Günlük harcanabilir: ${formatMoney(dailySpendingLimit)}
            """.trimIndent()

        } else {

            val exceededAmount = abs(rawRemainingLimit)

            """
            Aylık limit: ${formatMoney(monthlyLimit)}
            Bu ayki gider: ${formatMoney(currentMonthExpense)}
            Limit ${formatMoney(exceededAmount)} aşıldı
            Ay sonuna $daysUntilMonthEnd gün kaldı
            Günlük harcanabilir: ${formatMoney(0.0)}
            """.trimIndent()
        }
    }

    /**
     * dd.MM.yyyy HH:mm biçimindeki tarihleri kullanarak
     * yalnızca içinde bulunduğumuz ayın giderlerini toplar.
     */
    private fun calculateCurrentMonthExpense(
        transactions: List<Transaction>
    ): Double {

        val calendar = Calendar.getInstance()

        val currentMonth = String.format(
            Locale.getDefault(),
            "%02d",
            calendar.get(Calendar.MONTH) + 1
        )

        val currentYear =
            calendar.get(Calendar.YEAR).toString()

        val currentMonthPattern =
            ".$currentMonth.$currentYear"

        return transactions
            .filter { transaction ->

                transaction.type == TransactionType.EXPENSE &&
                        transaction.date.contains(currentMonthPattern)
            }
            .sumOf { transaction ->
                transaction.amount
            }
    }

    /**
     * Türkçe para biçimi:
     * 50000 -> 50.000,00 ₺
     */
    private fun formatMoney(value: Double): String {
        return String.format(
            Locale.forLanguageTag("tr-TR"),
            "%,.2f ₺",
            value
        )
    }
}