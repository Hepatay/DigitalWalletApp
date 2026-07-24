package com.epatay.digitalwallet.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.TimeZone

class TransactionDateUtilsTest {

    @Test
    fun legacyDate_isConvertedToSortableDateKey() {
        assertEquals(
            20260724,
            TransactionDateUtils.toDateKey(
                "24.07.2026 18:45"
            )
        )
        assertEquals(
            20260724,
            TransactionDateUtils.toDateKey(
                "24.07.2026"
            )
        )
    }

    @Test
    fun invalidLegacyDates_returnUnknownKey() {
        assertEquals(
            TransactionDateUtils.UNKNOWN_DATE_KEY,
            TransactionDateUtils.toDateKey(
                "31.02.2026 10:00"
            )
        )
        assertEquals(
            TransactionDateUtils.UNKNOWN_DATE_KEY,
            TransactionDateUtils.toDateKey(
                "2026-07-24"
            )
        )
        assertEquals(
            TransactionDateUtils.UNKNOWN_DATE_KEY,
            TransactionDateUtils.toDateKey("")
        )
    }

    @Test
    fun leapYearAndMonthBoundaries_areCalculatedCorrectly() {
        assertEquals(
            20240229,
            TransactionDateUtils.monthEndDateKey(202402)
        )
        assertEquals(
            20250228,
            TransactionDateUtils.monthEndDateKey(202502)
        )
        assertEquals(
            20261231,
            TransactionDateUtils.monthEndDateKey(202612)
        )
        assertEquals(
            20260701,
            TransactionDateUtils.monthStartDateKey(202607)
        )
    }

    @Test
    fun transaction_defaultDateKey_matchesLegacyDate() {
        val transaction =
            Transaction(
                title = "Market",
                amount = 250.0,
                category = "Gıda",
                date = "05.08.2026 09:15",
                type = TransactionType.EXPENSE
            )

        assertEquals(
            20260805,
            transaction.occurredOn
        )
    }

    @Test
    fun repositoryNormalization_replacesStaleDateKey() {
        val staleTransaction =
            Transaction(
                id = 4,
                title = "Düzenlenmiş kayıt",
                amount = 100.0,
                category = "Diğer",
                date = "11.09.2026 12:00",
                type = TransactionType.EXPENSE,
                occurredOn = 20260101
            )

        assertEquals(
            20260911,
            normalizeTransactionDate(
                staleTransaction
            ).occurredOn
        )
    }

    @Test
    fun currentKeys_useCalendarDateComponents() {
        val calendar =
            GregorianCalendar(
                TimeZone.getTimeZone("UTC")
            ).apply {
                clear()
                set(2026, Calendar.DECEMBER, 31)
            }

        assertEquals(
            20261231,
            TransactionDateUtils.currentDateKey(calendar)
        )
        assertEquals(
            202612,
            TransactionDateUtils.currentMonthKey(calendar)
        )
        assertTrue(
            TransactionDateUtils.isValidDateKey(20260228)
        )
        assertFalse(
            TransactionDateUtils.isValidDateKey(20260229)
        )
    }
}
