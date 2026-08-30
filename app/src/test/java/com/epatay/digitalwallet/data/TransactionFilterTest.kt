package com.epatay.digitalwallet.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TransactionFilterTest {

    @Test
    fun `TransactionFilter hasActiveFilters returns false for default filter`() {
        val filter = TransactionFilter()
        assertFalse(filter.hasActiveFilters)
    }

    @Test
    fun `TransactionFilter hasActiveFilters returns true when amount range is set`() {
        val filterMin = TransactionFilter(minAmount = 100.0)
        assertTrue(filterMin.hasActiveFilters)

        val filterMax = TransactionFilter(maxAmount = 5000.0)
        assertTrue(filterMax.hasActiveFilters)

        val filterRange = TransactionFilter(minAmount = 100.0, maxAmount = 5000.0)
        assertTrue(filterRange.hasActiveFilters)
    }

    @Test
    fun `CurrencyFlagProvider returns valid flags and display names`() {
        assertEquals("ABD Doları", CurrencyFlagProvider.getCurrencyDisplayName("USD"))
        assertEquals("Euro", CurrencyFlagProvider.getCurrencyDisplayName("EUR"))
        assertEquals("İngiliz Sterlini", CurrencyFlagProvider.getCurrencyDisplayName("GBP"))
        
        assertTrue(CurrencyFlagProvider.getFlagResIdSafe("USD") != 0)
        assertTrue(CurrencyFlagProvider.getFlagResIdSafe("EUR") != 0)
        assertTrue(CurrencyFlagProvider.getFlagResIdSafe("XYZ") != 0) // safe fallback
    }
}