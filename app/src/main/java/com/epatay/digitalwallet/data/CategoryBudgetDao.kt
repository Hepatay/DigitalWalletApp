package com.epatay.digitalwallet.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryBudgetDao {

    @Query(
        """
        SELECT *
        FROM category_budgets
        WHERE monthKey = :monthKey
        ORDER BY category COLLATE NOCASE ASC
        """
    )
    fun observeForMonth(
        monthKey: Int
    ): Flow<List<CategoryBudget>>

    @Query(
        """
        SELECT *
        FROM category_budgets
        WHERE monthKey = :monthKey
        ORDER BY category COLLATE NOCASE ASC
        """
    )
    suspend fun getForMonth(
        monthKey: Int
    ): List<CategoryBudget>

    @Query(
        "SELECT * FROM category_budgets " +
            "ORDER BY monthKey DESC, category COLLATE NOCASE ASC"
    )
    suspend fun getAll(): List<CategoryBudget>

    @Upsert
    suspend fun upsert(
        categoryBudget: CategoryBudget
    )

    @Query(
        """
        DELETE FROM category_budgets
        WHERE monthKey = :monthKey AND category = :category
        """
    )
    suspend fun delete(
        monthKey: Int,
        category: String
    )
}
