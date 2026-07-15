package com.epatay.digitalwallet.ui

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.epatay.digitalwallet.R
import com.epatay.digitalwallet.databinding.FragmentDashboardBinding

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
        }

        // 3. FAB (Harcama Ekleme) Butonu
        binding.fabAddExpense.setOnClickListener {
            showAddBottomSheet()
        }

        // 4. Swipe to Delete (Silme) Özelliği
        val itemTouchHelperCallback = object : androidx.recyclerview.widget.ItemTouchHelper.SimpleCallback(
            0, androidx.recyclerview.widget.ItemTouchHelper.LEFT
        ) {
            override fun onMove(recyclerView: androidx.recyclerview.widget.RecyclerView, viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder, target: androidx.recyclerview.widget.RecyclerView.ViewHolder) = false

            override fun onSwiped(viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val silinecekHarcama = adapter.getExpenseAt(position)
                expenseViewModel.delete(silinecekHarcama)
                android.widget.Toast.makeText(requireContext(), "Harcama silindi", android.widget.Toast.LENGTH_SHORT).show()
            }

            override fun onChildDraw(
                c: android.graphics.Canvas,
                recyclerView: androidx.recyclerview.widget.RecyclerView,
                viewHolder: androidx.recyclerview.widget.RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                val itemView = viewHolder.itemView
                // Kırmızı arka plan
                val background = android.graphics.drawable.ColorDrawable(android.graphics.Color.parseColor("#F44336"))

                // Çöp kutusu ikonu
                val icon = androidx.core.content.ContextCompat.getDrawable(requireContext(), android.R.drawable.ic_menu_delete)!!
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
        androidx.recyclerview.widget.ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(binding.rvTransactions)
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


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}