package com.epatay.digitalwallet.ui

import com.epatay.digitalwallet.data.GoldRate

sealed interface GoldUiState {
    data object Loading : GoldUiState

    data class Success(
        val rates: List<GoldRate>,
        val isOfflineData: Boolean,
        val message: String? = null
    ) : GoldUiState

    data class Error(val message: String) : GoldUiState
    data object Empty : GoldUiState
}
