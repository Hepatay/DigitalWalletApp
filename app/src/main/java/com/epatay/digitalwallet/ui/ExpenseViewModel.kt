package com.epatay.digitalwallet.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.epatay.digitalwallet.data.AppDatabase
import com.epatay.digitalwallet.data.Expense
import com.epatay.digitalwallet.data.ExpenseRepository
import kotlinx.coroutines.launch

class ExpenseViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ExpenseRepository
    val allExpenses: LiveData<List<Expense>>

    init {
        // Veritabanını ve DAO'yu başlatıp Repository'ye veriyoruz
        val expenseDao = AppDatabase.getDatabase(application).expenseDao()
        repository = ExpenseRepository(expenseDao)
        allExpenses = repository.allExpenses
    }

    // Arayüzden çağrılacak ekleme fonksiyonu (Arka planda çalışması için viewModelScope kullanıyoruz)
    fun insert(expense: Expense) = viewModelScope.launch {
        repository.insert(expense)
    }
}