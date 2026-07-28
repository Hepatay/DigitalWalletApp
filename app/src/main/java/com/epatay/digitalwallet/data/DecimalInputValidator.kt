package com.epatay.digitalwallet.data

import java.math.BigDecimal
import java.math.RoundingMode

sealed interface DecimalInputResult {
    data class Valid(
        val value: BigDecimal
    ) : DecimalInputResult

    data class Invalid(
        val message: String
    ) : DecimalInputResult
}

object DecimalInputValidator {

    fun positiveMoney(
        rawValue: CharSequence?,
        fieldName: String = "Tutar",
        maxScale: Int = MONEY_SCALE
    ): DecimalInputResult =
        parsePositive(
            rawValue = rawValue,
            fieldName = fieldName,
            maxScale = maxScale
        )

    fun positiveQuantity(
        rawValue: CharSequence?,
        fieldName: String = "Miktar",
        wholeNumberOnly: Boolean = false
    ): DecimalInputResult =
        parsePositive(
            rawValue = rawValue,
            fieldName = fieldName,
            maxScale =
                if (wholeNumberOnly) 0 else QUANTITY_SCALE
        )

    private fun parsePositive(
        rawValue: CharSequence?,
        fieldName: String,
        maxScale: Int
    ): DecimalInputResult {
        val raw = rawValue?.toString()?.trim().orEmpty()

        if (raw.isEmpty()) {
            return DecimalInputResult.Invalid(
                "$fieldName boş bırakılamaz"
            )
        }

        if (
            raw.any { character ->
                character.isWhitespace() ||
                    Character.isSpaceChar(character)
            } ||
            raw.any { character ->
                !character.isDigit() &&
                    character != ',' &&
                    character != '.'
            }
        ) {
            return DecimalInputResult.Invalid(
                "$fieldName yalnızca rakam, virgül veya nokta içerebilir"
            )
        }

        val normalized = normalizeDecimal(raw)
            ?: return DecimalInputResult.Invalid(
                "$fieldName geçerli bir sayı olmalıdır"
            )

        val integerDigits =
            normalized
                .substringBefore('.')
                .trimStart('0')
                .length
                .coerceAtLeast(1)
        val fractionDigits =
            normalized
                .substringAfter('.', missingDelimiterValue = "")
                .length

        if (integerDigits > MAX_INTEGER_DIGITS) {
            return DecimalInputResult.Invalid(
                "$fieldName en fazla $MAX_INTEGER_DIGITS tam basamak olabilir"
            )
        }

        if (fractionDigits > maxScale) {
            return DecimalInputResult.Invalid(
                if (maxScale == 0) {
                    "$fieldName tam sayı olmalıdır"
                } else {
                    "$fieldName en fazla $maxScale ondalık basamak içerebilir"
                }
            )
        }

        val decimal =
            normalized.toBigDecimalOrNull()
                ?: return DecimalInputResult.Invalid(
                    "$fieldName geçerli bir sayı olmalıdır"
                )

        if (decimal <= BigDecimal.ZERO) {
            return DecimalInputResult.Invalid(
                "$fieldName sıfırdan büyük olmalıdır"
            )
        }

        return DecimalInputResult.Valid(decimal)
    }

    private fun normalizeDecimal(
        raw: String
    ): String? {
        val commaCount = raw.count { it == ',' }
        val dotCount = raw.count { it == '.' }

        return when {
            commaCount > 0 && dotCount > 0 -> {
                val decimalSeparator =
                    if (raw.lastIndexOf(',') > raw.lastIndexOf('.')) {
                        ','
                    } else {
                        '.'
                    }
                val groupingSeparator =
                    if (decimalSeparator == ',') '.' else ','

                if (raw.count { it == decimalSeparator } != 1) {
                    return null
                }

                val decimalIndex = raw.lastIndexOf(decimalSeparator)
                val integerPart = raw.substring(0, decimalIndex)
                val fractionPart = raw.substring(decimalIndex + 1)

                if (
                    fractionPart.isEmpty() ||
                    fractionPart.any { !it.isDigit() } ||
                    !isValidGroupedInteger(
                        integerPart,
                        groupingSeparator
                    )
                ) {
                    null
                } else {
                    integerPart.replace(
                        groupingSeparator.toString(),
                        ""
                    ) + "." + fractionPart
                }
            }

            commaCount == 1 ->
                normalizeSingleDecimalSeparator(raw, ',')

            commaCount > 1 ->
                null

            dotCount == 1 ->
                normalizeSingleDecimalSeparator(raw, '.')

            dotCount > 1 ->
                if (isValidGroupedInteger(raw, '.')) {
                    raw.replace(".", "")
                } else {
                    null
                }

            raw.all(Char::isDigit) -> raw
            else -> null
        }
    }

    private fun normalizeSingleDecimalSeparator(
        raw: String,
        separator: Char
    ): String? {
        val parts = raw.split(separator)

        if (
            parts.size != 2 ||
            parts[0].isEmpty() ||
            parts[1].isEmpty() ||
            parts.any { part -> part.any { !it.isDigit() } }
        ) {
            return null
        }

        return parts[0] + "." + parts[1]
    }

    private fun isValidGroupedInteger(
        raw: String,
        separator: Char
    ): Boolean {
        if (raw.isEmpty()) {
            return false
        }

        if (separator !in raw) {
            return raw.all(Char::isDigit)
        }

        val groups = raw.split(separator)

        return groups.first().length in 1..3 &&
            groups.first().all(Char::isDigit) &&
            groups.drop(1).all { group ->
                group.length == 3 &&
                    group.all(Char::isDigit)
            }
    }

    private const val MONEY_SCALE = 2
    private const val QUANTITY_SCALE = 8
    private const val MAX_INTEGER_DIGITS = 15
}

object DecimalMath {

    fun normalizeMoney(value: Double): Double? =
        value.toSafeDecimal()
            ?.setScale(2, RoundingMode.HALF_UP)
            ?.toDouble()

    fun normalizeUnitPrice(value: Double): Double? =
        value.toSafeDecimal()
            ?.setScale(6, RoundingMode.HALF_UP)
            ?.stripTrailingZeros()
            ?.toDouble()

    fun normalizeQuantity(value: Double): Double? =
        value.toSafeDecimal()
            ?.setScale(8, RoundingMode.HALF_UP)
            ?.stripTrailingZeros()
            ?.toDouble()

    fun multiplyMoney(
        first: Double,
        second: Double
    ): Double? {
        val left = first.toSafeDecimal() ?: return null
        val right = second.toSafeDecimal() ?: return null

        return left.multiply(right)
            .setScale(2, RoundingMode.HALF_UP)
            .toDouble()
    }

    fun subtractMoney(
        first: Double,
        second: Double
    ): Double? {
        val left = first.toSafeDecimal() ?: return null
        val right = second.toSafeDecimal() ?: return null

        return left.subtract(right)
            .setScale(2, RoundingMode.HALF_UP)
            .toDouble()
    }

    fun sumMoney(values: Iterable<Double>): Double =
        values.fold(BigDecimal.ZERO) { total, value ->
            value.toSafeDecimal()?.let(total::add) ?: total
        }
            .setScale(2, RoundingMode.HALF_UP)
            .toDouble()

    private fun Double.toSafeDecimal(): BigDecimal? =
        takeIf { it.isFinite() }
            ?.let(BigDecimal::valueOf)
}
