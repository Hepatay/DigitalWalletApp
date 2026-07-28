package com.epatay.digitalwallet.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.epatay.digitalwallet.R
import com.epatay.digitalwallet.data.CurrencyItem
import com.epatay.digitalwallet.data.CurrencyFlagProvider
import com.epatay.digitalwallet.data.CurrencyRate
import com.epatay.digitalwallet.databinding.FragmentCurrencyBinding
import kotlinx.coroutines.launch

class CurrencyFragment : Fragment(R.layout.fragment_currency) {

    private var _binding: FragmentCurrencyBinding? = null

    private val binding
        get() = _binding!!

    private lateinit var adapter: CurrencyAdapter

    private val currencyViewModel:
        CurrencyViewModel by viewModels(
            ownerProducer = {
                parentFragment ?: this
            }
        )

    private val transactionViewModel:
        TransactionViewModel by activityViewModels()

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        _binding =
            FragmentCurrencyBinding.bind(view)

        adapter =
            CurrencyAdapter(emptyList())

        binding.rvCurrencies.layoutManager =
            LinearLayoutManager(requireContext())

        binding.rvCurrencies.adapter =
            adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {
                currencyViewModel.uiState.collect { state ->
                    renderState(state)
                }
            }
        }
    }

    private fun renderState(
        state: CurrencyUiState
    ) {
        when (state) {
            CurrencyUiState.Loading -> {
                binding.progressBar.visibility =
                    View.VISIBLE
                binding.tvError.visibility =
                    View.GONE
            }

            is CurrencyUiState.Success -> {
                binding.progressBar.visibility =
                    View.GONE
                showRates(
                    rates = state.rates,
                    lastUpdatedText =
                        state.lastUpdatedText,
                    message = null
                )
            }

            is CurrencyUiState.ShowingCachedData -> {
                binding.progressBar.visibility =
                    View.GONE
                showRates(
                    rates = state.rates,
                    lastUpdatedText =
                        state.lastUpdatedText,
                    message = state.message
                )
            }

            is CurrencyUiState.NoInternet -> {
                showError(state.message)
            }

            is CurrencyUiState.ServiceUnavailable -> {
                showError(state.message)
            }

            is CurrencyUiState.XmlParseError -> {
                showError(state.message)
            }

            is CurrencyUiState.Empty -> {
                showError(state.message)
            }
        }
    }

    private fun showRates(
        rates: List<CurrencyRate>,
        lastUpdatedText: String,
        message: String?
    ) {
        adapter.updateData(
            rates.map { rate ->
                CurrencyItem(
                    code = rate.currencyCode,
                    name = rate.name,
                    unit = rate.unit,
                    forexBuying = rate.forexBuying,
                    forexSelling = rate.forexSelling,
                    flagResId =
                        CurrencyFlagProvider.getFlagResId(
                            rate.currencyCode
                        )
                )
            }
        )

        updateTransactionRates(rates)
        updateLastUpdatedText(lastUpdatedText)

        binding.rvCurrencies.visibility =
            View.VISIBLE

        if (message.isNullOrBlank()) {
            binding.tvError.visibility =
                View.GONE
        } else {
            binding.tvError.text =
                message
            binding.tvError.visibility =
                View.VISIBLE
        }
    }

    private fun showError(
        message: String
    ) {
        binding.progressBar.visibility =
            View.GONE
        binding.rvCurrencies.visibility =
            View.GONE
        binding.tvLastUpdated.visibility =
            View.GONE
        binding.tvError.text =
            message
        binding.tvError.visibility =
            View.VISIBLE
    }

    private fun updateLastUpdatedText(
        lastUpdatedText: String
    ) {
        if (lastUpdatedText.isBlank()) {
            binding.tvLastUpdated.visibility =
                View.GONE
            return
        }

        binding.tvLastUpdated.text =
            getString(
                R.string.last_updated,
                lastUpdatedText
            )
        binding.tvLastUpdated.visibility =
            View.VISIBLE
    }

    private fun updateTransactionRates(
        rates: List<CurrencyRate>
    ) {
        rates.unitRate("USD")?.let { rate ->
            transactionViewModel.dolarKuru.value =
                rate
        }

        rates.unitRate("EUR")?.let { rate ->
            transactionViewModel.euroKuru.value =
                rate
        }

        rates.unitRate("GBP")?.let { rate ->
            transactionViewModel.sterlinKuru.value =
                rate
        }
    }

    private fun List<CurrencyRate>.unitRate(
        currencyCode: String
    ): Double? {
        val rate =
            firstOrNull {
                it.currencyCode == currencyCode
            } ?: return null

        val value =
            rate.forexSelling
                ?: rate.forexBuying
                ?: return null

        return (value / rate.unit)
            .takeIf {
                it.isFinite() &&
                    it > 0.0
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
