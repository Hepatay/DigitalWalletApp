package com.epatay.digitalwallet.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.epatay.digitalwallet.data.TransactionDatabase
import com.epatay.digitalwallet.data.InvestmentItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class InvestmentViewModel(application: Application) : AndroidViewModel(application) {
    
    private val dao = TransactionDatabase.getDatabase(application).investmentDao()

    val allInvestments: LiveData<List<InvestmentItem>> = dao.getAllInvestments()

    fun insert(investment: InvestmentItem) = viewModelScope.launch(Dispatchers.IO) {
        dao.insertInvestment(investment)
    }

    fun delete(investment: InvestmentItem) = viewModelScope.launch(Dispatchers.IO) {
        dao.deleteInvestment(investment)
    }
}