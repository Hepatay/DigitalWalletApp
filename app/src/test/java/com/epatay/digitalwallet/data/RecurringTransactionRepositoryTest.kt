package com.epatay.digitalwallet.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecurringTransactionRepositoryTest {

    @Test
    fun unrelatedEdit_doesNotCancelExistingReminder() {
        val previous = recurring()
        val updated =
            previous.copy(
                title = "Güncel kira",
                amount = 15_000.0
            )

        assertFalse(
            shouldCancelReminderAfterUpdate(
                previous,
                updated
            )
        )
    }

    @Test
    fun dueDayOrDisabledNotification_cancelsExistingReminder() {
        val previous = recurring()

        assertTrue(
            shouldCancelReminderAfterUpdate(
                previous,
                previous.copy(dayOfMonth = 7)
            )
        )
        assertTrue(
            shouldCancelReminderAfterUpdate(
                previous,
                previous.copy(
                    notificationEnabled = false
                )
            )
        )
    }

    @Test
    fun enablingNotification_doesNotNeedStaleNotificationCancel() {
        val previous =
            recurring().copy(
                notificationEnabled = false
            )

        assertFalse(
            shouldCancelReminderAfterUpdate(
                previous,
                previous.copy(
                    notificationEnabled = true
                )
            )
        )
    }

    private fun recurring(): RecurringTransaction {
        return RecurringTransaction(
            id = 4,
            title = "Kira",
            amount = 12_500.0,
            category = "Konut",
            type = TransactionType.EXPENSE,
            dayOfMonth = 5,
            autoCreate = true,
            notificationEnabled = true
        )
    }
}
