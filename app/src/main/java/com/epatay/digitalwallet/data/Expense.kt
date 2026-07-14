package com.epatay.digitalwallet.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses_table")
data class Expense(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,      // Örn: "Market Alışverişi"
    val amount: Double,     // Örn: 150.50
    val category: String,   // Örn: "Gıda"
    val date: String        // Örn: "14.07.2026"
)