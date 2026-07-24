package com.epatay.digitalwallet.ui

import com.epatay.digitalwallet.data.CategoryBudget
import com.epatay.digitalwallet.data.CategoryTransactionTotal
import com.epatay.digitalwallet.data.Transaction
import com.epatay.digitalwallet.data.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.TimeZone

class BudgetReportViewModelTest {

    @Test
    fun progress_containsSpentAndBudgetOnlyCategories() {
        val result =
            buildCategoryBudgetProgress(
                categoryTotals = listOf(
                    CategoryTransactionTotal(
                        category = "Gıda",
                        totalAmount = 1_200.0,
                        transactionCount = 4
                    ),
                    CategoryTransactionTotal(
                        category = "Ulaşım",
                        totalAmount = 300.0,
                        transactionCount = 2
                    )
                ),
                budgets = listOf(
                    CategoryBudget(
                        monthKey = 202607,
                        category = "Gıda",
                        limitAmount = 1_000.0,
                        updatedAtMillis = 1L
                    ),
                    CategoryBudget(
                        monthKey = 202607,
                        category = "Eğlence",
                        limitAmount = 500.0,
                        updatedAtMillis = 1L
                    )
                )
            )

        assertEquals(3, result.size)

        val food = result.first { it.category == "Gıda" }
        assertTrue(food.hasBudget)
        assertTrue(food.isExceeded)
        assertEquals(200.0, food.exceededAmount, 0.001)
        assertEquals(120, food.usagePercent)
        assertEquals(100, food.progressBarPercent)

        val entertainment =
            result.first {
                it.category == "Eğlence"
            }
        assertEquals(
            0.0,
            entertainment.spentAmount,
            0.001
        )
        assertEquals(
            500.0,
            entertainment.remainingAmount,
            0.001
        )

        val transport =
            result.first {
                it.category == "Ulaşım"
            }
        assertFalse(transport.hasBudget)
        assertNull(transport.limitAmount)
    }

    @Test
    fun monthlySummary_recalculatesDailyLimitAfterMonthChanges() {
        val transactions = listOf(
            Transaction(
                title = "Temmuz market",
                amount = 1_000.0,
                category = "Gıda",
                date = "31.07.2026 18:00",
                type = TransactionType.EXPENSE
            ),
            Transaction(
                title = "Ağustos market",
                amount = 100.0,
                category = "Gıda",
                date = "01.08.2026 09:00",
                type = TransactionType.EXPENSE
            )
        )

        val julySummary =
            calculateMonthlyBudgetSummary(
                monthlyLimit = 3_100.0,
                transactions = transactions,
                calendar =
                    calendar(
                        year = 2026,
                        month = Calendar.JULY,
                        day = 31
                    )
            )
        val augustSummary =
            calculateMonthlyBudgetSummary(
                monthlyLimit = 3_100.0,
                transactions = transactions,
                calendar =
                    calendar(
                        year = 2026,
                        month = Calendar.AUGUST,
                        day = 1
                    )
            )

        assertEquals(
            1_000.0,
            julySummary.currentMonthExpense,
            0.001
        )
        assertEquals(
            2_100.0,
            julySummary.dailySpendingLimit,
            0.001
        )

        assertEquals(
            100.0,
            augustSummary.currentMonthExpense,
            0.001
        )
        assertEquals(
            3_000.0 / 31.0,
            augustSummary.dailySpendingLimit,
            0.001
        )
        assertEquals(30, augustSummary.daysUntilMonthEnd)
    }

    private fun calendar(
        year: Int,
        month: Int,
        day: Int
    ): Calendar {
        return GregorianCalendar(
            TimeZone.getTimeZone("UTC")
        ).apply {
            clear()
            set(year, month, day, 12, 0, 0)
        }
    }
}
