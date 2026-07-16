package com.epatay.digitalwallet.ui


import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.epatay.digitalwallet.R
import com.epatay.digitalwallet.databinding.FragmentAnalysisBinding
import com.epatay.digitalwallet.databinding.BottomSheetAddInvestmentBinding
import com.epatay.digitalwallet.data.InvestmentItem
import com.google.android.material.bottomsheet.BottomSheetDialog
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AnalysisFragment : Fragment(R.layout.fragment_analysis) {

    private var _binding: FragmentAnalysisBinding? = null
    private val binding get() = _binding!!

    // Hem yatırımlar hem de kurlar için ViewModel'leri bağladık
    private val investmentViewModel: InvestmentViewModel by activityViewModels()
    private val expenseViewModel: ExpenseViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentAnalysisBinding.bind(view)

        // 1. Adapter'ı başlat
        val adapter = InvestmentAdapter()
        binding.rvInvestments.adapter = adapter
        binding.rvInvestments.layoutManager = LinearLayoutManager(requireContext())

        adapter.setOnItemLongClickListener { investment ->
            investmentViewModel.delete(investment)
            Toast.makeText(requireContext(), "${investment.assetName} kaydı silindi", Toast.LENGTH_SHORT).show()
        }

        // 2. Verileri gözlemle
        investmentViewModel.allInvestments.observe(viewLifecycleOwner) { investments ->
            adapter.setData(investments)
        }

        // 3. Ekleme butonu
        binding.fabAddInvestment.setOnClickListener {
            showAddInvestmentBottomSheet()
        }
    }

    private fun showAddInvestmentBottomSheet() {
        val bottomSheetDialog = BottomSheetDialog(requireContext())
        val dialogBinding = BottomSheetAddInvestmentBinding.inflate(layoutInflater)
        bottomSheetDialog.setContentView(dialogBinding.root)

        val varliklar = arrayOf("USD", "EUR", "GBP", "Altın")
        dialogBinding.etAssetType.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, varliklar)
        )

        dialogBinding.btnSaveInvestment.setOnClickListener {
            val secilenVarlik = dialogBinding.etAssetType.text.toString().trim()
            val miktarStr = dialogBinding.etInvestmentAmount.text.toString().trim()

            if (secilenVarlik.isNotEmpty() && miktarStr.isNotEmpty()) {
                val miktar = miktarStr.toDouble()

                // Kur verilerini ViewModel'den çekiyoruz
                val anlikKur = when {
                    secilenVarlik.contains("USD") -> expenseViewModel.dolarKuru.value ?: 0.0
                    secilenVarlik.contains("EUR") -> expenseViewModel.euroKuru.value ?: 0.0
                    secilenVarlik.contains("GBP") -> expenseViewModel.sterlinKuru.value ?: 0.0
                    else -> 1.0
                }

                // 1.0 ve altı hatalı veri demektir
                if (anlikKur <= 1.0) {
                    Toast.makeText(requireContext(), "Kur verisi alınamadı, ana sayfadan güncelleyin!", Toast.LENGTH_SHORT).show()
                } else {
                    val gercekTarih = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date())

                    val yeniYatirim = InvestmentItem(
                        assetName = secilenVarlik,
                        amount = miktar,
                        buyPrice = anlikKur,
                        buyDate = gercekTarih
                    )

                    investmentViewModel.insert(yeniYatirim)
                    Toast.makeText(requireContext(), "Yatırım eklendi (Kur: ₺$anlikKur)", Toast.LENGTH_SHORT).show()
                    bottomSheetDialog.dismiss()
                }
            } else {
                Toast.makeText(requireContext(), "Lütfen tüm alanları doldurun!", Toast.LENGTH_SHORT).show()
            }
        }
        bottomSheetDialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}