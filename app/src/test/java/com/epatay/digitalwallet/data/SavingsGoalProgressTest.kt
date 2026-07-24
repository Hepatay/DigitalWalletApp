package com.epatay.digitalwallet.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SavingsGoalProgressTest {

    @Test
    fun progress_calculatesRemainingAndCompletion() {
        val goal =
            SavingsGoal(
                id = 3,
                title = "Acil durum fonu",
                targetAmount = 10_000.0,
                createdAtMillis = 1L
            )

        val partial =
            SavingsGoalProgress(
                goal = goal,
                savedAmount = 2_500.0,
                entryCount = 2
            )

        assertEquals(
            7_500.0,
            partial.remainingAmount,
            0.001
        )
        assertEquals(25, partial.progressPercent)
        assertFalse(partial.isCompleted)

        val completed =
            SavingsGoalProgress(
                goal = goal,
                savedAmount = 12_000.0,
                entryCount = 5
            )

        assertEquals(
            2_000.0,
            completed.exceededAmount,
            0.001
        )
        assertEquals(120, completed.progressPercent)
        assertEquals(100, completed.progressBarPercent)
        assertTrue(completed.isCompleted)
    }
}
