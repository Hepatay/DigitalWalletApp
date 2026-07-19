package com.epatay.digitalwallet.data

import kotlinx.coroutines.flow.Flow

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
            transaction
        )
    }

    suspend fun update(
        transaction: Transaction
    ) {
        transactionDao.updateTransaction(
            transaction
        )
    }

    suspend fun delete(
        transaction: Transaction
    ) {
        transactionDao.deleteTransaction(
            transaction
        )
    }
}