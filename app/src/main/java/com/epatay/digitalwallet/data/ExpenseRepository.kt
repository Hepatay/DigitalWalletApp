package com.epatay.digitalwallet.data

class ExpenseRepository(private val expenseDao: ExpenseDao) {

    // Tüm harcamaları DAO'dan alıyoruz
    val allExpenses = expenseDao.getAllExpenses()

    // Yeni harcama ekleme işlemini arka planda (suspend) yapıyoruz
    suspend fun insert(expense: Expense) {
        expenseDao.insertExpense(expense)
    }
}