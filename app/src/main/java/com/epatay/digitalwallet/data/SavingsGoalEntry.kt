package com.epatay.digitalwallet.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "savings_goal_entries",
    foreignKeys = [
        ForeignKey(
            entity = SavingsGoal::class,
            parentColumns = ["id"],
            childColumns = ["goalId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["goalId", "occurredOn"])
    ]
)
data class SavingsGoalEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val goalId: Int,
    val amountDelta: Double,
    val occurredOn: Int,
    val note: String? = null,
    val createdAtMillis: Long
)
