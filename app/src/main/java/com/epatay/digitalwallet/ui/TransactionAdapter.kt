package com.epatay.digitalwallet.ui

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.epatay.digitalwallet.data.Transaction
import com.epatay.digitalwallet.data.TransactionType
import com.epatay.digitalwallet.databinding.ItemExpenseBinding // Kendi tasarım dosyanın adıyla değiştir

class TransactionAdapter(private var transactionList: List<Transaction>) :
    RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {

    class TransactionViewHolder(val binding: ItemExpenseBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
        val binding = ItemExpenseBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TransactionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
        val currentItem = transactionList[position]

        holder.binding.tvTitle.text = currentItem.title
        holder.binding.tvCategory.text = currentItem.category
        holder.binding.tvDate.text = currentItem.date

        // GELİR ve GİDER durumuna göre renk ve işaret ayarlıyoruz!
        if (currentItem.type == TransactionType.INCOME) {
            holder.binding.tvAmount.text = "+ ₺${"%.2f".format(currentItem.amount)}"
            holder.binding.tvAmount.setTextColor(Color.parseColor("#4CAF50")) // Yeşil renk
        } else {
            holder.binding.tvAmount.text = "- ₺${"%.2f".format(currentItem.amount)}"
            holder.binding.tvAmount.setTextColor(Color.parseColor("#F44336")) // Kırmızı renk
        }
    }

    override fun getItemCount(): Int {
        return transactionList.size
    }

    fun updateData(newList: List<Transaction>) {
        transactionList = newList
        notifyDataSetChanged()
    }
}