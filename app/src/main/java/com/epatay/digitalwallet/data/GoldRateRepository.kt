package com.epatay.digitalwallet.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.google.gson.JsonParseException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException

interface GoldRateRepository {
    suspend fun loadRates(forceRefresh: Boolean = false): GoldLoadResult
}

class RoomGoldRateRepository(
    private val context: Context,
    private val dao: GoldRateDao,
    private val remoteDataSource: GoldRemoteDataSource =
        ApinoktamGoldRemoteDataSource(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : GoldRateRepository {

    override suspend fun loadRates(
        forceRefresh: Boolean
    ): GoldLoadResult =
        withContext(ioDispatcher) {
            val cached = cachedRates()

            if (
                !forceRefresh &&
                cached.isNotEmpty() &&
                cached.maxOf(GoldRate::fetchedAt) >=
                    System.currentTimeMillis() - CACHE_MAX_AGE_MILLIS
            ) {
                return@withContext GoldLoadResult.Success(cached)
            }

            if (!hasValidatedInternet()) {
                return@withContext cached.orFallback(
                    GoldCacheReason.NO_INTERNET,
                    GoldLoadResult.NoInternet
                )
            }

            try {
                val rates = remoteDataSource.fetchRates()

                if (rates.isEmpty()) {
                    cached.orFallback(
                        GoldCacheReason.EMPTY_REMOTE_DATA,
                        GoldLoadResult.Empty
                    )
                } else {
                    dao.replaceAll(rates.map(GoldRate::toEntity))
                    GoldLoadResult.Success(rates)
                }
            } catch (exception: JsonParseException) {
                cached.orFallback(
                    GoldCacheReason.PARSE_ERROR,
                    GoldLoadResult.ParseError
                )
            } catch (exception: HttpException) {
                cached.orFallback(
                    GoldCacheReason.SERVICE_UNAVAILABLE,
                    GoldLoadResult.ServiceUnavailable
                )
            } catch (exception: IOException) {
                cached.orFallback(
                    GoldCacheReason.SERVICE_UNAVAILABLE,
                    GoldLoadResult.ServiceUnavailable
                )
            } catch (exception: GoldDataValidationException) {
                cached.orFallback(
                    GoldCacheReason.INVALID_REMOTE_DATA,
                    GoldLoadResult.InvalidData
                )
            } catch (exception: IllegalStateException) {
                cached.orFallback(
                    GoldCacheReason.PARSE_ERROR,
                    GoldLoadResult.ParseError
                )
            }
        }

    private suspend fun cachedRates(): List<GoldRate> =
        dao.getAllOnce()
            .mapNotNull(GoldRateEntity::toGoldRate)
            .sortedBy { GoldType.entries.indexOf(it.type) }

    private fun List<GoldRate>.orFallback(
        reason: GoldCacheReason,
        error: GoldLoadResult
    ): GoldLoadResult = GoldFallbackPolicy.resolve(this, reason, error)

    private fun hasValidatedInternet(): Boolean {
        val manager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE)
                as ConnectivityManager
        val network = manager.activeNetwork ?: return false
        val capabilities =
            manager.getNetworkCapabilities(network) ?: return false

        return capabilities.hasCapability(
            NetworkCapabilities.NET_CAPABILITY_INTERNET
        ) && capabilities.hasCapability(
            NetworkCapabilities.NET_CAPABILITY_VALIDATED
        )
    }

    private companion object {
        const val CACHE_MAX_AGE_MILLIS = 30L * 60L * 1000L
    }
}

object GoldFallbackPolicy {
    fun resolve(
        cachedRates: List<GoldRate>,
        reason: GoldCacheReason,
        error: GoldLoadResult
    ): GoldLoadResult =
        if (cachedRates.isNotEmpty()) {
            GoldLoadResult.Cached(cachedRates, reason)
        } else {
            error
        }
}

sealed interface GoldLoadResult {
    data class Success(val rates: List<GoldRate>) : GoldLoadResult
    data class Cached(
        val rates: List<GoldRate>,
        val reason: GoldCacheReason
    ) : GoldLoadResult
    data object NoInternet : GoldLoadResult
    data object ServiceUnavailable : GoldLoadResult
    data object ParseError : GoldLoadResult
    data object InvalidData : GoldLoadResult
    data object Empty : GoldLoadResult
}

enum class GoldCacheReason {
    NO_INTERNET,
    SERVICE_UNAVAILABLE,
    PARSE_ERROR,
    INVALID_REMOTE_DATA,
    EMPTY_REMOTE_DATA
}
