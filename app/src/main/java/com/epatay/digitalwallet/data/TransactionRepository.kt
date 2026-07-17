package com.epatay.digitalwallet.data

import kotlinx.coroutines.flow.Flow

class TransactionRepository(private val transactionDao: TransactionDao) {

    // Tüm işlemleri arayüze (ViewModel'e) aktarmak için köprü
    val allTransactions: Flow<List<Transaction>> = transactionDao.getAllTransactions()

    // Toplam gelir ve gider verilerini aktarmak için köprüler
    val totalIncome: Flow<Double?> = transactionDao.getTotalIncome()
    val totalExpense: Flow<Double?> = transactionDao.getTotalExpense()

    // Yeni işlem ekleme fonksiyonu
    suspend fun insert(transaction: Transaction) {
        transactionDao.insertTransaction(transaction)
    }

    // İşlem silme fonksiyonu
    suspend fun delete(transaction: Transaction) {
        transactionDao.deleteTransaction(transaction)
    }
}