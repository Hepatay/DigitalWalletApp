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
        val normalized =
            recurringTransaction.normalizedForStorage()
                ?: return INVALID_INSERT_ID

        return database.withTransaction {
            val recurringId =
                recurringTransactionDao.insert(
                    normalized
                )

            normalized.lastGeneratedPeriod
                ?.let { periodKey ->
                    recurringOccurrenceDao.insert(
                        RecurringOccurrence(
                            recurringId =
                                normalized.uuid,
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
        val normalized =
            recurringTransaction.normalizedForStorage()
                ?: return RecurringUpdateResult(
                    shouldCancelReminder = false
                )

        return database.withTransaction {
            val previous =
                recurringTransactionDao.getById(
                    normalized.uuid
                )

            if (previous == null) {
                return@withTransaction RecurringUpdateResult(
                    shouldCancelReminder = true
                )
            }

            recurringTransactionDao.updateEditableFields(
                id = normalized.uuid,
                title = normalized.title,
                amount = normalized.amount,
                category = normalized.category,
                type = normalized.type,
                dayOfMonth =
                    normalized.dayOfMonth,
                autoCreate =
                    normalized.autoCreate,
                notificationEnabled =
                    normalized
                        .notificationEnabled,
                isActive = normalized.isActive
            )

            normalized.lastGeneratedPeriod
                ?.let { periodKey ->
                    recurringOccurrenceDao.insert(
                        RecurringOccurrence(
                            recurringId =
                                normalized.uuid,
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
                        updated = normalized
                    )
            )
        }
    }

    suspend fun delete(
        recurringTransaction: RecurringTransaction
    ) {
        recurringTransactionDao.update(
            recurringTransaction.copy(is_deleted = true, is_synced = false, updated_at = System.currentTimeMillis())
        )
    }

    private fun RecurringTransaction.normalizedForStorage():
        RecurringTransaction? {
        val normalizedAmount =
            DecimalMath.normalizeMoney(amount)
                ?.takeIf { it > 0.0 }
                ?: return null
        val normalizedTitle = title.trim()
        val normalizedCategory =
            if (type == TransactionType.INCOME) {
                "Gelir"
            } else {
                category.trim()
            }

        if (
            normalizedTitle.isEmpty() ||
            normalizedCategory.isEmpty() ||
            dayOfMonth !in 1..31
        ) {
            return null
        }

        return copy(
            title = normalizedTitle,
            amount = normalizedAmount,
            category = normalizedCategory,
            user_id = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: user_id
        )
    }

    private companion object {
        const val INVALID_INSERT_ID = -1L
    }
}
