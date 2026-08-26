package com.epatay.digitalwallet.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "savings_goal_entries",
    foreignKeys = [
        ForeignKey(
            entity = SavingsGoal::class,
            parentColumns = ["uuid"],
            childColumns = ["goalId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["goalId", "occurredOn"])
    ]
)
data class SavingsGoalEntry(
    @PrimaryKey
    val uuid: String = UUID.randomUUID().toString(),
    val goalId: String = "",
    val amountDelta: Double = 0.0,
    val occurredOn: Int = 0,
    val note: String? = null,
    val createdAtMillis: Long = 0L,
    val updated_at: Long = System.currentTimeMillis(),
    val is_deleted: Boolean = false,
    val is_synced: Boolean = false,
    val user_id: String? = null
)
