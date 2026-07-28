package com.epatay.digitalwallet.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.epatay.digitalwallet.data.CurrencyCacheReason
import com.epatay.digitalwallet.data.CurrencyLoadResult
import com.epatay.digitalwallet.data.CurrencyRate
import com.epatay.digitalwallet.data.CurrencyRateRepository
import com.epatay.digitalwallet.data.TransactionDatabase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CurrencyViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository: CurrencyRateRepository
    private var loadJob: Job? = null

    private val _uiState =
        MutableStateFlow<CurrencyUiState>(
            CurrencyUiState.Loading
        )

    val uiState: StateFlow<CurrencyUiState> =
        _uiState.asStateFlow()

    init {
        val database =
            TransactionDatabase.getDatabase(application)

        repository =
            CurrencyRateRepository(
                context = application,
                dao = database.currencyRateDao()
            )

        loadRates()
    }

    fun loadRates() {
        if (loadJob?.isActive == true) {
            return
        }

        loadJob =
            viewModelScope.launch {
                _uiState.value =
                    CurrencyUiState.Loading

                _uiState.value =
                    repository.loadRates()
                        .toUiState()
            }
    }

    private fun CurrencyLoadResult.toUiState():
        CurrencyUiState {
        return when (this) {
            is CurrencyLoadResult.Success -> {
                CurrencyUiState.Success(
                    rates = rates,
                    lastUpdatedText = rates.lastUpdatedText()
                )
            }

            is CurrencyLoadResult.Cached -> {
                CurrencyUiState.ShowingCachedData(
                    rates = rates,
                    lastUpdatedText = rates.lastUpdatedText(),
                    message = reason.cachedMessage()
                )
            }

            CurrencyLoadResult.NoInternet -> {
                CurrencyUiState.NoInternet(
                    "Internet baglantisi yok. Kullanilabilir yerel veri bulunamadi."
                )
            }

            CurrencyLoadResult.ServiceUnavailable -> {
                CurrencyUiState.ServiceUnavailable(
                    "TCMB kur verilerine erişilemiyor. Lütfen daha sonra tekrar deneyin."
                )
            }

            CurrencyLoadResult.XmlParseError -> {
                CurrencyUiState.XmlParseError(
                    "TCMB kur verileri işlenemedi."
                )
            }

            CurrencyLoadResult.Empty -> {
                CurrencyUiState.Empty(
                    "Kullanilabilir kur verisi bulunamadi."
                )
            }
        }
    }

    private fun CurrencyCacheReason.cachedMessage(): String {
        return when (this) {
            CurrencyCacheReason.NO_INTERNET ->
                "Internet baglantisi yok. Son kaydedilen veriler gosteriliyor."

            CurrencyCacheReason.SERVICE_UNAVAILABLE ->
                "TCMB kur verilerine erişilemiyor. Son kaydedilen veriler gösteriliyor."

            CurrencyCacheReason.XML_PARSE_ERROR ->
                "TCMB kur verileri işlenemedi. Son kaydedilen veriler gösteriliyor."

            CurrencyCacheReason.EMPTY_REMOTE_DATA ->
                "TCMB kur verileri alınamadı. Son kaydedilen veriler gösteriliyor."
        }
    }

    private fun List<CurrencyRate>.lastUpdatedText(): String {
        return firstOrNull()
            ?.updateDateTime
            .orEmpty()
    }
}
