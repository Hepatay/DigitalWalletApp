package com.epatay.digitalwallet.recurring

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.epatay.digitalwallet.data.RecurringOccurrence
import com.epatay.digitalwallet.data.RecurringTransaction
import com.epatay.digitalwallet.data.Transaction
import com.epatay.digitalwallet.data.TransactionDatabase
import kotlinx.coroutines.CancellationException
import java.util.Calendar

class RecurringTransactionWorker(
    appContext: Context,
    workerParameters: WorkerParameters
) : CoroutineWorker(
    appContext,
    workerParameters
) {

    override suspend fun doWork(): Result {
        return try {
            reconcile(Calendar.getInstance())
            Result.success()
        } catch (cancellationException: CancellationException) {
            throw cancellationException
        } catch (exception: Exception) {
            Log.e(
                TAG,
                "Düzenli kayıtlar eşitlenemedi.",
                exception
            )
            Result.retry()
        }
    }

    private suspend fun reconcile(
        calendar: Calendar
    ) {
        val database =
            TransactionDatabase.getDatabase(applicationContext)
        val recurringDao =
            database.recurringTransactionDao()

        val activeTransactions =
            recurringDao.getActiveRecurringTransactions()

        activeTransactions.forEach { recurringTransaction ->
            generateTransactionIfNeeded(
                database,
                recurringTransaction,
                calendar
            )

            notifyIfNeeded(
                database,
                recurringTransaction,
                calendar
            )
        }
    }

    private suspend fun generateTransactionIfNeeded(
        database: TransactionDatabase,
        recurringTransaction: RecurringTransaction,
        calendar: Calendar
    ) {
        database.withTransaction {
            val recurringDao =
                database.recurringTransactionDao()
            val occurrenceDao =
                database.recurringOccurrenceDao()

            val current =
                recurringDao.getById(recurringTransaction.uuid)
                    ?: return@withTransaction

            val periodKey =
                RecurringDateUtils.currentPeriod(calendar)
            val occurrenceExists =
                occurrenceDao.exists(
                    recurringId = current.uuid,
                    periodKey = periodKey
                )

            if (
                !RecurringDateUtils.shouldCreateOccurrence(
                    current,
                    calendar,
                    occurrenceExists
                )
            ) {
                return@withTransaction
            }

            val occurrenceClaim =
                occurrenceDao.insert(
                    RecurringOccurrence(
                        recurringId = current.uuid,
                        periodKey = periodKey,
                        createdAtMillis =
                            System.currentTimeMillis(),
                        user_id = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                    )
                )

            if (occurrenceClaim == INSERT_IGNORED) {
                return@withTransaction
            }

            database.transactionDao().insertTransaction(
                Transaction(
                    title = current.title,
                    amount = current.amount,
                    category = current.category,
                    date = RecurringDateUtils.transactionDate(
                        current.dayOfMonth,
                        calendar
                    ),
                    type = current.type,
                    user_id = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                )
            )

            recurringDao.updateLastGeneratedPeriod(
                id = current.uuid,
                periodKey = periodKey
            )
        }
    }

    private suspend fun notifyIfNeeded(
        database: TransactionDatabase,
        recurringTransaction: RecurringTransaction,
        calendar: Calendar
    ) {
        if (
            !RecurringNotificationHelper.canPostNotifications(
                applicationContext
            )
        ) {
            RecurringNotificationHelper.cancelReminder(
                applicationContext,
                recurringTransaction.uuid
            )
            return
        }

        val shouldCancel =
            database.withTransaction {
                val recurringDao =
                    database.recurringTransactionDao()

                val current =
                    recurringDao.getById(recurringTransaction.uuid)
                        ?: return@withTransaction true

                val dueDate =
                    RecurringDateUtils.nextDueDate(
                        current.dayOfMonth,
                        calendar
                    )

                val isInReminderWindow =
                    current.isActive &&
                        current.notificationEnabled &&
                        RecurringDateUtils.daysUntilNextDue(
                            current.dayOfMonth,
                            calendar
                        ) in
                        0..RecurringDateUtils.NOTIFICATION_LEAD_DAYS

                if (!isInReminderWindow) {
                    if (
                        current.lastNotifiedPeriod ==
                        RecurringDateUtils.currentPeriod(
                            dueDate
                        )
                    ) {
                        recurringDao.updateLastNotifiedPeriod(
                            id = current.uuid,
                            periodKey = null
                        )
                    }

                    return@withTransaction true
                }

                if (
                    current.lastNotifiedPeriod ==
                    RecurringDateUtils.currentPeriod(
                        dueDate
                    )
                ) {
                    return@withTransaction false
                }

                val notificationShown =
                    RecurringNotificationHelper.showDueReminder(
                        applicationContext,
                        current,
                        dueDate
                    )

                if (notificationShown) {
                    recurringDao.updateLastNotifiedPeriod(
                        id = current.uuid,
                        periodKey =
                            RecurringDateUtils.currentPeriod(
                                dueDate
                            )
                    )
                }

                false
            }

        if (shouldCancel) {
            RecurringNotificationHelper.cancelReminder(
                applicationContext,
                recurringTransaction.uuid
            )
        }
    }

    private companion object {
        const val TAG = "RecurringWorker"
        const val INSERT_IGNORED = -1L
    }
}
