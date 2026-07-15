package com.epatay.digitalwallet.ui

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
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
import androidx.core.widget.addTextChangedListener

class CurrencyFragment : Fragment(R.layout.fragment_currency) {

    private var _binding: FragmentCurrencyBinding? = null
    private val binding get() = _binding!!
    private lateinit var currencyManager: CurrencyManager
    private lateinit var adapter: CurrencyAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {


        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentCurrencyBinding.bind(view)
        currencyManager = CurrencyManager(requireContext())

        // RecyclerView Ayarları
        adapter = CurrencyAdapter(emptyList())
        binding.rvCurrencies.layoutManager = LinearLayoutManager(requireContext())
        binding.rvCurrencies.adapter = adapter

        // İlk açılışta eski veriyi yükle
        loadData(currencyManager.getSavedRates())

        binding.btnUpdate.setOnClickListener {
            if (isInternetAvailable()) {
                fetchDataFromApi()
            } else {
                showError("İnternet bağlantısı yok. Lütfen kontrol edin.")
            }
        }
        // Klavyeden girilen değeri anlık olarak dinliyoruz
        binding.etAmount.addTextChangedListener { text ->
            val amountText = text.toString()
            // Eğer kutu boşsa varsayılan olarak 1.0 al, doluysa girilen sayıyı al
            val amount = if (amountText.isNotEmpty()) amountText.toDoubleOrNull() ?: 1.0 else 1.0
            adapter.updateMultiplier(amount)
        }
    }

    private fun fetchDataFromApi() {
        // Yükleniyor durumunu göster
        binding.progressBar.visibility = View.VISIBLE
        binding.rvCurrencies.visibility = View.GONE
        binding.tvError.visibility = View.GONE
        binding.btnUpdate.isEnabled = false

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    RetrofitInstance.api.getLatestRates()
                }
                currencyManager.saveRates(response)
                loadData(response)
                Toast.makeText(requireContext(), "Güncellendi", Toast.LENGTH_SHORT).show()

            } catch (e: Exception) {
                showError("Sunucu hatası: ${e.message}")
            } finally {
                // Yükleme bitince ekranı eski haline getir
                binding.progressBar.visibility = View.GONE
                binding.rvCurrencies.visibility = View.VISIBLE
                binding.btnUpdate.isEnabled = true
            }
        }
    }

    private fun loadData(response: ExchangeRateResponse?) {
        if (response == null) return
        binding.tvError.visibility = View.GONE
        binding.rvCurrencies.visibility = View.VISIBLE


        val usdRate = response.conversion_rates["USD"] ?: 1.0
        val eurRate = response.conversion_rates["EUR"] ?: 1.0
        val gbpRate = response.conversion_rates["GBP"] ?: 1.0
        val jpyRate = response.conversion_rates["JPY"] ?: 1.0
        val chfRate = response.conversion_rates["CHF"] ?: 1.0

        // Listeye dönüştür
        val currencyList = listOf(
            CurrencyItem("USD", "*ABD Doları", 1.0 / usdRate,R.drawable.flag_usd),
            CurrencyItem("EUR", "*Euro", 1.0 / eurRate, R.drawable.flag_eur),
            CurrencyItem("GBP", "*İngiliz Sterlini", 1.0 / gbpRate, R.drawable.flag_gbp),
            CurrencyItem("JPY", "*Japon Yeni", 1.0 / jpyRate, R.drawable.flag_jpy),
            CurrencyItem("CHF", "*İsviçre Frangı", 1.0/ chfRate, R.drawable.flag_chf)
        )

        // Adapter'a yeni veriyi gönder
        adapter.updateData(currencyList)
    }

    private fun showError(message: String) {
        binding.tvError.text = message
        binding.tvError.visibility = View.VISIBLE

        // Eğer daha önce kaydedilmiş veri yoksa listeyi gizle
        if (currencyManager.getSavedRates() == null) {
            binding.rvCurrencies.visibility = View.GONE
        }
    }

    // İnternet kontrol fonksiyonu
    private fun isInternetAvailable(): Boolean {
        val connectivityManager = requireContext().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}