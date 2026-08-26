package com.epatay.digitalwallet.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.epatay.digitalwallet.data.SavingsGoal
import com.epatay.digitalwallet.data.SavingsGoalEntry
import com.epatay.digitalwallet.data.SavingsGoalProgress
import com.epatay.digitalwallet.data.SavingsGoalRepository
import com.epatay.digitalwallet.data.TransactionDatabase
import com.epatay.digitalwallet.data.TransactionDateUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SavingsGoalViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository =
        SavingsGoalRepository(
            TransactionDatabase.getDatabase(application)
        )

    val goalsWithProgress:
        StateFlow<List<SavingsGoalProgress>> =
        repository.goalsWithProgress.stateIn(
            scope = viewModelScope,
            started =
                SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    fun observeEntries(
        goalId: String
    ): Flow<List<SavingsGoalEntry>> {
        return repository.observeEntries(goalId)
    }

    fun createGoal(
        title: String,
        targetAmount: Double,
        targetDateKey: Int? = null
    ) = viewModelScope.launch {
        repository.insertGoal(
            SavingsGoal(
                title = title,
                targetAmount = targetAmount,
                targetDateKey = targetDateKey,
                createdAtMillis =
                    System.currentTimeMillis(),
                user_id = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            )
        )
        com.epatay.digitalwallet.sync.FirebaseSyncWorker.trigger(getApplication())
    }

    fun updateGoal(
        goal: SavingsGoal
    ) = viewModelScope.launch {
        repository.updateGoal(goal)
        com.epatay.digitalwallet.sync.FirebaseSyncWorker.trigger(getApplication())
    }

    fun setArchived(
        goalId: String,
        isArchived: Boolean
    ) = viewModelScope.launch {
        repository.setArchived(
            goalId = goalId,
            isArchived = isArchived
        )
        com.epatay.digitalwallet.sync.FirebaseSyncWorker.trigger(getApplication())
    }

    fun deleteGoal(
        goal: SavingsGoal
    ) = viewModelScope.launch {
        repository.deleteGoal(goal)
        com.epatay.digitalwallet.sync.FirebaseSyncWorker.trigger(getApplication())
    }

    fun addEntry(
        goalId: String,
        amountDelta: Double,
        occurredOn: Int =
            TransactionDateUtils.currentDateKey(),
        note: String? = null
    ) = viewModelScope.launch {
        repository.addEntry(
            SavingsGoalEntry(
                goalId = goalId,
                amountDelta = amountDelta,
                occurredOn = occurredOn,
                note = note,
                createdAtMillis =
                    System.currentTimeMillis(),
                user_id = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
            )
        )
        com.epatay.digitalwallet.sync.FirebaseSyncWorker.trigger(getApplication())
    }

    fun deleteEntry(
        entry: SavingsGoalEntry
    ) = viewModelScope.launch {
        repository.deleteEntry(entry)
        com.epatay.digitalwallet.sync.FirebaseSyncWorker.trigger(getApplication())
    }
}
