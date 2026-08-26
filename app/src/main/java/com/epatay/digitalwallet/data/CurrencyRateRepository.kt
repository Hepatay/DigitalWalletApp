package com.epatay.digitalwallet.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException
import java.net.UnknownHostException

class CurrencyRateRepository(
    private val context: Context,
    private val dao: CurrencyRateDao,
    private val service: TcmbCurrencyService = TcmbCurrencyService(),
    private val parser: TcmbXmlParser = TcmbXmlParser(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    suspend fun loadRates(): CurrencyLoadResult =
        withContext(ioDispatcher) {
            dao.deleteXdr()

            val cachedRates =
                getCachedRates()

            val latestFetchedAt =
                dao.getLatestFetchedAt() ?: 0L

            if (
                cachedRates.isNotEmpty() &&
                latestFetchedAt >=
                    System.currentTimeMillis() - CACHE_MAX_AGE_MILLIS
            ) {
                return@withContext CurrencyLoadResult.Success(cachedRates)
            }

            if (!hasValidatedInternet()) {
                return@withContext if (cachedRates.isNotEmpty()) {
                    CurrencyLoadResult.Cached(
                        rates = cachedRates,
                        reason = CurrencyCacheReason.NO_INTERNET
                    )
                } else {
                    CurrencyLoadResult.NoInternet
                }
            }

            try {
                val document =
                    parser.parse(
                        service.fetchTodayXml()
                    )

                if (document.rates.isEmpty()) {
                    return@withContext if (cachedRates.isNotEmpty()) {
                        CurrencyLoadResult.Cached(
                            rates = cachedRates,
                            reason =
                                CurrencyCacheReason.EMPTY_REMOTE_DATA
                        )
                    } else {
                        CurrencyLoadResult.Empty
                    }
                }

                val fetchedAtMillis =
                    System.currentTimeMillis()

                val ratesWithTimestamp = document.rates.map { rate ->
                    rate.copy(fetchedAtMillis = fetchedAtMillis)
                }

                dao.replaceAll(
                    ratesWithTimestamp.map { rate ->
                        rate.toEntity(fetchedAtMillis)
                    }
                )

                CurrencyLoadResult.Success(
                    rates = ratesWithTimestamp
                )
            } catch (exception: XmlPullParserException) {
                cachedOrError(
                    cachedRates = cachedRates,
                    reason = CurrencyCacheReason.XML_PARSE_ERROR,
                    error = CurrencyLoadResult.XmlParseError
                )
            } catch (exception: UnknownHostException) {
                cachedOrError(
                    cachedRates = cachedRates,
                    reason = CurrencyCacheReason.NO_INTERNET,
                    error = CurrencyLoadResult.NoInternet
                )
            } catch (exception: IOException) {
                cachedOrError(
                    cachedRates = cachedRates,
                    reason = CurrencyCacheReason.SERVICE_UNAVAILABLE,
                    error = CurrencyLoadResult.ServiceUnavailable
                )
            }
        }

    private suspend fun getCachedRates(): List<CurrencyRate> {
        return TcmbXmlParser.sortRates(
            dao.getAllRates()
                .filterNot {
                    it.currencyCode.equals("XDR", ignoreCase = true)
                }
                .map(CurrencyRateEntity::toCurrencyRate)
        )
    }

    private fun cachedOrError(
        cachedRates: List<CurrencyRate>,
        reason: CurrencyCacheReason,
        error: CurrencyLoadResult
    ): CurrencyLoadResult {
        return if (cachedRates.isNotEmpty()) {
            CurrencyLoadResult.Cached(
                rates = cachedRates,
                reason = reason
            )
        } else {
            error
        }
    }

    private fun hasValidatedInternet(): Boolean {
        val connectivityManager =
            context.getSystemService(
                Context.CONNECTIVITY_SERVICE
            ) as ConnectivityManager

        val network =
            connectivityManager.activeNetwork
                ?: return false

        val capabilities =
            connectivityManager.getNetworkCapabilities(
                network
            ) ?: return false

        return capabilities.hasCapability(
            NetworkCapabilities.NET_CAPABILITY_INTERNET
        )
    }

    private companion object {
        const val CACHE_MAX_AGE_MILLIS = 6L * 60L * 60L * 1000L
    }
}

sealed class CurrencyLoadResult {
    data class Success(
        val rates: List<CurrencyRate>
    ) : CurrencyLoadResult()

    data class Cached(
        val rates: List<CurrencyRate>,
        val reason: CurrencyCacheReason
    ) : CurrencyLoadResult()

    data object NoInternet : CurrencyLoadResult()
    data object ServiceUnavailable : CurrencyLoadResult()
    data object XmlParseError : CurrencyLoadResult()
    data object Empty : CurrencyLoadResult()
}

enum class CurrencyCacheReason {
    NO_INTERNET,
    SERVICE_UNAVAILABLE,
    XML_PARSE_ERROR,
    EMPTY_REMOTE_DATA
}
