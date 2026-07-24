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
            parentColumns = ["id"],
            childColumns = ["recurringId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["transactionId"])
    ]
)
data class RecurringOccurrence(
    val recurringId: Int,
    val periodKey: String,
    val transactionId: Int? = null,
    val createdAtMillis: Long
)
