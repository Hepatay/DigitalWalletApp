package com.epatay.digitalwallet.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.epatay.digitalwallet.data.Expense
import com.epatay.digitalwallet.databinding.ItemExpenseBinding

// 1. DÜZELTME: <ExpenseAdapter.ExpenseViewHolder> eklendi
class ExpenseAdapter : RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder>() {

    // 2. DÜZELTME: <Expense> eklendi
    private var expenseList = emptyList<Expense>()

    class ExpenseViewHolder(val binding: ItemExpenseBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpenseViewHolder {
        val binding = ItemExpenseBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ExpenseViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ExpenseViewHolder, position: Int) {
        val currentItem = expenseList[position]
        holder.binding.tvItemTitle.text = currentItem.title
        holder.binding.tvItemDate.text = currentItem.date
        holder.binding.tvItemAmount.text = "₺${currentItem.amount}"
    }

    override fun getItemCount(): Int {
        return expenseList.size
    }

    // 3. DÜZELTME: <Expense> eklendi
    fun setData(expenses: List<Expense>) {
        this.expenseList = expenses
        notifyDataSetChanged()
    }
}