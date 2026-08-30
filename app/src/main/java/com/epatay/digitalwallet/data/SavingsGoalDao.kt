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
    @Query("UPDATE savings_goals SET user_id = :uid, updated_at = :timestamp, is_synced = 0 WHERE user_id IS NULL OR user_id = 'guest'")
    suspend fun assignUserToGuestRecords(uid: String, timestamp: Long)

    @Query("UPDATE savings_goal_entries SET user_id = :uid, updated_at = :timestamp, is_synced = 0 WHERE user_id IS NULL OR user_id = 'guest'")
    suspend fun assignUserToGuestEntries(uid: String, timestamp: Long)

    @Update
    suspend fun updateEntry(entry: SavingsGoalEntry)


    @Query("""SELECT
            goals.*,
            COALESCE(SUM(entries.amountDelta), 0) AS savedAmount,
            COUNT(entries.uuid) AS entryCount
        FROM savings_goals AS goals
        LEFT JOIN savings_goal_entries AS entries
            ON entries.goalId = goals.uuid
        WHERE goals.is_deleted = 0
        GROUP BY goals.uuid
        ORDER BY
            goals.isArchived ASC,
            goals.createdAtMillis DESC,
            goals.uuid DESC""")
    fun observeGoalsWithProgress():
        Flow<List<SavingsGoalProgress>>

    @Query("SELECT * FROM savings_goals WHERE is_deleted = 0 AND uuid = :goalId LIMIT 1")
    suspend fun getGoalById(
        goalId: String
    ): SavingsGoal?

    @Query("""SELECT COALESCE(SUM(amountDelta), 0)
        FROM savings_goal_entries
        WHERE is_deleted = 0 AND goalId = :goalId""")
    suspend fun getSavedAmount(
        goalId: String
    ): Double

    @Insert
    suspend fun insertGoal(
        goal: SavingsGoal
    ): Long

    @Update
    suspend fun updateGoal(
        goal: SavingsGoal
    )

    @Query("UPDATE savings_goals SET isArchived = :isArchived WHERE uuid = :goalId"
    )
    suspend fun setArchived(
        goalId: String,
        isArchived: Boolean
    )

    @Query("UPDATE savings_goals SET is_deleted = 1, is_synced = 0, updated_at = :timestamp WHERE uuid = :goalId")
    suspend fun deleteGoal(goalId: String, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM savings_goals WHERE uuid = :goalId")
    suspend fun hardDeleteGoal(goalId: String)

    @Query("DELETE FROM savings_goals WHERE uuid LIKE 'DEMO_TUTORIAL_%'")
    suspend fun clearDemoGoals()

    @Query("SELECT * FROM savings_goals")
    suspend fun getAllGoalsSnapshot(): List<SavingsGoal>

    @Query("SELECT * FROM savings_goals")
    suspend fun getAllGoalsSync(): List<SavingsGoal>

    @Query(
        """
        SELECT *
        FROM savings_goal_entries
        WHERE is_deleted = 0 AND goalId = :goalId
        ORDER BY occurredOn DESC, uuid DESC
        """)
    fun observeEntries(
        goalId: String
    ): Flow<List<SavingsGoalEntry>>

    @Query(
        """
        SELECT *
        FROM savings_goal_entries
        WHERE is_deleted = 0 AND goalId = :goalId
        ORDER BY occurredOn DESC, uuid DESC
        """)
    suspend fun getEntries(
        goalId: String
    ): List<SavingsGoalEntry>

    @Query("SELECT * FROM savings_goal_entries WHERE is_deleted = 0 AND uuid = :entryId LIMIT 1")
    suspend fun getEntryById(
        entryId: String
    ): SavingsGoalEntry?

    @Insert
    suspend fun insertEntry(
        entry: SavingsGoalEntry
    ): Long

    @Query("UPDATE savings_goal_entries SET is_deleted = 1, is_synced = 0, updated_at = :timestamp WHERE uuid = :entryId")
    suspend fun deleteEntry(entryId: String, timestamp: Long = System.currentTimeMillis())

    @Query("DELETE FROM savings_goal_entries WHERE uuid = :entryId")
    suspend fun hardDeleteEntry(entryId: String)

    @Query("DELETE FROM savings_goal_entries WHERE uuid LIKE 'DEMO_TUTORIAL_%' OR goalId LIKE 'DEMO_TUTORIAL_%'")
    suspend fun clearDemoEntries()

    @Query("SELECT * FROM savings_goal_entries")
    suspend fun getAllEntriesSnapshot(): List<SavingsGoalEntry>

    @Query("SELECT * FROM savings_goal_entries")
    suspend fun getAllEntriesSync(): List<SavingsGoalEntry>
    @Query("DELETE FROM savings_goals")
    suspend fun clearAllGoals()

    @Query("DELETE FROM savings_goal_entries")
    suspend fun clearAllEntries()
}
