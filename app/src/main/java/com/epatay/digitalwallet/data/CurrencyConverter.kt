package com.epatay.digitalwallet.data

import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

enum class CurrencyRateKind {
    BUYING,
    SELLING
}

data class CurrencyConversionRate(
    val code: String,
    val name: String,
    val unit: Int,
    val buying: BigDecimal?,
    val selling: BigDecimal?
) {
    fun unitTryRate(kind: CurrencyRateKind): BigDecimal? {
        if (code == TRY_CODE) {
            return BigDecimal.ONE
        }

        val rawRate =
            when (kind) {
                CurrencyRateKind.BUYING -> buying
                CurrencyRateKind.SELLING -> selling
            }
                ?: return null

        if (unit <= 0 || rawRate <= BigDecimal.ZERO) {
            return null
        }

        return rawRate.divide(
            BigDecimal.valueOf(unit.toLong()),
            RATE_SCALE,
            RoundingMode.HALF_UP
        )
    }

    companion object {
        const val TRY_CODE = "TRY"
        private const val RATE_SCALE = 12
    }
}

data class CurrencyConversionRequest(
    val amount: BigDecimal,
    val fromCode: String,
    val toCode: String,
    val rateKind: CurrencyRateKind
)

data class CurrencyConversionResult(
    val sourceAmount: BigDecimal,
    val targetAmount: BigDecimal,
    val fromCode: String,
    val toCode: String,
    val rateKind: CurrencyRateKind
)

object CurrencyConverter {

    fun buildRates(
        rates: List<CurrencyRate>
    ): List<CurrencyConversionRate> {
        val convertedRates =
            rates.map { rate ->
                CurrencyConversionRate(
                    code = rate.currencyCode.uppercase(Locale.ROOT),
                    name = rate.name,
                    unit = rate.unit,
                    buying = rate.forexBuying.toDecimalOrNull(),
                    selling = rate.forexSelling.toDecimalOrNull()
                )
            }

        return listOf(
            CurrencyConversionRate(
                code = CurrencyConversionRate.TRY_CODE,
                name = "Türk Lirası",
                unit = 1,
                buying = BigDecimal.ONE,
                selling = BigDecimal.ONE
            )
        ) + convertedRates
    }

    fun convert(
        request: CurrencyConversionRequest,
        rates: List<CurrencyConversionRate>
    ): CurrencyConversionResult? {
        if (request.amount <= BigDecimal.ZERO) {
            return null
        }

        val fromCode =
            request.fromCode.uppercase(Locale.ROOT)
        val toCode =
            request.toCode.uppercase(Locale.ROOT)

        val source =
            rates.firstOrNull { it.code == fromCode }
                ?: return null
        val target =
            rates.firstOrNull { it.code == toCode }
                ?: return null

        val sourceTryRate =
            source.unitTryRate(request.rateKind)
                ?: return null
        val targetTryRate =
            target.unitTryRate(request.rateKind)
                ?: return null

        if (
            sourceTryRate <= BigDecimal.ZERO ||
            targetTryRate <= BigDecimal.ZERO
        ) {
            return null
        }

        val result =
            request.amount
                .multiply(sourceTryRate)
                .divide(
                    targetTryRate,
                    RESULT_SCALE,
                    RoundingMode.HALF_UP
                )
                .stripTrailingZeros()

        return CurrencyConversionResult(
            sourceAmount = request.amount.stripTrailingZeros(),
            targetAmount = result,
            fromCode = fromCode,
            toCode = toCode,
            rateKind = request.rateKind
        )
    }

    private fun Double?.toDecimalOrNull(): BigDecimal? =
        this
            ?.takeIf { it.isFinite() }
            ?.takeIf { it > 0.0 }
            ?.let(BigDecimal::valueOf)

    private const val RESULT_SCALE = 8
}
