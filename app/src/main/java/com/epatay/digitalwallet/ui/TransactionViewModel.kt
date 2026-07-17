package com.epatay.digitalwallet.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.epatay.digitalwallet.data.Transaction
import com.epatay.digitalwallet.data.TransactionDatabase
import com.epatay.digitalwallet.data.TransactionRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TransactionViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: TransactionRepository

    // UI'da (ekranda) göstereceğimiz veriler
    val allTransactions: StateFlow<List<Transaction>>
    val totalIncome: StateFlow<Double?>
    val totalExpense: StateFlow<Double?>

    // Döviz kurlarını tuttuğumuz değişkenler (CurrencyFragment buradan okuyup yazıyordu)
    val dolarKuru = MutableLiveData(1.0)
    val euroKuru = MutableLiveData(1.0)
    val sterlinKuru = MutableLiveData(1.0)

    init {
        // Yeni veritabanımızı ve Repository'mizi başlatıyoruz
        val transactionDao = TransactionDatabase.getDatabase(application).transactionDao()
        repository = TransactionRepository(transactionDao)

        // Veritabanından gelen anlık verileri ekrana (UI) bağlamak için StateFlow kullanıyoruz
        allTransactions = repository.allTransactions.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        totalIncome = repository.totalIncome.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            0.0
        )

        totalExpense = repository.totalExpense.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            0.0
        )
    }

    fun insert(transaction: Transaction) = viewModelScope.launch {
        repository.insert(transaction)
    }

    fun delete(transaction: Transaction) = viewModelScope.launch {
        repository.delete(transaction)
    }
}