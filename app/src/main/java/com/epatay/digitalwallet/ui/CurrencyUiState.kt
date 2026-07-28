package com.epatay.digitalwallet.ui

import com.epatay.digitalwallet.data.CurrencyRate

sealed class CurrencyUiState {
    data object Loading : CurrencyUiState()

    data class Success(
        val rates: List<CurrencyRate>,
        val lastUpdatedText: String
    ) : CurrencyUiState()

    data class ShowingCachedData(
        val rates: List<CurrencyRate>,
        val lastUpdatedText: String,
        val message: String
    ) : CurrencyUiState()

    data class NoInternet(
        val message: String
    ) : CurrencyUiState()

    data class ServiceUnavailable(
        val message: String
    ) : CurrencyUiState()

    data class XmlParseError(
        val message: String
    ) : CurrencyUiState()

    data class Empty(
        val message: String
    ) : CurrencyUiState()
}
