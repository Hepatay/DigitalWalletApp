package com.epatay.digitalwallet.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "recurring_occurrences",
    primaryKeys = [
        "recurringId",
        "periodKey"
    ],
    foreignKeys = [
        ForeignKey(
            entity = RecurringTransaction::class,
            parentColumns = ["uuid"],
            childColumns = ["recurringId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["transactionId"])
    ]
)
data class RecurringOccurrence(
    val recurringId: String = "",
    val periodKey: String = "",
    val transactionId: String? = null,
    val createdAtMillis: Long = 0L,
    val updated_at: Long = System.currentTimeMillis(),
    val is_deleted: Boolean = false,
    val is_synced: Boolean = false,
    val user_id: String? = null
)
