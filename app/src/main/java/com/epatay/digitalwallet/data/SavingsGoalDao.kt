package com.epatay.digitalwallet.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class SavingsGoalProgress(
    @Embedded
    val goal: SavingsGoal,
    val savedAmount: Double,
    val entryCount: Int
) {
    val remainingAmount: Double
        get() =
            (goal.targetAmount - savedAmount)
                .coerceAtLeast(0.0)

    val exceededAmount: Double
        get() =
            (savedAmount - goal.targetAmount)
                .coerceAtLeast(0.0)

    val progressPercent: Int
        get() =
            if (goal.targetAmount > 0.0) {
                (
                    savedAmount /
                        goal.targetAmount *
                        100.0
                    )
                    .toInt()
                    .coerceAtLeast(0)
            } else {
                0
            }

    val progressBarPercent: Int
        get() = progressPercent.coerceIn(0, 100)

    val isCompleted: Boolean
        get() = savedAmount >= goal.targetAmount
}

@Dao
interface SavingsGoalDao {

    @Query(
        """
        SELECT
            goals.*,
            COALESCE(SUM(entries.amountDelta), 0) AS savedAmount,
            COUNT(entries.id) AS entryCount
        FROM savings_goals AS goals
        LEFT JOIN savings_goal_entries AS entries
            ON entries.goalId = goals.id
        GROUP BY goals.id
        ORDER BY
            goals.isArchived ASC,
            goals.createdAtMillis DESC,
            goals.id DESC
        """
    )
    fun observeGoalsWithProgress():
        Flow<List<SavingsGoalProgress>>

    @Query(
        "SELECT * FROM savings_goals WHERE id = :goalId LIMIT 1"
    )
    suspend fun getGoalById(
        goalId: Int
    ): SavingsGoal?

    @Query(
        """
        SELECT COALESCE(SUM(amountDelta), 0)
        FROM savings_goal_entries
        WHERE goalId = :goalId
        """
    )
    suspend fun getSavedAmount(
        goalId: Int
    ): Double

    @Insert
    suspend fun insertGoal(
        goal: SavingsGoal
    ): Long

    @Update
    suspend fun updateGoal(
        goal: SavingsGoal
    )

    @Query(
        "UPDATE savings_goals SET isArchived = :isArchived WHERE id = :goalId"
    )
    suspend fun setArchived(
        goalId: Int,
        isArchived: Boolean
    )

    @Delete
    suspend fun deleteGoal(
        goal: SavingsGoal
    )

    @Query(
        """
        SELECT *
        FROM savings_goal_entries
        WHERE goalId = :goalId
        ORDER BY occurredOn DESC, id DESC
        """
    )
    fun observeEntries(
        goalId: Int
    ): Flow<List<SavingsGoalEntry>>

    @Query(
        "SELECT * FROM savings_goal_entries WHERE id = :entryId LIMIT 1"
    )
    suspend fun getEntryById(
        entryId: Int
    ): SavingsGoalEntry?

    @Insert
    suspend fun insertEntry(
        entry: SavingsGoalEntry
    ): Long

    @Delete
    suspend fun deleteEntry(
        entry: SavingsGoalEntry
    )
}
