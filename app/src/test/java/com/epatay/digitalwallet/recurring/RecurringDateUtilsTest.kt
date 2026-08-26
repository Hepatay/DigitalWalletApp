package com.epatay.digitalwallet.recurring

import com.epatay.digitalwallet.data.RecurringTransaction
import com.epatay.digitalwallet.data.TransactionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.TimeZone

class RecurringDateUtilsTest {

    @Test
    fun effectiveDueDay_movesDay31ToLastDayOfMonth() {
        val nonLeapFebruary =
            calendar(year = 2027, month = Calendar.FEBRUARY, day = 10)
        val leapFebruary =
            calendar(year = 2028, month = Calendar.FEBRUARY, day = 10)

        assertEquals(
            28,
            RecurringDateUtils.effectiveDueDay(
                31,
                nonLeapFebruary
            )
        )
        assertEquals(
            29,
            RecurringDateUtils.effectiveDueDay(
                31,
                leapFebruary
            )
        )
    }

    @Test
    fun shouldAutoGenerate_onlyOnceAfterDueDay() {
        val beforeDue =
            calendar(year = 2026, month = Calendar.JULY, day = 14)
        val onDue =
            calendar(year = 2026, month = Calendar.JULY, day = 15)
        val recurring =
            recurring(dayOfMonth = 15, autoCreate = true)

        assertFalse(
            RecurringDateUtils.shouldAutoGenerate(
                recurring,
                beforeDue
            )
        )
        assertTrue(
            RecurringDateUtils.shouldAutoGenerate(
                recurring,
                onDue
            )
        )
        assertFalse(
            RecurringDateUtils.shouldAutoGenerate(
                recurring.copy(lastGeneratedPeriod = "2026-07"),
                onDue
            )
        )
    }

    @Test
    fun shouldAutoGenerate_usesEffectiveLastDayForShortMonth() {
        val february27 =
            calendar(year = 2027, month = Calendar.FEBRUARY, day = 27)
        val february28 =
            calendar(year = 2027, month = Calendar.FEBRUARY, day = 28)
        val recurring =
            recurring(dayOfMonth = 31, autoCreate = true)

        assertFalse(
            RecurringDateUtils.shouldAutoGenerate(
                recurring,
                february27
            )
        )
        assertTrue(
            RecurringDateUtils.shouldAutoGenerate(
                recurring,
                february28
            )
        )
        assertEquals(
            "28.02.2027 09:00",
            RecurringDateUtils.transactionDate(
                recurring.dayOfMonth,
                february28
            )
        )
    }

    @Test
    fun shouldNotify_fromThreeDaysBeforeThroughDueDayOnlyOnce() {
        val fourDaysBefore =
            calendar(year = 2026, month = Calendar.JULY, day = 21)
        val threeDaysBefore =
            calendar(year = 2026, month = Calendar.JULY, day = 22)
        val dueDay =
            calendar(year = 2026, month = Calendar.JULY, day = 25)
        val recurring =
            recurring(
                dayOfMonth = 25,
                notificationEnabled = true
            )

        assertFalse(
            RecurringDateUtils.shouldNotify(
                recurring,
                fourDaysBefore
            )
        )
        assertTrue(
            RecurringDateUtils.shouldNotify(
                recurring,
                threeDaysBefore
            )
        )
        assertTrue(
            RecurringDateUtils.shouldNotify(
                recurring,
                dueDay
            )
        )
        assertFalse(
            RecurringDateUtils.shouldNotify(
                recurring.copy(lastNotifiedPeriod = "2026-07"),
                dueDay
            )
        )
    }

    @Test
    fun fiveDaysBefore_reportsFiveDaysAndDoesNotNotifyEarly() {
        val july24 =
            calendar(
                year = 2026,
                month = Calendar.JULY,
                day = 24
            )
        val recurring =
            recurring(
                dayOfMonth = 29,
                notificationEnabled = true
            )

        assertEquals(
            5,
            RecurringDateUtils.daysUntilNextDue(
                recurring.dayOfMonth,
                july24
            )
        )
        assertFalse(
            RecurringDateUtils.shouldNotify(
                recurring,
                july24
            )
        )
    }

    @Test
    fun nextDueDate_crossesIntoNextMonthWithCorrectDayCount() {
        val july30 =
            calendar(
                year = 2026,
                month = Calendar.JULY,
                day = 30
            )

        assertEquals(
            3,
            RecurringDateUtils.daysUntilNextDue(
                2,
                july30
            )
        )

        val nextDue =
            RecurringDateUtils.nextDueDate(
                2,
                july30
            )

        assertEquals(
            Calendar.AUGUST,
            nextDue.get(Calendar.MONTH)
        )
        assertEquals(
            2,
            nextDue.get(Calendar.DAY_OF_MONTH)
        )
    }

    @Test
    fun shouldNotify_usesDueMonthAsNotificationMarker() {
        val july30 =
            calendar(
                year = 2026,
                month = Calendar.JULY,
                day = 30
            )
        val recurring =
            recurring(
                dayOfMonth = 2,
                notificationEnabled = true
            )

        assertTrue(
            RecurringDateUtils.shouldNotify(
                recurring,
                july30
            )
        )
        assertFalse(
            RecurringDateUtils.shouldNotify(
                recurring.copy(
                    lastNotifiedPeriod = "2026-08"
                ),
                july30
            )
        )
    }

    @Test
    fun existingOccurrence_preventsDuplicateAfterClockRollback() {
        val july30 =
            calendar(
                year = 2026,
                month = Calendar.JULY,
                day = 30
            )
        val recurring =
            recurring(
                dayOfMonth = 2,
                autoCreate = true
            ).copy(
                lastGeneratedPeriod = "2026-08"
            )

        assertTrue(
            RecurringDateUtils.shouldAutoGenerate(
                recurring,
                july30
            )
        )
        assertFalse(
            RecurringDateUtils.shouldCreateOccurrence(
                recurringTransaction = recurring,
                calendar = july30,
                occurrenceExists = true
            )
        )
    }

    @Test
    fun inactiveRecord_neverGeneratesOrNotifies() {
        val today =
            calendar(year = 2026, month = Calendar.JULY, day = 25)
        val recurring =
            recurring(
                dayOfMonth = 25,
                autoCreate = true,
                notificationEnabled = true
            ).copy(isActive = false)

        assertFalse(
            RecurringDateUtils.shouldAutoGenerate(
                recurring,
                today
            )
        )
        assertFalse(
            RecurringDateUtils.shouldNotify(
                recurring,
                today
            )
        )
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

    private fun recurring(
        dayOfMonth: Int,
        autoCreate: Boolean = false,
        notificationEnabled: Boolean = false
    ): RecurringTransaction {
        return RecurringTransaction(
            uuid = "7",
            title = "Kira",
            amount = 12_500.0,
            category = "Konut",
            type = TransactionType.EXPENSE,
            dayOfMonth = dayOfMonth,
            autoCreate = autoCreate,
            notificationEnabled = notificationEnabled
        )
    }
}
