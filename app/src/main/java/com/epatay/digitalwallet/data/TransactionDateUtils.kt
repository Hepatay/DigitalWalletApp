package com.epatay.digitalwallet.data

import java.util.Calendar
import java.util.GregorianCalendar

/**
 * İşlem günlerini saat diliminden bağımsız, sorgulanabilir bir
 * YYYYMMDD anahtarına dönüştürür.
 */
object TransactionDateUtils {

    const val UNKNOWN_DATE_KEY = 0

    private val legacyDatePattern =
        Regex(
            pattern =
                """^(\d{2})\.(\d{2})\.(\d{4})(?:\s+\d{2}:\d{2})?$"""
        )

    fun toDateKey(
        date: String
    ): Int {
        val match =
            legacyDatePattern.matchEntire(date.trim())
                ?: return UNKNOWN_DATE_KEY

        val day =
            match.groupValues[1].toIntOrNull()
                ?: return UNKNOWN_DATE_KEY
        val month =
            match.groupValues[2].toIntOrNull()
                ?: return UNKNOWN_DATE_KEY
        val year =
            match.groupValues[3].toIntOrNull()
                ?: return UNKNOWN_DATE_KEY

        if (!isValidDate(year, month, day)) {
            return UNKNOWN_DATE_KEY
        }

        return year * 10_000 + month * 100 + day
    }

    fun currentDateKey(
        calendar: Calendar = Calendar.getInstance()
    ): Int {
        return dateKey(
            year = calendar.get(Calendar.YEAR),
            month = calendar.get(Calendar.MONTH) + 1,
            day = calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    fun currentMonthKey(
        calendar: Calendar = Calendar.getInstance()
    ): Int {
        return calendar.get(Calendar.YEAR) * 100 +
            calendar.get(Calendar.MONTH) + 1
    }

    fun monthKeyFromDateKey(
        dateKey: Int
    ): Int {
        return if (isValidDateKey(dateKey)) {
            dateKey / 100
        } else {
            UNKNOWN_DATE_KEY
        }
    }

    fun monthStartDateKey(
        monthKey: Int
    ): Int {
        require(isValidMonthKey(monthKey)) {
            "Geçersiz ay anahtarı: $monthKey"
        }

        return monthKey * 100 + 1
    }

    fun monthEndDateKey(
        monthKey: Int
    ): Int {
        require(isValidMonthKey(monthKey)) {
            "Geçersiz ay anahtarı: $monthKey"
        }

        val year = monthKey / 100
        val month = monthKey % 100
        val calendar =
            GregorianCalendar(
                year,
                month - 1,
                1
            )

        return dateKey(
            year = year,
            month = month,
            day =
                calendar.getActualMaximum(
                    Calendar.DAY_OF_MONTH
                )
        )
    }

    fun isValidDateKey(
        dateKey: Int
    ): Boolean {
        if (dateKey <= 0) {
            return false
        }

        val year = dateKey / 10_000
        val month = dateKey / 100 % 100
        val day = dateKey % 100

        return isValidDate(year, month, day)
    }

    fun isValidMonthKey(
        monthKey: Int
    ): Boolean {
        val year = monthKey / 100
        val month = monthKey % 100

        return year in 1..9999 &&
            month in 1..12
    }

    private fun dateKey(
        year: Int,
        month: Int,
        day: Int
    ): Int {
        return year * 10_000 + month * 100 + day
    }

    private fun isValidDate(
        year: Int,
        month: Int,
        day: Int
    ): Boolean {
        if (
            year !in 1..9999 ||
            month !in 1..12 ||
            day !in 1..31
        ) {
            return false
        }

        val calendar =
            GregorianCalendar().apply {
                isLenient = false
                clear()
                set(
                    year,
                    month - 1,
                    day
                )
            }

        return runCatching {
            calendar.timeInMillis
        }.isSuccess
    }
}
