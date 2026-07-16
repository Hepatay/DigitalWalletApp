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
import com.epatay.digitalwallet.data.RetrofitInstance
import com.epatay.digitalwallet.databinding.FragmentCurrencyBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CurrencyFragment : Fragment(R.layout.fragment_currency) {

    private var _binding: FragmentCurrencyBinding? = null
    private val binding get() = _binding!!
    private lateinit var currencyManager: CurrencyManager
    private lateinit var adapter: CurrencyAdapter
    private val expenseViewModel: ExpenseViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentCurrencyBinding.bind(view)
        currencyManager = CurrencyManager(requireContext())

        adapter = CurrencyAdapter(emptyList())
        binding.rvCurrencies.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCurrencies.adapter = adapter

        // İlk açılışta eski veriyi yükle
        loadData(currencyManager.getSavedRates())

        // Uygulama açıldığında API'den güncel veriyi çek
        if (isInternetAvailable()) {
            fetchDataFromApi()
        }

        binding.btnUpdate.setOnClickListener {
            if (isInternetAvailable()) {
                fetchDataFromApi()
            } else {
                showError("İnternet bağlantısı yok.")
            }
        }

        binding.etAmount.addTextChangedListener { text ->
            val amount = text.toString().toDoubleOrNull() ?: 1.0
            adapter.updateMultiplier(amount)
        }
    }

    private fun fetchDataFromApi() {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnUpdate.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) { RetrofitInstance.api.getLatestRates() }

                currencyManager.saveRates(response)
                loadData(response)
                Toast.makeText(requireContext(), "Güncellendi", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e("KUR_DEBUG", "API Hatası: ${e.message}")
                showError("Sunucu hatası: ${e.message}")
            } finally {
                binding.progressBar.visibility = View.GONE
                binding.btnUpdate.isEnabled = true
            }
        }
    }

    private fun loadData(response: ExchangeRateResponse?) {
        if (response == null || response.conversion_rates.isNullOrEmpty()) {
            binding.rvCurrencies.visibility = View.GONE
            return
        }

        val rates = response.conversion_rates

        // API'den USD bazlı geldiği için 1/kur yaparak TRY bazlına çeviriyoruz
        val usdRate = 1.0 / (rates["USD"] ?: 1.0)
        val eurRate = 1.0 / (rates["EUR"] ?: 1.0)
        val gbpRate = 1.0 / (rates["GBP"] ?: 1.0)
        val jpyRate = 1.0 / (rates["JPY"] ?: 1.0)
        val chfRate = 1.0 / (rates["CHF"] ?: 1.0)

        binding.tvError.visibility = View.GONE
        binding.rvCurrencies.visibility = View.VISIBLE

        expenseViewModel.dolarKuru.value = usdRate
        expenseViewModel.euroKuru.value = eurRate
        expenseViewModel.sterlinKuru.value = gbpRate

        val currencyList = listOf(
            CurrencyItem("USD", "ABD Doları", usdRate, R.drawable.flag_usd),
            CurrencyItem("EUR", "Euro", eurRate, R.drawable.flag_eur),
            CurrencyItem("GBP", "İngiliz Sterlini", gbpRate, R.drawable.flag_gbp),
            CurrencyItem("JPY", "Japon Yeni", jpyRate, R.drawable.flag_jpy),
            CurrencyItem("CHF", "İsviçre Frangı", chfRate, R.drawable.flag_chf)
        )
        adapter.updateData(currencyList)
    }

    private fun showError(message: String) {
        binding.tvError.text = message
        binding.tvError.visibility = View.VISIBLE
        if (currencyManager.getSavedRates() == null) binding.rvCurrencies.visibility = View.GONE
    }

    private fun isInternetAvailable(): Boolean {
        val cm = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val n = cm.activeNetwork ?: return false
        val cap = cm.getNetworkCapabilities(n) ?: return false
        return cap.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}