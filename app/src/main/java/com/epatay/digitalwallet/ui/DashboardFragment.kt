package com.epatay.digitalwallet.ui

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.epatay.digitalwallet.R
import com.epatay.digitalwallet.databinding.FragmentDashboardBinding
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.utils.ColorTemplate

class DashboardFragment : Fragment(R.layout.fragment_dashboard) {

    private var _binding: FragmentDashboardBinding? = null
    private val binding get() = _binding!!
    private val expenseViewModel: ExpenseViewModel by activityViewModels() // SharedViewModel

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentDashboardBinding.bind(view)

        // 1. Adapter'ı başlat
        val adapter = ExpenseAdapter()
        adapter.onItemClick = { expense ->
            showEditBottomSheet(expense)
        }
            binding.rvTransactions.adapter = adapter
        binding.rvTransactions.layoutManager = LinearLayoutManager(requireContext())

        // 2. ViewModel Gözlemi
        expenseViewModel.allExpenses.observe(viewLifecycleOwner) { expenses ->
            adapter.setData(expenses)
            val toplamTutar = expenses.sumOf { it.amount }
            binding.tvTotalBalance.text = "₺$toplamTutar"

            updatePieChart(expenses)
        }

        // 3. FAB (Harcama Ekleme) Butonu
        binding.fabAddExpense.setOnClickListener {
            showAddBottomSheet()
        }

        // 4. Swipe to Delete (Silme) Özelliği
        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(
            0, ItemTouchHelper.LEFT
        ) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder) = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val silinecekHarcama = adapter.getExpenseAt(position)
                expenseViewModel.delete(silinecekHarcama)
                Toast.makeText(requireContext(), "Harcama silindi", Toast.LENGTH_SHORT).show()
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                val itemView = viewHolder.itemView
                // Kırmızı arka plan
                val background = ColorDrawable(Color.parseColor("#F44336"))

                // Çöp kutusu ikonu
                val icon = ContextCompat.getDrawable(requireContext(), android.R.drawable.ic_menu_delete)!!
                val iconMargin = (itemView.height - icon.intrinsicHeight) / 2
                val iconTop = itemView.top + iconMargin
                val iconBottom = iconTop + icon.intrinsicHeight

                // Sola kaydırdığımızda (dX < 0)
                if (dX < 0) {
                    background.setBounds(itemView.right + dX.toInt(), itemView.top, itemView.right, itemView.bottom)
                    val iconRight = itemView.right - iconMargin
                    val iconLeft = iconRight - icon.intrinsicWidth
                    icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                    background.draw(c)
                    icon.draw(c)
                }

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        }
        ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(binding.rvTransactions)
    }
    // Harcama Ekleme Penceresi
    private fun showAddBottomSheet() {
        val bottomSheetDialog = com.google.android.material.bottomsheet.BottomSheetDialog(requireContext())
        val dialogBinding = com.epatay.digitalwallet.databinding.BottomSheetAddExpenseBinding.inflate(layoutInflater)
        bottomSheetDialog.setContentView(dialogBinding.root)

        val kategoriler = arrayOf("Gıda", "Ulaşım", "Fatura", "Eğitim", "Eğlence", "Diğer")
        dialogBinding.etCategory.setAdapter(android.widget.ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, kategoriler))

        dialogBinding.btnSaveExpense.setOnClickListener {
            val title = dialogBinding.etExpenseTitle.text.toString().trim()
            val amountStr = dialogBinding.etExpenseAmount.text.toString().trim()
            val category = dialogBinding.etCategory.text.toString().trim()

            if (title.isNotEmpty() && amountStr.isNotEmpty()) {
                val gercekTarih = java.text.SimpleDateFormat("dd.MM.yyyy HH:mm", java.util.Locale.getDefault()).format(java.util.Date())
                expenseViewModel.insert(com.epatay.digitalwallet.data.Expense(title = title, amount = amountStr.toDouble(), category = category, date = gercekTarih))
                bottomSheetDialog.dismiss()
            }
        }
        bottomSheetDialog.show()
    }

    // Düzenleme Penceresi
    private fun showEditBottomSheet(expense: com.epatay.digitalwallet.data.Expense) {
        val bottomSheetDialog = com.google.android.material.bottomsheet.BottomSheetDialog(requireContext())
        val dialogBinding = com.epatay.digitalwallet.databinding.BottomSheetAddExpenseBinding.inflate(layoutInflater)
        bottomSheetDialog.setContentView(dialogBinding.root)

        dialogBinding.etExpenseTitle.setText(expense.title)
        dialogBinding.etExpenseAmount.setText(expense.amount.toString())
        dialogBinding.etCategory.setText(expense.category, false)
        dialogBinding.btnSaveExpense.text = "GÜNCELLE"

        dialogBinding.btnSaveExpense.setOnClickListener {
            val guncelHarcama = expense.copy(
                title = dialogBinding.etExpenseTitle.text.toString(),
                amount = dialogBinding.etExpenseAmount.text.toString().toDouble(),
                category = dialogBinding.etCategory.text.toString()
            )
            expenseViewModel.update(guncelHarcama)
            bottomSheetDialog.dismiss()
        }
        bottomSheetDialog.show()
    }

    // Pasta Grafiği Güncelleme Fonksiyonu
    private fun updatePieChart(expenses: List<com.epatay.digitalwallet.data.Expense>) {
        if (expenses.isEmpty()) {
            binding.pieChart.clear()
            return
        }

        // Harcamaları kategorilerine göre grupla ve topla
        val categorySums = expenses.groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }

        // Grafik için veri listesini oluştur
        val entries = ArrayList<PieEntry>()
        for ((category, sum) in categorySums) {
            // Eğer kategori boşsa "Diğer" yazsın
            val kategoriAdi = if (category.isNullOrEmpty()) "Diğer" else category
            entries.add(PieEntry(sum.toFloat(), kategoriAdi))
        }

        // Veri setini ve renkleri ayarla
        val dataSet = PieDataSet(entries, "")
        dataSet.colors = ColorTemplate.MATERIAL_COLORS.toList() + ColorTemplate.PASTEL_COLORS.toList()
        dataSet.valueTextSize = 14f
        dataSet.valueTextColor = Color.WHITE

        // Grafiği ekrana bas
        val data = PieData(dataSet)
        binding.pieChart.data = data
        binding.pieChart.description.isEnabled = false // Sağ alttaki gereksiz yazıyı gizle
        binding.pieChart.centerText = "Dağılım"
        binding.pieChart.setCenterTextSize(16f)
        binding.pieChart.animateY(800) // 0.8 saniyelik şık bir açılış animasyonu
        binding.pieChart.invalidate() // Grafiği yenile
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}