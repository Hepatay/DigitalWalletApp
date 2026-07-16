package com.epatay.digitalwallet.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData // Bu import şart!
import androidx.lifecycle.viewModelScope
import com.epatay.digitalwallet.data.AppDatabase
import com.epatay.digitalwallet.data.Expense
import com.epatay.digitalwallet.data.ExpenseRepository
import kotlinx.coroutines.launch

class ExpenseViewModel(application: Application) : AndroidViewModel(application) {

    // Buradaki değişkenleri PUBLIC olarak tanımlıyoruz
    val dolarKuru = MutableLiveData<Double>(0.0)
    val euroKuru = MutableLiveData<Double>(0.0)
    val sterlinKuru = MutableLiveData<Double>(0.0)

    private val repository: ExpenseRepository
    val allExpenses: LiveData<List<Expense>>

    init {
        val expenseDao = AppDatabase.getDatabase(application).expenseDao()
        repository = ExpenseRepository(expenseDao)
        allExpenses = repository.allExpenses
    }

    fun insert(expense: Expense) = viewModelScope.launch { repository.insert(expense) }
    fun delete(expense: Expense) = viewModelScope.launch { repository.delete(expense) }
    fun update(expense: Expense) = viewModelScope.launch { repository.update(expense) }
}