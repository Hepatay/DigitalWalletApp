package com.epatay.digitalwallet.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.epatay.digitalwallet.data.RecurringTransaction
import com.epatay.digitalwallet.data.RecurringTransactionRepository
import com.epatay.digitalwallet.data.TransactionDatabase
import com.epatay.digitalwallet.recurring.RecurringNotificationHelper
import com.epatay.digitalwallet.recurring.RecurringTransactionScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RecurringTransactionViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository: RecurringTransactionRepository

    val allRecurringTransactions:
        StateFlow<List<RecurringTransaction>>

    init {
        val database =
            TransactionDatabase
                .getDatabase(application)

        repository =
            RecurringTransactionRepository(
                database
            )

        allRecurringTransactions =
            repository.allRecurringTransactions.stateIn(
                scope = viewModelScope,
                started =
                    SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )
    }

    fun insert(
        recurringTransaction: RecurringTransaction
    ) = viewModelScope.launch {
        repository.insert(recurringTransaction)
        runReconciliation()
    }

    fun update(
        recurringTransaction: RecurringTransaction
    ) = viewModelScope.launch {
        val result =
            repository.update(recurringTransaction)

        if (result.shouldCancelReminder) {
            RecurringNotificationHelper.cancelReminder(
                getApplication(),
                recurringTransaction.id
            )
        }

        runReconciliation()
    }

    fun delete(
        recurringTransaction: RecurringTransaction
    ) = viewModelScope.launch {
        RecurringNotificationHelper.cancelReminder(
            getApplication(),
            recurringTransaction.id
        )
        repository.delete(recurringTransaction)
        runReconciliation()
    }

    private fun runReconciliation() {
        RecurringTransactionScheduler.runNow(
            getApplication()
        )
    }
}
