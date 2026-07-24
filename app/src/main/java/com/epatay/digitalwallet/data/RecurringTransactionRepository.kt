package com.epatay.digitalwallet.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow

data class RecurringUpdateResult(
    val shouldCancelReminder: Boolean
)

internal fun shouldCancelReminderAfterUpdate(
    previous: RecurringTransaction,
    updated: RecurringTransaction
): Boolean {
    return previous.dayOfMonth != updated.dayOfMonth ||
        (
            previous.notificationEnabled &&
                !updated.notificationEnabled
            ) ||
        (
            previous.isActive &&
                !updated.isActive
            )
}

class RecurringTransactionRepository(
    private val database: TransactionDatabase
) {

    private val recurringTransactionDao =
        database.recurringTransactionDao()
    private val recurringOccurrenceDao =
        database.recurringOccurrenceDao()

    val allRecurringTransactions:
        Flow<List<RecurringTransaction>> =
        recurringTransactionDao
            .getAllRecurringTransactions()

    suspend fun insert(
        recurringTransaction: RecurringTransaction
    ): Long {
        return database.withTransaction {
            val recurringId =
                recurringTransactionDao.insert(
                    recurringTransaction
                )

            recurringTransaction.lastGeneratedPeriod
                ?.let { periodKey ->
                    recurringOccurrenceDao.insert(
                        RecurringOccurrence(
                            recurringId =
                                recurringId.toInt(),
                            periodKey = periodKey,
                            createdAtMillis =
                                System.currentTimeMillis()
                        )
                    )
                }

            recurringId
        }
    }

    suspend fun update(
        recurringTransaction: RecurringTransaction
    ): RecurringUpdateResult {
        return database.withTransaction {
            val previous =
                recurringTransactionDao.getById(
                    recurringTransaction.id
                )

            if (previous == null) {
                return@withTransaction RecurringUpdateResult(
                    shouldCancelReminder = true
                )
            }

            recurringTransactionDao.updateEditableFields(
                id = recurringTransaction.id,
                title = recurringTransaction.title,
                amount = recurringTransaction.amount,
                category = recurringTransaction.category,
                type = recurringTransaction.type,
                dayOfMonth =
                    recurringTransaction.dayOfMonth,
                autoCreate =
                    recurringTransaction.autoCreate,
                notificationEnabled =
                    recurringTransaction
                        .notificationEnabled,
                isActive = recurringTransaction.isActive
            )

            recurringTransaction.lastGeneratedPeriod
                ?.let { periodKey ->
                    recurringOccurrenceDao.insert(
                        RecurringOccurrence(
                            recurringId =
                                recurringTransaction.id,
                            periodKey = periodKey,
                            createdAtMillis =
                                System.currentTimeMillis()
                        )
                    )
                }

            RecurringUpdateResult(
                shouldCancelReminder =
                    shouldCancelReminderAfterUpdate(
                        previous = previous,
                        updated = recurringTransaction
                    )
            )
        }
    }

    suspend fun delete(
        recurringTransaction: RecurringTransaction
    ) {
        recurringTransactionDao.delete(
            recurringTransaction
        )
    }
}
