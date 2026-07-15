package com.epatay.digitalwallet.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.epatay.digitalwallet.R
import com.epatay.digitalwallet.databinding.FragmentAnalysisBinding
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.utils.ColorTemplate

class   AnalysisFragment : Fragment(R.layout.fragment_analysis) {

    private var _binding: FragmentAnalysisBinding? = null
    private val binding get() = _binding!!

    // SharedViewModel kullanımı (Dashboard ve Analysis aynı veriyi paylaşır)
    private val expenseViewModel: ExpenseViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentAnalysisBinding.bind(view)

        // Verileri dinle ve grafiği güncelle
        expenseViewModel.allExpenses.observe(viewLifecycleOwner) { expenses ->
            if (expenses.isNullOrEmpty()) {
                binding.pieChart.clear()
                binding.pieChart.setNoDataText("Henüz veri girişi yapılmadı.")
                return@observe
            }

            // Kategorilere göre grupla ve tutarları topla
            val categoryTotals = expenses.groupBy { it.category }
                .mapValues { entry -> entry.value.sumOf { it.amount } }

            // Verileri grafik formatına (PieEntry) dönüştür
            val entries = categoryTotals.map { PieEntry(it.value.toFloat(), it.key) }

            // Grafik tasarımı
            val dataSet = PieDataSet(entries, "Harcama Dağılımı")
            dataSet.colors = ColorTemplate.MATERIAL_COLORS.toList()
            dataSet.valueTextSize = 14f
            dataSet.valueTextColor = android.graphics.Color.WHITE

            val data = PieData(dataSet)
            binding.pieChart.data = data
            binding.pieChart.description.isEnabled = false

            // Şık bir giriş animasyonu
            binding.pieChart.animateY(1000)
            binding.pieChart.invalidate() // Grafiği çiz
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Bellek sızıntısını önlemek için
    }
}