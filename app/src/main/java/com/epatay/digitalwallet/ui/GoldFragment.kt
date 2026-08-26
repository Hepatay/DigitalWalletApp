package com.epatay.digitalwallet.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.epatay.digitalwallet.R
import com.epatay.digitalwallet.databinding.FragmentGoldBinding
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GoldFragment : Fragment(R.layout.fragment_gold) {

    private var _binding: FragmentGoldBinding? = null
    private val binding get() = _binding!!
    private val viewModel: GoldViewModel by viewModels()
    private val adapter = GoldAdapter()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentGoldBinding.bind(view)
        binding.rvGoldRates.layoutManager = LinearLayoutManager(requireContext())
        binding.rvGoldRates.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect(::render)
            }
        }
    }

    private fun render(state: GoldUiState) {
        binding.progressGold.visibility =
            if (state is GoldUiState.Loading) View.VISIBLE else View.GONE

        when (state) {
            GoldUiState.Loading -> {
                binding.tvGoldMessage.visibility = View.GONE
                binding.rvGoldRates.visibility = View.GONE
            }
            is GoldUiState.Success -> {
                adapter.submitList(state.rates)
                binding.rvGoldRates.visibility = View.VISIBLE
                binding.tvGoldMessage.text = state.message.orEmpty()
                binding.tvGoldMessage.visibility =
                    if (state.message.isNullOrBlank()) View.GONE else View.VISIBLE
                
                val firstRate = state.rates.firstOrNull()
                if (firstRate != null) {
                    binding.tvSourceTime.text = "Kaynak Veri Zamanı: " + (firstRate.sourceDate ?: "")
                    binding.tvSourceTime.visibility = View.VISIBLE
                    
                    val sdf = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                    val fetchTimeStr = sdf.format(Date(firstRate.fetchedAt))
                    binding.tvAppFetchTime.text = "Uygulamanın Çektiği Zaman: " + fetchTimeStr
                    binding.tvAppFetchTime.visibility = View.VISIBLE
                } else {
                    binding.tvSourceTime.visibility = View.GONE
                    binding.tvAppFetchTime.visibility = View.GONE
                }
            }
            is GoldUiState.Error -> showEmpty(state.message)
            GoldUiState.Empty -> showEmpty("Kullanılabilir altın verisi bulunamadı.")
        }
    }

    private fun showEmpty(message: String) {
        binding.rvGoldRates.visibility = View.GONE
        binding.tvGoldMessage.text = message
        binding.tvGoldMessage.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        binding.rvGoldRates.adapter = null
        _binding = null
        super.onDestroyView()
    }
}
