package com.epatay.digitalwallet.data

import kotlinx.coroutines.flow.Flow

class CategoryBudgetRepository(
    private val categoryBudgetDao: CategoryBudgetDao
) {

    fun observeForMonth(
        monthKey: Int
    ): Flow<List<CategoryBudget>> {
        require(
            TransactionDateUtils.isValidMonthKey(monthKey)
        ) {
            "Geçersiz ay anahtarı: $monthKey"
        }

        return categoryBudgetDao.observeForMonth(monthKey)
    }

    suspend fun getForMonth(
        monthKey: Int
    ): List<CategoryBudget> {
        require(
            TransactionDateUtils.isValidMonthKey(monthKey)
        ) {
            "Geçersiz ay anahtarı: $monthKey"
        }

        return categoryBudgetDao.getForMonth(monthKey)
    }

    suspend fun upsert(
        categoryBudget: CategoryBudget
    ) {
        val normalized =
            categoryBudget.copy(
                category = categoryBudget.category.trim()
            )

        require(
            TransactionDateUtils.isValidMonthKey(
                normalized.monthKey
            )
        ) {
            "Geçersiz ay anahtarı: ${normalized.monthKey}"
        }
        require(normalized.category.isNotEmpty()) {
            "Kategori boş bırakılamaz."
        }
        require(
            normalized.limitAmount.isFinite() &&
                normalized.limitAmount > 0.0
        ) {
            "Kategori bütçesi sıfırdan büyük olmalıdır."
        }

        categoryBudgetDao.upsert(normalized)
    }

    suspend fun delete(
        monthKey: Int,
        category: String
    ) {
        categoryBudgetDao.delete(
            monthKey = monthKey,
            category = category.trim()
        )
    }
}
