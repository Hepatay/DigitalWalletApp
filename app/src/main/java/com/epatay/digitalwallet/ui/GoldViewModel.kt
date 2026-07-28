package com.epatay.digitalwallet.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.epatay.digitalwallet.data.GoldCacheReason
import com.epatay.digitalwallet.data.GoldLoadResult
import com.epatay.digitalwallet.data.RoomGoldRateRepository
import com.epatay.digitalwallet.data.TransactionDatabase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GoldViewModel(application: Application) : AndroidViewModel(application) {

    private val repository =
        RoomGoldRateRepository(
            context = application,
            dao = TransactionDatabase.getDatabase(application).goldRateDao()
        )
    private var loadJob: Job? = null

    private val _uiState = MutableStateFlow<GoldUiState>(GoldUiState.Loading)
    val uiState: StateFlow<GoldUiState> = _uiState.asStateFlow()

    init {
        loadRates()
    }

    fun loadRates(forceRefresh: Boolean = false) {
        if (loadJob?.isActive == true) return

        loadJob = viewModelScope.launch {
            _uiState.value = GoldUiState.Loading
            _uiState.value =
                when (val result = repository.loadRates(forceRefresh)) {
                    is GoldLoadResult.Success ->
                        GoldUiState.Success(result.rates, false)
                    is GoldLoadResult.Cached ->
                        GoldUiState.Success(
                            result.rates,
                            true,
                            result.reason.message()
                        )
                    GoldLoadResult.NoInternet ->
                        GoldUiState.Error(
                            "İnternet bağlantısı yok ve kayıtlı altın fiyatı bulunamadı."
                        )
                    GoldLoadResult.ServiceUnavailable ->
                        GoldUiState.Error(
                            "Altın fiyatı servisine şu anda erişilemiyor."
                        )
                    GoldLoadResult.ParseError ->
                        GoldUiState.Error(
                            "Altın fiyatı yanıtı okunamadı."
                        )
                    GoldLoadResult.Empty -> GoldUiState.Empty
                }
        }
    }

    private fun GoldCacheReason.message(): String =
        when (this) {
            GoldCacheReason.NO_INTERNET ->
                "İnternet bağlantısı kurulamadı. Son kaydedilen referans fiyatlar gösteriliyor."
            GoldCacheReason.SERVICE_UNAVAILABLE ->
                "Veri sağlayıcısına erişilemedi. Son kaydedilen referans fiyatlar gösteriliyor."
            GoldCacheReason.PARSE_ERROR ->
                "Yeni veri okunamadı. Son kaydedilen referans fiyatlar gösteriliyor."
            GoldCacheReason.EMPTY_REMOTE_DATA ->
                "Yeni veri bulunamadı. Son kaydedilen referans fiyatlar gösteriliyor."
        }
}
