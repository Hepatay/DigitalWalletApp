package com.epatay.digitalwallet.recurring

import com.epatay.digitalwallet.data.RecurringTransaction
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.Locale
import java.util.TimeZone

object RecurringDateUtils {

    const val NOTIFICATION_LEAD_DAYS = 3
    private const val MILLIS_PER_DAY = 86_400_000L

    fun currentPeriod(
        calendar: Calendar
    ): String {
        return String.format(
            Locale.ROOT,
            "%04d-%02d",
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH) + 1
        )
    }

    fun effectiveDueDay(
        dayOfMonth: Int,
        calendar: Calendar
    ): Int {
        return dayOfMonth.coerceIn(
            1,
            calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        )
    }

    fun daysUntilDue(
        dayOfMonth: Int,
        calendar: Calendar
    ): Int {
        return daysBetween(
            calendar,
            currentPeriodDueDate(
                dayOfMonth,
                calendar
            )
        )
    }

    fun currentPeriodDueDate(
        dayOfMonth: Int,
        calendar: Calendar
    ): Calendar {
        return (calendar.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            set(
                Calendar.DAY_OF_MONTH,
                effectiveDueDay(
                    dayOfMonth,
                    this
                )
            )
        }
    }

    fun nextDueDate(
        dayOfMonth: Int,
        calendar: Calendar
    ): Calendar {
        val dueDate =
            currentPeriodDueDate(
                dayOfMonth,
                calendar
            )

        if (compareDates(dueDate, calendar) < 0) {
            dueDate.set(Calendar.DAY_OF_MONTH, 1)
            dueDate.add(Calendar.MONTH, 1)
            dueDate.set(
                Calendar.DAY_OF_MONTH,
                effectiveDueDay(
                    dayOfMonth,
                    dueDate
                )
            )
        }

        return dueDate
    }

    fun daysUntilNextDue(
        dayOfMonth: Int,
        calendar: Calendar
    ): Int {
        return daysBetween(
            calendar,
            nextDueDate(
                dayOfMonth,
                calendar
            )
        )
    }

    fun shouldAutoGenerate(
        recurringTransaction: RecurringTransaction,
        calendar: Calendar
    ): Boolean {
        return recurringTransaction.isActive &&
            recurringTransaction.autoCreate &&
            recurringTransaction.lastGeneratedPeriod !=
            currentPeriod(calendar) &&
            daysUntilDue(
                recurringTransaction.dayOfMonth,
                calendar
            ) <= 0
    }

    fun shouldCreateOccurrence(
        recurringTransaction: RecurringTransaction,
        calendar: Calendar,
        occurrenceExists: Boolean
    ): Boolean {
        return !occurrenceExists &&
            shouldAutoGenerate(
                recurringTransaction,
                calendar
            )
    }

    fun shouldNotify(
        recurringTransaction: RecurringTransaction,
        calendar: Calendar
    ): Boolean {
        val nextDueDate = nextDueDate(
            recurringTransaction.dayOfMonth,
            calendar
        )

        val daysUntilDue =
            daysBetween(
                calendar,
                nextDueDate
            )

        return recurringTransaction.isActive &&
            recurringTransaction.notificationEnabled &&
            recurringTransaction.lastNotifiedPeriod !=
            currentPeriod(nextDueDate) &&
            daysUntilDue in 0..NOTIFICATION_LEAD_DAYS
    }

    fun transactionDate(
        dayOfMonth: Int,
        calendar: Calendar
    ): String {
        return String.format(
            Locale.ROOT,
            "%02d.%02d.%04d 09:00",
            effectiveDueDay(dayOfMonth, calendar),
            calendar.get(Calendar.MONTH) + 1,
            calendar.get(Calendar.YEAR)
        )
    }

    private fun compareDates(
        first: Calendar,
        second: Calendar
    ): Int {
        return dateKey(first).compareTo(
            dateKey(second)
        )
    }

    private fun dateKey(
        calendar: Calendar
    ): Int {
        return calendar.get(Calendar.YEAR) * 10_000 +
            (calendar.get(Calendar.MONTH) + 1) * 100 +
            calendar.get(Calendar.DAY_OF_MONTH)
    }

    private fun daysBetween(
        start: Calendar,
        end: Calendar
    ): Int {
        val startUtc =
            utcDateMillis(start)
        val endUtc =
            utcDateMillis(end)

        return (
            (endUtc - startUtc) /
                MILLIS_PER_DAY
            ).toInt()
    }

    private fun utcDateMillis(
        calendar: Calendar
    ): Long {
        return GregorianCalendar(
            TimeZone.getTimeZone("UTC")
        ).apply {
            clear()
            set(
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH),
                12,
                0,
                0
            )
        }.timeInMillis
    }
}
