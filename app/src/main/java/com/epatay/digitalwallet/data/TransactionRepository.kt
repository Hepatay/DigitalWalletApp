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

internal fun normalizeTransactionForStorage(
    transaction: Transaction
): Transaction? {
    val title = transaction.title.trim()
    val category =
        if (transaction.type == TransactionType.INCOME) {
            "Gelir"
        } else {
            transaction.category.trim()
        }
    val amount =
        DecimalMath.normalizeMoney(transaction.amount)
            ?.takeIf { it > 0.0 }

    if (
        title.isEmpty() ||
        category.isEmpty() ||
        amount == null
    ) {
        return null
    }

    val normalized = normalizeTransactionDate(
        transaction.copy(
            title = title,
            category = category,
            amount = amount
        )
    )

    if (
        normalized.id == 0 &&
        normalized.occurredOn ==
            TransactionDateUtils.UNKNOWN_DATE_KEY
    ) {
        return null
    }

    return normalized
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
        val normalized =
            normalizeTransactionForStorage(transaction)
                ?: return

        transactionDao.insertTransaction(normalized)
    }

    suspend fun update(
        transaction: Transaction
    ) {
        val normalized =
            normalizeTransactionForStorage(transaction)
                ?.takeIf { it.id > 0 }
                ?: return

        transactionDao.updateTransaction(normalized)
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

    suspend fun getAllSnapshot(): List<Transaction> {
        return transactionDao.getAllTransactionsSnapshot()
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
