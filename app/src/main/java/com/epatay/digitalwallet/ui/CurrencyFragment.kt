package com.epatay.digitalwallet.ui

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.epatay.digitalwallet.R
import com.epatay.digitalwallet.data.CurrencyItem
import com.epatay.digitalwallet.data.CurrencyManager
import com.epatay.digitalwallet.data.ExchangeRateResponse
import com.epatay.digitalwallet.data.GoldRetrofitInstance
import com.epatay.digitalwallet.data.RetrofitInstance
import com.epatay.digitalwallet.databinding.FragmentCurrencyBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CurrencyFragment : Fragment(R.layout.fragment_currency) {

    companion object {
        private const val TROY_OUNCE_GRAMS = 31.1034768
    }

    private var _binding: FragmentCurrencyBinding? = null

    private val binding
        get() = _binding!!

    private lateinit var currencyManager: CurrencyManager
    private lateinit var adapter: CurrencyAdapter

    private val transactionViewModel:
            TransactionViewModel by activityViewModels()

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentCurrencyBinding.bind(view)

        currencyManager = CurrencyManager(requireContext())

        adapter = CurrencyAdapter(emptyList())

        binding.rvCurrencies.layoutManager =
            LinearLayoutManager(requireContext())

        binding.rvCurrencies.adapter = adapter

        /*
         * Önceden kaydedilen döviz ve altın
         * verilerini hemen ekranda gösterir.
         */
        val savedRates =
            currencyManager.getSavedRates()

        loadData(savedRates)

        /*
         * Wi-Fi veya mobil ağın bulunması yeterli değildir.
         * Ağın gerçekten internete çıkabildiği de kontrol edilir.
         */
        if (isInternetAvailable()) {
            fetchDataFromApi()
        } else {
            showError(getOfflineMessage())
        }

        binding.btnUpdate.setOnClickListener {

            if (isInternetAvailable()) {
                fetchDataFromApi()
            } else {
                showError(getOfflineMessage())
            }
        }

        binding.etAmount.addTextChangedListener { text ->

            val amount =
                text
                    ?.toString()
                    ?.replace(",", ".")
                    ?.toDoubleOrNull()
                    ?: 1.0

            adapter.updateMultiplier(amount)
        }
    }

    private fun fetchDataFromApi() {

        binding.progressBar.visibility = View.VISIBLE
        binding.btnUpdate.isEnabled = false
        binding.tvError.visibility = View.GONE

        viewLifecycleOwner.lifecycleScope.launch {

            try {

                /*
                 * Önce döviz kurları çekilir.
                 */
                val exchangeResponse =
                    withContext(Dispatchers.IO) {

                        RetrofitInstance.api
                            .getLatestRates()
                    }

                currencyManager.saveRates(exchangeResponse)

                var goldUpdated = false

                try {

                    /*
                     * Altın fiyatı USD/ons olarak çekilir.
                     */
                    val goldResponse =
                        withContext(Dispatchers.IO) {

                            GoldRetrofitInstance.api
                                .getGoldPrice()
                        }

                    val usdTryRate =
                        getSafeRate(
                            exchangeResponse.conversion_rates,
                            "USD"
                        )

                    /*
                     * Gold API:
                     * 1 ons altının USD fiyatını verir.
                     *
                     * Gram altın TL:
                     * Altın ons/USD × USD/TL ÷ 31.1034768
                     */
                    val gramGoldTry =
                        (
                                goldResponse.price *
                                        usdTryRate
                                ) / TROY_OUNCE_GRAMS

                    if (
                        gramGoldTry.isFinite() &&
                        gramGoldTry > 0.0
                    ) {

                        currencyManager.saveGramGoldPrice(
                            gramGoldTry
                        )

                        goldUpdated = true
                    }

                } catch (goldException: Exception) {

                    /*
                     * Altın API çalışmazsa dövizlerin
                     * güncellenmesini engellemiyoruz.
                     */
                    Log.e(
                        "ALTIN_DEBUG",
                        "Altın API hatası",
                        goldException
                    )
                }

                loadData(exchangeResponse)

                val message =
                    if (goldUpdated) {
                        "Döviz ve altın güncellendi"
                    } else {
                        "Döviz güncellendi, altın alınamadı"
                    }

                Toast.makeText(
                    requireContext(),
                    message,
                    Toast.LENGTH_SHORT
                ).show()

            } catch (exception: Exception) {

                Log.e(
                    "KUR_DEBUG",
                    "Döviz API hatası",
                    exception
                )

                /*
                 * Kullanıcıya API adresi ve teknik hata
                 * ayrıntısı gösterilmez.
                 */
                val errorMessage =
                    when {

                        !isInternetAvailable() -> {
                            getOfflineMessage()
                        }

                        currencyManager.getSavedRates() != null -> {
                            "Sunucuya ulaşılamadı. " +
                                    "Son kaydedilen veriler gösteriliyor."
                        }

                        else -> {
                            "Veriler alınamadı. " +
                                    "Lütfen daha sonra tekrar deneyin."
                        }
                    }

                showError(errorMessage)

            } finally {

                binding.progressBar.visibility = View.GONE
                binding.btnUpdate.isEnabled = true
            }
        }
    }

    private fun getSafeRate(
        rates: Map<String, Double>,
        currencyCode: String
    ): Double {

        val rawRate =
            rates[currencyCode]
                ?: return 0.0

        return if (
            rawRate.isFinite() &&
            rawRate > 0.0
        ) {
            1.0 / rawRate
        } else {
            0.0
        }
    }

    private fun loadData(
        response: ExchangeRateResponse?
    ) {

        if (
            response == null ||
            response.conversion_rates.isEmpty()
        ) {
            binding.rvCurrencies.visibility = View.GONE
            return
        }

        val rates =
            response.conversion_rates

        binding.tvError.visibility = View.GONE
        binding.rvCurrencies.visibility = View.VISIBLE

        val usdRate =
            getSafeRate(rates, "USD")

        val eurRate =
            getSafeRate(rates, "EUR")

        val gbpRate =
            getSafeRate(rates, "GBP")

        transactionViewModel.dolarKuru.value =
            usdRate

        transactionViewModel.euroKuru.value =
            eurRate

        transactionViewModel.sterlinKuru.value =
            gbpRate

        val currencyList =
            mutableListOf<CurrencyItem>()

        val gramGoldPrice =
            currencyManager.getSavedGramGoldPrice()

        if (
            gramGoldPrice != null &&
            gramGoldPrice.isFinite() &&
            gramGoldPrice > 0.0
        ) {
            currencyList.add(
                CurrencyItem(
                    "GRAM ALTIN",
                    "Gram Altın",
                    gramGoldPrice,
                    R.drawable.ic_gold
                )
            )
        }

        currencyList.addAll(
            listOf(
                CurrencyItem(
                    "USD",
                    "ABD Doları",
                    usdRate,
                    R.drawable.flag_usd
                ),
                CurrencyItem(
                    "EUR",
                    "Euro",
                    eurRate,
                    R.drawable.flag_eur
                ),
                CurrencyItem(
                    "GBP",
                    "İngiliz Sterlini",
                    gbpRate,
                    R.drawable.flag_gbp
                ),
                CurrencyItem(
                    "CHF",
                    "İsviçre Frangı",
                    getSafeRate(rates, "CHF"),
                    R.drawable.flag_chf
                ),
                CurrencyItem(
                    "JPY",
                    "Japon Yeni",
                    getSafeRate(rates, "JPY"),
                    R.drawable.flag_jpy
                ),
                CurrencyItem(
                    "CAD",
                    "Kanada Doları",
                    getSafeRate(rates, "CAD"),
                    R.drawable.flag_cad
                ),
                CurrencyItem(
                    "AUD",
                    "Avustralya Doları",
                    getSafeRate(rates, "AUD"),
                    R.drawable.flag_aud
                ),
                CurrencyItem(
                    "RUB",
                    "Rus Rublesi",
                    getSafeRate(rates, "RUB"),
                    R.drawable.flag_rub
                ),
                CurrencyItem(
                    "CNY",
                    "Çin Yuanı",
                    getSafeRate(rates, "CNY"),
                    R.drawable.flag_cny
                )
            )
        )

        adapter.updateData(currencyList)
    }

    private fun getOfflineMessage(): String {

        return if (
            currencyManager.getSavedRates() != null
        ) {
            "İnternet bağlantısı yok. " +
                    "Son kaydedilen veriler gösteriliyor."
        } else {
            "İnternet bağlantısı yok. " +
                    "Lütfen bağlantınızı kontrol edin."
        }
    }

    private fun showError(message: String) {

        binding.tvError.text = message
        binding.tvError.visibility = View.VISIBLE

        /*
         * Kayıtlı kur varsa liste ekranda kalır.
         * Hiç kayıt yoksa liste gizlenir.
         */
        if (
            currencyManager.getSavedRates() == null
        ) {
            binding.rvCurrencies.visibility = View.GONE
        } else {
            binding.rvCurrencies.visibility = View.VISIBLE
        }
    }

    private fun isInternetAvailable(): Boolean {

        val connectivityManager =
            requireContext().getSystemService(
                Context.CONNECTIVITY_SERVICE
            ) as ConnectivityManager

        val activeNetwork =
            connectivityManager.activeNetwork
                ?: return false

        val capabilities =
            connectivityManager.getNetworkCapabilities(
                activeNetwork
            ) ?: return false

        val hasInternetCapability =
            capabilities.hasCapability(
                NetworkCapabilities.NET_CAPABILITY_INTERNET
            )

        val isInternetValidated =
            capabilities.hasCapability(
                NetworkCapabilities.NET_CAPABILITY_VALIDATED
            )

        return hasInternetCapability &&
                isInternetValidated
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}