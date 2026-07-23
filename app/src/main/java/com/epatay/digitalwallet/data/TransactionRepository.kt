package com.epatay.digitalwallet.data

import kotlinx.coroutines.flow.Flow

data class TransactionFilter(
    val query: String = "",
    val startDateKey: Int? = null,
    val endDateKey: Int? = null,
    val category: String? = null,
    val type: TransactionType? = null
)

internal fun normalizeTransactionDate(
    transaction: Transaction
): Transaction {
    return transaction.copy(
        occurredOn =
            TransactionDateUtils.toDateKey(
                transaction.date
            )
    )
}

class TransactionRepository(
    private val transactionDao: TransactionDao
) {

    val allTransactions:
            Flow<List<Transaction>> =
        transactionDao.getAllTransactions()

    val totalIncome:
            Flow<Double?> =
        transactionDao.getTotalIncome()

    val totalExpense:
            Flow<Double?> =
        transactionDao.getTotalExpense()

    suspend fun insert(
        transaction: Transaction
    ) {
        transactionDao.insertTransaction(
            normalizeTransactionDate(transaction)
        )
    }

    suspend fun update(
        transaction: Transaction
    ) {
        transactionDao.updateTransaction(
            normalizeTransactionDate(transaction)
        )
    }

    suspend fun delete(
        transaction: Transaction
    ) {
        transactionDao.deleteTransaction(
            transaction
        )
    }

    fun observeFiltered(
        filter: TransactionFilter
    ): Flow<List<Transaction>> {
        return transactionDao.observeFilteredTransactions(
            escapedQuery = escapeLike(filter.query.trim()),
            startDateKey = filter.startDateKey,
            endDateKey = filter.endDateKey,
            category =
                filter.category
                    ?.trim()
                    ?.takeIf(String::isNotEmpty),
            type = filter.type
        )
    }

    suspend fun getFilteredSnapshot(
        filter: TransactionFilter
    ): List<Transaction> {
        return transactionDao.getFilteredTransactionsSnapshot(
            escapedQuery = escapeLike(filter.query.trim()),
            startDateKey = filter.startDateKey,
            endDateKey = filter.endDateKey,
            category =
                filter.category
                    ?.trim()
                    ?.takeIf(String::isNotEmpty),
            type = filter.type
        )
    }

    fun observeMonthlyTotals(
        monthKey: Int
    ): Flow<MonthlyTransactionTotals> {
        return transactionDao.observeMonthlyTotals(
            startDateKey =
                TransactionDateUtils.monthStartDateKey(monthKey),
            endDateKey =
                TransactionDateUtils.monthEndDateKey(monthKey)
        )
    }

    fun observeCategoryTotals(
        monthKey: Int,
        type: TransactionType
    ): Flow<List<CategoryTransactionTotal>> {
        return transactionDao.observeCategoryTotals(
            startDateKey =
                TransactionDateUtils.monthStartDateKey(monthKey),
            endDateKey =
                TransactionDateUtils.monthEndDateKey(monthKey),
            type = type
        )
    }

    fun observeCategories(): Flow<List<String>> {
        return transactionDao.observeCategories()
    }

    fun observeUnknownDateCount(): Flow<Int> {
        return transactionDao.observeUnknownDateCount()
    }

    private fun escapeLike(
        query: String
    ): String {
        return buildString(query.length) {
            query.forEach { character ->
                when (character) {
                    '\\', '%', '_' -> append('\\')
                }
                append(character)
            }
        }
    }
}
