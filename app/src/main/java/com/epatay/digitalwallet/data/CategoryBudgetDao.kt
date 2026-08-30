package com.epatay.digitalwallet.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryBudgetDao {

    @Query("UPDATE category_budgets SET user_id = :uid, updated_at = :timestamp, is_synced = 0 WHERE user_id IS NULL OR user_id = 'guest'")
    suspend fun assignUserToGuestRecords(uid: String, timestamp: Long)

    @Query("""SELECT *
        FROM category_budgets
        WHERE is_deleted = 0 AND monthKey = :monthKey
        ORDER BY category COLLATE NOCASE ASC""")
    fun observeForMonth(
        monthKey: Int
    ): Flow<List<CategoryBudget>>

    @Query("""SELECT *
        FROM category_budgets
        WHERE is_deleted = 0 AND monthKey = :monthKey
        ORDER BY category COLLATE NOCASE ASC""")
    suspend fun getForMonth(
        monthKey: Int
    ): List<CategoryBudget>

    @Query("SELECT * FROM category_budgets " +
            "WHERE is_deleted = 0 ORDER BY monthKey DESC, category COLLATE NOCASE ASC")
    suspend fun getAll(): List<CategoryBudget>

    @Query("SELECT * FROM category_budgets ORDER BY monthKey DESC, category COLLATE NOCASE ASC")
    suspend fun getAllSnapshot(): List<CategoryBudget>

    @Query("SELECT * FROM category_budgets")
    suspend fun getAllSync(): List<CategoryBudget>

    @Upsert
    suspend fun upsert(
        categoryBudget: CategoryBudget
    )

    @Query(
        """
        UPDATE category_budgets SET is_deleted = 1, is_synced = 0
        WHERE monthKey = :monthKey AND category = :category
        """
    )
    suspend fun delete(
        monthKey: Int,
        category: String
    )

    @Query(
        """
        DELETE FROM category_budgets
        WHERE monthKey = :monthKey AND category = :category
        """
    )
    suspend fun hardDelete(
        monthKey: Int,
        category: String
    )

    @Query("DELETE FROM category_budgets WHERE category IN ('Market', 'Yiyecek ve İçecek') AND (limitAmount = 6000.0 OR limitAmount = 3500.0)")
    suspend fun clearDemoBudgets()

    @Query("DELETE FROM category_budgets")
    suspend fun clearAll()
}
