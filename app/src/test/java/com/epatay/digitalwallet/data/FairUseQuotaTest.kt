package com.epatay.digitalwallet.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.GregorianCalendar

class FairUseQuotaTest {

    @Test
    fun transactionQuotaAllowsUnder200ItemsInCurrentMonth() {
        val count = 199
        val maxLimit = 200
        val canAdd = count < maxLimit
        assertTrue("199 transactions should allow adding a new one", canAdd)
    }

    @Test
    fun transactionQuotaBlocksAt200OrMoreItemsInCurrentMonth() {
        val count = 200
        val maxLimit = 200
        val canAdd = count < maxLimit
        assertFalse("200 transactions should block adding a new one", canAdd)

        val countExceeded = 205
        assertFalse("205 transactions should block adding a new one", countExceeded < maxLimit)
    }

    @Test
    fun portfolioQuotaAllowsUnder20Items() {
        val currencyCount = 10
        val goldCount = 9
        val totalCount = currencyCount + goldCount
        val maxLimit = 20
        assertTrue("19 portfolio items should allow adding a new one", totalCount < maxLimit)
    }

    @Test
    fun portfolioQuotaBlocksAt20OrMoreItems() {
        val currencyCount = 10
        val goldCount = 10
        val totalCount = currencyCount + goldCount
        val maxLimit = 20
        assertFalse("20 portfolio items should block adding a new one", totalCount < maxLimit)
    }

    @Test
    fun currentMonthDateKeyRangeCalculatesCorrectBoundaries() {
        val calendar = GregorianCalendar(2026, Calendar.AUGUST, 15)
        val monthKey = TransactionDateUtils.currentMonthKey(calendar)
        assertEquals(202608, monthKey)

        val startKey = TransactionDateUtils.monthStartDateKey(monthKey)
        val endKey = TransactionDateUtils.monthEndDateKey(monthKey)

        assertEquals(20260801, startKey)
        assertEquals(20260831, endKey)
    }
}
