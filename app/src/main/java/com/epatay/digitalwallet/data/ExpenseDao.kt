package com.epatay.digitalwallet.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query


@Dao
interface ExpenseDao {

    // Yeni harcama ekleme komutu (Arka planda çalışması için suspend yapıyoruz)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense)

    // Tüm harcamaları en yenisi en üstte olacak şekilde (DESC) getirme komutu
    @Query("SELECT * FROM expenses_table ORDER BY id DESC")
    fun getAllExpenses(): LiveData<List<Expense>>
}