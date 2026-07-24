package com.epatay.digitalwallet.recurring

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

object RecurringTransactionScheduler {

    private const val PERIODIC_WORK_NAME =
        "recurring_transaction_daily_check"
    private const val IMMEDIATE_WORK_NAME =
        "recurring_transaction_immediate_check"

    fun schedule(
        context: Context
    ) {
        val applicationContext = context.applicationContext

        RecurringNotificationHelper.createChannel(
            applicationContext
        )

        val periodicRequest =
            PeriodicWorkRequestBuilder<RecurringTransactionWorker>(
                1,
                TimeUnit.DAYS
            )
                .setInitialDelay(
                    delayUntilNextMorning(),
                    TimeUnit.MILLISECONDS
                )
                .build()

        WorkManager.getInstance(applicationContext)
            .enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                periodicRequest
            )
    }

    fun runNow(
        context: Context
    ) {
        val request =
            OneTimeWorkRequestBuilder<RecurringTransactionWorker>()
                .build()

        WorkManager.getInstance(context.applicationContext)
            .enqueueUniqueWork(
                IMMEDIATE_WORK_NAME,
                ExistingWorkPolicy.KEEP,
                request
            )
    }

    private fun delayUntilNextMorning(): Long {
        val now = Calendar.getInstance()
        val nextRun =
            (now.clone() as Calendar).apply {
                set(Calendar.HOUR_OF_DAY, 9)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)

                if (!after(now)) {
                    add(Calendar.DAY_OF_MONTH, 1)
                }
            }

        return (nextRun.timeInMillis - now.timeInMillis)
            .coerceAtLeast(0L)
    }
}
